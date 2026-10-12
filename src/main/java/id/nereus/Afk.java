package id.nereus;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** AFK + AFK reward (uang naik bertahap / key CrazyCrates acak). */
public class Afk implements Listener {
    static class State { long since; String reason; int rewards; }

    private final Nereus p;
    private final Map<UUID, State> afk = new HashMap<>();
    private final Map<UUID, Long> last = new HashMap<>();

    public Afk(Nereus p) { this.p = p; }

    public void start() { Bukkit.getScheduler().runTaskTimer(p, this::tick, 20L, 20L); }

    public boolean isAfk(Player pl) { return afk.containsKey(pl.getUniqueId()); }
    public String reason(Player pl) { State s = afk.get(pl.getUniqueId()); return s == null ? "" : s.reason; }
    public long since(Player pl) { State s = afk.get(pl.getUniqueId()); return s == null ? 0 : s.since; }

    public void set(Player pl, String reason) { set(pl, reason, false); }

    public void set(Player pl, String reason, boolean auto) {
        State s = new State();
        s.since = System.currentTimeMillis();
        s.reason = reason;
        afk.put(pl.getUniqueId(), s);
        String m = p.msg(auto ? "afk-auto" : "afk-on", "{player}", pl.getName(), "{reason}", reason);
        if (p.getConfig().getBoolean("afk.broadcast", true)) Bukkit.broadcastMessage(m); else pl.sendMessage(m);
    }

    public void unset(Player pl) {
        State s = afk.remove(pl.getUniqueId());
        last.put(pl.getUniqueId(), System.currentTimeMillis());
        if (s == null) return;
        String m = p.msg("afk-off", "{player}", pl.getName(), "{time}", p.fmt(System.currentTimeMillis() - s.since),
                "{rewards}", String.valueOf(s.rewards));
        if (p.getConfig().getBoolean("afk.broadcast", true)) Bukkit.broadcastMessage(m); else pl.sendMessage(m);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        int idle = p.getConfig().getInt("afk.auto-afk-seconds", 300);
        for (Player pl : Bukkit.getOnlinePlayers()) {
            UUID u = pl.getUniqueId();
            State s = afk.get(u);
            if (s == null) {
                last.putIfAbsent(u, now);
                if (idle > 0 && pl.hasPermission("nereus.afk") && now - last.get(u) >= idle * 1000L)
                    set(pl, p.getConfig().getString("afk.default-reason", "AFK"), true);
            } else reward(pl, s, now);
        }
    }

    private void reward(Player pl, State s, long now) {
        ConfigurationSection r = p.getConfig().getConfigurationSection("afk.reward");
        if (r == null || !r.getBoolean("enabled", true)) return;
        String perm = r.getString("permission", "");
        if (!perm.isEmpty() && !pl.hasPermission(perm)) return;
        if (r.getStringList("disabled-worlds").contains(pl.getWorld().getName())) return;
        int interval = Math.max(1, r.getInt("interval-minutes", 5));
        long minutes = (now - s.since) / 60000L;
        if (minutes < (long) (s.rewards + 1) * interval) return;
        int max = r.getInt("max-per-session", 0);
        if (max > 0 && s.rewards >= max) return;
        s.rewards++;

        ConfigurationSection key = r.getConfigurationSection("key");
        boolean keyEnabled = key != null && key.getBoolean("enabled", true);
        if (keyEnabled && ThreadLocalRandom.current().nextDouble() * 100 < key.getDouble("chance", 20)) {
            String crate = pick(key.getConfigurationSection("pool"));
            if (crate != null) {
                p.runCmds(pl, java.util.List.of(key.getString("command", "crazycrates give virtual {crate} 1 {player}")
                        .replace("{crate}", crate)));
                p.send(pl, "afk-reward-key", "{crate}", crate, "{stage}", String.valueOf(s.rewards));
                return;
            }
        }
        double base = r.getDouble("money.base", 1000), inc = r.getDouble("money.increment", 0), cap = r.getDouble("money.max", 0);
        double money = base + (s.rewards - 1) * inc;
        if (cap > 0) money = Math.min(money, cap);
        p.giveMoney(pl, money);
        p.send(pl, "afk-reward-money", "{money}", String.format("%,.0f", money), "{stage}", String.valueOf(s.rewards));
    }

    private String pick(ConfigurationSection pool) {
        if (pool == null || pool.getKeys(false).isEmpty()) return null;
        double total = 0;
        for (String k : pool.getKeys(false)) total += pool.getDouble(k);
        double r = ThreadLocalRandom.current().nextDouble() * total;
        for (String k : pool.getKeys(false)) { r -= pool.getDouble(k); if (r < 0) return k; }
        return pool.getKeys(false).iterator().next();
    }

    // ---------- aktivitas ----------
    private void activity(Player pl) {
        last.put(pl.getUniqueId(), System.currentTimeMillis());
        if (afk.containsKey(pl.getUniqueId())) unset(pl);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        activity(e.getPlayer());
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player pl = e.getPlayer();
        Bukkit.getScheduler().runTask(p, () -> activity(pl));
    }
    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        String m = e.getMessage().toLowerCase();
        if (m.equals("/afk") || m.startsWith("/afk ")) return;
        activity(e.getPlayer());
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) activity(e.getPlayer());
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent e) { last.put(e.getPlayer().getUniqueId(), System.currentTimeMillis()); }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) { afk.remove(e.getPlayer().getUniqueId()); last.remove(e.getPlayer().getUniqueId()); }
}
