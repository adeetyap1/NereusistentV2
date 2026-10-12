package id.nereus;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * NereusThrone - Custom join/leave, AFK reward, Daily, Kits & Menu.
 * Developer: naook1/adityap
 */
public class Nereus extends JavaPlugin implements TabExecutor {
    public static final String DEV = "naook1/adityap";

    public Store store;
    public Afk afk;
    public Menus menus;
    public Streams streams;
    public Daily daily;
    private Economy econ;
    private boolean papi;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new Store(this);
        papi = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Economy> r = getServer().getServicesManager().getRegistration(Economy.class);
            if (r != null) econ = r.getProvider();
        }
        afk = new Afk(this);
        menus = new Menus(this);
        streams = new Streams(this);
        daily = new Daily(this);
        getServer().getPluginManager().registerEvents(afk, this);
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(streams, this);
        afk.start();
        for (String c : new String[]{"nereus", "afk"}) {
            getCommand(c).setExecutor(this);
            getCommand(c).setTabCompleter(this);
        }
        if (papi) new NereusExpansion(this).register();
        Bukkit.getScheduler().runTaskTimer(this, () -> store.save(), 1200L, 1200L);
        getLogger().info("NereusThrone aktif | Vault=" + (econ != null) + " PAPI=" + papi + " | Developer: " + DEV);
    }

    @Override
    public void onDisable() { if (store != null) store.save(); }

    // ---------- helpers ----------
    /** Teks GUI (small caps otomatis jika small-caps.gui: true). */
    public String t(String s) {
        if (s == null) return "";
        return Txt.color(getConfig().getBoolean("small-caps.gui", true) ? Txt.sc(s) : s);
    }
    public String m(String s) {
        if (s == null) return "";
        return Txt.color(getConfig().getBoolean("small-caps.messages", false) ? Txt.sc(s) : s);
    }
    public String rep(String s, String... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace(kv[i], kv[i + 1]);
        return s;
    }
    public String msg(String key, String... kv) {
        String body = getConfig().getString("messages." + key, key);
        return rep(Txt.color(getConfig().getString("settings.prefix", "")) + m(body), kv);
    }
    public void send(Player pl, String key, String... kv) { pl.sendMessage(msg(key, kv)); }
    public String papi(Player pl, String s) {
        return papi ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(pl, s) : s;
    }
    public void giveMoney(Player pl, double amt) {
        if (econ != null && amt > 0) econ.depositPlayer(pl, amt);
    }
    public void runCmds(Player pl, List<String> cmds) {
        for (String c : cmds) {
            c = papi(pl, c.replace("{player}", pl.getName()));
            if (c.startsWith("[player] ")) pl.performCommand(c.substring(9));
            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.startsWith("[console] ") ? c.substring(10) : c);
        }
    }
    public String fmt(long ms) {
        long s = Math.max(0, ms) / 1000, h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return h + "j " + m + "m";
        if (m > 0) return m + "m " + sec + "d";
        return sec + "d";
    }

    // ---------- commands ----------
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (c.getName().equalsIgnoreCase("afk")) {
            if (!(s instanceof Player pl)) { s.sendMessage("Hanya untuk pemain."); return true; }
            if (!pl.hasPermission("nereus.afk")) { send(pl, "no-permission"); return true; }
            if (afk.isAfk(pl)) afk.unset(pl);
            else afk.set(pl, a.length > 0 ? String.join(" ", a) : getConfig().getString("afk.default-reason", "AFK"));
            return true;
        }
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            if (!s.hasPermission("nereus.admin")) { s.sendMessage(msg("no-permission")); return true; }
            reloadConfig();
            s.sendMessage(msg("reloaded"));
            return true;
        }
        if (a.length > 0 && a[0].equalsIgnoreCase("about")) {
            s.sendMessage(Txt.color("&#7FDBFF&l" + Txt.sc("nereusthrone") + " &8v" + getDescription().getVersion()));
            s.sendMessage(Txt.color("&7" + Txt.sc("developer") + " &8» &f" + DEV));
            return true;
        }
        if (!(s instanceof Player pl)) { s.sendMessage("Hanya untuk pemain."); return true; }
        if (!pl.hasPermission("nereus.use")) { send(pl, "no-permission"); return true; }
        if (a.length == 0) { menus.main(pl); return true; }
        switch (a[0].toLowerCase()) {
            case "join" -> menus.styles(pl, true);
            case "leave" -> menus.styles(pl, false);
            case "daily" -> menus.daily(pl);
            case "kits" -> menus.kits(pl);
            case "afk" -> menus.afkMenu(pl);
            case "info" -> menus.info(pl);
            default -> menus.main(pl);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (c.getName().equalsIgnoreCase("nereus") && a.length == 1) {
            for (String x : new String[]{"join", "leave", "daily", "kits", "afk", "info", "about", "reload"})
                if (x.startsWith(a[0].toLowerCase())) out.add(x);
        }
        return out;
    }
}
