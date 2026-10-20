package emu.nebula.game.quest;

import emu.nebula.data.resources.StarTowerQuestDef;
import emu.nebula.game.achievement.AchievementParamCond;

// TODO clean up and merge with other quest helpers
public class StarTowerQuestHelper {
    
    /**
     * Checks if all prerequisite quests for the specified quest have been claimed.
     */
    public static boolean isUnlocked(QuestManager manager, StarTowerQuestDef data) {
        for (int preId : data.getPreTowerQuestIds()) {
            var quest = manager.getQuest(QuestType.Tower, preId);
            
            if (quest == null || !quest.isClaimed()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks whether a trigger payload matches the quest's generated params.
     */
    public static boolean matches(StarTowerQuestDef data, int param1, int param2) {
        // Growth node quests can require one or two specific nodes.
        // The trigger payload puts the unlocked node id in param1.
        if (data.getCompleteCond() == QuestCondition.TowerGrowthWithSpecificNote.getValue()) {
            boolean matchesFirst = data.getParam1() != 0 && param1 == data.getParam1();
            boolean matchesSecond = data.getParam2() != 0 && param1 == data.getParam2();
            
            return matchesFirst || matchesSecond;
        }
        
        return matchesParam(data.getParam1(), data.getParamCond1(), param1) && matchesParam(data.getParam2(), data.getParamCond2(), param2);
    }
    
    private static boolean matchesParam(int param, int cond, int value) {
        // A param of zero is a wildcard, matching the existing GameQuest behavior
        if (param == 0) {
            return true;
        }
        
        return AchievementParamCond.getByValue(cond).test(param, value);
    }
}
