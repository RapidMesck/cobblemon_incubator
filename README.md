# Cobblemon Incubator

Cobblemon Incubator is a cross-loader addon for Cobblemon and Cobbreeding that
adds an automated egg incubation machine, breeding-data visualization, filtering,
PC delivery, and inventory automation.

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| Cobblemon | 1.7.0+1.21.1 |
| Cobbreeding | Required |
| Architectury API | 13.0.8 |
| Fabric Loader | 0.17.0+ |
| Fabric API | 0.116.6+1.21.1 |
| Fabric Language Kotlin | 1.13.3+kotlin.2.1.21 |
| NeoForge | 21.1.172+ |
| Kotlin for Forge | 5.9.0+ |

## Features

### Egg Incubator

- Incubates Cobbreeding eggs using a configurable base speed.
- Displays the egg's Pokemon model, species, Nature, Ability, and IV chart.
- Uses separate block models for empty and occupied states.
- Uses an active texture while at least one player has the interface open.
- Supports horizontal placement and translucent glass rendering.
- Includes an animated egg progress indicator with ten frames.

### Upgrades

The machine has three upgrade slots.

**Speed Upgrade**

- Multiplies the configured base incubation speed.
- The multiplier is configurable and displayed dynamically in the item tooltip.

**PC Upgrade**

- Bound to a player by right-clicking while holding the upgrade.
- Stores the player's UUID for stable identity and name for tooltip display.
- Sends hatched Pokemon directly to the linked player's PC.
- Shows a chat notification with hoverable Nature, Ability, and IV information.
- Falls back to the output slot when the owner is offline or the PC is full.

**Filter Upgrade**

- Filters by species, Nature, Ability, and individual IV rules.
- Species, Nature, and Ability use searchable selectors.
- Selecting a species limits Ability choices to that species' possible abilities.
- Each IV supports `>=`, `<=`, and `=` operators with values from 0 to 31.
- Rejected eggs can be moved to the output or deleted.
- No filter field is mandatory.

### Automation

- Pulls eggs automatically from a Cobbreeding-enabled Pasture touching any face.
- Pushes output eggs into adjacent containers and compatible machines.
- Respects sided inventory insertion rules.
- Pastures are excluded as output destinations.

## Configuration

The configuration is generated on startup:

`config/cobblemon_incubator.json`

```json
{
  "baseSpeed": 5,
  "speedUpgradeMultiplier": 2.0
}
```

The upgraded speed is calculated as:

```text
round(baseSpeed * speedUpgradeMultiplier)
```

Both values are validated when loaded. Restart the game or server after changing
the file.

## Recipes

### Egg Incubator

```text
Glass        Copper       Glass
Copper       Electirizer  Copper
Iron Ingot   Iron Ingot   Iron Ingot
```

### Speed Upgrade

```text
Redstone   Sugar              Redstone
Sugar      Cobblemon Upgrade  Sugar
Redstone   Sugar              Redstone
```

### PC Upgrade

```text
Redstone    Ender Pearl        Redstone
Ender Pearl Cobblemon Upgrade  Ender Pearl
Redstone    Ender Pearl        Redstone
```

### Filter Upgrade

```text
Redstone Comparator          Redstone
Quartz   Cobblemon Upgrade   Quartz
Redstone Quartz              Redstone
```

## Project Structure

```text
common/
  src/main/kotlin/com/nbp/cobblemon_incubator/
    block/          Block behavior and placement states
    blockentity/    Incubation, upgrades, automation, and inventory logic
    client/         Client initialization and machine screen
    config/         JSON configuration loading and validation
    item/           Upgrade item behavior and tooltips
    menu/           Container slots, synchronization, and button actions
    registry/       Blocks, items, components, menu, and creative tab
    util/           Cobbreeding compatibility and filter model
  src/main/resources/
    assets/         Models, textures, blockstates, and translations
    data/           Crafting recipes
fabric/             Fabric entrypoints and metadata
neoforge/           NeoForge entrypoint and metadata
media/              Editable source artwork
```

## Technical Notes

- Language: Kotlin.
- Architecture: Architectury multi-loader project.
- Supported loaders: Fabric and NeoForge.
- Cobbreeding egg access uses its item components and a reflection-based
  compatibility layer.
- Filter and PC ownership data are stored in custom synchronized data components.
- The PC owner is resolved by UUID; the saved player name is display-only.
- The Pasture integration transfers the original egg `ItemStack`, preserving all
  breeding data.
- Server-side machine data is synchronized through `ContainerData`.
- Client rendering reuses Cobblemon GUI resources and rendering conventions.
- English (`en_us`) and Brazilian Portuguese (`pt_br`) are included.

## Building

Clone the repository and run:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

Generated JARs are available under:

```text
fabric/build/libs/
neoforge/build/libs/
```

Use the remapped JAR without the `dev-shadow` or `sources` classifier.

## Development

1. Install Java 21.
2. Import the Gradle project into IntelliJ IDEA or another Kotlin-compatible IDE.
3. Allow Gradle to download the Minecraft, Cobblemon, and loader dependencies.
4. Run the appropriate Fabric or NeoForge client configuration.
5. Test both manual and automated egg flows before publishing changes.

Recommended checks:

- Empty, occupied, open, and closed block states.
- All four horizontal block directions.
- PC owner online, offline, and with a full PC.
- Pasture input and adjacent-container output.
- Every filter operator and rejection action.
- Fabric and NeoForge builds.

## License

This project is licensed under the [MIT License](LICENSE).

Cobblemon and Cobbreeding are separate projects owned by their respective
authors. This addon is not an official Cobblemon project.
