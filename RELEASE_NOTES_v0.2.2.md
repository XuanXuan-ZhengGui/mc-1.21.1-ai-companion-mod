# Release Notes - v0.2.2

## Improvements

- **Player-form rendering**: Companion now uses natural `GameProfile` name display instead of entity name tags, making it look like a real player.
- **Damage system**: Added client-side fall damage detection. Companion takes damage from falls and dies when health reaches zero.
- **Death & respawn**: Companion respawns near the player after a 3-second cooldown upon death.
- **No longer invincible**: Removed forced full-health tick, allowing the companion to take and retain damage.

### Compatibility

- Minecraft 1.21.1
- Fabric Loader >= 0.16.9
- Fabric API 0.102.1+1.21.1
- Java >= 21
