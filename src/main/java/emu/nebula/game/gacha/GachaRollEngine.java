package emu.nebula.game.gacha;

import emu.nebula.data.resources.GachaATypeProbDef;
import emu.nebula.data.resources.GachaDef;
import emu.nebula.data.resources.GachaDef.GachaPackage;
import emu.nebula.data.resources.GachaPkgDef;
import emu.nebula.game.gacha.GachaPityState.GachaPityDraft;
import emu.nebula.util.WeightedList;
import emu.nebula.util.Utils;

public final class GachaRollEngine {
    private static final int PROBABILITY_BASE = 10000;
    private static final int DEFAULT_A_TYPE_PROB = 200;
    private static final int DEFAULT_B_GUARANTEE_TIMES = 10;

    private record Rates(int rollBase, int chanceA, int chanceB, int aupGuaranteeTimes, int bGuaranteeTimes) {}

    private static Rates resolveRates(GachaDef data, int missTimesA) {
        int rollBase = Math.max(PROBABILITY_BASE, GachaATypeProbDef.getMaxProb());
        int aupGuaranteeTimes = 0;
        int chanceA = 0;
        int chanceB = 0;
        int bGuaranteeTimes = 0;

        var storageData = data.getStorageData();
        if (storageData != null) {
            aupGuaranteeTimes = storageData.getAUpGuaranteeTimes();
            chanceA = GachaATypeProbDef.getProb(storageData.getATypeGroup(), missTimesA, DEFAULT_A_TYPE_PROB);
            chanceB = storageData.getBTypeProb();
            bGuaranteeTimes = storageData.getBGuaranteeTimes();

            if (bGuaranteeTimes <= 0) {
                bGuaranteeTimes = DEFAULT_B_GUARANTEE_TIMES;
            }

            if (aupGuaranteeTimes > 0 && missTimesA >= aupGuaranteeTimes - 1) {
                chanceA = rollBase;
            }
        }

        if (chanceA >= rollBase) {
            chanceA = rollBase;
            chanceB = 0;
        }

        return new Rates(rollBase, chanceA, chanceB, aupGuaranteeTimes, bGuaranteeTimes);
    }

    private static GachaPackage choosePackage(GachaDef data, int random, int chanceA, int chanceB, boolean forceAUp) {
        if (forceAUp) {
            if (data.getATypeUpPkg() > 0) {
                return new GachaPackage(GachaDef.GachaPackageType.A_UP, data.getATypeUpPkg());
            }
            return safeNext(data.getPackageA());
        }

        if (random <= chanceA) {
            return safeNext(data.getPackageA());
        }
        if (random <= chanceB) {
            return safeNext(data.getPackageB());
        }

        return safeNext(data.getPackageC());
    }

    private static GachaPackage safeNext(WeightedList<GachaPackage> list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.next();
    }

    private static RollOutcome roll(GachaDef data, int missTimesA, int missTimesUpA, int missTimesB, boolean bGuaranteeDebt) {
        Rates rates = resolveRates(data, missTimesA);

        boolean forceAUp = rates.aupGuaranteeTimes > 0 && missTimesUpA >= rates.aupGuaranteeTimes - 1;

        int chanceA = rates.chanceA;
        int chanceB = rates.chanceB;
        boolean bGuaranteeTriggered = false;

        if (rates.bGuaranteeTimes > 0 && missTimesB >= rates.bGuaranteeTimes - 1) {
            chanceB = rates.rollBase;
            bGuaranteeTriggered = true;
        }

        int newMissTimesA = missTimesA + 1;
        int newMissTimesB = missTimesB + 1;
        int newMissTimesUpA = missTimesUpA;

        int random = Utils.randomRange(1, rates.rollBase);
        GachaPackage gachaPackage = choosePackage(data, random, chanceA, chanceB, forceAUp);

        boolean newBGuaranteeDebt = bGuaranteeDebt;

        if (forceAUp || random <= chanceA) {
            newMissTimesA = 0;
            if (bGuaranteeTriggered) {
                newBGuaranteeDebt = true;
            }
        } else if (random <= chanceB) {
            newMissTimesB = 0;
            newBGuaranteeDebt = false;
        }

        if (newBGuaranteeDebt && gachaPackage != null && gachaPackage.getType() == GachaDef.GachaPackageType.C) {
            var compensated = safeNext(data.getPackageB());
            if (compensated != null) {
                gachaPackage = compensated;
                newMissTimesB = 0;
                newBGuaranteeDebt = false;
            }
        }

        if (gachaPackage != null && gachaPackage.getType() == GachaDef.GachaPackageType.A_UP) {
            newMissTimesUpA = 0;
        } else {
            newMissTimesUpA++;
        }

        return new RollOutcome(gachaPackage, newMissTimesA, newMissTimesUpA, newMissTimesB, newBGuaranteeDebt);
    }

    public static PullOutcome pull(GachaDef data, GachaPityDraft pityDraft) {
        if (data == null || pityDraft == null) {
            return null;
        }

        var outcome = roll(data, pityDraft.missTimesA(), pityDraft.missTimesUpA(),
                pityDraft.missTimesB(), pityDraft.bGuaranteeDebt());

        var gachaPackage = outcome.gachaPackage();
        if (gachaPackage == null) {
            return null;
        }

        var pkg = GachaPkgDef.getPackageById(gachaPackage.getId());
        if (pkg == null) {
            return null;
        }

        return new PullOutcome(
                pkg.next(),
                new GachaPityDraft(
                        outcome.missTimesA(),
                        outcome.missTimesUpA(),
                        outcome.missTimesB(),
                        outcome.bGuaranteeDebt()
                )
        );
    }

    public record RollOutcome(
            GachaPackage gachaPackage,
            int missTimesA,
            int missTimesUpA,
            int missTimesB,
            boolean bGuaranteeDebt
    ) {}

    public record PullOutcome(
            int itemId,
            GachaPityDraft pityDraft
    ) {
    }
}
