package emu.nebula.game.vampire;

import java.util.ArrayList;
import java.util.List;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.player.PlayerProgress;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.UseBuild;
import emu.nebula.proto.VampireTalentDetail.VampireTalentDetailResp;
import emu.nebula.util.Bitset;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import lombok.Getter;

import us.hebi.quickbuf.RepeatedMessage;

@Getter
public class VampireSurvivorManager extends PlayerManager {
    // Game
    private VampireSurvivorGame game;
    
    public VampireSurvivorManager(Player player) {
        super(player);
    }
    
    public PlayerProgress getProgress() {
        return this.getPlayer().getProgress();
    }

    public Bitset getTalents() {
        return this.getProgress().getVampireTalents();
    }
    
    public int getTalentPoints() {
        return this.getProgress().getFateCards().size() * 5;
    }
    
    public int getUsedTalentPoints() {
        int amount = 0;
        
        for (var talent : GameData.getVampireTalentDataTable()) {
            if (this.getTalents().isSet(talent.getId())) {
                amount += talent.getPoint();
            }
        }
        
        return amount;
    }
    
    public boolean unlockTalent(int id) {
        // Get talent data
        var talent = GameData.getVampireTalentDataTable().get(id);
        if (talent == null) {
            return false;
        }
        
        // Check to make sure talent isnt already set
        if (this.getTalents().isSet(talent.getId())) {
            return false;
        }
        
        // Make sure any prerequisite talent is set
        if (talent.getPrev() != null) {
            boolean hasPrev = false;
            
            for (int prevTalent : talent.getPrev()) {
                if (this.getTalents().isSet(prevTalent)) {
                    hasPrev = true;
                }
            }
            
            if (!hasPrev) {
                return false;
            }
        }
        
        // Calculate available points
        int points = this.getTalentPoints() - this.getUsedTalentPoints();
        
        // Make sure we have enough points to unlock this talent
        if (talent.getPoint() > points) {
            return false;
        }
        
        // Set & save in database
        this.getTalents().setBit(talent.getId());
        Nebula.getGameDatabase().update(this.getProgress(), this.getPlayerUid(), "vampireTalents", this.getTalents());
        
        // Success
        return true;
    }
    
    public void resetTalents() {
        // Sanity check to prevent resetting empty talents
        if (this.getTalents().isEmpty()) {
            return;
        }
        
        // Clear talents
        this.getTalents().clear();
        
        // Save in database
        Nebula.getGameDatabase().update(this.getProgress(), this.getPlayerUid(), "vampireTalents", this.getTalents());
    }
    
    public VampireSurvivorGame apply(int levelId, RepeatedMessage<UseBuild> builds) {
        // Get data
        var data = GameData.getVampireSurvivorDataTable().get(levelId);
        if (data == null) {
            return null;
        }
        
        // Check player level (world class)
        if (this.getPlayer().getLevel() < data.getNeedWorldClass()) {
            return null;
        }
        
        // Check builds
        if (builds.length() != data.getMode()) {
            return null;
        }
        
        // Create build ids
        var buildIds = new LongArrayList();
        
        // Make sure our builds exist
        for (var build : builds) {
            long buildId = build.getBuildId();
            boolean hasBuild = this.getPlayer().getStarTowerManager().hasBuild(buildId);
            
            if (hasBuild == false) {
                return null;
            }
            
            buildIds.add(buildId);
        }
        
        // Create game
        this.game = new VampireSurvivorGame(this, data, buildIds.toLongArray());
        
        // Success
        return this.game;
    }

    public void settle(boolean isWin, int score) {
        // Sanity check
        if (this.game == null) {
            return;
        }
        
        // Save earned cards to the database
        this.updateSavedCards();
        
        // Skip if we didn't win
        if (!isWin) {
            return;
        }
        
        // Get log from database
        var log = this.getProgress().getVampireLog().computeIfAbsent(
            game.getId(),
            id -> new VampireSurvivorLog(id)
        );
        
        // Check if we should update score
        if (score >= log.getScore() || !log.isPassed()) {
            // Set record info
            log.setPassed(isWin);
            log.setScore(score);
            log.setBuilds(game.getBuilds());
            
            // Save record to database if we set any data
            Nebula.getGameDatabase().update(
                this.getProgress(),
                this.getPlayerUid(),
                "vampireLog." + game.getId(),
                log
            );
        }
        
        // Clear game
        this.game = null;
        
        // Trigger achievement
        getPlayer().trigger(AchievementCondition.VampireWithSpecificClearTotal, 1);
    }

    private void updateSavedCards() {
        // Get new cards
        List<Integer> newCards = new ArrayList<>();
        
        for (int card : game.getCards()) {
            if (this.getProgress().getFateCards().contains(card)) {
                continue;
            }
            
            this.getProgress().getFateCards().add(card);
            newCards.add(card);
        }
        
        if (newCards.size() == 0) {
            return;
        }
        
        // Save to database
        Nebula.getGameDatabase().addToSet(this.getProgress(), this.getPlayerUid(), "fateCards", newCards);
        
        // Notify player
        this.getPlayer().addNextPackage(
            NetMsgId.vampire_survivor_talent_node_notify,
            VampireTalentDetailResp.newInstance()
                .setNodes(this.getTalents().toByteArray())
                .setActiveCount(this.getProgress().getFateCards().size())
                .setObtainCount(newCards.size())
        );
    }
}
