package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

@Getter
@ResourceType(name = "SoldierEventBattlePool.json")
public class SoldierEventBattlePoolDef extends BaseDef {
    private int Id;
    private int PoolId;
    private int LevelName;
    private int AddDifficult;
    private int weight;
    private int FloorGroup;
    private int coin;
    private int CharacterCount;
    private int StrategyCardCount;

    @Override
    public int getId() {
        return Id;
    }

    public int getWeight() {
        return weight;
    }

    public int getCoin() {
        return coin;
    }
}
