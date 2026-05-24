package emu.nebula.game.quest;

import dev.morphia.annotations.Entity;
import emu.nebula.util.Utils;
import emu.nebula.util.ResetCycle;
import emu.nebula.proto.Public.Quest;
import emu.nebula.proto.Public.QuestProgress;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity(useDiscriminator = false)
public class GameQuest {
    private int id;
    private int type;
    private int cond;
    private int param;
    
    private int curProgress;
    private int maxProgress;
    
    @Setter
    private boolean claimed;
    
    @Deprecated
    public GameQuest() {
        
    }
    
    public GameQuest(QuestData data) {
        this.id = data.getId();
        this.type = data.getQuestType();
        this.cond = data.getCompleteCond();
        this.maxProgress = data.getCompleteCondParams()[0];
        
        if (data.getCompleteCondParams().length >= 2) {
            this.param = data.getCompleteCondParams()[1];
        }
    }
    
    public long getKey() {
        return ((long) this.getType() << 32) + this.getId();
    }

    public void resetProgress() {
        this.curProgress = 0;
        this.claimed = false;
    }
    
    public boolean isComplete() {
        return this.curProgress >= this.maxProgress;
    }
    
    public boolean canClaim() {
        return this.isComplete() && !this.isClaimed();
    }
    
    private int getStatus() {
        if (this.isClaimed()) {
            return 2;
        } else if (this.isComplete()) {
            return 1;
        }
        
        return 0;
    }

    public boolean trigger(int condition, int progress, int param1, int param2) {
        // Sanity check
        if (this.isComplete()) {
            return false;
        }
        
        // Skip if not the correct condition
        if (this.cond != condition) {
            return false;
        }
        
        // Check quest param
        if (this.param != 0 && param1 != this.param) {
            return false;
        }
        
        // Get new progress
        int newProgress = Math.min(this.curProgress + progress, this.maxProgress);
        
        // Set
        if (this.curProgress != newProgress) {
            this.curProgress = newProgress;
            return true;
        }
        
        return false;
    }
    
    // Proto

    public Quest toProto() {
        var progress = QuestProgress.newInstance()
                .setCur(this.getCurProgress())
                .setMax(this.getMaxProgress());
        
        var proto = Quest.newInstance()
                .setId(this.getId())
                .setTypeValue(this.getType())
                .setStatus(this.getStatus())
                .addProgress(progress);

        long expire = this.getExpireTime();
        if (expire > 0) {
            proto.setExpire(expire);
        }
        
        return proto;
    }

    private long getExpireTime() {
        return switch (this.type) {
            case QuestType.BattlePassDaily -> Utils.getNextResetTimeSeconds(ResetCycle.DAILY);
            case QuestType.BattlePassWeekly -> Utils.getNextResetTimeSeconds(ResetCycle.WEEKLY);
            default -> 0L;
        };
    }

}
