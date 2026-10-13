# Upgrader — Minecraft 1.12.2 (Forge) port

Backport of EXEcheINZ's Upgrader (originally for Minecraft 26.2) to Forge 1.12.2, done with the author's permission.
Gambling-style item upgrader: right-click the Upgrader item, put an item in, pick a target item and amount, and
spin the wheel. Odds = `houseEdge * value(input) / value(target * amount)`, clamped to `[minChance, maxChance]`.

Recipe: gold ingots in the corners, diamonds on the sides, anvil in the middle.

## Building (needs internet; JDK 8; Gradle 4.9)
    gradle setupDecompWorkspace      # first time only
    gradle build                     # jar ends up in build/libs/upgrader-forge-1.3.0-1.12.2.jar
(ForgeGradle 2.3 needs Gradle 4.9 and JDK 8; newer Gradle/JDK versions will not work.)

## Config: config/upgrader.cfg
houseEdge, minChance, maxChance, spinTicks, maxTargetCount, attemptLimit, attemptWindowSeconds,
globalValueMultiplier, plus `valueOverrides`, `valueMultipliers` and `blacklist` lists (`modid:item[@meta]`).

## Differences from the 26.2 version
- No in-game config screens (edit the .cfg file instead).
- Prices come from a built-in anchor table plus cheapest-recipe pricing; the 26.2 version's loot-table sampling
  and smithing-recipe pricing don't exist in 1.12.2. Unpriced items fall back to a rarity-based price.
- Item identity uses `modid:item` or `modid:item@meta` for items with subtypes.

## Building without installing anything (GitHub Actions)
Create a new GitHub repository, upload this folder's contents, then open the Actions tab -> "Build jar" -> Run workflow.
When it finishes, download the `upgrader-forge-1.12.2` artifact: the jar inside (not `-sources`) goes in your mods folder.
Locally: `./gradlew build` with JDK 8 (the wrapper uses Gradle 4.9).
