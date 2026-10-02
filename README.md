# PrideCarts

TrainCarts-style trains, action signs and train properties for Minecraft 1.12.2 (Forge / Cleanroom).

## About

PrideCarts brings the main ideas of the TrainCarts server plugin to Forge: you put signs under your track, and trains that roll over them stop at stations, launch, switch junctions, load cargo, announce stops, and so on. Each train also carries its own settings (speed limit, who may ride, destination, route, tags) saved on the carts themselves, so they survive restarts.

It was built for the Pride modpack, a 1.12.2 pack with about 750 mods, but it only needs Forge and works in other packs. With Railcraft installed, Railcraft's linked carts count as one train, so signs and properties act on the whole train. Without Railcraft, every cart is its own train.

Everything runs on the server. Clients do not need the mod to join a server that has it.

## Features

- **15 sign actions** (plus aliases): station, launcher, blocker, waiter, mutex, switcher, destination, property, flip, skip, enter, eject, destroy, transfer, announce, sound, effect.
- **TrainCarts sign format**: header line with redstone modes, per-train or per-cart signs, and direction filters.
- **18 train properties** stored in the cart's NBT, editable with `/train set` or a `property` sign.
- **Destinations and routes**: a train with a `destination` steers itself through junctions by the shortest rail path, even where there is no switcher sign. A `route` list makes it cycle through several destinations.
- **Junction routing on any rail**: works with vanilla rails and any modded rail that extends the vanilla rail block (Railcraft and others). Route lookups never load chunks, are capped at 30,000 rail nodes, and the cache is cleared whenever a block is placed or broken.
- **Railcraft linking**: when Railcraft is present, trains are Railcraft's linked carts, and `/train link` / `/train unlink` use Railcraft's linkage API. If the API is missing or changes, PrideCarts falls back to single-cart trains and logs a warning.
- **Station levers**: while a train waits at a station, blocker, or similar sign, a lever on or next to the sign is switched on, so you can drive redstone from it.
- **Locked-in passengers**: with `playerexit false`, players can only leave when a sign ejects them.
- **Cargo**: `transfer` signs move items between storage carts and any inventory within 2 blocks of the rail.
- **Optional PrideQuests hook**: when a train stops at a station, each player aboard gets credit for "ride a train to a station" quests if PrideQuests is installed. It is looked up by reflection, so PrideQuests is not required.
- **Debug log**: put an empty file named `pridecarts-debug` in the game folder and every sign decision is written to the log.

## Action signs

### How to place them

Put a sign **under the rail**, anywhere in the column up to 6 blocks below it (you can stack several), or hang wall signs on the sides of that column. Each time a cart rolls onto a new rail, PrideCarts reads every action sign belonging to it.

| Line | Content |
|---|---|
| 1 | Header, e.g. `[train]`, `[cart]`, `[+train]`, `[train:lr]` |
| 2 | The action name, e.g. `station` (case and spaces ignored) |
| 3 | First setting for the action |
| 4 | Second setting for the action |

### Header (line 1)

| Header | Meaning |
|---|---|
| `[train]` | Active only while the sign gets redstone power (default). Acts once per train. |
| `[cart]` | Acts on each cart separately instead of the whole train. Combine with any prefix below, e.g. `[+cart]`. |
| `[+train]` | Always active. |
| `[!train]` | Active until powered (inverted). |
| `[-train]` | Never active. |
| `[/train]` `[\train]` `[/\train]` | Pulse modes. When a train arrives they behave like `[train]`. |
| `[train:<dirs>]` | Direction filter. `f` `b` `l` `r` are relative to the sign, `n` `e` `s` `w` are compass directions, `*` means any. Example: `[train:lr]`. |

Without a direction filter, a sign reacts to trains moving toward its text side. If the track runs across the sign, it reacts both ways.

A `[train]` sign fires once per train within 5 seconds, not once per cart. Once a sign holds a train (station, blocker, waiter, mutex), the remaining signs on that rail wait until it is released.

### Direction words

These are used by station, launcher, flip and `/train launch`. They are relative to where the train is going.

| Word | Direction |
|---|---|
| empty, `continue`, `c`, `forward`, `f` | Keep going |
| `back`, `b`, `reverse` | Turn around |
| `left`, `l` / `right`, `r` | Turn left / right |
| `north`/`n`, `east`/`e`, `south`/`s`, `west`/`w` | Compass direction |

### Actions

| Action (line 2) | Line 3 | Line 4 | What it does |
|---|---|---|---|
| `station` | Wait time in seconds (e.g. `5`). Empty = wait until the sign's redstone changes. | Leave direction plus optional speed, e.g. `continue`, `back 0.6`, `north`. `none` = stay until a command or another sign moves it. | Stops the train centred on the rail, waits, then sends it off (default speed 0.4). Switches on the nearby lever while waiting. |
| `launcher` / `launch` | Target speed in blocks/tick. A leading `+` or `-` is relative to the current speed. Empty = 0.4. Capped at 3.0. | Direction word. Empty = continue. | Sets the train's speed and direction. |
| `blocker` / `block` | Optional leave speed. | – | While the sign is active, arriving trains stop and wait. When it turns off they continue the same way at their old speed (or the speed given). |
| `waiter` / `wait` | Look-ahead distance in blocks (default 10, max 64). | – | If another train is that close ahead, this train stops until the way is clear. |
| `mutex` | Zone name. Signs with the same name share a zone. Empty = this sign only. | Zone radius (default 8, range 1–64). | Only one train at a time in the zone, for crossings and single-track sections. The zone frees once its train is more than twice the radius away. Put one at every entrance. |
| `switcher` / `switch` / `tag` | Rule for turning **left**. | Rule for turning **right**. | Placed under a junction (a normal rail touching 3–4 rails). Rules: `tag X`, `!tag X`, `dest X`, `name X`, `*` (always), or a bare word (a tag). The first matching rule wins. With no match, the train is routed to its destination, or goes straight on if it has none. |
| `destination` / `dest` | This destination's name. | Optional next destination. | If the train's destination matches line 3, it has arrived: it moves to the next stop in its `route` (wrapping around) or clears its destination. Line 4, if set, always becomes the new destination. Auto-routing searches for these signs. |
| `property` / `prop` / `set` | `name value`, e.g. `speedlimit 0.8` | A second `name value` (optional). | Sets train properties. Also accepts `tag add X`, `tag remove X`, `addtag X`, `remtag X`, `dest X`. Unknown names are ignored. |
| `flip` / `reverse` | – | Empty, `reverse` or `back` = turn around. Any other direction word = go that way. | Sends the train back at its current speed (at least 0.1). |
| `skip` | How many signs to skip (default 1, max 100). | Optional `tag X`: only trains with that tag skip. | The train ignores the next N action signs, e.g. for express trains. |
| `enter` | Radius in blocks (default 2, max 8). | `players` (default), `mobs` or `all`. | Seats the closest eligible entity in each empty cart. |
| `eject` | Optional offset `dx dy dz` from the cart. Default is 1.5 blocks toward the sign's text side. | `all` or `mobs` to eject non-players too. Default is players only. | Lets passengers out, even with `playerexit false`. |
| `destroy` / `destroyer` | `drop` = drop the carts as items. | – | Ejects passengers and removes the train. |
| `transfer` | `load` (chests to carts, default) or `unload` (carts to chests). | Optional item filter: a full id (`minecraft:coal`) or part of one (`ore`). | Moves items between storage carts and any inventory within 2 blocks of the rail. It fills matching stacks first, and anything that does not fit stays where it was. |
| `announce` | Message. `&` colour codes, `{name}` and `{dest}` placeholders. | Extra text, appended after a space. | Sends the message to every player on the train. |
| `sound` / `playsound` | Sound id, e.g. `block.note.bell` (`minecraft:` is added when there is no namespace). | Optional `volume pitch` (default `1 1`). | Plays the sound at the front cart. |
| `effect` / `particle` | Particle name, e.g. `flame`, `heart`, `smoke`, `note`, `portal`. Default `cloud`. | Optional `count spread` (default `20 0.5`). | Spawns particles around every cart. |

## Train properties

Properties are saved on each cart and apply to the whole train. Set them with `/train set <property> <value>` or a `property` sign. Setting a property back to its default removes it from the cart.

| Property | Default | Effect |
|---|---|---|
| `speedlimit` | `0.4` | Top speed in blocks per tick. 0.4 is vanilla; Railcraft high-speed track allows more. Clamped to 0.01–3.0. |
| `slowdown` | `true` | `false` = the train keeps its speed on flat track instead of slowing down. |
| `gravity` | `1.0` | How strongly slopes speed the train up or slow it down (1 = normal, 0 = flat-like). |
| `playerenter` | `true` | Players may get in. |
| `playerexit` | `true` | Players may get out by sneaking. `false` = locked in until a sign ejects them. |
| `ownersonly` | `false` | Only the train's owners may get in. |
| `pickup` | `false` | Storage carts pick up items they roll over. |
| `invincible` | `false` | Players cannot break the train. |
| `keepchunks` | `false` | Keep the chunks around the train loaded so it runs with nobody near. |
| `mobenter` | `true` | Mobs may get in when the train bumps into them. |
| `collision` | `push` | What happens to mobs in the way: `push`, `kill`, `ignore`, `enter`. |
| `sound` | `true` | Rolling sounds. |
| `name` | (empty) | The train's name, shown by `/train info` and used by signs. |
| `destination` | (empty) | Where the train is going. Destination signs and auto-routing use it. |
| `route` | (empty) | Comma-separated list of destinations. The next one is picked when one is reached. |
| `tags` | (empty) | Comma-separated labels for signs to check. |
| `entermessage` | (empty) | Message shown to a player who gets in (`&` colour codes). |
| `owners` | (empty) | Comma-separated player names who own the train. |

Note: in version 0.1.0, the event code applies `speedlimit`, `slowdown`, `playerenter`, `playerexit`, `ownersonly`/`owners`, `mobenter`, `invincible`, `entermessage`, `destination`, `route`, `name` and `tags`. `gravity`, `pickup`, `keepchunks`, `collision` and `sound` can be stored and read by signs, but have no gameplay effect yet.

## Commands

The command is `/train`, with the alias `/cart`. If another mod already registers `/train`, PrideCarts uses `/ptrain` instead (with no alias). It needs permission level 2 (op). The target is the cart you are riding, or the cart you are looking at within 6 blocks.

| Command | Arguments | Description |
|---|---|---|
| `/train info` | – | Cart count, speed, direction, and every property that differs from its default. |
| `/train settings` | – | Every property with its current value and a one-line explanation. |
| `/train set` | `<property> <value…>` | Sets a property on the whole train. An unknown name lists the valid ones. |
| `/train tag` | `add\|remove <tag>` | Adds or removes a tag. |
| `/train dest` | `<destination>` | Sets the destination. |
| `/train route` | `<a,b,c>` | Sets the route list. |
| `/train launch` | `[speed] [direction]` | Launches the train (default speed 0.4, default direction continue) and releases any hold. |
| `/train stop` | – | Stops the train and holds it until it is launched. |
| `/train destroy` | – | Ejects passengers and removes the train. |
| `/train eject` | – | Ejects all passengers. |
| `/train link` | – | Links the cart you ride (or look at) to the cart you look at. Requires Railcraft. |
| `/train unlink` | – | Breaks the cart's links. Requires Railcraft. |

Tab completion covers subcommands, property names, `add`/`remove` and launch directions.

## Config

PrideCarts has no config file. Behaviour is set per train through properties and signs. Auto-routing at junctions without a switcher sign is always on.

To turn on debug logging, create an empty file named `pridecarts-debug` in the game (or server) folder.

## Requirements

- Minecraft 1.12.2
- Forge 14.23.5.2860 or newer, or Cleanroom
- Optional: **Railcraft**, for multi-cart trains and `/train link`/`unlink`
- Optional: **PrideQuests**, for station-arrival quest credit

Server-side. It is not needed on clients (`acceptableRemoteVersions = "*"`).

## Install

1. Download `PrideCarts-1.12.2-<version>.jar`.
2. Put it in the `mods/` folder of your server, or of your client for single-player.
3. Start the game. The log shows `PrideCarts: N sign actions, /train ready`.

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/`. The build targets Java 8. Railcraft, MixinBooter and Mixin jars in `libs/` are compile-only and are not bundled.

## License

MIT License. © 2026 crunkazcanbe.

Inspired by the TrainCarts plugin (MIT). No TrainCarts code is included.

## Credits

Made by crunkazcanbe, with Claude.


## Compile-only jars

The build compiles against these jars in `libs/` (other authors' mods / APIs). They are not included in this repo — get them from their official pages and drop them in `libs/` before building:

- `mixinbooter-api.jar`
- `railcraft.jar`
- `sponge-mixin.jar`
