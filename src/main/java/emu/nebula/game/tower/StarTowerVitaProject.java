package emu.nebula.game.tower;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.Public.TowerVitaProject;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class StarTowerVitaProject {
    private int id;
    private String name;
    private Map<Integer, StarTowerVitaOption> options;

    @Deprecated // Morphia only
    public StarTowerVitaProject() {

    }

    public StarTowerVitaProject(int id) {
        this.id = id;
        this.name = "Exploration Plan" + id;
        this.options = new HashMap<>();
    }

    public void setName(String name) {
        if (name == null) {
            name = "";
        }

        // Clamp name length to prevent long names
        if (name.length() > 32) {
            name = name.substring(0, 31);
        }

        this.name = name;
    }

    public Map<Integer, StarTowerVitaOption> getOptions() {
        return this.options;
    }

    public void putOption(StarTowerVitaOption option) {
        this.getOptions().put(option.getId(), option);
    }

    public TowerVitaProject toProto() {
        var proto = TowerVitaProject.newInstance()
                .setId(this.getId())
                .setName(this.getName() == null ? "" : this.getName());

        // Sort option ids so the response is deterministic
        this.getOptions().keySet().stream().sorted().forEach(id -> {
            var option = this.getOptions().get(id).toProto();
            if (option != null) {
                proto.addOptions(option);
            }
        });

        return proto;
    }
}
