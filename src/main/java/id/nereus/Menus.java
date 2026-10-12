package id.nereus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.function.BiConsumer;

/** Semua GUI Nereus (inventory biasa -> kompatibel Bedrock via Geyser/Floodgate). */
public class Menus implements Listener {
    static class H implements InventoryHolder {
        Inventory inv;
        final Map<Integer, BiConsumer<Player, ClickType>> acts = new HashMap<>();
        @Override public Inventory getInventory() { return inv; }
    }

    private final Nereus p;
    public Menus(Nereus p) { this.p = p; }

    // ---------- helpers ----------
    private String P(String s, String... kv) { return p.rep(p.t(s), kv); }
    private List<String> lt(List<String> in, String... kv) {
        List<String> o = new ArrayList<>();
        for (String x : in) o.add(P(x, kv));
        return o;
    }
    private ItemStack item(String mat, String name, List<String> lore, boolean glow) {
        Material m = mat == null ? null : Material.matchMaterial(mat);
        if (m == null || !m.isItem()) m = Material.STONE;
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        if (lore != null) im.setLore(lore);
        im.addItemFlags(ItemFlag.values());
        if (glow) im.addEnchant(Enchantment.UNBREAKING, 1, true);
        it.setItemMeta(im);
        return it;
    }
    private H make(String title, int rows) {
        H h = new H();
        h.inv = Bukkit.createInventory(h, rows * 9, p.t(title));
        String f = p.getConfig().getString("menus.filler", "");
        if (f != null && !f.isEmpty()) {
            ItemStack fill = item(f, " ", null, false);
            for (int i = 0; i < h.inv.getSize(); i++) h.inv.setItem(i, fill);
        }
        return h;
    }
    private List<Integer> inner(int rows) {
        List<Integer> l = new ArrayList<>();
        for (int r = 1; r < rows - 1; r++) for (int c = 1; c < 8; c++) l.add(r * 9 + c);
        return l;
    }
    private void back(H h, int slot) {
        ConfigurationSection b = p.getConfig().getConfigurationSection("menus.back");
        h.inv.setItem(slot, item(b.getString("material", "ARROW"), P(b.getString("name", "&cKembali")), lt(b.getStringList("lore")), false));
        h.acts.put(slot, (pl, ct) -> main(pl));
    }
    private String cfg(String path, String def) { return p.getConfig().getString(path, def); }

    // ---------- MAIN ----------
    public void main(Player pl) {
        ConfigurationSection s = p.getConfig().getConfigurationSection("menus.main");
        H h = make(s.getString("title", "Nereus"), s.getInt("rows", 5));
        ConfigurationSection items = s.getConfigurationSection("items");
        if (items != null) for (String k : items.getKeys(false)) {
            ConfigurationSection i = items.getConfigurationSection(k);
            int slot = i.getInt("slot");
            h.inv.setItem(slot, item(i.getString("material"), P(i.getString("name", k), "{player}", pl.getName()),
                    lt(i.getStringList("lore"), "{player}", pl.getName()), i.getBoolean("glow")));
            String act = i.getString("action", "");
            h.acts.put(slot, (pp, ct) -> doAction(pp, act));
        }
        pl.openInventory(h.inv);
    }

    private void doAction(Player pl, String act) {
        if (act.startsWith("menu:")) {
            switch (act.substring(5)) {
                case "join" -> styles(pl, true);
                case "leave" -> styles(pl, false);
                case "daily" -> daily(pl);
                case "kits" -> kits(pl);
                case "afk" -> afkMenu(pl);
                case "info" -> info(pl);
                default -> main(pl);
            }
        } else if (act.equals("close")) pl.closeInventory();
        else if (act.startsWith("player:")) { pl.closeInventory(); pl.performCommand(act.substring(7)); }
        else if (act.startsWith("console:")) { pl.closeInventory(); p.runCmds(pl, List.of(act.substring(8))); }
    }

    // ---------- JOIN / LEAVE STYLES ----------
    public void styles(Player pl, boolean join) {
        String root = join ? "join-styles" : "leave-styles";
        ConfigurationSection sec = p.getConfig().getConfigurationSection(root);
        List<String> ids = new ArrayList<>(sec.getKeys(false));
        if (ids.size() > 35) ids = ids.subList(0, 35);
        int rows = Math.min(6, (int) Math.ceil(ids.size() / 7.0) + 2);
        H h = make(cfg("menus.styles." + (join ? "join" : "leave") + "-title", "Style"), rows);
        String cur = p.store.get(pl.getUniqueId(), join ? "join" : "leave", "default");
        List<Integer> slots = inner(rows);
        for (int n = 0; n < ids.size(); n++) {
            final String id = ids.get(n);
            final ConfigurationSection st = sec.getConfigurationSection(id);
            int slot = slots.get(n);
            boolean ok = p.streams.allowed(pl, join, id, st);
            boolean sel = id.equalsIgnoreCase(cur);
            List<String> lore = lt(st.getStringList("description"));
            lore.add(" ");
            lore.add(P(cfg("menus.styles.preview-label", "&8Preview:")) + " " + p.streams.format(pl, st));
            lore.add(" ");
            lore.add(P(cfg("menus.styles." + (sel ? "status-selected" : ok ? "status-available" : "status-locked"), "")));
            if (ok) lore.add(P(cfg("menus.styles.hint", "")));
            h.inv.setItem(slot, item(st.getString("material", "PAPER"), P(st.getString("name", id)), lore, sel));
            h.acts.put(slot, (pp, ct) -> {
                if (ct.isRightClick()) { pp.sendMessage(p.streams.format(pp, st)); return; }
                if (!p.streams.allowed(pp, join, id, st)) { p.send(pp, "style-locked"); return; }
                p.store.set(pp.getUniqueId(), join ? "join" : "leave", id);
                p.send(pp, "style-selected", "{style}", ChatColor.stripColor(Txt.color(st.getString("name", id))));
                styles(pp, join);
            });
        }
        back(h, rows * 9 - 5);
        pl.openInventory(h.inv);
    }

    // ---------- DAILY ----------
    public void daily(Player pl) {
        ConfigurationSection rs = p.getConfig().getConfigurationSection("daily.rewards");
        int rows = 4;
        H h = make(cfg("menus.daily.title", "Daily"), rows);
        List<Integer> slots = inner(rows);
        int next = p.daily.nextDay(pl);
        boolean hasStreak = p.daily.streak(pl) > 0;
        long rem = p.daily.remaining(pl);
        int n = 0;
        for (String k : rs.getKeys(false)) {
            if (n >= slots.size()) break;
            ConfigurationSection r = rs.getConfigurationSection(k);
            int day = Integer.parseInt(k);
            String mat; String state; boolean glow = false;
            if (hasStreak && day < next) { mat = cfg("menus.daily.claimed-material", "LIME_STAINED_GLASS_PANE"); state = cfg("menus.daily.status-claimed", ""); }
            else if (day == next) { mat = r.getString("icon", "CHEST"); glow = rem == 0; state = rem == 0 ? cfg("menus.daily.status-ready", "") : cfg("menus.daily.status-wait", ""); }
            else { mat = r.getString("icon", "CHEST"); state = cfg("menus.daily.status-locked", ""); }
            List<String> lore = new ArrayList<>();
            lore.add(P(cfg("menus.daily.reward-line", "&7Hadiah: &f{reward}"), "{reward}", r.getString("display", "-")));
            lore.add(" ");
            lore.add(P(state, "{time}", p.fmt(rem)));
            int slot = slots.get(n++);
            h.inv.setItem(slot, item(mat, P(cfg("menus.daily.day-name", "&eHari {day}"), "{day}", k), lore, glow));
            if (day == next) h.acts.put(slot, (pp, ct) -> { p.daily.claim(pp); daily(pp); });
        }
        List<String> cl = lt(p.getConfig().getStringList("menus.daily.claim-lore"), "{streak}", String.valueOf(p.daily.streak(pl)), "{time}", p.fmt(rem));
        h.inv.setItem(31, item(rem == 0 ? "LIME_DYE" : "GRAY_DYE", P(rem == 0 ? cfg("menus.daily.claim-ready", "&aKlaim") : cfg("menus.daily.claim-wait", "&cTunggu")), cl, rem == 0));
        h.acts.put(31, (pp, ct) -> { p.daily.claim(pp); daily(pp); });
        back(h, 35);
        pl.openInventory(h.inv);
    }

    // ---------- KITS ----------
    public void kits(Player pl) {
        ConfigurationSection list = p.getConfig().getConfigurationSection("kits.list");
        int rows = 4;
        H h = make(cfg("menus.kits.title", "Kits"), rows);
        List<Integer> slots = inner(rows);
        int n = 0;
        ConfigurationSection pk = p.getConfig().getConfigurationSection("kits.playerkits");
        if (pk != null && pk.getBoolean("enabled", true)) {
            int slot = slots.get(n++);
            h.inv.setItem(slot, item(pk.getString("material", "CHEST"), P(pk.getString("name", "PlayerKits")), lt(pk.getStringList("lore")), true));
            String cmd = pk.getString("command", "kit");
            h.acts.put(slot, (pp, ct) -> { pp.closeInventory(); pp.performCommand(cmd); });
        }
        if (list != null) for (String id : list.getKeys(false)) {
            if (n >= slots.size()) break;
            ConfigurationSection k = list.getConfigurationSection(id);
            int slot = slots.get(n++);
            String perm = k.getString("permission", "");
            boolean ok = perm.isEmpty() || pl.hasPermission(perm);
            long cd = k.getLong("cooldown", 0) * 1000L;
            long left = Math.max(0, p.store.getLong(pl.getUniqueId(), "kit." + id, 0) + cd - System.currentTimeMillis());
            List<String> lore = lt(k.getStringList("lore"));
            lore.add(" ");
            lore.add(P(!ok ? cfg("menus.kits.status-locked", "") : left > 0 ? cfg("menus.kits.status-wait", "") : cfg("menus.kits.status-ready", ""), "{time}", p.fmt(left)));
            h.inv.setItem(slot, item(k.getString("material", "CHEST"), P(k.getString("name", id)), lore, ok && left == 0));
            h.acts.put(slot, (pp, ct) -> { claimKit(pp, id, k); kits(pp); });
        }
        back(h, rows * 9 - 5);
        pl.openInventory(h.inv);
    }

    private void claimKit(Player pl, String id, ConfigurationSection k) {
        String perm = k.getString("permission", "");
        if (!perm.isEmpty() && !pl.hasPermission(perm)) { p.send(pl, "kit-locked"); return; }
        long cd = k.getLong("cooldown", 0) * 1000L, now = System.currentTimeMillis();
        long left = p.store.getLong(pl.getUniqueId(), "kit." + id, 0) + cd - now;
        if (left > 0) { p.send(pl, "kit-wait", "{time}", p.fmt(left)); return; }
        p.store.set(pl.getUniqueId(), "kit." + id, now);
        p.runCmds(pl, k.getStringList("commands"));
        p.send(pl, "kit-claimed", "{kit}", ChatColor.stripColor(Txt.color(k.getString("name", id))));
    }

    // ---------- AFK ----------
    public void afkMenu(Player pl) {
        H h = make(cfg("menus.afk.title", "AFK"), 3);
        boolean on = p.afk.isAfk(pl);
        h.inv.setItem(11, item(on ? "LIME_BED" : "RED_BED", P(on ? cfg("menus.afk.toggle-on", "") : cfg("menus.afk.toggle-off", "")),
                lt(p.getConfig().getStringList("menus.afk.toggle-lore")), on));
        h.acts.put(11, (pp, ct) -> {
            if (p.afk.isAfk(pp)) p.afk.unset(pp); else p.afk.set(pp, cfg("afk.default-reason", "AFK"));
            pp.closeInventory();
        });
        ConfigurationSection r = p.getConfig().getConfigurationSection("afk.reward");
        StringBuilder pool = new StringBuilder();
        ConfigurationSection pc = r.getConfigurationSection("key.pool");
        if (pc != null) for (String k : pc.getKeys(false)) pool.append(pool.length() > 0 ? ", " : "").append(k).append(" ").append(pc.getInt(k)).append("%");
        h.inv.setItem(15, item("GOLD_INGOT", P(cfg("menus.afk.reward-name", "")),
                lt(p.getConfig().getStringList("menus.afk.reward-lore"),
                        "{interval}", String.valueOf(r.getInt("interval-minutes")),
                        "{base}", String.format("%,.0f", r.getDouble("money.base")),
                        "{increment}", String.format("%,.0f", r.getDouble("money.increment")),
                        "{max}", String.format("%,.0f", r.getDouble("money.max")),
                        "{chance}", String.valueOf(r.getInt("key.chance")),
                        "{pool}", pool.toString()), false));
        back(h, 22);
        pl.openInventory(h.inv);
    }

    // ---------- INFO ----------
    public void info(Player pl) {
        H h = make(cfg("menus.info.title", "Info"), 3);
        List<String> lore = lt(p.getConfig().getStringList("menus.info.lines"));
        lore.add(" ");
        lore.add(P("&7Developer &8» &f" + Nereus.DEV));
        h.inv.setItem(13, item("NETHER_STAR", P(cfg("menus.info.name", "&bNereusThrone")), lore, true));
        back(h, 22);
        pl.openInventory(h.inv);
    }

    // ---------- listener ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof H h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (!(e.getWhoClicked() instanceof Player pl)) return;
        BiConsumer<Player, ClickType> a = h.acts.get(e.getRawSlot());
        if (a == null) return;
        ClickType ct = e.getClick();
        Bukkit.getScheduler().runTask(p, () -> a.accept(pl, ct));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof H) e.setCancelled(true);
    }
}
