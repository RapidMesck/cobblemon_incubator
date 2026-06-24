# Cobblemon Incubator

Turn Pokemon breeding into a polished, visual, and automatable workflow.

Cobblemon Incubator is an addon for **Cobblemon** and **Cobbreeding** that adds a
dedicated Egg Incubator machine. Insert an egg to incubate it faster, inspect its
breeding information, configure advanced filters, or connect the machine to a
Pasture and storage system for full automation.

## A Complete Egg Incubation Machine

The Egg Incubator provides a custom interface inspired by Cobblemon's PC screens.
While an egg is inside, the interface displays:

- The Pokemon's 3D model
- Species
- Nature
- Ability
- All six IVs in a visual radar chart
- An animated egg showing incubation progress

The block also changes visually when it contains an egg and lights up while its
interface is open.

## Faster Incubation

Eggs incubate at **5x speed by default**. Add a Speed Upgrade to reach **10x
speed** with the default configuration.

Server owners can customize both the base speed and the Speed Upgrade multiplier
in:

```text
config/cobblemon_incubator.json
```

## Three Powerful Upgrades

### Speed Upgrade

Multiplies the machine's incubation speed, allowing eggs to finish much faster.

### PC Upgrade

Right-click while holding the upgrade to bind it to your player. Once installed,
hatched Pokemon are sent directly to your PC.

The link uses your UUID, so changing your Minecraft name will not break it. When
a Pokemon is delivered, you receive a chat notification. Hover over the Pokemon's
name to view its Nature, Ability, and IVs.

If you are offline or your PC is full, the egg safely falls back to the machine's
output slot.

### Filter Upgrade

Build precise breeding rules without requiring every field:

- Search and select a species
- Search and select a Nature
- Select an Ability
- Filter every IV using `>=`, `<=`, or `=`
- Choose any IV value from 0 to 31

After selecting a species, the Ability selector only shows abilities that species
can actually have, including hidden abilities.

Rejected eggs can either be moved to the output slot or deleted, depending on
your chosen action.

## Pasture and Storage Automation

Place the Incubator directly against a compatible Pasture and it will
automatically collect available eggs whenever its input is empty.

Place a chest or another compatible inventory against the machine and completed
output eggs will be inserted automatically. Input and output automation works on
all six faces and respects sided inventory rules.

This enables complete production lines:

```text
Pasture -> Egg Incubator -> Chest
```

## Additional Features

- Three upgrade slots
- Dedicated creative-mode tab
- Searchable filter selectors
- Empty and occupied block models
- Active machine texture while in use
- Transparent glass rendering
- Directional placement
- Custom upgrade textures and tooltips
- English and Brazilian Portuguese translations
- Fabric and NeoForge support

## Requirements

- Minecraft 1.21.1
- Cobblemon 1.7.0
- Cobbreeding
- Architectury API

Fabric installations also require:

- Fabric API
- Fabric Language Kotlin

NeoForge installations also require:

- Kotlin for Forge

## Compatibility

Supported loaders:

- Fabric
- NeoForge

Cobblemon Incubator must be installed on both the server and client.

## Configuration

Default configuration:

```json
{
  "baseSpeed": 5,
  "speedUpgradeMultiplier": 2.0
}
```

Restart the game or server after editing the configuration.

## Credits

Created by **RapidMesck**.

Cobblemon Incubator is an unofficial addon. Cobblemon and Cobbreeding are
separate projects owned by their respective authors.

