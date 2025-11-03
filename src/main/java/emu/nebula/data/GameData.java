package emu.nebula.data;

import java.lang.reflect.Field;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import emu.nebula.data.resources.*;

import lombok.Getter;

@SuppressWarnings("unused")
public class GameData {
    @Getter private static DataTable<CharacterDef> CharacterDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterAdvanceDef> CharacterAdvanceDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterSkillUpgradeDef> CharacterSkillUpgradeDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterUpgradeDef> CharacterUpgradeDataTable = new DataTable<>();
    @Getter private static DataTable<CharItemExpDef> CharItemExpDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterSkinDef> CharacterSkinDataTable = new DataTable<>();
    @Getter private static DataTable<TalentGroupDef> TalentGroupDataTable = new DataTable<>();
    @Getter private static DataTable<TalentDef> TalentDataTable = new DataTable<>();
    
    @Getter private static DataTable<DiscDef> DiscDataTable = new DataTable<>();
    @Getter private static DataTable<DiscStrengthenDef> DiscStrengthenDataTable = new DataTable<>();
    @Getter private static DataTable<DiscItemExpDef> DiscItemExpDataTable = new DataTable<>();
    @Getter private static DataTable<DiscPromoteDef> DiscPromoteDataTable = new DataTable<>();
    @Getter private static DataTable<DiscPromoteLimitDef> DiscPromoteLimitDataTable = new DataTable<>();
    
    @Getter private static DataTable<ItemDef> ItemDataTable = new DataTable<>();
    @Getter private static DataTable<ProductionDef> ProductionDataTable = new DataTable<>();
    @Getter private static DataTable<PlayerHeadDef> PlayerHeadDataTable = new DataTable<>();
    @Getter private static DataTable<TitleDef> titleDataTable = new DataTable<>();
    
    @Getter private static DataTable<MallMonthlyCardDef> MallMonthlyCardDataTable = new DataTable<>();
    @Getter private static DataTable<MallPackageDef> MallPackageDataTable = new DataTable<>();
    @Getter private static DataTable<MallShopDef> MallShopDataTable = new DataTable<>();
    @Getter private static DataTable<MallGemDef> MallGemDataTable = new DataTable<>();
    
    @Getter private static DataTable<DictionaryTabDef> DictionaryTabDataTable = new DataTable<>();
    @Getter private static DataTable<DictionaryEntryDef> DictionaryEntryDataTable = new DataTable<>();
    
    @Getter private static DataTable<DailyInstanceDef> DailyInstanceDataTable = new DataTable<>();
    @Getter private static DataTable<DailyInstanceRewardGroupDef> DailyInstanceRewardGroupDataTable = new DataTable<>();
    @Getter private static DataTable<RegionBossLevelDef> RegionBossLevelDataTable = new DataTable<>();
    @Getter private static DataTable<SkillInstanceDef> SkillInstanceDataTable = new DataTable<>();
    @Getter private static DataTable<CharGemInstanceDef> CharGemInstanceDataTable = new DataTable<>();
    @Getter private static DataTable<WeekBossLevelDef> WeekBossLevelDataTable = new DataTable<>();
    
    @Getter private static DataTable<GachaDef> GachaDataTable = new DataTable<>();
    @Getter private static DataTable<GachaStorageDef> GachaStorageDataTable = new DataTable<>();
    
    @Getter private static DataTable<WorldClassDef> WorldClassDataTable = new DataTable<>();
    @Getter private static DataTable<GuideGroupDef> GuideGroupDataTable = new DataTable<>();
    @Getter private static DataTable<HandbookDef> HandbookDataTable = new DataTable<>();
    @Getter private static DataTable<StoryDef> StoryDataTable = new DataTable<>();
    @Getter private static DataTable<StorySetSectionDef> StorySetSectionDataTable = new DataTable<>();
    
    @Getter private static DataTable<StarTowerDef> StarTowerDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerStageDef> StarTowerStageDataTable = new DataTable<>();
    @Getter private static DataTable<PotentialDef> PotentialDataTable = new DataTable<>();
}
