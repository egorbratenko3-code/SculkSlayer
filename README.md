# Sculk Slayer — Fabric mod for Minecraft Java 1.21.11

A sculk plague slowly consumes the world. Raise the monolith, arm yourself with the Hallow arsenal,
hold the line through 11 infection stages and finally destroy the Sculk Core.

## Build & run
Requirements: JDK 21 and internet access (Gradle downloads Fabric Loom, Minecraft and Fabric API).

    ./gradlew build          # jar appears in build/libs/
    ./gradlew runClient      # dev client
    ./gradlew runServer

Put `sculk-slayer-1.2.0.jar` and Fabric API `0.141.6+1.21.11` into `mods/` (Fabric Loader >= 0.19.5).

> The project was written against the 1.21.11 Mojang-mapped API (verified against the Fabric API 1.21.11 sources
> and the official Fabric docs reference mod) but could NOT be compiled in the authoring sandbox.
> If Gradle reports a compile error, it is a one-line API mismatch — fix it or report it.

## Starting
`/sculkslayer start` builds the command-block monolith, the temple and the Hallow Altar ~20 blocks in front of you.
Right-click the two glowing signs on the monolith for the **Hallow Sword** and **Hallow Pickaxe**.
`/sculkslayer status` shows progress. Ops: `stop`, `setinfection <0-100>`, `setstage <0-10>`.

The HUD (top-right) shows: infected %, stage, monolith coordinates (and the core coordinates once it exists).

## Stages (infection %)
| Stage | From | What changes |
|---|---|---|
| 0 | 0% | kills spawn a little sculk + veins |
| 1 | 6% | + sensors |
| 2 | 12% | + shriekers (can summon wardens), rare wardens |
| 3 | 19% | + catalysts, natural creeping |
| 4 | 27% | kills spawn the maximum sculk; crystallization begins |
| 5 | 36% | infected mobs, natural wardens |
| 6 | 46% | standing on sculk hurts, parasites, nests |
| 7 | 56% | infected mobs break weak blocks, walkers appear |
| Critical 1 | 68% | max spread speed, damage near sculk, brutes, crystal caves |
| Critical 2 | 80% | underground bias, infected mobs phase through sculk walls |
| Critical 3 | 92% | the Sculk Core awakens |

### Progression economy
Infection % grows from time, unholy kills and newly infected blocks, but every source is divided by
`1.6^stage`: the first stage takes minutes, then each stage is ~1.6x harder than the last. Cleansing blocks
always removes a flat amount, so the player can push the plague back. All numbers live in `Stage.java`.

### The infection front (territory)
Separately from the %, the plague also grows as a circular, noisy **front** centered on the monolith,
starting the moment the monolith is built (even at stage 0). Its radius is capped per stage — see `Stage.java`
`RADIUS_CAP` — from 100 blocks at stage 0 up to 2500 blocks (a ~5000x5000 area) at Critical 3, so a large,
believable territory requires reaching a late stage, not just waiting.

This costs nothing on an unvisited map: the front never touches a chunk until that chunk actually loads
(`ServerChunkEvents.CHUNK_LOAD`). A newly loaded chunk inside the front gets a **one-time** seeding of sculk,
on the surface and underground (so the plague also spreads down into caves independently of the surface),
with density scaled by how deep inside old territory the chunk is. Once seeded, the fine block-by-block
growth (`SculkManager.spreadStep`, driven by `SPREAD_PER_SEC`) takes over locally whenever a player is nearby
to tick it, growing pockets outward and downward on their own. That rate ramps up hard from stage 4 onward
(`SPREAD_PER_SEC`, plus more neighbour-infection attempts per step and a growing underground bias), and
underground cave seeding (`seedChunkCaves`) also gets denser and probes more heights per stage.

### Crystal caves, nests and Sculk Hoards
Underground crystal fields (`SculkManager.caveBloom`) start appearing from **stage 3** and get larger, denser
and more frequent every stage after that; from stage 7 a single cave can hide more than one nest. Parasite
nests, and the Sculk Hoard chest they sometimes guard, still specifically require **stage 6**, matching the
stage table below.

### Runaway growth from stage 5
The plague starts visibly growing from the very first second (the ground around the monolith is seeded
immediately, not just once the front reaches still-unloaded chunks). From stage 5 growth also compounds: the
more of the map is already infected, the faster new growth happens (up to 4.5x the base rate at a large,
mature infection), on top of `SPREAD_PER_SEC` already ramping hard from stage 4. Fully "dead" interior sculk
(no air or infectable neighbour left at all) is pruned from the growth tracker so the plague keeps chasing
real frontier instead of wasting effort on cells that can't do anything more.

### Atmosphere
Stand deep inside dense sculk territory (stage 4+) for a while and the Darkness effect will pulse over you now
and then; from stage 5 a distant Warden roar occasionally echoes out of the dark, honestly reflecting that
wardens really do spawn there. Both only trigger when you're genuinely surrounded by sculk, not near a stray patch.

Holy kills (Hallow Sword, Holy Water, Crucifix, Holy Grenade, shield dash) never feed the plague.

## Items (all recipes are plain JSON in `data/sculkslayer/recipe/`)
* **Hallow Bar** — 8 gold + 1 diamond (shapeless).  **Hallow Block** — 9 bars.  **Hallow Wood** — bar + birch planks.
* **Hallow Altar** — crafting table + 4 hallow blocks + 4 obsidian (workbench). Unbreakable, immovable.
  Everything below is crafted **only at the altar**.
* **Crucifix** — totem + 4 hallow blocks + 4 netherite ingots. Single use, purges radius 40 and slays wardens/unholy creatures.
* **Holy Water** — water bottle + 4 hallow bars. Thrown; cleanses, damages the unholy, heals/buffs players, cures parasites.
* **Holy Grenade** — 4 hallow blocks + 4 TNT + crucifix. Thrown; radius-32 purge and blast. Weakens the Sculk Core.
* **Sacrament** — rare drop (3% per Sculk Hoard chest, see below); sacrament + crystal block + 7 hallow blocks -> 2 sacraments.
* **Hallow armor** — netherite piece + sacrament + hallow blocks. Helmet -5% damage; breastplate -50% warden damage;
  greaves -10% sculk-mob damage and -30% parasite infection; boots -50% sculk/crystal damage and +10% speed.
* **Hallow Shield** — shield + sacrament + bar. Blocks every warden attack; raising it dashes forward (5 s cooldown)
  and splashes damage (extra vs sculk and nether mobs).
* **Hallow Sword** — 10% of the target's max health as bonus damage (not dragon/wither), unbreakable, returns after death.
* **Hallow Axe** — unlocked at stage 7; right-click sweeps a 2×2×2 area (ordinary creatures are slain, bosses take 10 damage), on a 30-second cooldown shown by the hotbar item cooldown overlay.
* **Hallow Pickaxe** — unbreakable, deals no damage, instantly mines Nether blocks, mines ancient debris very fast,
  breaks sculk growth instantly, right-click cleanses a 3x3x3 area, breaks the Sculk Core in exactly 3 minutes.

## Sculk Hoard chests
Parasite nests (underground, stage 6+) sometimes hide a **Sculk Hoard**: a named chest with its own curated
loot table (`data/sculkslayer/loot_table/chests/sculk_hoard.json`), not flat randomness:
1. **Hive junk** (3-6 rolls, always) — bones, rotten flesh, string, sculk, coal, gunpowder: worthless but thematic.
2. **Salvage** (2-4 rolls) — iron, gold, echo shards, amethyst, lapis, experience bottles, sensors, shriekers.
3. **Rare finds** (1 roll, ~58% chance of something) — diamonds, Holy Water, specific enchanted books
   (Unbreaking III, Protection IV, Sharpness IV, Mending, Silk Touch), golden apples, crystal blocks.
4. **Treasure** (1 roll, ~9% chance) — enchanted golden apple, totem of undying, netherite ingot, heart of the sea.
5. **Sacrament** — exactly 3%, independent of everything else.

## Endgame
At Critical 3 a **Sculk Core** appears in the sculk field (coordinates are broadcast and shown on the HUD).
1. Throw a Holy Grenade next to it -> it is weakened (and goes into a frenzy: massive spread, brutes, wardens).
2. Mine it with the Hallow Pickaxe for 3 minutes. The plague dies and a cleansing wave sweeps the world.

## Changelog

### 1.2.0
* Raised the monolith sanctuary onto a small generated Hallow-reinforced mound and enclosed the altar with three-block Hallow-wood walls.
* Added the stage-7 Hallow Axe, 30-second area attack and cooldown overlay, and a Hallow Block uncrafting recipe.
* Reduced passive infection and territory-front growth; parasite nests favor underground caves and the Nether is excluded from sculk infection. Nether mobs hunt players, while piglins ignore players wearing full gold armor.
* Infection replacements remember and save original block states and fluids so cleansing restores them after relaunch.
* Added the Sculk Creeper, whose blast damages and seeds an infection scar; late-stage surface infection forms mold, stalk and crystal patches, and mature sculk can consume water sources.
* Added Hallow Power I–III books, rare in ordinary structure chests and more common in parasite hoards. The enchantment adds attack and mining power to Hallow tools; a lectern in the temple contains the recipe guide.
* Gold ore and ancient debris generate more often. Piglin bartering has a 5% ancient-debris chance; bastion chests have a 3% netherite-ingot chance.

### 1.0.3
* **Fixed the critical freeze**: chunks kept loading but the server stopped ticking, and the world could not be left or
  re-entered (only killing the game worked). Cause: the infection front seeded terrain from inside
  `ServerChunkEvents.CHUNK_LOAD`, i.e. while the chunk was still being promoted; touching the world there makes the
  server thread wait for the very chunk it is finishing. The event now only queues the chunk, and
  `SculkManager.processChunkQueue()` seeds up to 2 queued chunks per tick from the normal server tick, in a 4 ms budget.
* All "is this chunk loaded" checks now use the non-blocking `SculkManager.loaded()` (`getChunkNow`) instead of
  `hasChunkAt`, and every neighbour lookup that could cross into an unloaded chunk is guarded, so the plague can no
  longer force chunks to load/generate on the server thread.
* Plague spreading has a 10 ms per-tick budget, so a large mature infection can no longer eat the whole tick.
* Static per-world state (dash cooldowns, death stash, pending chunks) is reset when the server stops, and the client HUD
  is cleared on disconnect, so leaving one world and joining another starts clean.

## Files
* `tools/generate_assets.py` regenerates every texture, model, recipe, loot table and tag.
* Plague state is saved in `<world>/sculkslayer_state.json`.
