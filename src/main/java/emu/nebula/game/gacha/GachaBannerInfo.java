package emu.nebula.game.gacha;

import dev.morphia.annotations.Entity;

import emu.nebula.data.GameData;
import emu.nebula.data.resources.GachaDef;
import emu.nebula.proto.GachaInformation.GachaInfo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(useDiscriminator = false)
public class GachaBannerInfo {
    private int id;
    
    private int total;
    private boolean usedFirstTen;
    private boolean usedGuarantee;
    
    @Deprecated //Morphia only
    public GachaBannerInfo() {
        
    }
    
    public GachaBannerInfo(GachaDef data) {
        this.id = data.getId();
    }

    public GachaBannerDraft copyForSpin() {
        return new GachaBannerDraft(this.total, this.usedFirstTen, this.usedGuarantee);
    }

    public void overwriteFrom(GachaBannerDraft draft) {
        if (draft == null) {
            return;
        }

        this.total = draft.total();
        this.usedFirstTen = draft.usedFirstTen();
        this.usedGuarantee = draft.usedGuarantee();
    }

    public GachaInfo toProto(GachaPityState pityState) {
        int aupGuaranteeTimes = 0;
        var showFirstTenBonusHintText = false;
        var gachaData = GameData.getGachaDataTable().get(this.getId());
        var storageData = gachaData != null ? gachaData.getStorageData() : null;
        if (storageData != null) {
            aupGuaranteeTimes = gachaData.getDisplayAUpGuaranteeTimes();
            showFirstTenBonusHintText = !storageData.getGiveItemsMap().isEmpty() && !this.isUsedFirstTen();
        }

        int missTimesUpA = 0;
        int missTimesA = 0;
        if (pityState != null) {
            missTimesUpA = pityState.getMissTimesUpA();
            missTimesA = pityState.getMissTimesA();
        }

        return GachaInfo.newInstance()
                .setId(this.getId())
                .setGachaTotalTimes(this.getTotal())
                .setTotalTimes(this.getTotal())
                .setAupMissTimes(missTimesUpA)
                .setAMissTimes(missTimesA)
                .setAupGuaranteeTimes(aupGuaranteeTimes)
                .setReveFirstTenReward(!showFirstTenBonusHintText)
                .setRecvGuaranteeReward(this.isUsedGuarantee());
    }

    public record GachaBannerDraft(
            int total,
            boolean usedFirstTen,
            boolean usedGuarantee
    ) {
    }

}
