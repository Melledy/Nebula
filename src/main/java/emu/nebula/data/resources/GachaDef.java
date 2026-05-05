package emu.nebula.data.resources;

import emu.nebula.Nebula;
import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import emu.nebula.util.Utils;
import emu.nebula.util.WeightedList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@ResourceType(name = "Gacha.json", loadPriority = LoadPriority.LOWEST)
public class GachaDef extends BaseDef {
    private int Id;
    private int StorageId;
    private int GachaType;
    
    private int GuaranteeTimes;
    private int GuaranteeTid;
    private int GuaranteeQty;
    private int ATypeGuaranteeTimes;

    private int SpecificTid;
    private int SpecificQty;
    
    private int FirstTenShow;
    private String StartTime;
    private String EndTime;
    
    // Packages
    private int ATypePkg;
    private int BTypePkg;
    private int CTypePkg;
    
    private int ATypeUpPkg;
    private int BTypeUpPkg;
    private int CTypeUpPkg;
    
    private int BGuaranteePkg;
    
    private transient WeightedList<GachaPackage> packageA;
    private transient WeightedList<GachaPackage> packageB;
    private transient WeightedList<GachaPackage> packageC;
    private transient GachaTypeDef typeData;
    private transient IntList allowedCoinItems;
    private transient long startTimeSeconds;
    private transient long endTimeSeconds;
    private transient boolean valid = true;
    
    @Override
    public int getId() {
        return Id;
    }

    public boolean canGuarantee() {
        return this.GuaranteeTimes > 0;
    }
    
    public GachaStorageDef getStorageData() {
        return GameData.getGachaStorageDataTable().get(this.getStorageId());
    }

    public GachaTypeDef getTypeData() {
        if (this.typeData != null) {
            return this.typeData;
        }

        return GameData.getGachaTypeDataTable().get(this.getGachaType());
    }

    public boolean containsAllowedCoinItem(int itemId) {
        return this.allowedCoinItems != null && this.allowedCoinItems.contains(itemId);
    }

    public boolean isActiveAt(long now) {
        return this.valid && now >= this.startTimeSeconds && now <= this.endTimeSeconds;
    }

    public int getDisplayAUpGuaranteeTimes() {
        if (this.ATypeGuaranteeTimes > 0) {
            return this.ATypeGuaranteeTimes;
        }

        var storage = this.getStorageData();
        return storage != null ? storage.getAUpGuaranteeTimes() : 0;
    }

    private boolean hasValidPackage(int packageId) {
        if (packageId <= 0) {
            return false;
        }

        var pkg = GachaPkgDef.getPackageById(packageId);
        return pkg != null && pkg.size() > 0;
    }

    private boolean isSpinConfigValid() {
        boolean hasValidA = (this.ATypePkg > 0 && hasValidPackage(this.ATypePkg))
                || (this.ATypeUpPkg > 0 && hasValidPackage(this.ATypeUpPkg));
        boolean hasValidB = (this.BTypePkg > 0 && hasValidPackage(this.BTypePkg))
                || (this.BGuaranteePkg > 0 && hasValidPackage(this.BGuaranteePkg))
                || (this.BTypeUpPkg > 0 && hasValidPackage(this.BTypeUpPkg));
        boolean hasValidC = this.CTypePkg > 0 && hasValidPackage(this.CTypePkg);

        return this.packageA != null && this.packageA.size() > 0
                && this.packageB != null && this.packageB.size() > 0
                && this.packageC != null && this.packageC.size() > 0
                && hasValidA && hasValidB && hasValidC;
    }

    private void markInvalid(String message) {
        this.valid = false;
        this.startTimeSeconds = 1L;
        this.endTimeSeconds = 0L;
        Nebula.getLogger().error("Skip invalid gacha config for banner {}: {}", this.getId(), message);
    }

    @Override
    public void onLoad() {
        this.valid = true;
        this.startTimeSeconds = 0L;
        this.endTimeSeconds = Long.MAX_VALUE;

        this.packageA = new WeightedList<>();
        this.packageB = new WeightedList<>();
        this.packageC = new WeightedList<>();
        this.allowedCoinItems = new IntArrayList();

        this.typeData = this.getTypeData();
        if (this.typeData == null) {
            markInvalid("invalid GachaType " + this.getGachaType());
            return;
        }
        if (this.typeData.getCoinItem() != null) {
            this.allowedCoinItems.addAll(this.typeData.getCoinItem());
        }

        if (this.StartTime != null && !this.StartTime.isBlank()) {
            this.startTimeSeconds = Utils.dateToMilliseconds(this.StartTime) / 1000;
        }
        if (this.EndTime != null && !this.EndTime.isBlank()) {
            this.endTimeSeconds = Utils.dateToMilliseconds(this.EndTime) / 1000;
        }

        // Get storage
        var storage = this.getStorageData();
        if (storage == null) {
            markInvalid("invalid StorageId " + this.getStorageId());
            return;
        }

        boolean isNewbieBanner = GameData.getGachaNewbieDataTable().containsKey(this.getId());
        if (!isNewbieBanner) {
            // DefaultId can be a dedicated ticket item that is not listed in CoinItem.
            // Only the fallback currency (CostId) is required to be in the type whitelist.
            if (!this.containsAllowedCoinItem(storage.getCostId())) {
                markInvalid("CostId not allowed by GachaType: " + storage.getCostId());
                return;
            }
        }

        // Package A
        if (this.ATypePkg > 0) {
            packageA.add(
                10000 - storage.getATypeUpProb(),
                new GachaPackage(GachaPackageType.A, this.ATypePkg)
            );
        }
        if (this.ATypeUpPkg > 0) {
            packageA.add(
                storage.getATypeUpProb(),
                new GachaPackage(GachaPackageType.A_UP, this.ATypeUpPkg)
            );
        }

        // Package B
        if (this.BTypePkg > 0) {
            packageB.add(
                storage.getBTypeProb(),
                new GachaPackage(GachaPackageType.B, this.BTypePkg)
            );
        }
        if (this.BGuaranteePkg > 0) {
            packageB.add(
                storage.getBTypeGuaranteeProb(),
                new GachaPackage(GachaPackageType.B, this.BGuaranteePkg)
            );
        }
        if (this.BTypeUpPkg > 0) {
            packageB.add(
                storage.getBTypeUpProb(),
                new GachaPackage(GachaPackageType.B_UP, this.BTypeUpPkg)
            );
        }

        // Package C
        if (this.CTypePkg > 0) {
            packageC.add(
                10000,
                new GachaPackage(GachaPackageType.C, this.CTypePkg)
            );
        }

        if (!isSpinConfigValid()) {
            markInvalid("spin package/probability composition is invalid");
        }
    }
    
    @Getter
    public static class GachaPackage {
        private GachaPackageType type;
        private int id;
        
        public GachaPackage(GachaPackageType type, int id) {
            this.type = type;
            this.id = id;
        }
    }
    
    public enum GachaPackageType {
        A,
        A_UP,
        B,
        B_UP,
        C;
    }
}
