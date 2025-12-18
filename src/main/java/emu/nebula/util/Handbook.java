package emu.nebula.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.resources.CharacterDef;
import emu.nebula.data.resources.ItemDef;
import emu.nebula.data.resources.PotentialDef;
import emu.nebula.game.inventory.ItemType;

public class Handbook {
    
    public static void generate() {
        // Temp vars
        List<Integer> list = null;
        
        // Save to file
        String file = "./Nebula Handbook.txt";

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true)) {
            // Format date for header
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            var time = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).format(dtf);
            
            // Header
            writer.println("# Nebula " + GameConstants.getGameVersion() + " Handbook");
            writer.println("# Created " + time);
            
            // Dump characters
            writer.println(System.lineSeparator());
            writer.println("# Characters");
            list = GameData.getCharacterDataTable().keySet().intStream().sorted().boxed().toList();
            var characterLanguageKey = loadLanguageKey(CharacterDef.class);
            for (int id : list) {
                CharacterDef data = GameData.getCharacterDataTable().get(id);
                writer.print(data.getId());
                writer.print(" : ");
                writer.print(characterLanguageKey.getOrDefault(data.getName(), data.getName()));
                writer.print(" (");
                writer.print(data.getElementType().toString());
                writer.println(")");
            }
            
            // Dump items
            writer.println(System.lineSeparator());
            writer.println("# Items");
            list = GameData.getItemDataTable().keySet().intStream().sorted().boxed().toList();
            var itemLanguageKey = loadLanguageKey(ItemDef.class);
            for (int id : list) {
                ItemDef data = GameData.getItemDataTable().get(id);
                writer.print(data.getId());
                writer.print(" : ");
                writer.print(itemLanguageKey.getOrDefault(data.getTitle(), data.getTitle()));
                
                writer.print(" [");
                writer.print(data.getItemType());
                writer.print("]");
                
                if (data.getItemType() == ItemType.Disc) {
                    var discData = GameData.getDiscDataTable().get(id);
                    if (discData != null) {
                        writer.print(" (");
                        writer.print(discData.getElementType().toString());
                        writer.print(")");
                    }
                }
                
                writer.println("");
            }
            
            // Dump potentials
            writer.println(System.lineSeparator());
            writer.println("# Potentials");
            list = GameData.getPotentialDataTable().keySet().intStream().sorted().boxed().toList();
            var potentialLanguageKey = loadLanguageKey(PotentialDef.class);
            for (int id : list) {
                PotentialDef data = GameData.getPotentialDataTable().get(id);
                writer.print(data.getId());
                writer.print(" : ");
                
                CharacterDef charData = GameData.getCharacterDataTable().get(data.getCharId());
                writer.print("[");
                writer.print(characterLanguageKey.getOrDefault(charData.getName(), charData.getName()));
                writer.print("] ");
                
                ItemDef itemData = GameData.getItemDataTable().get(id);
                writer.print(itemLanguageKey.getOrDefault(itemData.getTitle(), itemData.getTitle()));
                writer.print(" - ");
                writer.print(potentialLanguageKey.getOrDefault(data.getBriefDesc(), data.getBriefDesc()));
                writer.println("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static Map<String, String> loadLanguageKey(Class<?> resourceClass) {
        // Get type
        ResourceType type = resourceClass.getAnnotation(ResourceType.class);
        if (type == null) {
            return Map.of();
        }
        
        // Load
        Map<String, String> map = null;
        
        try {
            String fileName = Nebula.getConfig().getResourceDir() + "/language/" + Nebula.getConfig().getLanguage() + "/" + type.name();
            map = JsonUtils.loadToMap(fileName, String.class, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (map == null) {
            return Map.of();
        }
        
        return map;
    }
}
