# Soul Pouch (Forge 1.20.1)

A Curios pouch for Eidolon: Repraised soul shards.

| Feature | How it works |
|---|---|
| Pickup | Wear the pouch in its own **Soul Pouch** Curios slot. Soul items you walk over go into the pouch, never your inventory (works even with a full inventory). |
| Death | The pouch is always kept. A configurable % of each stored type is lost (or dropped, if `dropLostItems = true`). |
| Soul Enchanter | Opening it fills the shard slot from the pouch, refills after each enchant, returns leftovers on close. |
| Villager trades | Selecting any trade (vanilla, datapack, KubeJS…) whose cost is a pouchable item fills the payment slot from the pouch; leftovers go back on close. |
| Waystones | Optional. Travel cost paid in soul shards instead of XP levels (`SOULS_THEN_XP` default, or `SOULS_ONLY`). Charged only after a successful teleport. |
| HUD | XP-level style counter with item icon. Anchor + offset + color in the client config. |
| Tooltip | Counts per item type; hold Shift for usage help. |
| Removing gems | No GUI. Right-click the pouch in any slot with an empty cursor → takes a stack. Right-click it while carrying shards → stores them. In hand: use = take a stack, sneak-use = store all from inventory. Keybind "Withdraw from Soul Pouch" (unbound by default; sneak = one). |
| Recipe | `" S "`, `"LGL"`, `" L "` → String, Leather, Eidolon Soul Shard. |


## Config

- `world/serverconfig/soulpouch-server.toml` (per world, synced to clients): pickup, capacity, death loss,
  enchanter/trade toggles, Waystones mode, currency and souls-per-level.
- `config/soulpouch-client.toml`: HUD anchor (`HOTBAR_RIGHT`, `HOTBAR_LEFT`, `ABOVE_XP_BAR`, `TOP_LEFT`,
  `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`), offsets, color, icon, hide-when-empty.

Which items the pouch accepts is the item tag `soulpouch:pouchable`
(default: `eidolon:soul_shard`, `eidolon:lesser_soul_gem`). Override it with a datapack to add more.
