# AGENTS.md

## Project identity
- Project name: `foxcraft`
- Package/group: `me.dragan.foxcore`
- Platform: Paper `1.21.11`
- Language/build: Kotlin with Gradle
- Java target: `21`

## Product direction
- This plugin is a lightweight essentials-style utility plugin for Paper.
- Keep features small, explicit, and safe by default.
- Current first feature set:
- `/tp <player>` teleports the sender to an online player.
- `/foxcore reload` reloads config and translations.

## Non-negotiable constraints
- Performance comes first. Avoid tasks that scale poorly with player count or tick rate.
- Security and correctness matter more than convenience. Do not add shortcuts that create abuse paths.
- No loopholes that could expose private data, bypass permissions, or trigger unsafe server actions.
- Paper-only APIs are acceptable when they reduce complexity or improve reliability.
- Prefer simple UX: clear messages, tab completion, readable configs, and future-friendly clickable chat or GUI flows.

## Architecture rules
- Keep the runtime footprint low: small services, no unnecessary schedulers, no reflection-heavy frameworks.
- Prefer straightforward command handlers and focused utility classes over deep abstraction.
- New features should be isolated behind clear permissions and config flags when appropriate.
- Avoid global mutable state unless there is a strong reason and thread-safety is obvious.
- Reload support must be safe. Re-read config/messages cleanly and avoid duplicate registrations or leaked tasks.

## Command and permission standards
- Every command must have:
- a permission node,
- a clear usage message,
- console/player handling defined explicitly,
- tab completion when useful.
- Permission checks must happen before side effects.
- Do not trust user input. Validate arguments and handle missing/offline targets cleanly.

## Config and translation standards
- User-facing text belongs in translation files, not inline in command logic.
- Store translation files under `src/main/resources/translations/` and in the plugin data folder under `translations/`.
- Keep config names readable and stable.
- Add sane defaults and fail safely when config entries are missing.
- Bundled YAML files should be synchronized on startup/reload so new keys are added and obsolete bundled keys are removed while preserving user-edited values.
- Translation placeholders should be simple and consistently named.

## Performance standards
- Avoid repeated scans of server state in hot paths unless the operation is small and unavoidable.
- Do not run blocking IO on the main thread during normal gameplay.
- If a feature needs async work, keep Bukkit/Paper API access on the correct thread.
- Prefer O(1) or small bounded work for command execution where possible.

## Security standards
- Do not implement permission bypasses for op-only convenience unless explicitly requested.
- Avoid commands that can target arbitrary files, URLs, raw console execution, or unsafe deserialization.
- Treat reloadable files as untrusted input and validate values before applying behavior.
- Prefer exact or clearly defined target resolution to avoid ambiguity or exploitation.

## Coding style
- Keep Kotlin code direct and readable.
- Do not introduce libraries unless they solve a real problem that the JDK, Kotlin stdlib, or Paper API cannot.
- Use comments sparingly and only where logic is non-obvious.
- Favor maintainability over cleverness.

## Delivery expectations
- Maintain a basic `README.md` as a living reference for implemented functionality.
- Update `README.md` whenever functionality changes so it documents commands, permissions, config-relevant behavior, and what each feature does.
- When adding a feature, update:
- command registration if needed,
- permissions,
- config defaults,
- translations,
- `README.md`,
- any relevant documentation.
- If a requested feature risks performance or security, say so and propose the safer design.
