package id.nereus;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text utils: warna (&, &#hex) dan konversi small caps. Developer: naook1/adityap */
public final class Txt {
    private static final String SC = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Txt() {}

    public static String color(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(ChatColor.of("#" + m.group(1)).toString()));
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /** Ubah huruf ke small caps; kode warna, {placeholder} dan %placeholder% dibiarkan. */
    public static String sc(String s) {
        if (s == null) return "";
        StringBuilder o = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length()) {
                if (s.charAt(i + 1) == '#' && i + 8 <= s.length()) { o.append(s, i, i + 8); i += 8; continue; }
                o.append(c).append(s.charAt(i + 1)); i += 2; continue;
            }
            if (c == '{' || c == '%') {
                int e = s.indexOf(c == '{' ? '}' : '%', i + 1);
                if (e > 0) { o.append(s, i, e + 1); i = e + 1; continue; }
            }
            char l = Character.toLowerCase(c);
            if (l >= 'a' && l <= 'z') o.append(SC.charAt(l - 'a')); else o.append(c);
            i++;
        }
        return o.toString();
    }
}
