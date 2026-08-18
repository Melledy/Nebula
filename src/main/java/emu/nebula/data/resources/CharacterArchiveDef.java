package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;

import lombok.Getter;

@Getter
@ResourceType(name = "CharacterArchive.json")
public class CharacterArchiveDef extends BaseDef {
    private int Id;
    private int ArchType;
    private int CharacterId;
    private int UnlockAffinityLevel;
    private int ArchReward;
    private int ArchRewardQuantity;
    
    @Override
    public int getId() {
        return Id;
    }
    
}
