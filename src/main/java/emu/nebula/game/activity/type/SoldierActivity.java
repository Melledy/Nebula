package emu.nebula.game.activity.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.annotations.Entity;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.data.resources.SoldierGradeChallengeDef;
import emu.nebula.data.resources.SoldierEventBattlePoolDef;
import emu.nebula.data.resources.SoldierNodePlanDef;
import emu.nebula.data.resources.SoldierShopPoolDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.PublicSoldier;
import emu.nebula.proto.SoldierApply.SoldierApplyResp;
import emu.nebula.proto.SoldierGiveUp.SoldierGiveUpResp;
import emu.nebula.proto.SoldierInteract.SoldierInteractReq;
import emu.nebula.proto.SoldierInteract.SoldierInteractResp;
import emu.nebula.util.Utils;
import emu.nebula.util.WeightedList;
import lombok.Getter;

@Getter
@Entity
public class SoldierActivity extends GameActivity {
    private static final int INITIAL_COIN = 5;
    private static final int INITIAL_HP = 80;
    private static final int MAX_HP = 100;
    private static final int COIN_TID = 40;
    private static final int MAX_SKILL_COIN = 300;
    private static final int MAX_CLIENT_DATA = 64 * 1024;
    private static final int CASE_SELECT = 1;
    private static final int CASE_FIGHT = 2;
    private static final int CASE_BATTLE = 3;
    private static final int CASE_EVENT = 4;
    private static final int CASE_EVENT_BATTLE = 5;
    private static final int SHOP_CASE_OFFSET = 100_000_000;
    private static final int DEPLOY_SYNC_CASE_OFFSET = 200_000_000;
    private static final int SHOP_SIZE = 5;
    private static final int SHOP_REFRESH_COST = 2;
    private static final int SHOP_EXP_COST = 4;
    private static final int INITIAL_SHOP_LEVEL = 3;
    private static final int EFFECT_ADD_RESOURCE = 1;
    private static final int EFFECT_ADD_SAN = 4;
    private static final int EFFECT_LOSE_SAN = 5;
    private static final int EFFECT_SET_SAN = 6;
    private static final int EFFECT_TWO_DICE = 25;
    private static final int EFFECT_SWAP_SAN_COIN = 30;
    private static final int[] LEGACY_INITIAL_CHESS = {2501006, 2501007, 2501009};

    private int score;
    private int gradeChallengeId;
    private int curGradeChallengeId;
    private int stage;
    private int nodeIndex;
    private int hp;
    private int coin;
    private int shopLevel;
    private int shopExp;
    private int winningStreak;
    private int losingStreak;
    private int caseId;
    private int caseType;
    private int currentFloorId;
    private int selectedCard;
    private int starterRerolls;
    private int dicePoint;
    private int eventBattleId;
    private boolean shopLocked;
    private int dataVersion;
    private String clientData;
    private Set<Integer> tracePartnerTypes;
    private Set<Integer> completedChallenges;
    private List<Integer> ownedChess;
    private List<Integer> starterChoices;
    private List<Integer> shopGoods;
    private List<Integer> masterDeploy;
    private List<Integer> assistDeploy;
    private List<Integer> waitingDeploy;
    private List<Integer> eventChoices;
    private List<Integer> strategyCards;
    private boolean opportunityTwoDice;

    @Deprecated // Morphia only
    public SoldierActivity() {
    }

    public SoldierActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
        tracePartnerTypes = new HashSet<>();
        completedChallenges = new HashSet<>();
        ownedChess = new ArrayList<>();
        starterChoices = new ArrayList<>();
        shopGoods = new ArrayList<>();
        masterDeploy = new ArrayList<>();
        assistDeploy = new ArrayList<>();
        waitingDeploy = new ArrayList<>();
        eventChoices = new ArrayList<>();
        strategyCards = new ArrayList<>();
        clientData = "";
    }

    private Set<Integer> tracePartnerTypes() {
        if (tracePartnerTypes == null) tracePartnerTypes = new HashSet<>();
        return tracePartnerTypes;
    }

    private Set<Integer> completedChallenges() {
        if (completedChallenges == null) completedChallenges = new HashSet<>();
        return completedChallenges;
    }

    private List<Integer> ownedChess() {
        if (ownedChess == null) ownedChess = new ArrayList<>();
        return ownedChess;
    }

    private List<Integer> starterChoices() {
        if (starterChoices == null) starterChoices = new ArrayList<>();
        return starterChoices;
    }

    private List<Integer> shopGoods() {
        if (shopGoods == null) shopGoods = new ArrayList<>();
        return shopGoods;
    }

    private List<Integer> masterDeploy() {
        if (masterDeploy == null) masterDeploy = new ArrayList<>();
        return masterDeploy;
    }

    private List<Integer> assistDeploy() {
        if (assistDeploy == null) assistDeploy = new ArrayList<>();
        return assistDeploy;
    }

    private List<Integer> waitingDeploy() {
        if (waitingDeploy == null) waitingDeploy = new ArrayList<>();
        return waitingDeploy;
    }

    private List<Integer> eventChoices() {
        if (eventChoices == null) eventChoices = new ArrayList<>();
        return eventChoices;
    }

    private List<Integer> strategyCards() {
        if (strategyCards == null) strategyCards = new ArrayList<>();
        return strategyCards;
    }

    public synchronized boolean inProgress() {
        return curGradeChallengeId != 0;
    }

    @Override
    public synchronized void onLogin() {
        if (inProgress() && migrateLegacyRun()) save();
    }

    public synchronized SoldierApplyResp apply(int seasonId, int challengeId) {
        if (inProgress() || seasonId != getId()) return null;

        var season = GameData.getSoldierSeasonDataTable().get(seasonId);
        var challenge = GameData.getSoldierGradeChallengeDataTable().get(challengeId);
        if (season == null || challenge == null || nodes(challenge.getNodeGroupId()).isEmpty()
                || !isChallengeUnlocked(challenge)) return null;

        var chessPool = new ArrayList<Integer>();
        for (var character : GameData.getSoldierCharacterDataTable()) {
            if (character.getGroupId() == season.getChessGroupId() && character.isBoardChess()) {
                chessPool.add(character.getId());
            }
        }
        if (chessPool.size() < 3) return null;

        curGradeChallengeId = challengeId;
        stage = nodes(challenge.getNodeGroupId()).getFirst().getStage();
        nodeIndex = 1;
        hp = INITIAL_HP;
        coin = INITIAL_COIN;
        shopLevel = INITIAL_SHOP_LEVEL;
        shopExp = 0;
        shopLocked = false;
        winningStreak = 0;
        losingStreak = 0;
        selectedCard = 0;
        starterRerolls = 1;
        currentFloorId = 0;
        dicePoint = 0;
        eventBattleId = 0;
        dataVersion = 1;
        clientData = "";
        tracePartnerTypes().clear();
        ownedChess().clear();
        Collections.shuffle(chessPool);
        ownedChess().addAll(chessPool.subList(0, 3));
        masterDeploy().clear();
        assistDeploy().clear();
        waitingDeploy().clear();
        waitingDeploy().addAll(ownedChess());
        shopGoods().clear();
        eventChoices().clear();
        strategyCards().clear();
        rollStarterChoices(season.getStarterGroupId());
        if (starterChoices().size() < 3) {
            clearRun();
            return null;
        }
        caseType = CASE_SELECT;
        nextCase();
        save();

        var rsp = SoldierApplyResp.newInstance().setCoinQty(INITIAL_COIN).setInfo(toInfoProto());
        for (int id : ownedChess()) rsp.addChess(chess(id));
        return rsp;
    }

    private boolean isChallengeUnlocked(SoldierGradeChallengeDef challenge) {
        int required = challenge.getUnlockGradeLevel();
        return required == 0 || completedChallenges().contains(required) || gradeChallengeId == required;
    }

    public synchronized PublicSoldier.SoldierInfo info() {
        if (inProgress() && migrateLegacyRun()) save();
        return inProgress() && currentNode() != null ? toInfoProto() : null;
    }

    public synchronized SoldierInteractResp interact(SoldierInteractReq req) {
        if (!inProgress()) return null;
        migrateLegacyRun();

        if (caseType == CASE_FIGHT && req.getId() == shopCaseId() && req.hasShopReq()) {
            return interactShop(req);
        }
        if (caseType == CASE_FIGHT && req.getId() == deploySyncCaseId() && req.hasDeploySyncReq()) {
            return interactDeploySync(req);
        }
        if (req.getId() != caseId) return null;
        if (caseType == CASE_SELECT && req.hasSelectReq()) return interactSelect(req);
        if (caseType == CASE_FIGHT && req.hasFightReq()) return interactFight(req);
        if (caseType == CASE_BATTLE && req.hasBattleReq()) return interactBattle(req);
        if (caseType == CASE_EVENT && req.hasEventSettlementOfDisputeReq()) return interactEvent(req);
        if (caseType == CASE_EVENT_BATTLE && req.hasEventSettlementOfDisputeBattleReq()) {
            return interactEventBattle(req);
        }
        return null;
    }

    private SoldierInteractResp interactSelect(SoldierInteractReq req) {
        var select = req.getSelectReq();
        var season = GameData.getSoldierSeasonDataTable().get(getId());
        if (season == null) return null;

        if (select.hasReRoll() && select.getReRoll()) {
            if (starterRerolls <= 0) return null;
            starterRerolls--;
            rollStarterChoices(season.getStarterGroupId());
            nextCase();
            save();
            return baseInteractResp(req.getId()).setSelectResp(
                    PublicSoldier.InteractSoldierSelectResp.newInstance().setSelectCard(selectCardData()));
        }

        int index = select.getIndex();
        if (index < 1 || index > starterChoices().size()) return null;
        int cardId = starterChoices().get(index - 1);
        var card = GameData.getSoldierStarterCardDataTable().get(cardId);
        if (card == null || card.getGroupId() != season.getStarterGroupId()) return null;

        int rewardCharacterId = 0;
        if (card.getCharacterShow() != null && !card.getCharacterShow().isEmpty()) {
            rewardCharacterId = card.getCharacterShow().getFirst();
            var rewardCharacter = GameData.getSoldierCharacterDataTable().get(rewardCharacterId);
            if (rewardCharacter == null || rewardCharacter.getGroupId() != season.getChessGroupId()
                    || !rewardCharacter.isBoardChess()) return null;
        }

        selectedCard = cardId;
        var change = new PlayerChangeInfo();
        change.add(starterCard(selectedCard));
        applyImmediateCardEffects(card.getCardEffectId(), change);
        if (rewardCharacterId != 0) {
            ownedChess().add(rewardCharacterId);
            addToWaiting(rewardCharacterId);
            change.add(PublicSoldier.SoldierTransform.newInstance().addObtain(chess(rewardCharacterId)));
        }
        starterChoices().clear();
        prepareCurrentNodeCase();
        nextCase();
        save();

        var selected = PublicSoldier.InteractSoldierSelectResp.Success.newInstance()
                .setTyp(PublicSoldier.SoldierCardType.Starter.getNumber()).setId(selectedCard);
        var response = baseInteractResp(req.getId()).setChange(change.toProto()).setSelectResp(
                PublicSoldier.InteractSoldierSelectResp.newInstance().setResp(selected));
        if (rewardCharacterId != 0) {
            response.addEffects(PublicSoldier.SoldierEffect.newInstance().setShowChess(
                    PublicSoldier.SoldierEffectAddChess.newInstance().addChess(chess(rewardCharacterId))));
        }
        return response;
    }

    private SoldierInteractResp interactFight(SoldierInteractReq req) {
        int floorId = rollFloorId();
        if (floorId == 0 || !req.getFightReq().hasDeploy()
                || !storeDeploy(req.getFightReq().getDeploy(), null, true, true)) return null;
        caseType = CASE_BATTLE;
        currentFloorId = floorId;
        nextCase();
        save();
        return baseInteractResp(req.getId());
    }

    private SoldierInteractResp interactShop(SoldierInteractReq req) {
        var shop = req.getShopReq();
        var response = SoldierInteractResp.newInstance().setId(req.getId());
        var shopResp = PublicSoldier.InteractShopResp.newInstance();
        var change = new PlayerChangeInfo();

        if (shop.hasPurchase()) {
            int index = shop.getPurchase() - 1;
            if (index < 0 || index >= shopGoods().size()) return null;
            int characterId = shopGoods().get(index);
            SoldierShopPoolDef goods = shopPool(characterId);
            if (characterId == 0 || goods == null || coin < goods.getCost()) return null;

            coin -= goods.getCost();
            ownedChess().add(characterId);
            shopGoods().set(index, 0);
            change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(-goods.getCost()));
            change.add(PublicSoldier.SoldierTransform.newInstance().addObtain(chess(characterId)));
            shopResp.setGoods(chess(characterId));
        } else if (shop.hasSell()) {
            var sold = shop.getSell();
            var character = GameData.getSoldierCharacterDataTable().get(sold.getId());
            if (sold.getQty() != 1 || sold.getStar() != 1 || character == null || !character.isBoardChess()
                    || !ownedChess().remove((Integer) sold.getId())) return null;
            int value = character.getCost();
            coin += value;
            removeFromDeploy(sold.getId());
            change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(value));
            change.add(PublicSoldier.SoldierTransform.newInstance()
                    .addConsume(PublicSoldier.SoldierChess.newInstance()
                            .setId(sold.getId()).setStar(1).setQty(-1)));
            shopResp.setRes(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(value));
        } else if (shop.hasRefresh() && shop.getRefresh()) {
            if (coin < SHOP_REFRESH_COST) return null;
            coin -= SHOP_REFRESH_COST;
            shopLocked = false;
            rollShop();
            change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(-SHOP_REFRESH_COST));
            shopResp.setShopData(shopData());
        } else if (shop.hasUpgrade() && shop.getUpgrade()) {
            if (coin < SHOP_EXP_COST || shopLevel >= 10) return null;
            coin -= SHOP_EXP_COST;
            shopExp += 4;
            updateShopLevel();
            change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(-SHOP_EXP_COST));
            shopResp.setUpgrade(PublicSoldier.InteractShopResp.UpgradeResult.newInstance()
                    .setLevel(shopLevel).setExp(shopExp).addAllRarityUp(0, 0, 0, 0, 0));
        } else if (shop.hasLock()) {
            shopLocked = shop.getLock();
            shopResp.setSucceed(shopLocked);
        } else {
            return null;
        }

        response.setShopResp(shopResp).setSyncData(syncData());
        if (!change.isEmpty()) response.setChange(change.toProto());
        addCurrentCases(response);
        save();
        return response;
    }

    private SoldierInteractResp interactDeploySync(SoldierInteractReq req) {
        var sync = req.getDeploySyncReq();
        if (!sync.hasDeploy()) return null;
        var deploy = sync.getDeploy();
        boolean empty = deploy.getMaster().length() == 0 && deploy.getAssist().length() == 0
                && deploy.getWaiting().length() == 0 && sync.getDeployEx().length() == 0;
        if (!empty && !storeDeploy(deploy, sync.getDeployEx(), false, false)) return null;
        var response = SoldierInteractResp.newInstance().setId(req.getId());
        response.getMutableNilResp();
        addCurrentCases(response);
        save();
        return response;
    }

    private SoldierInteractResp interactBattle(SoldierInteractReq req) {
        var battle = req.getBattleReq();
        boolean victory = battle.hasVictory();
        boolean defeat = battle.hasDefeat() && battle.getDefeat();
        SoldierNodePlanDef node = currentNode();
        if (node == null || victory == defeat || battle.getChessSkillAddGold() > MAX_SKILL_COIN
                || !validClientData(battle.getClientData(), battle.getDataVersion())) return null;

        int battleTime = 90;
        var floor = GameData.getSoldierFloorDataTable().get(currentFloorId);
        if (floor != null && floor.getBattleTime() > 0) battleTime = floor.getBattleTime();
        if (victory && (!battle.getVictory().hasTime() || battle.getVictory().getTime() > battleTime)) return null;

        clientData = battle.getClientData();
        dataVersion = battle.getDataVersion();
        int baseReward = node.getCoin();
        int interest = Math.min(coin / 10, 5);
        int streakReward;
        if (victory) {
            winningStreak++;
            losingStreak = 0;
            streakReward = streakReward(winningStreak);
            hp = Math.min(MAX_HP, hp + node.getAddHp());
        } else {
            losingStreak++;
            winningStreak = 0;
            streakReward = streakReward(losingStreak);
            hp = Math.max(0, hp - node.getLoseHp());
        }
        int coinDelta = baseReward + interest + streakReward + battle.getChessSkillAddGold();
        coin += coinDelta;
        shopExp += node.getExperience();
        updateShopLevel();

        var response = SoldierInteractResp.newInstance().setId(req.getId())
                .setBattleResp(PublicSoldier.InteractSoldierBattleResp.newInstance()
                        .setBaseReward(baseReward).setInterest(interest)
                        .setWinningStreak(victory ? streakReward : 0)
                        .setLosingStreak(victory ? 0 : streakReward));
        response.setChange(soldierResChange(coinDelta).toProto());

        boolean finalNode = nodeIndex >= nodesForCurrentChallenge().size();
        if (hp == 0 || finalNode) {
            return finishRun(response, finalNode && victory);
        }

        nodeIndex++;
        SoldierNodePlanDef next = currentNode();
        if (next == null) return finishRun(response, victory);
        stage = next.getStage();
        currentFloorId = 0;
        prepareCurrentNodeCase();
        nextCase();
        response.setNode(nodeData()).setSyncData(syncData());
        addCurrentCases(response);
        save();
        return response;
    }

    private SoldierInteractResp interactEvent(SoldierInteractReq req) {
        var eventReq = req.getEventSettlementOfDisputeReq();
        if (eventReq.hasThrowDice() && eventReq.getThrowDice()) {
            if (dicePoint != 0 || eventChoices().size() != 3) return null;
            int first = Utils.randomRange(1, 20);
            int second = 0;
            dicePoint = first;
            var result = PublicSoldier.InteractEventSettlementOfDisputeResp.ThrowResult.newInstance();
            if (hasTwoDice()) {
                second = Utils.randomRange(1, 20);
                if (first == 1 && second == 1) second = 2;
                dicePoint = Math.max(first, second);
                result.addAllTwoDicePoint(first, second);
            }
            result.setPoint(dicePoint);
            save();
            return baseInteractResp(req.getId()).setEventSettlementOfDisputeResp(
                    PublicSoldier.InteractEventSettlementOfDisputeResp.newInstance().setResult(result));
        }

        int index = eventReq.getIndex();
        if (!eventReq.hasIndex() || dicePoint == 0 || index < 1 || index > eventChoices().size()) return null;
        var event = GameData.getSoldierEventBattlePoolDataTable().get(eventChoices().get(index - 1));
        if (event == null) return null;
        int required = switch (index) {
            case 1 -> 5;
            case 2 -> 11;
            default -> 16;
        };

        if (dicePoint < required) {
            int floorId = rollFloorForGroup(event.getFloorGroup());
            if (floorId == 0) return null;
            eventBattleId = event.getId();
            currentFloorId = floorId;
            caseType = CASE_EVENT_BATTLE;
            nextCase();
            save();
            return baseInteractResp(req.getId());
        }

        var change = new PlayerChangeInfo();
        var reward = grantEventReward(event, change);
        var response = SoldierInteractResp.newInstance().setId(req.getId())
                .setEventSettlementOfDisputeResp(
                        PublicSoldier.InteractEventSettlementOfDisputeResp.newInstance().setReward(reward));
        if (!change.isEmpty()) response.setChange(change.toProto());
        completeEventNode(response, true);
        return response;
    }

    private SoldierInteractResp interactEventBattle(SoldierInteractReq req) {
        var battle = req.getEventSettlementOfDisputeBattleReq();
        boolean victory = battle.hasVictory();
        boolean defeat = battle.hasDefeat() && battle.getDefeat();
        var event = GameData.getSoldierEventBattlePoolDataTable().get(eventBattleId);
        var node = currentNode();
        if (event == null || node == null || victory == defeat || battle.getChessSkillAddGold() > MAX_SKILL_COIN
                || !validClientData(battle.getClientData(), battle.getDataVersion())) return null;
        var floor = GameData.getSoldierFloorDataTable().get(currentFloorId);
        if (victory && (!battle.getVictory().hasTime()
                || (floor != null && floor.getBattleTime() > 0 && battle.getVictory().getTime() > floor.getBattleTime()))) {
            return null;
        }

        clientData = battle.getClientData();
        dataVersion = battle.getDataVersion();
        int interest = Math.min(coin / 10, 5);
        int baseReward = victory ? event.getCoin() : 0;
        int coinDelta = baseReward + interest + battle.getChessSkillAddGold();
        coin += coinDelta;
        var change = soldierResChange(coinDelta);
        var battleResp = PublicSoldier.InteractEventSettlementOfDisputeBattleResp.newInstance()
                .setCoin(baseReward).setBaseReward(baseReward).setInterest(interest);
        if (victory) {
            for (int id : grantEventCharacters(event.getCharacterCount(), change)) battleResp.addChess(chess(id));
            grantEventStrategyCards(event.getStrategyCardCount(), change);
        }
        var response = SoldierInteractResp.newInstance().setId(req.getId())
                .setEventSettlementOfDisputeBattleResp(battleResp).setChange(change.toProto());
        completeEventNode(response, victory);
        return response;
    }

    private void completeEventNode(SoldierInteractResp response, boolean victory) {
        var node = currentNode();
        if (node == null) return;
        hp = victory ? Math.min(MAX_HP, hp + node.getAddHp()) : Math.max(0, hp - node.getLoseHp());
        shopExp += node.getExperience();
        updateShopLevel();
        boolean finalNode = nodeIndex >= nodesForCurrentChallenge().size();
        if (hp == 0 || finalNode) {
            finishRun(response, finalNode && victory);
            return;
        }
        nodeIndex++;
        stage = currentNode().getStage();
        currentFloorId = 0;
        eventBattleId = 0;
        dicePoint = 0;
        eventChoices().clear();
        prepareCurrentNodeCase();
        nextCase();
        response.setNode(nodeData()).setSyncData(syncData());
        addCurrentCases(response);
        save();
    }

    private boolean validClientData(String value, int version) {
        return value != null && value.length() <= MAX_CLIENT_DATA && version > 0;
    }

    private SoldierInteractResp finishRun(SoldierInteractResp response, boolean victory) {
        var challenge = GameData.getSoldierGradeChallengeDataTable().get(curGradeChallengeId);
        if (challenge == null) return null;
        int rewardScore = runScore(challenge, victory ? nodesForCurrentChallenge().size() : nodeIndex);
        score += rewardScore;
        if (victory) {
            gradeChallengeId = Math.max(gradeChallengeId, curGradeChallengeId);
            completedChallenges().add(curGradeChallengeId);
        }
        int totalScore = score;
        response.setNode(nodeData()).setSyncData(syncData());
        clearRun();
        response.setSettle(PublicSoldier.SoldierSettleDataResp.newInstance()
                .setTotalScore(totalScore).setRewardScore(rewardScore).setResult(victory ? 1 : 0));
        save();
        return response;
    }

    private static int streakReward(int streak) {
        if (streak >= 5) return 3;
        if (streak >= 2) return 2;
        return 1;
    }

    private PlayerChangeInfo soldierResChange(int delta) {
        var change = new PlayerChangeInfo();
        change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(delta));
        return change;
    }

    public synchronized SoldierGiveUpResp giveUp() {
        if (!inProgress()) return null;
        var challenge = GameData.getSoldierGradeChallengeDataTable().get(curGradeChallengeId);
        int rewardScore = challenge == null ? 0 : runScore(challenge, Math.max(0, nodeIndex - 1));
        score += rewardScore;
        clearRun();
        save();
        return SoldierGiveUpResp.newInstance().setTotalScore(score).setRewardScore(rewardScore);
    }

    public synchronized boolean stepOut(PublicSoldier.SoldierDeploy deploy) {
        if (!inProgress()) return false;
        migrateLegacyRun();
        storeDeploy(deploy, null, false, false);
        save();
        return true;
    }

    private int runScore(SoldierGradeChallengeDef challenge, int completedNodes) {
        int totalNodes = nodes(challenge.getNodeGroupId()).size();
        if (totalNodes == 0 || completedNodes <= 0) return 0;
        return challenge.getScore() * Math.min(completedNodes, totalNodes) / totalNodes;
    }

    private boolean storeDeploy(PublicSoldier.SoldierDeploy deploy,
            us.hebi.quickbuf.RepeatedMessage<PublicSoldier.SoldierChess> deployEx,
            boolean requireMaster, boolean requireAllOwned) {
        if (deploy.getMaster().length() > 3 || deploy.getAssist().length() > 6
                || deploy.getWaiting().length() > 9) return false;

        var remaining = new HashMap<Integer, Integer>();
        for (int id : ownedChess()) remaining.merge(id, 1, Integer::sum);
        var master = new ArrayList<Integer>();
        var assist = new ArrayList<Integer>();
        var waiting = new ArrayList<Integer>();
        if (!validateChessList(deploy.getMaster(), master, remaining, true, false)
                || !validateChessList(deploy.getAssist(), assist, remaining, true, false)
                || !validateChessList(deploy.getWaiting(), waiting, remaining, false, false)) return false;
        if (deployEx != null && !validateChessList(deployEx, null, remaining, false, true)) return false;
        if (requireMaster && master.stream().noneMatch(id -> id != 0)) return false;
        if (requireAllOwned) {
            for (int count : remaining.values()) if (count != 0) return false;
        }

        masterDeploy().clear();
        masterDeploy().addAll(master);
        assistDeploy().clear();
        assistDeploy().addAll(assist);
        waitingDeploy().clear();
        waitingDeploy().addAll(waiting);
        return true;
    }

    private boolean validateChessList(us.hebi.quickbuf.RepeatedMessage<PublicSoldier.SoldierChess> entries,
            List<Integer> output, Map<Integer, Integer> remaining, boolean uniqueDeployed, boolean allowQuantity) {
        var localIds = uniqueDeployed ? new HashSet<Integer>() : null;
        for (int i = 0; i < entries.length(); i++) {
            var chess = entries.get(i);
            if (chess.getId() == 0) {
                if (output == null || chess.getQty() != 1 || chess.getStar() != 0) return false;
                output.add(0);
                continue;
            }
            int count = remaining.getOrDefault(chess.getId(), 0);
            int quantity = chess.getQty();
            if ((!allowQuantity && quantity != 1) || quantity <= 0 || chess.getStar() != 1 || count < quantity
                    || (localIds != null && !localIds.add(chess.getId()))) return false;
            remaining.put(chess.getId(), count - quantity);
            if (output != null) {
                for (int j = 0; j < quantity; j++) output.add(chess.getId());
            }
        }
        return true;
    }

    private void removeFromDeploy(int id) {
        replaceInDeploy(id, 0);
    }

    private void addToWaiting(int id) {
        int empty = waitingDeploy().indexOf(0);
        if (empty >= 0) waitingDeploy().set(empty, id);
        else if (waitingDeploy().size() < 9) waitingDeploy().add(id);
    }

    private void replaceInDeploy(int oldId, int newId) {
        int index = masterDeploy().indexOf(oldId);
        if (index >= 0) {
            masterDeploy().set(index, newId);
            return;
        }
        index = assistDeploy().indexOf(oldId);
        if (index >= 0) {
            assistDeploy().set(index, newId);
            return;
        }
        index = waitingDeploy().indexOf(oldId);
        if (index >= 0) waitingDeploy().set(index, newId);
    }

    public synchronized boolean setPartnerTrace(int partnerType, boolean trace) {
        if (!inProgress() || partnerType <= 0 || partnerType > 19) return false;
        if (trace) tracePartnerTypes().add(partnerType);
        else tracePartnerTypes().remove(partnerType);
        save();
        return true;
    }

    private void clearRun() {
        curGradeChallengeId = 0;
        stage = 0;
        nodeIndex = 0;
        hp = 0;
        coin = 0;
        shopLevel = 0;
        shopExp = 0;
        winningStreak = 0;
        losingStreak = 0;
        caseId = 0;
        caseType = 0;
        currentFloorId = 0;
        selectedCard = 0;
        starterRerolls = 0;
        dicePoint = 0;
        eventBattleId = 0;
        opportunityTwoDice = false;
        shopLocked = false;
        dataVersion = 0;
        clientData = "";
        tracePartnerTypes().clear();
        ownedChess().clear();
        starterChoices().clear();
        shopGoods().clear();
        masterDeploy().clear();
        assistDeploy().clear();
        waitingDeploy().clear();
        eventChoices().clear();
        strategyCards().clear();
    }

    private boolean migrateLegacyRun() {
        boolean changed = false;
        if (!opportunityTwoDice) {
            for (int id : strategyCards()) {
                var card = GameData.getSoldierStrategyCardDataTable().get(id);
                if (card != null && cardHasImmediateEffect(card.getCardEffectId(), EFFECT_TWO_DICE)) {
                    opportunityTwoDice = true;
                    changed = true;
                    break;
                }
            }
        }
        if (shopLevel < INITIAL_SHOP_LEVEL) {
            shopLevel = INITIAL_SHOP_LEVEL;
            changed = true;
        }
        int previousLevel = shopLevel;
        int previousExp = shopExp;
        updateShopLevel();
        if (shopLevel != previousLevel || shopExp != previousExp) changed = true;
        if (dataVersion == 0) {
            dataVersion = 1;
            changed = true;
        }
        if (coin == 0) {
            coin = INITIAL_COIN;
            changed = true;
        }
        if (ownedChess().isEmpty()) {
            for (int id : LEGACY_INITIAL_CHESS) ownedChess().add(id);
            changed = true;
        }
        var season = GameData.getSoldierSeasonDataTable().get(getId());
        if (season != null) {
            var replacements = new ArrayList<Integer>();
            for (var character : GameData.getSoldierCharacterDataTable()) {
                if (character.getGroupId() == season.getChessGroupId() && character.isBoardChess()
                        && !ownedChess().contains(character.getId())) replacements.add(character.getId());
            }
            Collections.shuffle(replacements);
            for (int i = 0; i < ownedChess().size(); i++) {
                int id = ownedChess().get(i);
                var character = GameData.getSoldierCharacterDataTable().get(id);
                if (character != null && character.getGroupId() == season.getChessGroupId()
                        && character.isBoardChess()) continue;
                if (replacements.isEmpty()) break;
                removeFromDeploy(id);
                ownedChess().set(i, replacements.removeLast());
                changed = true;
            }
        }
        if (masterDeploy().isEmpty() && assistDeploy().isEmpty() && waitingDeploy().isEmpty()) {
            waitingDeploy().addAll(ownedChess().subList(0, Math.min(9, ownedChess().size())));
            changed = true;
        }
        if (caseType == CASE_SELECT && starterChoices().isEmpty()) {
            if (season != null) rollStarterChoices(season.getStarterGroupId());
            changed = true;
        }
        if (caseType == CASE_BATTLE && currentFloorId == 0) {
            currentFloorId = rollFloorId();
            changed = true;
        }
        if (caseType == CASE_EVENT && eventChoices().isEmpty()) {
            rollEventChoices();
            changed = true;
        }
        if (caseType == CASE_FIGHT && isOpportunityNode()) {
            prepareCurrentNodeCase();
            changed = true;
        }
        if (caseType == CASE_FIGHT && shopGoods().isEmpty()) {
            rollShop();
            changed = true;
        }
        return changed;
    }

    private void rollShop() {
        var season = GameData.getSoldierSeasonDataTable().get(getId());
        var level = GameData.getSoldierShopLevelDataTable().get(shopLevel);
        shopGoods().clear();
        if (season == null || level == null) return;

        var excluded = new HashSet<Integer>(ownedChess());
        for (int i = 0; i < SHOP_SIZE; i++) {
            var choices = new WeightedList<SoldierShopPoolDef>();
            for (var goods : GameData.getSoldierShopPoolDataTable()) {
                var character = GameData.getSoldierCharacterDataTable().get(goods.getChessCharacterId());
                if (goods.getGroupId() != season.getShopPoolGroupId() || excluded.contains(goods.getChessCharacterId())
                        || character == null || !character.isBoardChess()) continue;
                choices.add((double) goods.getWeight() * level.rarityWeight(goods.getRarity()), goods);
            }
            if (choices.size() == 0) break;
            var selected = choices.next();
            shopGoods().add(selected.getChessCharacterId());
            excluded.add(selected.getChessCharacterId());
        }
    }

    private SoldierShopPoolDef shopPool(int characterId) {
        var season = GameData.getSoldierSeasonDataTable().get(getId());
        if (season == null) return null;
        for (var goods : GameData.getSoldierShopPoolDataTable()) {
            if (goods.getGroupId() == season.getShopPoolGroupId()
                    && goods.getChessCharacterId() == characterId) return goods;
        }
        return null;
    }

    private void updateShopLevel() {
        while (shopLevel < 10) {
            var def = GameData.getSoldierShopLevelDataTable().get(shopLevel);
            if (def == null || def.getExp() <= 0 || shopExp < def.getExp()) break;
            shopExp -= def.getExp();
            shopLevel++;
        }
    }

    private PublicSoldier.ShopCaseData shopData() {
        var proto = PublicSoldier.ShopCaseData.newInstance().setLevel(shopLevel).setExp(shopExp)
                .setLock(shopLocked).setFree(0).setReRollPrice(SHOP_REFRESH_COST)
                .setPurchaseExpPrice(SHOP_EXP_COST).addAllRarityUp(0, 0, 0, 0, 0);
        for (int id : shopGoods()) {
            var goods = shopPool(id);
            proto.addGoods(PublicSoldier.SoldierGoods.newInstance().setId(id).setStar(id == 0 ? 0 : 1)
                    .setPrice(goods == null ? 0 : goods.getCost()));
        }
        while (proto.getGoods().length() < SHOP_SIZE) {
            proto.addGoods(PublicSoldier.SoldierGoods.newInstance().setId(0).setStar(0).setPrice(0));
        }
        return proto;
    }

    private int rollFloorId() {
        var node = currentNode();
        if (node == null) return 0;
        var candidates = new ArrayList<Integer>();
        for (var floor : GameData.getSoldierFloorDataTable()) {
            int group = floor.getDefault();
            boolean matches = switch (node.getNodeType()) {
                case 3 -> group >= 300 && group < 400;
                case 4 -> group == (nodesForCurrentChallenge().size() <= 12 ? 401 : 402);
                default -> group >= 100 && group < 300;
            };
            if (matches) candidates.add(floor.getId());
        }
        return candidates.isEmpty() ? 0 : Utils.randomElement(candidates);
    }

    private int rollFloorForGroup(int groupId) {
        var candidates = new ArrayList<Integer>();
        for (var floor : GameData.getSoldierFloorDataTable()) {
            if (floor.getDefault() == groupId) candidates.add(floor.getId());
        }
        return candidates.isEmpty() ? 0 : Utils.randomElement(candidates);
    }

    private void prepareCurrentNodeCase() {
        if (isOpportunityNode()) {
            caseType = CASE_EVENT;
            dicePoint = 0;
            eventBattleId = 0;
            rollEventChoices();
            return;
        }
        caseType = CASE_FIGHT;
        if (!shopLocked) rollShop();
    }

    private boolean isOpportunityNode() {
        var node = currentNode();
        var eventPlan = node == null ? null : GameData.getSoldierEventPlanDataTable().get(node.getId());
        return eventPlan != null && eventPlan.getEventType() == 4;
    }

    private void rollEventChoices() {
        eventChoices().clear();
        var node = currentNode();
        if (node == null) return;
        var candidates = new ArrayList<SoldierEventBattlePoolDef>();
        for (int stagePool = node.getStage(); stagePool >= 1 && candidates.isEmpty(); stagePool--) {
            int poolId = node.getNodeGroupId() * 100000 + 40100 + stagePool;
            for (var event : GameData.getSoldierEventBattlePoolDataTable()) {
                if (event.getPoolId() == poolId) candidates.add(event);
            }
        }
        while (eventChoices().size() < 3 && !candidates.isEmpty()) {
            var weighted = new WeightedList<SoldierEventBattlePoolDef>();
            for (var event : candidates) weighted.add(event.getWeight(), event);
            var selected = weighted.next();
            eventChoices().add(selected.getId());
            candidates.remove(selected);
        }
        eventChoices().sort((a, b) -> {
            var left = GameData.getSoldierEventBattlePoolDataTable().get(a);
            var right = GameData.getSoldierEventBattlePoolDataTable().get(b);
            return Integer.compare(left.getLevelName(), right.getLevelName());
        });
    }

    private PublicSoldier.InteractEventSettlementOfDisputeResp.ChooseReward grantEventReward(
            SoldierEventBattlePoolDef event, PlayerChangeInfo change) {
        var reward = PublicSoldier.InteractEventSettlementOfDisputeResp.ChooseReward.newInstance()
                .setCoin(event.getCoin());
        if (event.getCoin() > 0) {
            coin += event.getCoin();
            change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(event.getCoin()));
        }
        for (int id : grantEventCharacters(event.getCharacterCount(), change)) reward.addChess(chess(id));
        grantEventStrategyCards(event.getStrategyCardCount(), change);
        return reward;
    }

    private List<Integer> grantEventCharacters(int count, PlayerChangeInfo change) {
        var season = GameData.getSoldierSeasonDataTable().get(getId());
        if (season == null || count <= 0) return List.of();
        var pool = new ArrayList<Integer>();
        for (var character : GameData.getSoldierCharacterDataTable()) {
            if (character.getGroupId() == season.getChessGroupId() && character.isBoardChess()) {
                pool.add(character.getId());
            }
        }
        if (pool.isEmpty()) return List.of();
        var result = new ArrayList<Integer>();
        var transform = PublicSoldier.SoldierTransform.newInstance();
        for (int i = 0; i < count; i++) {
            int id = Utils.randomElement(pool);
            result.add(id);
            ownedChess().add(id);
            addToWaiting(id);
            transform.addObtain(chess(id));
        }
        if (!result.isEmpty()) change.add(transform);
        return result;
    }

    private void grantEventStrategyCards(int count, PlayerChangeInfo change) {
        var season = GameData.getSoldierSeasonDataTable().get(getId());
        if (season == null || count <= 0) return;
        var pool = new ArrayList<Integer>();
        for (var card : GameData.getSoldierStrategyCardDataTable()) {
            if (card.getGroupId() == season.getStrategyGroupId() && !strategyCards().contains(card.getId())
                    && (card.getGradeLevelCond() == 0 || curGradeChallengeId >= card.getGradeLevelCond())) {
                pool.add(card.getId());
            }
        }
        Collections.shuffle(pool);
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            int id = pool.get(i);
            strategyCards().add(id);
            change.add(strategyCard(id));
            var card = GameData.getSoldierStrategyCardDataTable().get(id);
            if (card != null) applyImmediateCardEffects(card.getCardEffectId(), change);
        }
    }

    private void applyImmediateCardEffects(int cardEffectId, PlayerChangeInfo change) {
        var cardEffect = GameData.getSoldierCardEffectDataTable().get(cardEffectId);
        if (cardEffect == null || cardEffect.getBuffIds() == null) return;
        for (int buffId : cardEffect.getBuffIds()) {
            var buff = GameData.getSoldierBuffDataTable().get(buffId);
            if (buff == null || buff.getCond1() != 0 || buff.getCond2() != 0) continue;
            applyImmediateEffect(buff.getEffect1(), buff.getEffectParams1(), change);
            applyImmediateEffect(buff.getEffect2(), buff.getEffectParams2(), change);
        }
    }

    private boolean cardHasImmediateEffect(int cardEffectId, int effect) {
        var cardEffect = GameData.getSoldierCardEffectDataTable().get(cardEffectId);
        if (cardEffect == null || cardEffect.getBuffIds() == null) return false;
        for (int buffId : cardEffect.getBuffIds()) {
            var buff = GameData.getSoldierBuffDataTable().get(buffId);
            if (buff != null && buff.getCond1() == 0 && buff.getCond2() == 0
                    && (buff.getEffect1() == effect || buff.getEffect2() == effect)) return true;
        }
        return false;
    }

    private void applyImmediateEffect(int effect, List<String> params, PlayerChangeInfo change) {
        if (effect == 0) return;
        if (effect == EFFECT_TWO_DICE) {
            opportunityTwoDice = true;
            return;
        }
        if (effect == EFFECT_SWAP_SAN_COIN) {
            int previousCoin = coin;
            coin = hp;
            hp = previousCoin;
            change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(coin - previousCoin));
            return;
        }
        int value = effectParam(params, effect == EFFECT_ADD_RESOURCE ? 1 : 0);
        if (effect == EFFECT_ADD_RESOURCE) {
            if (effectParam(params, 0) != COIN_TID || value == 0) return;
            coin += value;
            change.add(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(value));
        } else if (effect == EFFECT_ADD_SAN) {
            hp = Math.min(MAX_HP, hp + value);
        } else if (effect == EFFECT_LOSE_SAN) {
            hp = Math.max(0, hp - value);
        } else if (effect == EFFECT_SET_SAN) {
            hp = Math.max(0, value);
        }
    }

    private static int effectParam(List<String> params, int index) {
        if (params == null || index < 0 || index >= params.size()) return 0;
        try {
            return Integer.parseInt(params.get(index));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean hasTwoDice() {
        return opportunityTwoDice;
    }

    private void rollStarterChoices(int groupId) {
        var pool = new ArrayList<Integer>();
        for (var card : GameData.getSoldierStarterCardDataTable()) {
            if (card.getGroupId() == groupId
                    && (card.getGradeLevelCond() == 0 || curGradeChallengeId >= card.getGradeLevelCond())) {
                pool.add(card.getId());
            }
        }
        Collections.shuffle(pool);
        starterChoices().clear();
        starterChoices().addAll(pool.subList(0, Math.min(3, pool.size())));
    }

    private List<SoldierNodePlanDef> nodesForCurrentChallenge() {
        var challenge = GameData.getSoldierGradeChallengeDataTable().get(curGradeChallengeId);
        return challenge == null ? List.of() : nodes(challenge.getNodeGroupId());
    }

    private List<SoldierNodePlanDef> nodes(int groupId) {
        var result = new ArrayList<SoldierNodePlanDef>();
        for (var node : GameData.getSoldierNodePlanDataTable()) {
            if (node.getNodeGroupId() == groupId) result.add(node);
        }
        result.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        return result;
    }

    private SoldierNodePlanDef currentNode() {
        var nodes = nodesForCurrentChallenge();
        return nodeIndex > 0 && nodeIndex <= nodes.size() ? nodes.get(nodeIndex - 1) : null;
    }

    private void nextCase() {
        caseId = caseId == Integer.MAX_VALUE ? 1 : caseId + 1;
        if (caseId == 0) caseId = 1;
    }

    private SoldierInteractResp baseInteractResp(int requestId) {
        var response = SoldierInteractResp.newInstance().setId(requestId).setNode(nodeData())
                .setSyncData(syncData());
        addCurrentCases(response);
        return response;
    }

    private void addCurrentCases(SoldierInteractResp response) {
        response.addCases(caseProto());
        if (caseType == CASE_FIGHT) {
            response.addCases(PublicSoldier.SoldierCase.newInstance().setId(shopCaseId()).setShopCase(shopData()));
            response.addCases(PublicSoldier.SoldierCase.newInstance().setId(deploySyncCaseId())
                    .setDeploySyncCase(PublicSoldier.DeploySyncCaseData.newInstance()));
        }
    }

    private int shopCaseId() {
        return caseId + SHOP_CASE_OFFSET;
    }

    private int deploySyncCaseId() {
        return caseId + DEPLOY_SYNC_CASE_OFFSET;
    }

    private PublicSoldier.SoldierSyncData syncData() {
        return PublicSoldier.SoldierSyncData.newInstance().setHp(hp).setLevel(shopLevel)
                .setExp(shopExp).setWinningStreak(winningStreak).setLosingStreak(losingStreak);
    }

    private PublicSoldier.SoldierInfo toInfoProto() {
        var info = PublicSoldier.SoldierInfo.newInstance();
        var meta = info.getMutableMeta().setId(getId()).setHp(hp)
                .setWinningStreak(winningStreak).setLosingStreak(losingStreak)
                .setShopLevel(shopLevel).setShopExp(shopExp)
                .setDataLen(clientData == null ? 0 : clientData.length()).setDataVersion(dataVersion)
                .setClientData(clientData == null ? "" : clientData);
        for (int type : tracePartnerTypes()) meta.addTracePartnerTypes(type);

        var node = info.getMutableNode().setData(nodeData()).addCases(caseProto());
        if (caseType == CASE_FIGHT) {
            node.addCases(PublicSoldier.SoldierCase.newInstance().setId(shopCaseId()).setShopCase(shopData()));
            node.addCases(PublicSoldier.SoldierCase.newInstance().setId(deploySyncCaseId())
                    .setDeploySyncCase(PublicSoldier.DeploySyncCaseData.newInstance()));
        }
        var bag = info.getMutableBag();
        var deploy = bag.getMutableDeploy();
        for (int i = 0; i < 3; i++) deploy.addMaster(slotChess(masterDeploy(), i));
        for (int i = 0; i < 6; i++) deploy.addAssist(slotChess(assistDeploy(), i));
        for (int i = 0; i < 9; i++) deploy.addWaiting(slotChess(waitingDeploy(), i));
        bag.addRes(PublicSoldier.SoldierRes.newInstance().setTid(COIN_TID).setQty(coin));
        for (int id : ownedChess()) bag.addChess(chess(id));
        if (selectedCard != 0) bag.addCards(starterCard(selectedCard));
        for (int id : strategyCards()) bag.addCards(strategyCard(id));
        return info;
    }

    private PublicSoldier.SoldierNodeData nodeData() {
        var node = currentNode();
        if (node == null) return PublicSoldier.SoldierNodeData.newInstance();
        return PublicSoldier.SoldierNodeData.newInstance().setStageId(node.getStage())
                .setNodeId(node.getId()).setType(node.getNodeType());
    }

    private PublicSoldier.SoldierCase caseProto() {
        var proto = PublicSoldier.SoldierCase.newInstance().setId(caseId);
        if (caseType == CASE_SELECT) proto.setSelectCard(selectCardData());
        else if (caseType == CASE_FIGHT) proto.getMutableFightCase();
        else if (caseType == CASE_BATTLE) proto.getMutableBattleCase().setFloorId(currentFloorId);
        else if (caseType == CASE_EVENT) {
            proto.getMutableEventSettlementOfDispute().setDicePoint(dicePoint).setTwoDice(hasTwoDice())
                    .addAllIds(eventChoices().stream().mapToInt(Integer::intValue).toArray());
        } else if (caseType == CASE_EVENT_BATTLE) {
            proto.getMutableEventSettlementOfDisputeBattle().setBattleId(eventBattleId).setFloorId(currentFloorId);
        }
        return proto;
    }

    private PublicSoldier.SelectCardData selectCardData() {
        return PublicSoldier.SelectCardData.newInstance().setType(PublicSoldier.SoldierCardType.Starter)
                .setReRoll(starterRerolls)
                .addAllIds(starterChoices().stream().mapToInt(Integer::intValue).toArray());
    }

    private static PublicSoldier.SoldierChess chess(int id) {
        return PublicSoldier.SoldierChess.newInstance().setId(id).setQty(1).setStar(1);
    }

    private static PublicSoldier.SoldierChess slotChess(List<Integer> slots, int index) {
        int id = index < slots.size() ? slots.get(index) : 0;
        return id == 0 ? PublicSoldier.SoldierChess.newInstance().setId(0).setQty(1).setStar(0) : chess(id);
    }

    private static PublicSoldier.SoldierCard starterCard(int id) {
        return PublicSoldier.SoldierCard.newInstance().setId(id)
                .setType(PublicSoldier.SoldierCardType.Starter);
    }

    private static PublicSoldier.SoldierCard strategyCard(int id) {
        return PublicSoldier.SoldierCard.newInstance().setId(id)
                .setType(PublicSoldier.SoldierCardType.Strategy);
    }

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        msg.getMutableSoldier().setCurGradeChallengeId(curGradeChallengeId)
                .setScore(score).setGradeChallengeId(gradeChallengeId).setStage(stage).setNodeIndex(nodeIndex);
    }
}
