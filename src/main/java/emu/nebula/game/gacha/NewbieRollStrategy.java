package emu.nebula.game.gacha;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import emu.nebula.data.resources.GachaDef;
import emu.nebula.data.resources.GachaPkgDef;
import emu.nebula.util.WeightedList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class NewbieRollStrategy {
    private static final int TEN_PULL_COUNT = 10;
    private static final int MAX_FIVE_COUNT = 1;
    private static final int MIN_FOUR_COUNT = 1;
    private static final int MAX_FOUR_COUNT_WITH_FIVE = 2;
    private static final int MAX_FOUR_COUNT_WITHOUT_FIVE = 3;
    private static final double DEFAULT_HAS_FIVE_RATE = 0.75;
    // Chance to upgrade 4-star count from 1 to multi-4 (2 or 3).
    private static final double DEFAULT_MULTI_FOUR_WHEN_FIVE_RATE = 10d / 15d;
    // In multi-4 results, chance to output 3 four-stars instead of 2.
    private static final double DEFAULT_THREE_FOUR_WHEN_MULTI_RATE = 0.35;

    /**
     * Newbie 10-pull shape profile:
     * - hasFiveRate: chance that this 10-pull contains exactly one 5-star
     * - multiFourWhenFiveRate: chance to upgrade 4-star count from 1 to multi-4
     * - threeFourWhenMultiRate: in no-5-star multi-4 results, chance of 3 four-stars (else 2)
     */
    public record Profile(double hasFiveRate, double multiFourWhenFiveRate, double threeFourWhenMultiRate) {}

    private static final Profile DEFAULT_PROFILE = new Profile(
            DEFAULT_HAS_FIVE_RATE,
            DEFAULT_MULTI_FOUR_WHEN_FIVE_RATE,
            DEFAULT_THREE_FOUR_WHEN_MULTI_RATE
    );

    public static Profile defaultProfile() {
        return DEFAULT_PROFILE;
    }

    private record PullCounts(int fiveCount, int fourCount, int threeCount) {}

    private static PullCounts resolvePullCounts(ThreadLocalRandom random, Profile profile) {
        // Rule 1: a newbie 10-pull contains at most one 5-star
        boolean hasFiveStar = random.nextDouble() < profile.hasFiveRate();
        int fiveCount = hasFiveStar ? MAX_FIVE_COUNT : 0;

        int fourCount = MIN_FOUR_COUNT;
        if (hasFiveStar) {
            // With a 5-star present, 4-star count is constrained to 1~2
            fourCount = random.nextDouble() < profile.multiFourWhenFiveRate() ? MAX_FOUR_COUNT_WITH_FIVE : MIN_FOUR_COUNT;
        } else {
            // Without a 5-star, still guarantee at least one 4-star and allow up to 3
            if (random.nextDouble() < profile.multiFourWhenFiveRate()) {
                boolean rollThreeFours = random.nextDouble() < profile.threeFourWhenMultiRate();
                fourCount = rollThreeFours ? MAX_FOUR_COUNT_WITHOUT_FIVE : 2;
            }
        }

        int threeCount = TEN_PULL_COUNT - fiveCount - fourCount;
        return new PullCounts(fiveCount, fourCount, threeCount);
    }

    private static int rollFromPackage(GachaDef.GachaPackage gachaPackage) {
        if (gachaPackage == null) {
            return 0;
        }

        var pkg = GachaPkgDef.getPackageById(gachaPackage.getId());
        if (pkg == null || pkg.size() == 0) {
            return 0;
        }

        return pkg.next();
    }

    private static boolean appendCardsFromTier(IntArrayList cards, WeightedList<GachaDef.GachaPackage> tier, int count) {
        for (int i = 0; i < count; i++) {
            int cardId = rollFromPackage(tier.next());
            if (cardId <= 0) {
                return false;
            }

            cards.add(cardId);
        }

        return true;
    }

    public static int[] rollTenPull(GachaDef bannerData, Profile profile) {
        if (bannerData == null) {
            return null;
        }

        var packageA = bannerData.getPackageA();
        var packageB = bannerData.getPackageB();
        var packageC = bannerData.getPackageC();
        if (packageA == null || packageA.size() == 0 || packageB == null || packageB.size() == 0 || packageC == null || packageC.size() == 0) {
            return null;
        }

        var random = ThreadLocalRandom.current();

        // Step 1: decide rarity counts for this newbie 10-pull
        PullCounts counts = resolvePullCounts(random, profile);

        // Step 2: draw concrete item ids from A/B/C packages
        var cards = new IntArrayList(TEN_PULL_COUNT);
        if (!appendCardsFromTier(cards, packageA, counts.fiveCount())
                || !appendCardsFromTier(cards, packageB, counts.fourCount())
                || !appendCardsFromTier(cards, packageC, counts.threeCount())
                || cards.size() != TEN_PULL_COUNT) {
            return null;
        }

        // Step 3: shuffle cards order
        Collections.shuffle(cards, random);

        return cards.toIntArray();
    }
}
