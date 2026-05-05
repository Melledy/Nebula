package emu.nebula.game.gacha;

import dev.morphia.annotations.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(useDiscriminator = false)
public class GachaPityState {

    private int missTimesA;
    private int missTimesUpA;
    private int missTimesB;
    private boolean bGuaranteeDebt;

    public GachaPityDraft copyForSpin() {
        return new GachaPityDraft(this.missTimesA, this.missTimesUpA, this.missTimesB, this.bGuaranteeDebt);
    }

    public void overwriteFrom(GachaPityDraft draft) {
        if (draft == null) {
            return;
        }

        this.missTimesA = draft.missTimesA();
        this.missTimesUpA = draft.missTimesUpA();
        this.missTimesB = draft.missTimesB();
        this.bGuaranteeDebt = draft.bGuaranteeDebt();
    }

    public record GachaPityDraft(
            int missTimesA,
            int missTimesUpA,
            int missTimesB,
            boolean bGuaranteeDebt
    ) {
    }

}
