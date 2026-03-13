# FoxCore

Lightweight essentials-style utilities for Paper `1.21.11`.

## Requirements
- Java `21`
- Paper `1.21.11`

## Build
```powershell
.\gradlew.bat build
```

Built jar output:
- `build/libs/foxcore-0.1.0-SNAPSHOT.jar`

## Current functionality
- `/afk`
  Toggles your AFK state manually.
- `/back`
  Teleports you back to your newest permitted back location.
- `/back teleport` or `/back death`
  Teleports you specifically to your last teleport/disconnect back location or your last death location.
- scheduled broadcasts
  Sends formatted server broadcast messages automatically on a timer using random selection without immediate repeats.
- `/broadcast <message...>` or `/bc <message...>`
  Sends a formatted manual broadcast to all online players and console.
- `/anvil`
  Opens a virtual anvil.
- `/cartographytable` or `/cartography`
  Opens a virtual cartography table.
- `/craft`, `/wb` or `/workbench`
  Opens a virtual crafting table.
- automatic AFK tracking
  Flags idle players as AFK, optionally kicks them after a longer idle period, and exposes AFK state to PlaceholderAPI when installed.
- `/delhome <home>`
  Deletes one of your saved homes.
- `/delhome <player> <home>`
  Admin form for deleting another player's home.
- `/day`, `/night`, `/sun`, `/rain`
  Set time or weather in your current world.
- `/enderchest` or `/ec`
  Opens your ender chest.
- `/fly [player]`
  Toggles flight for yourself, or for another player if you have admin permission.
- `/gms [player]`, `/gmc [player]`, `/gma [player]`, `/gmsp [player]`
  Gamemode shortcuts for yourself or another player.
- `/grindstone`
  Opens a virtual grindstone.
- `/hat`
  Puts the item in your hand on your head slot.
- `/head [player] [amount]` or `/skull [player] [amount]`
  Gives you a player head by name.
- `/help` or `/commands`
  Opens a player help GUI with only the commands and features available to you.
- `/sethome [name]`
  Saves your current location as a named home.
- `/renamehome <old> <new>`
  Renames one of your saved homes.
- `/sethomeicon <name> [material]`
  Changes the icon used for a home in the homes GUI.
- `/home [name]`
  Opens your homes menu, or teleports to a specific saved home.
- `/homes [player]`
  Lists your homes, or another player's homes with admin permission.
- `/onlinetime [player]` or `/playtime [player]`
  Shows current session time, total playtime, and first join date.
- `/portal ...`
  Admin-only portal management for hub navigation portals with wand selection, built-in `spawn`/`warp`/`rtp` actions, optional console commands, and visible particles.
- `/loom`
  Opens a virtual loom.
- `/warp`
  Opens the public warp GUI, teleports to a named warp, or manages your own player-owned warps.
- `/setwarp <name>`
  Creates or updates a server-owned warp.
- `/delwarp <name>`
  Deletes a server-owned warp.
- `/adminwarp ...`
  Admin management commands for editing any warp.
- `/rtp`
  Opens a world menu and teleports you to a random safe location in the selected world.
- `/seen <playername>`
  Shows when a player was last online and how many days ago that was.
- `/whois <playername>`
  Shows admin profile details for a player, including clickable copy and teleport actions.
- `/setspawn`
  Sets the server spawn to your current location.
- `/speed <1-10> [player]`
  Sets flight speed for yourself or another online player.
- `/smithingtable` or `/smithing`
  Opens a virtual smithing table.
- `/spawn [player]`
  Teleports you, or another player, to the configured server spawn.
- `/stonecutter`
  Opens a virtual stonecutter.
- `/tp <player>`
  Teleports you to an online player.
- `/tphere <player>`
  Teleports an online player to you.
- `/tpa <player>`
  Sends a teleport request that expires after a configurable amount of time.
- `/tpahere <player>`
  Sends a teleport-here request that expires after a configurable amount of time.
- `/tpaccept [player]`
  Accepts the last pending teleport request, or a specific player's request.
- `/tpadeny [player]`
  Denies the last pending teleport request, or a specific player's request.
- `/foxcore reload`
  Reloads config and translations.

## Commands
### `/afk`
- Description: Toggles your AFK state manually.
- Player only: yes
- Permission: `foxcore.afk.command`
- Notes:
- Marks you as AFK immediately without waiting for the idle timer.
- Running `/afk` again clears the manual AFK state.
- Real activity such as movement, chat, commands, interaction, or inventory use also clears manual AFK.

### `/anvil`
- Description: Opens a virtual anvil.
- Player only: yes
- Permission: `foxcore.anvil`

### `/cartographytable` or `/cartography`
- Description: Opens a virtual cartography table.
- Player only: yes
- Permission: `foxcore.cartographytable`

### `/craft`, `/wb` or `/workbench`
- Description: Opens a virtual crafting table.
- Player only: yes
- Permission: `foxcore.craft`

### Automatic AFK
- Description: Tracks idle players automatically without a command.
- Player only: no
- Permissions:
- `foxcore.afk.bypass`
- `foxcore.afk.bypass-kick`
- Notes:
- One periodic check scans online players using configurable timings from `config.yml`.
- Explicit actions such as chat, commands, inventory clicks, interactions, combat, teleports, and block changes reset activity.
- Movement only resets activity after a real block-position change and ignores passive movement such as water drift, bubble columns, vehicles, gliding, and flight.
- Players with `foxcore.afk.bypass` are excluded from AFK flagging and AFK kicks.
- Players with `foxcore.afk.bypass-kick` are still flagged AFK normally but are not kicked by the AFK timeout.
- Players detected as vanished through FoxCore's PlaceholderAPI vanish check are also excluded from the AFK system entirely.
- The bypass is explicit and negative-style: `op` alone does not bypass AFK.
- If PlaceholderAPI is installed, FoxCore registers `%foxcore_afk%`, `%foxcore_afk_status%`, `%foxcore_afk_duration_seconds%`, and `%foxcore_afk_duration_human%`.

### `/back`, `/back teleport`, and `/back death`
- Description: Teleports you back to your most recent permitted back location, or a specific back location type.
- Player only: yes
- Permissions:
- `foxcore.back.teleport`
- `foxcore.back.death`
- Notes:
- `/back` chooses the newest saved destination you are allowed to use.
- `/back teleport` uses only your saved teleport/disconnect back location and requires `foxcore.back.teleport`.
- `/back death` uses only your saved death location and requires `foxcore.back.death`.
- Data survives disconnects, restarts, and reloads.
- If you are flying, you are kept in the air.
- If you are not flying, FoxCore tries to place you on safe ground and cancels the teleport if none is found.
- Supports tab completion for `teleport` and `death`.

### Scheduled Broadcasts
- Description: Sends configured server-wide broadcasts automatically on a repeating timer.
- Player only: no
- Permission: none
- Notes:
- Broadcast entries can be single-line or multi-line.
- Broadcast content supports MiniMessage formatting.
- FoxCore supports `%online%`, `%max%`, and `%server%` placeholders inside scheduled and manual broadcasts.
- In `random` mode, FoxCore avoids repeating the same scheduled broadcast twice in a row when multiple broadcasts are configured.

### `/broadcast <message...>` or `/bc <message...>`
- Description: Sends a formatted manual broadcast to all online players and console.
- Player only: no
- Permission: `foxcore.broadcast`
- Notes:
- Supports MiniMessage formatting such as `<gold>`, `<gray>`, `<red>`, and similar tags used elsewhere in FoxCore.
- Manual broadcasts are currently single-line.

### `/delhome <home>` and `/delhome <player> <home>`
- Description: Deletes saved homes.
- Player only: self use yes, console only for admin delete
- Permission: `foxcore.delhome`
- Notes:
- `/delhome <home>` deletes one of your own homes.
- `/delhome <player> <home>` deletes another player's home and requires `foxcore.delhome.others`.
- Admin deletion works from stored database data, so it can target offline players too.

### `/day`, `/night`, `/sun`, `/rain`
- Description: Set time or weather in your current world.
- Player only: yes
- Permissions:
- `foxcore.day`
- `foxcore.night`
- `foxcore.sun`
- `foxcore.rain`
- Notes:
- These shortcuts only affect the world you are currently standing in.
- `/day` sets time to day.
- `/night` sets time to night.
- `/sun` clears weather.
- `/rain` starts rain without thunder.

### `/enderchest` or `/ec`
- Description: Opens your own ender chest.
- Player only: yes
- Permission: `foxcore.enderchest`
- Notes:
- `/ec` is an alias of `/enderchest`.

### `/grindstone`
- Description: Opens a virtual grindstone.
- Player only: yes
- Permission: `foxcore.grindstone`

### `/hat`
- Description: Puts the item in your main hand on your head slot.
- Player only: yes
- Permission: `foxcore.hat`
- Notes:
- Swaps your current helmet back into your hand.
- Rejects empty hands.

### `/head [player] [amount]` or `/skull [player] [amount]`
- Description: Gives you a player head item by player name.
- Player only: yes
- Permission: `foxcore.head`
- Notes:
- With no arguments, gives your own head.
- If the first argument is a number, it is treated as the amount for your own head.
- Amount defaults to `1` and is clamped to a maximum of `64`.

### `/help` or `/commands`
- Description: Opens a player help GUI that lists only the commands available to you.
- Player only: yes
- Permission: `foxcore.help`
- Notes:
- The main menu groups commands into `Teleport`, `Homes`, `Warps`, and `Utility`.
- Categories are hidden when you do not have any matching player commands in them.
- Command entries show a short description, usage, and player-specific extra details where useful.
- The help GUI shows dynamic information for homes, `/back`, `/rtp`, `/warp`, and spawn availability.
- Clicking a command entry closes the GUI and shows that command's description and usage in chat.

### `/fly [player]`
- Description: Toggles flight for yourself or another online player.
- Player only: self use yes, console no
- Permission: `foxcore.fly`
- Notes:
- `/fly` toggles your own flight.
- `/fly <player>` toggles another player's flight and requires `foxcore.fly.others`.
- World access is controlled by `foxcore.fly.world.<worldname>`.
- If a player enters a world without that permission, FoxCore disables their flight automatically.
- Supports tab completion for online players when using the admin form.

### `/gms [player]`, `/gmc [player]`, `/gma [player]`, `/gmsp [player]`
- Description: Sets a gamemode using shortcut commands.
- Player only: self use yes, console only for targeting others
- Permissions:
- `foxcore.gms`, `foxcore.gms.others`
- `foxcore.gmc`, `foxcore.gmc.others`
- `foxcore.gma`, `foxcore.gma.others`
- `foxcore.gmsp`, `foxcore.gmsp.others`
- Notes:
- Without an argument, changes your own gamemode.
- With a player argument, changes another online player's gamemode.
- Uses one generic shortcut executor internally so future shortcuts can reuse the same target and permission handling.

### `/sethome [name]`
- Description: Saves your current location as a home.
- Player only: yes
- Permission: `foxcore.sethome`
- Notes:
- Without an argument, sets the default home named `home`.
- Home names are normalized to lowercase.
- New homes are limited by `homes.default-max-count` unless a higher `foxcore.sethome.limit.<number>` permission is present.
- Homes are stored in the configured database and survive restarts and reloads.

### `/renamehome <old> <new>`
- Description: Renames one of your homes.
- Player only: yes
- Permission: `foxcore.renamehome`
- Notes:
- Preserves the home location and any custom home icon.
- Rejects renaming if the old home does not exist.
- Rejects renaming if the target home name already exists.

### `/sethomeicon <name> [material]`
- Description: Changes the icon shown for a home in the homes GUI.
- Player only: yes
- Permission: `foxcore.sethomeicon`
- Notes:
- With a material argument, uses that item material as the icon.
- Without a material argument, uses the item currently held in your main hand.
- The icon is stored in the database with the home and shown in `/home` and `/homes`.

### `/home [name]`
- Description: Opens your homes menu or teleports to a specific home.
- Player only: yes
- Permission: `foxcore.home`
- Notes:
- Without an argument, opens the same paginated homes GUI as `/homes`.
- With a name, targets one of your named homes directly.
- Uses the same safe teleport rules as other FoxCore teleports.
- Supports tab completion from your loaded home list.

### `/homes [player]`
- Description: Lists saved homes.
- Player only: self use yes, console only for admin lookup
- Permission: `foxcore.homes`
- Notes:
- `/homes` opens a paginated GUI for your homes.
- `/homes <player>` opens a paginated admin GUI for another player's homes and requires `foxcore.homes.others`.
- Clicking a home teleports you to it.
- Admin lookup works from stored database data, so it can inspect offline players too.

### `/onlinetime [player]` or `/playtime [player]`
- Description: Shows current session time, total playtime, and first join date.
- Player only: self use yes, console target use yes
- Permission: `foxcore.onlinetime`
- Notes:
- `/onlinetime` shows your own data.
- `/playtime` is an alias of `/onlinetime`.
- `/onlinetime <player>` requires `foxcore.onlinetime.others`.
- The target form supports online players and cached offline player profiles without doing a blocking lookup.
- Session time is only available while the target is currently online.
- Supports tab completion for online players when using the admin form.

### `/portal ...`
- Description: Creates and manages cuboid navigation portals using a wand or your current position.
- Player only: yes
- Permission: `foxcore.portal.admin`
- Notes:
- `/portal wand` gives you a portal wand.
- Left click with the wand sets `pos1`, and right click sets `pos2`.
- `/portal pos1` and `/portal pos2` set the selection from your current block.
- `/portal create <id>` creates a portal from the current selection.
- `/portal redefine <id>` replaces an existing portal's bounds with the current selection.
- `/portal setaction <id> spawn` sends players to server spawn.
- `/portal setaction <id> warp <name>` sends players to a FoxCore warp.
- `/portal setaction <id> rtp <world>` starts RTP directly into a configured RTP world.
- `/portal setaction <id> command <command...>` runs one console command and replaces `%player%`.
- `/portal setcooldown <id> <seconds>` sets the per-player cooldown after a successful trigger.
- `/portal setparticles <id> <overworld|nether|end|gold|aqua>` changes the particle preset players see near the portal.
- Portals trigger only when a player enters from outside, require leaving before re-triggering, and also respect a per-portal cooldown.
- A short global portal teleport immunity prevents instant portal-to-portal loops.
- `/portal enable`, `/portal disable`, `/portal delete`, `/portal info`, and `/portal list` manage existing portals.

### `/loom`
- Description: Opens a virtual loom.
- Player only: yes
- Permission: `foxcore.loom`

### `/warp`
- Description: Opens the public warp GUI, teleports to a named warp, or manages your own player-owned warps.
- Player only: yes
- Permission: `foxcore.warp`
- Notes:
- `/warp` opens a paginated GUI of all public warps.
- `/warp <name>` teleports directly to a public warp.
- Server warps are shown first in the GUI, then player-owned warps alphabetically.
- `/warp create|delete|rename|movehere|icon|title|description ...` manage your own player-owned warps.
- Warp names are globally unique across both server and player warps.
- Warp titles and descriptions support formatting and are shown in the GUI.
- Player-owned warp management commands:
- `/warp create <name>`
- `/warp delete <name>`
- `/warp rename <old> <new>`
- `/warp movehere <name>`
- `/warp icon <name> [material]`
- `/warp title <name> <title...>`
- `/warp description <name> <description...>`

### `/setwarp <name>`
- Description: Creates or updates a server-owned warp at your current location.
- Player only: yes
- Permission: `foxcore.warp.server.manage`

### `/delwarp <name>`
- Description: Deletes a server-owned warp.
- Player only: no
- Permission: `foxcore.warp.server.manage`

### `/adminwarp ...`
- Description: Admin editing commands for any warp.
- Player only: mostly yes for `/adminwarp movehere` or icon-from-hand usage, otherwise no
- Permission: `foxcore.adminwarp`
- Notes:
- Supports `delete`, `rename`, `movehere`, `icon`, `title`, and `description`.
- Intended for editing player-owned warps or non-standard admin maintenance.
- Admin examples:
- `/adminwarp rename shop spawnshop`
- `/adminwarp movehere market`
- `/adminwarp title market <gold><b>Market</b></gold>`
- `/adminwarp description market <gray>Main server market with <gold>shops</gold>.</gray>`

### `/rtp`
- Description: Opens a menu of configured worlds and teleports you to a random safe location in the one you select.
- Player only: yes
- Permission: `foxcore.rtp`
- Notes:
- Opens a GUI menu with all enabled RTP worlds that are currently available on the server.
- Each world can define its own menu icon in config.
- Uses chunk-based random selection with a bounded number of chunk attempts and samples per chunk.
- Respects the configured cooldown unless the player has `foxcore.rtp.bypasscooldown`.
- Only works in worlds that are explicitly enabled under `rtp.worlds`.
- Uses separate placement logic for Nether worlds so it does not place players above the bedrock roof.
- Rejects dangerous spots such as lava, water, fire, cactus, campfires, powder snow, and leaves.

### `/seen <playername>`
- Description: Shows when a player was last online.
- Player only: no
- Permission: `foxcore.seen`
- Notes:
- Uses online players first, then cached offline player data without forcing a blocking lookup.
- Reports the last seen timestamp and the whole number of days since that date.
- If the target is currently online, the command reports that directly and shows `0` days ago.
- Supports tab completion for currently online players.

### `/whois <playername>`
- Description: Shows detailed admin information about a player profile.
- Player only: no
- Permission: `foxcore.whois`
- Notes:
- Uses online players first, then cached offline player data without forcing a blocking lookup.
- Shows player name, UUID, status, location when available, last seen time, first join, total playtime, and current session time when online.
- Clicking the player name or UUID copies that value to the clipboard.
- Clicking the location runs `/tp <player>` so admins can jump to the target's current or stored last location using the existing teleport rules.
- Intended as an admin command and defaults to `op`.

### `/setspawn`
- Description: Sets the server spawn to your current location.
- Player only: yes
- Permission: `foxcore.setspawn`
- Notes:
- Updates the spawn location stored in `config.yml`.

### `/speed <1-10> [player]`
- Description: Sets flight speed for yourself or another online player.
- Player only: self use yes, console only for targeting others
- Permission: `foxcore.speed`
- Notes:
- Accepts whole numbers from `1` to `10`.
- `/speed <1-10>` sets your own flight speed.
- `/speed <1-10> <player>` sets another player's flight speed and requires `foxcore.speed.others`.
- The command changes flight speed only, not walking speed.
- Supports tab completion for speed values and online player names on the admin form.

### `/smithingtable` or `/smithing`
- Description: Opens a virtual smithing table.
- Player only: yes
- Permission: `foxcore.smithingtable`

### `/spawn [player]`
- Description: Teleports to the configured server spawn.
- Player only: self use yes, console no
- Permission: `foxcore.spawn`
- Notes:
- `/spawn` teleports yourself to spawn.
- `/spawn <player>` teleports another online player to spawn and requires `foxcore.spawn.others`.
- If spawn is disabled or unset, the command fails cleanly.
- Uses the same safe teleport rules as other FoxCore teleports.

### `/stonecutter`
- Description: Opens a virtual stonecutter.
- Player only: yes
- Permission: `foxcore.stonecutter`

### `/tp <player>`
- Description: Teleport yourself to another online player.
- Player only: yes
- Permission: `foxcore.tp`
- Notes:
- Target must be online.
- Admins with `foxcore.tp.offline` can also teleport to an offline player's stored last location.
- You cannot target yourself.
- Supports tab completion for online players.
- If you are flying, you are teleported in the air.
- If you are not flying, FoxCore tries to place you on safe ground and cancels the teleport if none is found.

### `/tphere <player>`
- Description: Teleport another online player to you.
- Player only: yes
- Permission: `foxcore.tphere`
- Notes:
- Target must be online.
- You cannot target yourself.
- Supports tab completion for online players.
- Flying targets stay in the air.
- Non-flying targets are placed on safe ground or the teleport is cancelled.

### `/tpa <player>`
- Description: Sends a teleport request to another online player.
- Player only: yes
- Permission: `foxcore.tpa`
- Notes:
- Target must be online.
- You cannot target yourself.
- The request expires after `tpa.request-expiration-seconds`.
- Sending a new request to the same target replaces your previous pending request to that target.
- The target receives clickable chat buttons for accept and deny.

### `/tpahere <player>`
- Description: Sends a teleport-here request to another online player.
- Player only: yes
- Permission: `foxcore.tpahere`
- Notes:
- Target must be online.
- You cannot target yourself.
- The request expires after `tpa.request-expiration-seconds`.
- Sending a new teleport-here request to the same target replaces your previous pending teleport-here request to that target.
- The target receives clickable chat buttons for accept and deny.
- Accepting teleports the target to the requester.

### `/tpaccept [player]`
- Description: Accepts a pending teleport request.
- Player only: yes
- Permission: `foxcore.tpaccept`
- Notes:
- Without an argument, accepts the most recent pending request.
- With a player argument, accepts that specific player's request.
- Accepting teleports the requester to you.
- The teleported player stays in the air if flying, otherwise FoxCore requires safe ground.

### `/tpadeny [player]`
- Description: Denies a pending teleport request.
- Player only: yes
- Permission: `foxcore.tpadeny`
- Notes:
- Without an argument, denies the most recent pending request.
- With a player argument, denies that specific player's request.

### `/foxcore reload`
- Description: Reloads the plugin config and translation files.
- Player only: no
- Permission: `foxcore.reload`
- Notes:
- Console can run this command.
- Reload also synchronizes bundled YAML files.

## Permissions
### `foxcore.afk.command`
- Default: `true`
- Allows toggling your own AFK state manually.

### `foxcore.anvil`
- Default: `op`
- Allows opening a virtual anvil.

### `foxcore.cartographytable`
- Default: `op`
- Allows opening a virtual cartography table.

### `foxcore.craft`
- Default: `op`
- Allows opening a virtual crafting table.

### `foxcore.afk.bypass`
- Default: `false`
- Excludes a player from automatic AFK flagging and AFK kicks.
- This permission must be granted explicitly and is not implied by `op`.

### `foxcore.afk.bypass-kick`
- Default: `op`
- Prevents AFK timeout kicks while still allowing the player to be marked AFK normally.

### `foxcore.back`
- Default: `op`
- Reserved base node for `/back` access control if you want to grant it alongside subtype permissions.
- FoxCore itself resolves `/back` using `foxcore.back.teleport` and `foxcore.back.death`.

### `foxcore.back.teleport`
- Default: `op`
- Allows teleporting to your last saved teleport or disconnect back location.

### `foxcore.back.death`
- Default: `op`
- Allows teleporting to your last saved death location.

### `foxcore.delhome`
- Default: `true`
- Allows deleting your own homes.

### `foxcore.delhome.others`
- Default: `op`
- Allows deleting another player's homes.

### `foxcore.day`, `foxcore.night`, `foxcore.sun`, `foxcore.rain`
- Default: `op`
- Allow changing time or weather in your current world with the corresponding shortcut command.

### `foxcore.enderchest`
- Default: `op`
- Allows opening your ender chest.

### `foxcore.grindstone`
- Default: `op`
- Allows opening a virtual grindstone.

### `foxcore.hat`
- Default: `op`
- Allows placing the item in your hand on your head slot.

### `foxcore.head`
- Default: `op`
- Allows giving yourself player heads by name.

### `foxcore.help`
- Default: `true`
- Allows opening the player help GUI with `/help` or `/commands`.

### `foxcore.broadcast`
- Default: `op`
- Allows sending manual server broadcasts with `/broadcast` or `/bc`.

### `foxcore.fly`
- Default: `op`
- Allows toggling your own flight.

### `foxcore.fly.others`
- Default: `op`
- Allows toggling another player's flight.

### `foxcore.gms`, `foxcore.gmc`, `foxcore.gma`, `foxcore.gmsp`
- Default: `op`
- Allow setting your own gamemode with the corresponding shortcut command.

### `foxcore.gms.others`, `foxcore.gmc.others`, `foxcore.gma.others`, `foxcore.gmsp.others`
- Default: `op`
- Allow setting another player's gamemode with the corresponding shortcut command.

### `foxcore.home`
- Default: `true`
- Allows teleporting to your own homes.

### `foxcore.homes`
- Default: `true`
- Allows listing your own homes.

### `foxcore.homes.others`
- Default: `op`
- Allows listing another player's homes.

### `foxcore.onlinetime`
- Default: `true`
- Allows viewing your own current session time, total playtime, and first join date.

### `foxcore.onlinetime.others`
- Default: `op`
- Allows viewing another player's current session time, total playtime, and first join date.

### `foxcore.portal.admin`
- Default: `op`
- Allows managing FoxCore portals and using the selection wand.

### `foxcore.loom`
- Default: `op`
- Allows opening a virtual loom.

### `foxcore.warp`
- Default: `true`
- Allows browsing and using public warps.

### `foxcore.warp.create`
- Default: `op`
- Allows creating your own public warps.

### `foxcore.warp.edit`
- Default: `op`
- Allows editing your own public warps.

### `foxcore.warp.limit.<number>`
- Default: none
- Sets the maximum number of player-owned warps a player may create.
- Highest granted numeric limit wins.
- Example: `foxcore.warp.limit.5`

### `foxcore.warp.limit.unlimited`
- Default: `op`
- Allows creating unlimited player-owned warps.

### `foxcore.warp.bypasscooldown`
- Default: `op`
- Allows teleporting to warps without waiting for the configured cooldown.

### `foxcore.warp.server.manage`
- Default: `op`
- Allows creating and deleting server warps.

### `foxcore.adminwarp`
- Default: `op`
- Allows editing any warp through admin commands.

### `foxcore.rtp`
- Default: `op`
- Allows teleporting to a random safe location.

### `foxcore.rtp.bypasscooldown`
- Default: `op`
- Allows using `/rtp` without waiting for the configured cooldown.

### `foxcore.seen`
- Default: `true`
- Allows viewing when a player was last online.

### `foxcore.whois`
- Default: `op`
- Allows viewing detailed player profile information.

### `foxcore.renamehome`
- Default: `true`
- Allows renaming homes.

### `foxcore.sethome`
- Default: `true`
- Allows setting homes.

### `foxcore.sethomeicon`
- Default: `true`
- Allows changing the icon used for homes.

### `foxcore.sethome.limit.<number>`
- Default: none
- Sets the maximum number of homes a player may have.
- Highest granted numeric limit wins.
- Example: `foxcore.sethome.limit.5`

### `foxcore.sethome.limit.unlimited`
- Default: `op`
- Allows setting unlimited homes.

### `foxcore.fly.world.<worldname>`
- Default: none
- Allows flight in a specific world.
- Example: `foxcore.fly.world.world_nether`

### `foxcore.setspawn`
- Default: `op`
- Allows setting the server spawn.

### `foxcore.speed`
- Default: `op`
- Allows setting your own flight speed.

### `foxcore.speed.others`
- Default: `op`
- Allows setting another player's flight speed.

### `foxcore.smithingtable`
- Default: `op`
- Allows opening a virtual smithing table.

### `foxcore.spawn`
- Default: `true`
- Allows teleporting yourself to the server spawn.

### `foxcore.spawn.others`
- Default: `op`
- Allows teleporting another player to the server spawn.

### `foxcore.stonecutter`
- Default: `op`
- Allows opening a virtual stonecutter.

### `foxcore.tp`
- Default: `op`
- Allows teleporting yourself to another online player.

### `foxcore.tp.offline`
- Default: `op`
- Allows teleporting to an offline player's stored last location.

### `foxcore.tphere`
- Default: `op`
- Allows teleporting another player to yourself.

### `foxcore.tpa`
- Default: `op`
- Allows sending teleport requests.

### `foxcore.tpahere`
- Default: `op`
- Allows sending teleport-here requests.

### `foxcore.tpaccept`
- Default: `op`
- Allows accepting teleport requests.

### `foxcore.tpadeny`
- Default: `op`
- Allows denying teleport requests.

### `foxcore.reload`
- Default: `op`
- Allows reloading config and translations.

## Config
File:
- `plugins/foxcore/config.yml`
- `plugins/foxcore/portals.yml`

Current options:
```yml
storage:
  type: sqlite
  sqlite:
    file: storage/foxcore.db
  mysql:
    host: 127.0.0.1
    port: 3306
    database: foxcore
    username: root
    password: ""
    pool-size: 5
    table-prefix: foxcore_

back:
  prioritize-death: true

homes:
  default-max-count: 1

join-messages:
  join-broadcast-enabled: true
  first-join-broadcast-enabled: true
  quit-broadcast-enabled: true
  personal-enabled: true

broadcasts:
  enabled: false
  interval-seconds: 600
  mode: random
  messages:
    - "<gray>[<gold>Discord</gold>]</gray> <yellow>Join our community on <aqua>Discord</aqua>. Use <white>/discord</white> for the invite.</yellow>"
    - "<gray>[<gold>Vote</gold>]</gray> <yellow>Support the server and claim rewards with <white>/vote</white>.</yellow>"
    - "<gray>[<gold>Back</gold>]</gray> <yellow>Died recently? Use <white>/back death</white> to return to your death location if you have access.</yellow>"
    - - "<gray>[<gold>Help</gold>]</gray> <yellow>Need a quick overview of player commands?</yellow>"
      - "<gray>[<gold>Help</gold>]</gray> <yellow>Open the command guide with <white>/foxhelp</white> or <white>/commands</white>.</yellow>"

afk:
  enabled: true
  idle-seconds: 300
  check-interval-seconds: 5
  broadcast-state-changes: true
  kick:
    enabled: true
    after-seconds: 1800

portals:
  enabled: true
  teleport-immunity-seconds: 2
  particles:
    interval-ticks: 10
    view-distance-blocks: 24

spawn:
  enabled: true
  on-respawn: false
  on-join: false
  on-first-join: false
  location:
    world: null
    x: 0.0
    y: 0.0
    z: 0.0
    yaw: 0.0
    pitch: 0.0

translations:
  locale: en

warp:
  teleport-cooldown-seconds: 0

teleport:
  notify-target: true
  vanish-check:
    enabled: true
    placeholders:
      - "%supervanish_isvanished%"
      - "%premiumvanish_isvanished%"
      - "%essentials_vanished%"
  effects:
    enabled: true
    particles:
      enabled: true
    sounds:
      enabled: true

tpa:
  request-expiration-seconds: 60
```

### `translations.locale`
- Controls which bundled translation file is loaded.
- Current bundled locale: `en`
- Translation files are stored in `plugins/foxcore/translations/`.

### `storage.type`
- Selects the persistence backend.
- Supported values: `sqlite`, `mysql`

### `storage.sqlite.file`
- Relative path inside the plugin data folder for the SQLite database file.

### `storage.mysql.*`
- Configures the MySQL connection.
- `storage.mysql.table-prefix` controls the table name prefix used by FoxCore.
- Changing storage backend settings should be followed by a full server restart, not just `/foxcore reload`.

### `back.prioritize-death`
- If `true`, `/back` prefers the death location when it is as recent or newer than the regular saved location.

### `homes.default-max-count`
- Default maximum number of homes for players without a `foxcore.sethome.limit.<number>` permission.
- Replacing an existing home with the same name is always allowed.

### `join-messages.join-broadcast-enabled`
- Enables or disables the broadcast join message shown to other players.

### `join-messages.first-join-broadcast-enabled`
- Enables or disables the special first-join broadcast shown to other players.

### `join-messages.quit-broadcast-enabled`
- Enables or disables the logout broadcast shown to other players.

### `join-messages.personal-enabled`
- Enables or disables the personal multi-line welcome shown to the joining player.
- The default translation includes server info and command hints.

### `broadcasts.enabled`
- Enables or disables scheduled broadcasts entirely.

### `broadcasts.interval-seconds`
- Controls how often FoxCore sends a scheduled broadcast.

### `broadcasts.mode`
- Controls how scheduled broadcasts are selected.
- Supported values: `random`, `sequential`
- In `random` mode, FoxCore avoids broadcasting the same message twice in a row when possible.

### `broadcasts.messages`
- Defines the scheduled broadcast pool.
- Each entry can be either one string or a list of strings for multi-line broadcasts.
- Supports MiniMessage formatting and the `%online%`, `%max%`, and `%server%` placeholders.

### `afk.enabled`
- Enables or disables the automatic AFK system entirely.

### `afk.idle-seconds`
- Controls how long a player must remain inactive before being marked AFK.

### `afk.check-interval-seconds`
- Controls how often FoxCore scans online players for AFK state changes.
- Lower values make AFK state react faster, while higher values reduce work further.

### `afk.broadcast-state-changes`
- If `true`, FoxCore broadcasts AFK enter and leave messages to players and console.

### `afk.kick.enabled`
- Enables or disables AFK kicking.

### `afk.kick.after-seconds`
- Controls how long a player may remain AFK before being kicked.
- Players with `foxcore.afk.bypass` are excluded from the AFK system entirely.
- Players with `foxcore.afk.bypass-kick` still enter AFK but are skipped by the AFK kick timeout.
- Players detected as vanished through FoxCore's PlaceholderAPI vanish check are excluded from the AFK system entirely.

### `portals.enabled`
- Enables or disables portal triggering and portal particles globally.

### `portals.teleport-immunity-seconds`
- Controls the short post-trigger immunity used to prevent portal-to-portal loops.

### `portals.particles.interval-ticks`
- Controls how often FoxCore renders nearby portal particles.

### `portals.particles.view-distance-blocks`
- Controls how close a player must be before they can see portal particles.

### `spawn.enabled`
- Enables or disables the server spawn system entirely.

### `spawn.on-respawn`
- If `true`, players respawn at the configured server spawn.

### `spawn.on-join`
- If `true`, players are teleported to spawn on every join.

### `spawn.on-first-join`
- If `true`, first-time players are teleported to spawn on first join.

### `spawn.location.*`
- Stores the configured server spawn position.
- Usually managed through `/setspawn`.

### `rtp.enabled`
- Enables or disables random teleport globally.

### `rtp.cooldown-seconds`
- Controls how long a player must wait between successful `/rtp` uses.

### `rtp.max-chunk-attempts`
- Maximum number of random chunks FoxCore will try for one `/rtp` request.
- Lower values reduce chunk loading work but increase the chance of failure.

### `rtp.samples-per-chunk`
- Number of random positions checked inside each loaded chunk.
- Higher values improve success rate while reusing the same chunk load.

### `rtp.worlds.<world>.enabled`
- Enables or disables `/rtp` in a specific world.

### `rtp.worlds.<world>.min-radius` and `rtp.worlds.<world>.max-radius`
- Define the random teleport ring in blocks around the configured center point.

### `rtp.worlds.<world>.center-x` and `rtp.worlds.<world>.center-z`
- Set the center point used for random chunk selection in that world.

### `rtp.worlds.<world>.icon`
- Sets the material used for that world in the `/rtp` GUI.
- Defaults are `GRASS_BLOCK` for `world` and `NETHERRACK` for `world_nether`.

### `warp.teleport-cooldown-seconds`
- Controls how long a player must wait after a successful warp teleport before using another warp.
- Players with `foxcore.warp.bypasscooldown` ignore this cooldown.

### `teleport.notify-target`
- If `true`, teleport targets receive a notification message.

### `teleport.vanish-check.enabled`
- Enables or disables PlaceholderAPI-based vanish checks for teleport effects and target notifications.

### `teleport.vanish-check.placeholders`
- Lists PlaceholderAPI vanish placeholders to test.
- If any configured placeholder resolves to `true`, `yes`, `on`, or `1`, FoxCore treats the teleported player as vanished.
- This lets FoxCore suppress teleport particles, sounds, and target notifications without depending on a specific vanish plugin.

### `teleport.effects.enabled`
- Enables or disables FoxCore teleport particles and sounds for successful teleports.

### `teleport.effects.particles.enabled`
- Enables or disables the particle burst shown at the origin and destination of successful teleports.

### `teleport.effects.sounds.enabled`
- Enables or disables the teleport sounds played at the origin and destination of successful teleports.

### `tpa.request-expiration-seconds`
- Controls how long a `/tpa` request remains valid.
- Minimum effective value is `1`.

## Translations
- Bundled translations live in `src/main/resources/translations/`.
- Runtime translations are stored in `plugins/foxcore/translations/`.
- Portal definitions are stored separately in `plugins/foxcore/portals.yml`.
- Current bundled file:
- `messages_en.yml`
- Join/quit broadcasts and the personal welcome lines are editable in `event.join.*` and `event.quit.*`.
- AFK messages are editable in `afk.*`.

## PlaceholderAPI
- FoxCore registers a PlaceholderAPI expansion automatically when PlaceholderAPI is installed.
- Placeholder identifier: `foxcore`
- Supported placeholders:
- `%foxcore_afk%`
- `%foxcore_afk_status%`
- `%foxcore_afk_duration_seconds%`
- `%foxcore_afk_duration_human%`
- `%foxcore_logout_<playername>%`
- `%foxcore_quit_broadcast_<playername>%`
- `%foxcore_login_<playername>%`
- `%foxcore_join_broadcast_<playername>%`
- The join/quit placeholders return plain text so they can be passed to other plugins safely.
- `%foxcore_logout_<playername>%` and `%foxcore_quit_broadcast_<playername>%` are aliases for the configured quit broadcast.
- `%foxcore_login_<playername>%` and `%foxcore_join_broadcast_<playername>%` return the configured join broadcast text.
- The join placeholder uses the first-join broadcast when the target player is cached and known to be a first-time player; otherwise it falls back to the normal join broadcast.

## Warp Formatting
- Warp titles and descriptions use MiniMessage formatting.
- Legacy `&` color codes such as `&6&lTitle` are not parsed for warps.
- Use tags like `<gold>`, `<red>`, `<gray>`, `<b>`, `<italic>`, or `<color:#55cfff>`.
- Example title:
```text
/adminwarp title truhly <gold><b>Truhly</b></gold>
```
- Example light blue title:
```text
/adminwarp title truhly <aqua><b>Truhly</b></aqua>
```
- Example custom hex color:
```text
/adminwarp title truhly <color:#55cfff><b>Truhly</b></color>
```
- Example description:
```text
/warp description market <gray>Main hub with <gold>shops</gold> and <aqua>portals</aqua>.</gray>
```
- Closing tags are recommended for nested formatting:
```text
<gold><b>Title</b></gold>
```

## Config synchronization
- Bundled YAML files are synchronized on startup and `/foxcore reload`.
- New bundled keys are added automatically.
- Removed bundled keys are removed automatically.
- Existing user values are preserved for keys that still exist.

## Current limitations
- Only `/tp` supports offline target lookup. Other teleport commands require online players.
- Teleport requests are stored in memory only and do not survive a restart or reload.
- Player back-state persistence supports SQLite and MySQL only.
- Homes and warps are persisted in the same SQLite/MySQL storage backend as back-location data.
