package id.nereus;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/** %nereus_afk%, %nereus_afk_status%, %nereus_afk_reason%, %nereus_afk_time%, %nereus_joinstyle%, %nereus_leavestyle%, %nereus_developer% */
public class NereusExpansion extends PlaceholderExpansion {
    private final Nereus p;
    public NereusExpansion(Nereus p) { this.p = p; }

    @Override public String getIdentifier() { return "nereus"; }
    @Override public String getAuthor() { return Nereus.DEV; }
    @Override public String getVersion() { return p.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player pl, String id) {
        if (pl == null) return "";
        switch (id.toLowerCase()) {
            case "afk": return p.afk.isAfk(pl) ? Txt.color(p.getConfig().getString("placeholders.afk-tag", "&7[AFK]")) : "";
            case "afk_status": return String.valueOf(p.afk.isAfk(pl));
            case "afk_reason": return p.afk.reason(pl);
            case "afk_time": return p.afk.isAfk(pl) ? p.fmt(System.currentTimeMillis() - p.afk.since(pl)) : "";
            case "joinstyle": return styleName(pl, true);
            case "leavestyle": return styleName(pl, false);
            case "developer": return Nereus.DEV;
            default: return null;
        }
    }

    private String styleName(Player pl, boolean join) {
        String id = p.store.get(pl.getUniqueId(), join ? "join" : "leave", "default");
        ConfigurationSection st = p.getConfig().getConfigurationSection((join ? "join-styles." : "leave-styles.") + id);
        return st == null ? id : Txt.color(st.getString("name", id));
    }
}
