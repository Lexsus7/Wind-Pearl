# WindPearl

**Automatic Ender Pearl and Wind Charge interception mod for Minecraft Fabric.**

WindPearl is a client-side Minecraft mod that automatically throws an Ender Pearl and launches a Wind Charge to intercept it mid-flight.

When the Wind Charge hits the Ender Pearl, the collision can teleport the player to the interception point.

## Features

- **Automatic interception** — throw an Ender Pearl and automatically aim and launch a Wind Charge.
- **Trajectory prediction** — calculates the predicted Ender Pearl trajectory.
- **Automatic aiming** — determines the required yaw and pitch for interception.
- **Single keybind** — activate the entire sequence with one key.
- **Hotbar support** — automatically selects the required items and restores the original selected slot.
- **Singleplayer and multiplayer support** — uses different timing compensation for local worlds and multiplayer servers.
- **Client-side Fabric mod** — designed to run on the Minecraft client.

## Supported Versions

| Minecraft | Mod Loader | Source Code |
|---|---|---|
| 26.3 | Fabric | [26.3-fabric](https://github.com/Lexsus7/Wind-Pearl/tree/26.3-fabric) |
| 26.2 | Fabric | [26.2-fabric](https://github.com/Lexsus7/Wind-Pearl/tree/26.2-fabric) |
| 1.21.11 | Fabric | [1.21.11-fabric](https://github.com/Lexsus7/Wind-Pearl/tree/1.21.11-fabric) |

## How to Use

1. Install Fabric Loader and the required Fabric API.
2. Place the WindPearl `.jar` file in your Minecraft `mods` folder.
3. Put Ender Pearls and Wind Charges in your hotbar.
4. Aim in the direction you want to travel.
5. Press **G** to activate WindPearl.

The mod will automatically throw the Ender Pearl, calculate an interception trajectory, and launch a Wind Charge when a suitable interception is found.

The keybind can be changed in Minecraft's Controls settings under **Wind Pearl**.

## Notes

- Interception accuracy may vary depending on launch angle, server latency, and server configuration.
- Steeper launch angles generally provide more reliable interceptions.
- Multiplayer servers may have restrictions affecting projectile behavior.
- WindPearl does not guarantee a successful interception every time.

## License

**Copyright © 2026 Lexsus7. All Rights Reserved.**

The source code is publicly available for viewing and reference only.

Copying, modifying, redistributing, or incorporating the source code into other projects without explicit permission from the copyright holder is prohibited.

See [LICENSE](LICENSE) for details.

## Author

Developed by **Lexsus7**.

GitHub: https://github.com/Lexsus7

*Note: README.md and LICENSE generated with AI*
