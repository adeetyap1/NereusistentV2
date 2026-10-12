package id.nereus;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Penyimpanan data pemain (data.yml). */
public class Store {
    private final Nereus p;
    private final File f;
    private final YamlConfiguration y;
    private boolean dirty;

    public Store(Nereus p) {
        this.p = p;
        p.getDataFolder().mkdirs();
        f = new File(p.getDataFolder(), "data.yml");
        y = YamlConfiguration.loadConfiguration(f);
    }
    private String k(UUID u, String key) { return "players." + u + "." + key; }
    public String get(UUID u, String key, String def) { return y.getString(k(u, key), def); }
    public long getLong(UUID u, String key, long def) { return y.getLong(k(u, key), def); }
    public int getInt(UUID u, String key, int def) { return y.getInt(k(u, key), def); }
    public void set(UUID u, String key, Object v) { y.set(k(u, key), v); dirty = true; }
    public void save() {
        if (!dirty) return;
        try { y.save(f); dirty = false; } catch (IOException e) { p.getLogger().warning("Gagal simpan data.yml: " + e.getMessage()); }
    }
}
