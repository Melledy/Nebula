package emu.nebula.data;

import java.lang.reflect.Field;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import emu.nebula.data.custom.CharGemAttrGroupDef;
import emu.nebula.data.resources.*;

import lombok.Getter;

@SuppressWarnings("unused")
public class GameData {
    // ===== Characters =====
    @Getter private static DataTable<CharacterDef> CharacterDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterAdvanceDef> CharacterAdvanceDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterSkillUpgradeDef> CharacterSkillUpgradeDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterUpgradeDef> CharacterUpgradeDataTable = new DataTable<>();
    @Getter private static DataTable<CharItemExpDef> CharItemExpDataTable = new DataTable<>();
    @Getter private static DataTable<CharacterSkinDef> CharacterSkinDataTable = new DataTable<>();
    @Getter private static DataTable<TalentGroupDef> TalentGroupDataTable = new DataTable<>();
    @Getter private static DataTable<TalentDef> TalentDataTable = new DataTable<>();
    
    // Characters: Emblems
    @Getter private static DataTable<CharGemDef> CharGemDataTable = new DataTable<>();
    @Getter private static DataTable<CharGemSlotControlDef> CharGemSlotControlDataTable = new DataTable<>();
    @Getter private static DataTable<CharGemAttrGroupDef> CharGemAttrGroupDataTable = new DataTable<>();
    @Getter private static DataTable<CharGemAttrValueDef> CharGemAttrValueDataTable = new DataTable<>();
    
    // Characters: Affinity
    @Getter private static DataTable<AffinityLevelDef> AffinityLevelDataTable = new DataTable<>();
    @Getter private static DataTable<AffinityGiftDef> AffinityGiftDataTable = new DataTable<>();
    @Getter private static DataTable<PlotDef> PlotDataTable = new DataTable<>();
    
    // Characters: Phone
    @Getter private static DataTable<ChatDef> ChatDataTable = new DataTable<>();
    
    // Characters: Dating
    @Getter private static DataTable<DatingLandmarkDef> DatingLandmarkDataTable = new DataTable<>();
    @Getter private static DataTable<DatingLandmarkEventDef> DatingLandmarkEventDataTable = new DataTable<>();
    @Getter private static DataTable<DatingCharacterEventDef> DatingCharacterEventDataTable = new DataTable<>();
    
    // ===== Discs =====
    @Getter private static DataTable<DiscDef> DiscDataTable = new DataTable<>();
    @Getter private static DataTable<DiscStrengthenDef> DiscStrengthenDataTable = new DataTable<>();
    @Getter private static DataTable<DiscItemExpDef> DiscItemExpDataTable = new DataTable<>();
    @Getter private static DataTable<DiscPromoteDef> DiscPromoteDataTable = new DataTable<>();
    @Getter private static DataTable<DiscPromoteLimitDef> DiscPromoteLimitDataTable = new DataTable<>();
    
    // Discs: Melody items
    @Getter private static DataTable<SecondarySkillDef> SecondarySkillDataTable = new DataTable<>();
    
    // ===== Items =====
    @Getter private static DataTable<ItemDef> ItemDataTable = new DataTable<>();
    @Getter private static DataTable<ProductionDef> ProductionDataTable = new DataTable<>();
    @Getter private static DataTable<PlayerHeadDef> PlayerHeadDataTable = new DataTable<>();
    @Getter private static DataTable<TitleDef> titleDataTable = new DataTable<>();
    @Getter private static DataTable<HonorDef> honorDataTable = new DataTable<>();
    
    // ===== Shops =====
    @Getter private static DataTable<MallMonthlyCardDef> MallMonthlyCardDataTable = new DataTable<>();
    @Getter private static DataTable<MallPackageDef> MallPackageDataTable = new DataTable<>();
    @Getter private static DataTable<MallShopDef> MallShopDataTable = new DataTable<>();
    @Getter private static DataTable<MallGemDef> MallGemDataTable = new DataTable<>();
    
    @Getter private static DataTable<ResidentShopDef> ResidentShopDataTable = new DataTable<>();
    @Getter private static DataTable<ResidentGoodsDef> ResidentGoodsDataTable = new DataTable<>();
    
    // ===== Battle Pass =====
    @Getter private static DataTable<BattlePassDef> BattlePassDataTable = new DataTable<>();
    @Getter private static DataTable<BattlePassLevelDef> BattlePassLevelDataTable = new DataTable<>();
    @Getter private static DataTable<BattlePassQuestDef> BattlePassQuestDataTable = new DataTable<>();
    @Getter private static DataTable<BattlePassRewardDef> BattlePassRewardDataTable = new DataTable<>();
    
    // ===== Commissions =====
    @Getter private static DataTable<AgentDef> AgentDataTable = new DataTable<>();
    
    // ===== Dictionary =====
    @Getter private static DataTable<DictionaryTabDef> DictionaryTabDataTable = new DataTable<>();
    @Getter private static DataTable<DictionaryEntryDef> DictionaryEntryDataTable = new DataTable<>();
    
    // ===== Gacha =====
    @Getter private static DataTable<GachaDef> GachaDataTable = new DataTable<>();
    @Getter private static DataTable<GachaStorageDef> GachaStorageDataTable = new DataTable<>();
    
    // ===== Story =====
    @Getter private static DataTable<StoryDef> StoryDataTable = new DataTable<>();
    @Getter private static DataTable<StorySetSectionDef> StorySetSectionDataTable = new DataTable<>();
    @Getter private static DataTable<StoryEvidenceDef> StoryEvidenceDataTable = new DataTable<>();
    
    // ===== Daily Quests =====
    @Getter private static DataTable<DailyQuestDef> DailyQuestDataTable = new DataTable<>();
    @Getter private static DataTable<DailyQuestActiveDef> DailyQuestActiveDataTable = new DataTable<>();
    
    // ===== Achievements =====
    @Getter private static DataTable<AchievementDef> AchievementDataTable = new DataTable<>();
    
    // ===== Tutorials =====
    @Getter private static DataTable<TutorialLevelDef> TutorialLevelDataTable = new DataTable<>();
    
    // ===== Instances =====
    @Getter private static DataTable<DailyInstanceDef> DailyInstanceDataTable = new DataTable<>();
    @Getter private static DataTable<DailyInstanceRewardGroupDef> DailyInstanceRewardGroupDataTable = new DataTable<>();
    @Getter private static DataTable<RegionBossLevelDef> RegionBossLevelDataTable = new DataTable<>();
    @Getter private static DataTable<SkillInstanceDef> SkillInstanceDataTable = new DataTable<>();
    @Getter private static DataTable<CharGemInstanceDef> CharGemInstanceDataTable = new DataTable<>();
    @Getter private static DataTable<WeekBossLevelDef> WeekBossLevelDataTable = new DataTable<>();
    
    // ===== Star Tower =====
    @Getter private static DataTable<StarTowerDef> StarTowerDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerStageDef> StarTowerStageDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerGrowthNodeDef> StarTowerGrowthNodeDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerFloorExpDef> StarTowerFloorExpDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerTeamExpDef> StarTowerTeamExpDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerEventDef> StarTowerEventDataTable = new DataTable<>();
    @Getter private static DataTable<SubNoteSkillPromoteGroupDef> SubNoteSkillPromoteGroupDataTable = new DataTable<>();
    
    @Getter private static DataTable<PotentialDef> PotentialDataTable = new DataTable<>();
    @Getter private static DataTable<CharPotentialDef> CharPotentialDataTable = new DataTable<>();
    
    @Getter private static DataTable<StarTowerBookFateCardBundleDef> StarTowerBookFateCardBundleDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerBookFateCardQuestDef> StarTowerBookFateCardQuestDataTable = new DataTable<>();
    @Getter private static DataTable<StarTowerBookFateCardDef> StarTowerBookFateCardDataTable = new DataTable<>();
    @Getter private static DataTable<FateCardDef> FateCardDataTable = new DataTable<>();

    // ===== Infinity Tower =====
    @Getter private static DataTable<InfinityTowerLevelDef> InfinityTowerLevelDataTable = new DataTable<>();
    @Getter private static DataTable<InfinityTowerDifficultyDef> InfinityTowerDifficultyDataTable = new DataTable<>();
    
    // ===== Vampire Survivor =====
    @Getter private static DataTable<VampireSurvivorDef> VampireSurvivorDataTable = new DataTable<>();
    @Getter private static DataTable<VampireTalentDef> VampireTalentDataTable = new DataTable<>();
    
    // ===== Score Boss =====
    @Getter private static DataTable<ScoreBossControlDef> ScoreBossControlDataTable = new DataTable<>();
    @Getter private static DataTable<ScoreBossRewardDef> ScoreBossRewardDataTable = new DataTable<>();
    
    // ===== Misc =====
    @Getter private static DataTable<WorldClassDef> WorldClassDataTable = new DataTable<>();
    @Getter private static DataTable<GuideGroupDef> GuideGroupDataTable = new DataTable<>();
    @Getter private static DataTable<HandbookDef> HandbookDataTable = new DataTable<>();
    @Getter private static DataTable<SignInDef> SignInDataTable = new DataTable<>();
    
    // ===== Activity =====
    @Getter private static DataTable<ActivityDef> ActivityDataTable = new DataTable<>();

    // Activity: Tower Defense
    @Getter private static DataTable<TowerDefenseLevelDef> TowerDefenseLevelDataTable = new DataTable<>();
    
    // Activity: Trials
    @Getter private static DataTable<TrialControlDef> TrialControlDataTable = new DataTable<>();
    @Getter private static DataTable<TrialGroupDef> TrialGroupDataTable = new DataTable<>();
    
    // Activity: Levels
    @Getter private static DataTable<ActivityLevelsLevelDef> ActivityLevelsLevelDataTable = new DataTable<>();
    
    // Activity: Task
    @Getter private static DataTable<ActivityTaskDef> ActivityTaskDataTable = new DataTable<>();
    @Getter private static DataTable<ActivityTaskGroupDef> ActivityTaskGroupDataTable = new DataTable<>();
    
    // Activity: Shop
    @Getter private static DataTable<ActivityShopDef> ActivityShopDataTable = new DataTable<>();
    @Getter private static DataTable<ActivityShopControlDef> ActivityShopControlDataTable = new DataTable<>();
    @Getter private static DataTable<ActivityGoodsDef> ActivityGoodsDataTable = new DataTable<>();
}