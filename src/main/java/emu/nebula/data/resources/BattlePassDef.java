package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@ResourceType(name = "BattlePass.json")
public class BattlePassDef extends BaseDef {
    private int ID;
    private String Name;
    private String StartTime;
    private String EndTime;
    private String LuxuryProductId;
    private int LuxuryPrice;
    private String LuxuryShowPrice;
    private int LuxuryBonusLevel;
    private int LuxuryTid;
    private int LuxuryQty;
    private String PremiumProductId;
    private int PremiumPrice;
    private String PremiumShowPrice;
    private String ComplementaryProductId;
    private int ComplementaryPrice;
    private String ComplementaryShowPrice;
    private int ComplementaryTid;
    private int ComplementaryQty;
    private String OriginShowPrice;
    private String CoverColor;
    private int Cover;
    private List<Integer> PremiumShowItems;
    private List<Integer> LuxuryShowItems;
    private int OutfitPackageShowItem;
    
    private transient long endTimeTimestamp;
    private transient long startTimeTimestamp;
    
    @Override
    public int getId() {
        return ID;
    }
    
    @Override
    public void onLoad() {
        // Parse start time to timestamp
        try {
            var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            var zonedDateTime = ZonedDateTime.parse(StartTime, formatter);
            this.startTimeTimestamp = zonedDateTime.toInstant().toEpochMilli() / 1000;
        } catch (Exception e) {
            this.startTimeTimestamp = 0;
        }
        
        // Parse end time to timestamp
        try {
            var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            var zonedDateTime = ZonedDateTime.parse(EndTime, formatter);
            this.endTimeTimestamp = zonedDateTime.toInstant().toEpochMilli() / 1000;
        } catch (Exception e) {
            this.endTimeTimestamp = Long.MAX_VALUE;
        }
    }

}
