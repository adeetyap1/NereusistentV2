# NereusThrone  (Developer: naook1/adityap)
Paper 1.21.1 | Java 21 | Crossplay Geyser/Floodgate (GUI = inventory biasa)

## Build
    mvn clean package        -> target/NereusThrone-1.0.0.jar  (taruh di /plugins)

## Command
/nereus (alias /nr) [join|leave|daily|kits|afk|info|about|reload]   /afk [alasan]

## Permission (LuckPerms)
- nereus.use, nereus.afk  (default true)   | nereus.admin (reload)
- nereus.join.<id> / nereus.leave.<id>  -> id = nama di config (vip, elite, kaiser, emperor, dst)
  contoh: /lp group vip permission set nereus.join.vip true
- Tambah style baru cukup copy blok di config.yml lalu /nereus reload

## Placeholder (PlaceholderAPI) - pakai di TAB
%nereus_afk% %nereus_afk_status% %nereus_afk_reason% %nereus_afk_time%
%nereus_joinstyle% %nereus_leavestyle% %nereus_developer%

## Catatan
- Command key CrazyCrates di afk.reward.key.command & daily.rewards perlu disesuaikan dgn versi/nama crate-mu.
- Tombol menu DeluxeMenus di menus.main (action "player:dm open <nama_menu>").
