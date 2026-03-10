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
- `/fly [player]`
  Toggles flight for yourself, or for another player if you have admin permission.
- `/setspawn`
  Sets the server spawn to your current location.
- `/spawn [player]`
  Teleports you, or another player, to the configured server spawn.
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

### `/setspawn`
- Description: Sets the server spawn to your current location.
- Player only: yes
- Permission: `foxcore.setspawn`
- Notes:
- Updates the spawn location stored in `config.yml`.

### `/spawn [player]`
- Description: Teleports to the configured server spawn.
- Player only: self use yes, console no
- Permission: `foxcore.spawn`
- Notes:
- `/spawn` teleports yourself to spawn.
- `/spawn <player>` teleports another online player to spawn and requires `foxcore.spawn.others`.
- If spawn is disabled or unset, the command fails cleanly.
- Uses the same safe teleport rules as other FoxCore teleports.

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
### `foxcore.back`
- Default: `op`
- Allows teleporting back to your last saved or death location.

### `foxcore.fly`
- Default: `op`
- Allows toggling your own flight.

### `foxcore.fly.others`
- Default: `op`
- Allows toggling another player's flight.

### `foxcore.fly.world.<worldname>`
- Default: none
- Allows flight in a specific world.
- Example: `foxcore.fly.world.world_nether`

### `foxcore.setspawn`
- Default: `op`
- Allows setting the server spawn.

### `foxcore.spawn`
- Default: `true`
- Allows teleporting yourself to the server spawn.

### `foxcore.spawn.others`
- Default: `op`
- Allows teleporting another player to the server spawn.

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

## Config synchronization
- Bundled YAML files are synchronized on startup and `/foxcore reload`.
- New bundled keys are added automatically.
- Removed bundled keys are removed automatically.
- Existing user values are preserved for keys that still exist.

## Current limitations
- Only `/tp` supports offline target lookup. Other teleport commands require online players.
- Teleport requests are stored in memory only and do not survive a restart or reload.
- Player back-state persistence supports SQLite and MySQL only.
