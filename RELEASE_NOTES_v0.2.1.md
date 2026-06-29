# Release Notes - v0.2.1

## Bug Fixes

- **Fixed crash on world entry**: `tryOpenLan` now waits for `client.player` to be non-null and adds a 2-second delay before calling `openToLan`.
- **Fixed compilation error**: `ClientWorld.addEntity(int, Entity)` does not exist in 1.21.1 Yarn mappings; switched to single-argument `addEntity(Entity)`.
- **Added defensive error handling**: All entity tick/update methods are wrapped in try-catch to prevent client crashes.

### Compatibility

- Minecraft 1.21.1
- Fabric Loader >= 0.16.9
- Fabric API 0.102.1+1.21.1
- Java >= 21
