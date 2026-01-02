package emu.nebula.game.inventory;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public enum ItemSubType {
    Res                 (1),
    Item                (2),
    Char                (3),
    Energy              (4),
    WorldRankExp        (5),
    CharShard           (6),
    Disc                (8),
    TalentStrengthen    (9),
    DiscStrengthen      (12),
    DiscPromote         (13),
    TreasureBox         (17),
    GearTreasureBox     (18),
    SubNoteSkill        (19),
    SkillStrengthen     (24),
    CharacterLimitBreak (25),
    MonthlyCard         (30),
    EnergyItem          (31),
    ComCYO              (32),
    OutfitCYO           (33),
    RandomPackage       (34),
    Equipment           (35),
    FateCard            (37),
    EquipmentExp        (38),
    DiscLimitBreak      (40),
    Potential           (41),
    SpecificPotential   (42),
    Honor               (43),
    CharacterYO         (44),
    PlayHead            (45),
    CharacterSkin       (46),
    SouvenirEnergyItem  (47),
    Build               (48);

    @Getter
    private final int value;
    private final static Int2ObjectMap<ItemSubType> map = new Int2ObjectOpenHashMap<>();

    static {
        for (ItemSubType type : ItemSubType.values()) {
            map.put(type.getValue(), type);
        }
    }

    private ItemSubType(int value) {
        this.value = value;
    }

    public static ItemSubType getByValue(int value) {
        return map.get(value);
    }
}
