package id.nereus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Daily reward dengan streak. */
public class Daily {
    private final Nereus p;
    public Daily(Nereus p) { this.p = p; }

    public long cdMs() { return p.getConfig().getLong("daily.cooldown-hours", 24) * 3600000L; }
    public long resetMs() { return p.getConfig().getLong("daily.streak-reset-hours", 48) * 3600000L; }

    public int cycle() {
        ConfigurationSection r = p.getConfig().getConfigurationSection("daily.rewards");
        return Math.max(1, r == null ? 1 : r.getKeys(false).size());
    }
    public int streak(Player pl) {
        UUID u = pl.getUniqueId();
        long last = p.store.getLong(u, "daily.last", 0);
        if (last == 0 || System.currentTimeMillis() - last > resetMs()) return 0;
        return p.store.getInt(u, "daily.streak", 0);
    }
    public long remaining(Player pl) {
        long last = p.store.getLong(pl.getUniqueId(), "daily.last", 0);
        return Math.max(0, last + cdMs() - System.currentTimeMillis());
    }
    public int nextDay(Player pl) { return (streak(pl) % cycle()) + 1; }

    public boolean claim(Player pl) {
        long rem = remaining(pl);
        if (rem > 0) { p.send(pl, "daily-wait", "{time}", p.fmt(rem)); return false; }
        int s = streak(pl) + 1;
        p.store.set(pl.getUniqueId(), "daily.last", System.currentTimeMillis());
        p.store.set(pl.getUniqueId(), "daily.streak", s);
        int day = ((s - 1) % cycle()) + 1;
        ConfigurationSection r = p.getConfig().getConfigurationSection("daily.rewards." + day);
        String disp = "-";
        if (r != null) {
            p.giveMoney(pl, r.getDouble("money", 0));
            p.runCmds(pl, r.getStringList("commands"));
            disp = r.getString("display", "-");
        }
        p.send(pl, "daily-claimed", "{day}", String.valueOf(day), "{streak}", String.valueOf(s), "{reward}", disp);
        return true;
    }
}
