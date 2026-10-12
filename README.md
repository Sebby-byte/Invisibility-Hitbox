# Invisible Hitboxes 1.0.1 (Fabric, Minecraft 26.3, client-side)

Draws a red hitbox outline around invisible entities (players with Invisibility, invisible armor stands, invisible mobs).
Visual only: nothing is sent to the server and no gameplay changes.

- **H** toggles it (rebind under Options > Controls > Invisible Hitboxes). It starts OFF each launch.
- Range is 64 blocks, but the server only sends entities within its tracking distance.

Build: Java 25, then `gradle build` (Gradle 9.6.0). The jar is in `build/libs/invisiblehitboxes-1.0.1.jar`.
Needs Fabric Loader 0.19.5+ and Fabric API 0.161.0+26.3.
