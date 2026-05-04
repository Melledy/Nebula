package emu.nebula.data;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.reflections.Reflections;

import emu.nebula.util.JsonUtils;
import emu.nebula.util.Utils;
import emu.nebula.Nebula;
import emu.nebula.game.achievement.AchievementHelper;

public class ResourceLoader {
    private static boolean loaded = false;

    // Load all resources
    public static void loadAll() {
        // Make sure we don't load more than once
        if (loaded) return;
        
        // Load
        loadResources();
        
        // Add hardcoded achievements params
        AchievementHelper.init();
        
        // Done
        loaded = true;
        Nebula.getLogger().info("Resource loading complete");
    }
    
    public static void loadResources() {
        // Get resource classes and sort
        List<Class<?>> classes = new Reflections(ResourceLoader.class.getPackage().getName())
                .getTypesAnnotatedWith(ResourceType.class)
                .stream()
                .collect(Collectors.toList());

        classes.sort((a, b) -> b.getAnnotation(ResourceType.class).loadPriority().value() - a.getAnnotation(ResourceType.class).loadPriority().value());

        // Load resource
        for (Class<?> def : classes) {
            loadFromResource(def);
        }
    }

    public static void loadFromResource(Class<?> resourceClass) {
        // Load to map
        DataTable<?> table = getTableForResource(GameData.class, resourceClass);
        ResourceType type = resourceClass.getAnnotation(ResourceType.class);
        
        // Sanity check
        if (type == null) {
            return;
        }
        
        int count = 0;
        
        try {
            // Init defs collection
            Iterable<?> defs = null;
            
            // Load resource file
            if (type.useInternal()) {
                // Load from internal resources in jar
                try (var in = ResourceLoader.class.getResourceAsStream("/defs/" + type.name()); var reader = new InputStreamReader(in)) {
                    defs = JsonUtils.loadToList(reader, resourceClass);
                } catch (Exception e) {
                    // Ignored
                }
            } else {
                // Load json from ./resources/bin/ folder
                var json = JsonUtils.loadToMap(Nebula.getConfig().resourceDir + "/bin/" + type.name(), String.class, resourceClass);
                
                // Get json values
                defs = json.values();
            }

            for (Object o : defs) {
                BaseDef res = (BaseDef) o;

                if (res == null) {
                    continue;
                }
                
                res.onLoad();
                
                count++;

                if (table != null) {
                    table.add(o);
                }
            }
        } catch (Exception e) {
            Nebula.getLogger().error("Error loading resource file: {}", type.name(), e);
        }

        Nebula.getLogger().info("Loaded {} {}s.", count, resourceClass.getSimpleName());
    }
    
    // Utility
    
    @SuppressWarnings("unchecked")
    private static <T> DataTable<T> getTableForResource(Class<?> dataClass, Class<T> resourceClass) {
        // Init
        DataTable<T> table = null;
        Field field = null;
        
        // Parse out "Def" in the resource name
        String simpleName = resourceClass.getSimpleName();
        
        if (simpleName.endsWith("Def")) {
            simpleName = simpleName.substring(0, simpleName.length() - 3) + "Data";
        }
        
        // Get table
        try {
            field = dataClass.getDeclaredField(simpleName + "Table");
        } catch (Exception e) {
            try {
                field = dataClass.getDeclaredField(Utils.lowerCaseFirstChar(simpleName) + "Table");
            } catch (Exception ex) {
                
            }
        }
        
        if (field != null) {
            try {
                field.setAccessible(true);
                table = (DataTable<T>) field.get(null);
            } catch (Exception e) {
                // ignore exception
            } finally {
                field.setAccessible(false);
            }
        }

        return table;
    }
}
