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
- `/back`
  Teleports you back to your last saved location or death location.
- `/anvil`
  Opens a virtual anvil.
- `/cartographytable` or `/cartography`
  Opens a virtual cartography table.
- `/craft`, `/wb` or `/workbench`
  Opens a virtual crafting table.
- `/delhome <home>`
  Deletes one of your saved homes.
- `/delhome <player> <home>`
  Admin form for deleting another player's home.
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
- `/loom`
  Opens a virtual loom.
- `/rtp`
  Opens a world menu and teleports you to a random safe location in the selected world.
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

### `/back`
- Description: Teleports you back to your most recent saved back location.
- Player only: yes
- Permission: `foxcore.back`
- Notes:
- Uses the most recent saved location from teleports, disconnects, or death.
- Death locations can be prioritized by config.
- Data survives disconnects, restarts, and reloads.
- If you are flying, you are kept in the air.
- If you are not flying, FoxCore tries to place you on safe ground and cancels the teleport if none is found.

### `/delhome <home>` and `/delhome <player> <home>`
- Description: Deletes saved homes.
- Player only: self use yes, console only for admin delete
- Permission: `foxcore.delhome`
- Notes:
- `/delhome <home>` deletes one of your own homes.
- `/delhome <player> <home>` deletes another player's home and requires `foxcore.delhome.others`.
- Admin deletion works from stored database data, so it can target offline players too.

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

### `/loom`
- Description: Opens a virtual loom.
- Player only: yes
- Permission: `foxcore.loom`

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
### `foxcore.anvil`
- Default: `op`
- Allows opening a virtual anvil.

### `foxcore.cartographytable`
- Default: `op`
- Allows opening a virtual cartography table.

### `foxcore.craft`
- Default: `op`
- Allows opening a virtual crafting table.

### `foxcore.back`
- Default: `op`
- Allows teleporting back to your last saved or death location.

### `foxcore.delhome`
- Default: `true`
- Allows deleting your own homes.

### `foxcore.delhome.others`
- Default: `op`
- Allows deleting another player's homes.

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

### `foxcore.loom`
- Default: `op`
- Allows opening a virtual loom.

### `foxcore.rtp`
- Default: `op`
- Allows teleporting to a random safe location.

### `foxcore.rtp.bypasscooldown`
- Default: `op`
- Allows using `/rtp` without waiting for the configured cooldown.

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

teleport:
  notify-target: true

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

### `teleport.notify-target`
- If `true`, teleport targets receive a notification message.

### `tpa.request-expiration-seconds`
- Controls how long a `/tpa` request remains valid.
- Minimum effective value is `1`.

## Translations
- Bundled translations live in `src/main/resources/translations/`.
- Runtime translations are stored in `plugins/foxcore/translations/`.
- Current bundled file:
- `messages_en.yml`
- Join/quit broadcasts and the personal welcome lines are editable in `event.join.*` and `event.quit.*`.

## Config synchronization
- Bundled YAML files are synchronized on startup and `/foxcore reload`.
- New bundled keys are added automatically.
- Removed bundled keys are removed automatically.
- Existing user values are preserved for keys that still exist.

## Current limitations
- Only `/tp` supports offline target lookup. Other teleport commands require online players.
- Teleport requests are stored in memory only and do not survive a restart or reload.
- Player back-state persistence supports SQLite and MySQL only.
- Homes are persisted in the same SQLite/MySQL storage backend as back-location data.
