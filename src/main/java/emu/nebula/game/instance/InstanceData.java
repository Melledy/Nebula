package emu.nebula.game.instance;

import emu.nebula.game.inventory.ItemRewardList;
import emu.nebula.game.player.Player;

public interface InstanceData {
    
    public int getId();
    
    public int getNeedWorldClass();
    
    public int getEnergyConsume();
    
    /**
     * Gets the highest star value that can be saved for this instance
     * @return the max star value
     */
    public default int getMaxStar() {
        return 3;
    }
    
    // Item rewards
    
    public ItemRewardList getFirstRewards();
    
    public default ItemRewardList getFirstRewards(int rewardType) {
        return getFirstRewards();
    }
    
    public ItemRewardList getRewards();
    
    public default ItemRewardList getRewards(int rewardType) {
        return getRewards();
    }
    
    /**
     * Checks if the player has enough energy to complete this instance
     * @return true if the player has enough energy
     */
    public default boolean hasEnergy(Player player) {
        return this.hasEnergy(player, 1);
    }
    
    /**
     * Checks if the player has enough energy to complete this instance
     * @return true if the player has enough energy
     */
    public default boolean hasEnergy(Player player, int count) {
        return (this.getEnergyConsume() * count) <= player.getEnergy();
    }
    
}
