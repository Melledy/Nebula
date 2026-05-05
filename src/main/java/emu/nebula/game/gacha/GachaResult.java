package emu.nebula.game.gacha;

import emu.nebula.Nebula;
import emu.nebula.data.resources.GachaDef;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.gacha.GachaPityState.GachaPityDraft;
import emu.nebula.proto.GachaSpin.GachaCard;
import emu.nebula.proto.GachaSpin.GachaSpinResp;
import emu.nebula.proto.Public.ItemTpl;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
public class GachaResult {
    private GachaBannerInfo info;
    private GachaPityDraft pityState;
    private PlayerChangeInfo change;
    private IntList cards;
    
    public GachaResult(GachaBannerInfo info, GachaPityDraft pityState, PlayerChangeInfo change, IntList cards) {
        this.info = info;
        this.pityState = pityState;
        this.change = change;
        this.cards = cards;
    }

    public GachaSpinResp toSpinResp(GachaDef gachaData) {
        int aupGuaranteeTimes = gachaData != null ? gachaData.getDisplayAUpGuaranteeTimes() : 0;

        var rsp = GachaSpinResp.newInstance()
                .setTime(Nebula.getCurrentServerTime())
                .setAMissTimes(this.pityState.missTimesA())
                .setAupMissTimes(this.pityState.missTimesUpA())
                .setTotalTimes(this.info.getTotal())
                .setGachaTotalTimes(this.info.getTotal())
                .setAupGuaranteeTimes(aupGuaranteeTimes)
                .setChange(this.change.toProto());

        for (int id : this.cards) {
            var card = GachaCard.newInstance()
                    .setCard(ItemTpl.newInstance().setTid(id).setQty(1));
            rsp.addCards(card);
        }

        return rsp;
    }

}
