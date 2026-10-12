package id.nereus;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Custom join & leave message berbasis permission (LuckPerms). */
public class Streams implements Listener {
    private final Nereus p;
    public Streams(Nereus p) { this.p = p; }

    public boolean allowed(Player pl, boolean join, String id, ConfigurationSection st) {
        if (id.equalsIgnoreCase("default")) return true;
        String perm = st.getString("permission", "nereus." + (join ? "join." : "leave.") + id);
        return perm.isEmpty() || pl.hasPermission(perm);
    }

    public String format(Player pl, ConfigurationSection st) {
        String m = st.getString("message", "")
                .replace("{player}", pl.getName())
                .replace("{displayname}", pl.getDisplayName());
        return Txt.color(p.papi(pl, m));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        if (!p.getConfig().getBoolean("join-leave.enabled", true)) return;
        e.setJoinMessage(null);
        announce(e.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        if (!p.getConfig().getBoolean("join-leave.enabled", true)) return;
        e.setQuitMessage(null);
        announce(e.getPlayer(), false);
    }

    private void announce(Player pl, boolean join) {
        String root = join ? "join-styles" : "leave-styles";
        String id = p.store.get(pl.getUniqueId(), join ? "join" : "leave", "default");
        ConfigurationSection st = p.getConfig().getConfigurationSection(root + "." + id);
        if (st == null || !allowed(pl, join, id, st)) {
            id = "default";
            st = p.getConfig().getConfigurationSection(root + ".default");
        }
        if (st == null) return;
        Bukkit.broadcastMessage(format(pl, st));
        String snd = st.getString("sound", "");
        if (!snd.isEmpty()) for (Player o : Bukkit.getOnlinePlayers()) o.playSound(o.getLocation(), snd, 1f, 1f);
    }
}
