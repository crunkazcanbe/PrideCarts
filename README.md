# PrideCarts

TrainCarts-style trains, action signs and train properties for Minecraft 1.12.2 (Forge / Cleanroom), plus 33 mini cars, mini engines with routes, couplers, 57 special tracks, block signals and cart loaders.

## About

PrideCarts brings the main ideas of the TrainCarts server plugin to Forge: you put signs under your track, and trains that roll over them stop at stations, launch, switch junctions, load cargo, announce stops, and so on. Each train also carries its own settings (speed limit, who may ride, destination, route, tags) saved on the carts themselves, so they survive restarts.

On top of that it adds its own rolling stock and track: seat, storage, tank and power cars, small locomotives that pull coupled trains and follow no-code routes, smart tracks for speed, stations, switching, cargo and detection, wide curves, and block signals.

It was built for the Pride modpack, a 1.12.2 pack with about 750 mods, but it only needs Forge and works in other packs. Carts coupled with PrideCarts' Cart Chain or Cart Rope count as one train, and with Railcraft installed, Railcraft's linked carts do too, so signs and properties act on the whole train.

PrideCarts adds blocks, items and entities, so install it on the server and on every client.

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
- **Mini cars, engines, couplers, tracks, signals and loaders:** see the sections below. Everything has a recipe and sits in the PrideCarts creative tab.
- **Optional PrideQuests hook**: when a train stops at a station, each player aboard gets credit for "ride a train to a station" quests if PrideQuests is installed. It is looked up by reflection, so PrideQuests is not required.
- **Debug log**: put an empty file named `pridecarts-debug` in the game folder and every sign decision is written to the log.

## Mini cars

Minecart-sized cars with Blockbench models, 3D item icons and their cargo block shown inside. Place them on any rail like a minecart.

### Seat cars

| Car | What it does |
|---|---|
| Passenger Car | A plain seat. |
| First-Class Car | Riders slowly heal and stay fed. |
| Sleeper Car | When everyone is riding a sleeper at night, the night is skipped. |
| Dining Car | Feeds its rider a bite every 10 seconds. |
| Observation Car | Night vision while you ride. |
| Prisoner Car | You can't get out while it's moving. |
| Ambulance Car | Heals its rider and clears poison. |
| Maintenance Coach | Slowly repairs the tool in your hand. |
| Crew Car | Haste while riding: the crew gets to work fast. |
| Caboose | End of the train: night vision and a lookout. |
| Subway Car | Metro seat. |
| Tram Car | Street tram seat. |
| High-Speed Car | Allowed to go faster on high-speed track. |

### Storage cars

Open with right-click. Hoppers, pipes, `transfer` signs, cargo tracks and the Cart Loader/Unloader work with them.

| Car | What it does |
|---|---|
| Baggage Car | 18 slots of luggage. |
| Mail Car | 27 slots of letters and parcels. |
| Covered Boxcar | Covered boxcar, 27 slots. |
| Open Boxcar | Open boxcar, 27 slots. |
| Container Car | Shipping container, 36 slots. |
| Warehouse Car | A rolling warehouse: 54 slots. |
| Refrigerated Car | Food only, 27 slots. |
| Ore Car | Ores, stone and dusts only, 36 slots. |
| Log Car | Logs, planks, saplings and sticks only, 36 slots. |
| Hopper Car | Picks up items lying on the track. |
| Dump Car | Dumps everything beside the track on a powered activator rail. |
| Explosives Car | Careful: if it burns or is blown up with TNT inside, it goes off. |
| Machinery Car | Machine parts, 27 slots. |
| Flatbed | Flatbed, 18 slots. |
| Heavy Flatbed | Heavy flatbed, 36 slots. |

### Tank cars

Forge fluid tanks: fill and empty them with buckets, pipes, or Fluid Fill / Fluid Drain tracks.

| Car | What it does |
|---|---|
| Tanker | Any fluid, 16 buckets. |
| Liquid Tanker | Liquids only, 32 buckets. |
| Gas Tanker | Gases only, 32 buckets. |

### Power cars

Real Forge Energy storage: cables, machines and the Cart Loader/Unloader can charge or drain them. A battery car also charges the electric and solar engines in its train.

| Car | What it does |
|---|---|
| Battery Car | Stores 2,000,000 FE; charges and discharges at Cart Loaders. |
| Generator Car | Right-click with fuel: burns it into FE. |

## Mini engines

Small locomotives that pull the train coupled to them. Four throttle notches; pulling power is shared over the train, so a long train starts slower.

| Engine | Power |
|---|---|
| Mini Steam Engine | Burns furnace fuel. Strong, smoky, a classic. |
| Mini Electric Engine | Runs on Forge Energy: charge it at a Cart Loader or with cables. |
| Mini Solar Engine | Basic solar: charges in sunlight, bursts of travel. |
| Advanced Mini Solar Engine | Advanced solar: runs a light train all day. |
| Elite Mini Solar Engine | Elite solar: full power in sunlight, a big battery for the night. |

- **Drive:** sit in the engine and hold **W** / **S** to notch the throttle up or down. Notch 0 brakes.
- **Refuel:** right-click a steam engine with fuel, use a Refuel Track (fuel from chests, FE from energy blocks beside it), or charge electric engines with cables, a Cart Loader or a battery car.
- **Control panel:** sneak + right-click with an empty hand. Notches, reverse, emergency stop, fuel/FE gauge, solar status, speed, odometer, train list, horn, a switch to obey or ignore smart tracks, and uncouple.
- **Routes (no code):** right-click Station Tracks with an empty hand to number them (sneak counts down). In the engine's **Route** screen, each row is "at station N → action value". Actions: `wait` seconds, `skip`, `reverse`, `speed` notch, `horn`, `load`, `unload` (chests beside the station), `hold` (until redstone). Station 0 means every station.

## Couplers

- **Cart Chain** (tight) and **Cart Rope** (slack): right-click one cart, then the next, to couple them. Shears uncouple. Works on PrideCarts cars, vanilla minecarts and other mods' carts.
- Couplings act like damped springs, so trains of up to 10 carts move as one, with 3D chain or rope links drawn between them.
- Signs, train properties, detectors and switches treat coupled carts as one train. Auto-Coupler and Decoupler tracks couple and uncouple automatically.

## Tracks

Every track has a tooltip and a recipe. "Powered: off" means a redstone signal switches the track's effect off.

### Smart tracks

| Track | What it does |
|---|---|
| Booster Track | Speeds carts up, no redstone needed. Powered: off. |
| Launcher Track | Fires carts off at top speed. Powered: off. |
| Brake Track | Slows carts to a crawl. Powered: off. |
| Slow Zone Track | Speed limit: slow (about 2 m/s). Powered: off. |
| Medium Zone Track | Speed limit: medium (about 4 m/s). Powered: off. |
| Station Track | Stops the train for 5 seconds, then it carries on. Powered: trains pass through. |
| Holding Track | Holds the train until it gets a redstone signal. |
| Reverse Track | Sends the train back the way it came. Powered: off. |
| Eject Track | Everyone gets out here. Powered: off. |
| Decoupler Track | Uncouples each cart that rolls over it (drops the chain). Powered: off. |
| Engine Start Track | Mini engines set off at notch 2. |
| Engine Faster Track | Mini engines notch up one. |
| Engine Slower Track | Mini engines notch down one. |
| Engine Full Track | Mini engines go full power. |
| Engine Stop Track | Mini engines shut off and brake. |
| Refuel Track | Steam engines take fuel from chests beside it; electric/solar engines charge from FE blocks beside it. |
| Rainbow Track | Pride track: a little boost and a trail of colour. |
| Booster Track II | A stronger booster: gets carts to top speed fast. Powered: off. |
| Booster Track III | The strongest booster: top speed at once, even uphill. Powered: off. |
| High-Speed Track | Lets carts go twice as fast as normal track (keep it straight!). |
| One-Way Track | Carts may only go east / south over it; others are turned back. Powered: west / north only. |
| Auto-Coupler Track | Couples a cart to the cart right behind or ahead of it with a chain (needs no item). Powered: off. |
| Delay Track | Stops the train for 2 seconds. Powered: trains pass through. |
| Whistle Track | Mini engines sound their whistle / horn as they pass. |
| Lamp Track | A glowing track: lights up tunnels and stations. |
| Announcer Track | Tells riders the next station number ahead. |
| Healing Track | Heals and feeds riders as they roll over it. |
| Pride Fireworks Track | Rainbow fireworks when a train passes. Powered: off. |
| Embark Track | Players and animals within 2 blocks climb into empty seat cars as they pass. Powered: off. |
| Priming Track | Lights TNT minecarts and Explosives Cars that roll over it. Powered: off. |
| Buffer Stop | The end of the line: carts stop dead here. |
| Pickup Track | Storage cars take items from chests beside it as they roll past (no stopping). |
| Drop-off Track | Storage cars give their items to chests beside it as they roll past. |
| Fluid Fill Track | Tank cars fill up from tanks beside it as they roll past. |
| Fluid Drain Track | Tank cars empty into tanks beside it as they roll past. |
| Item Vacuum Track | Storage cars hoover up items lying within 3 blocks. |
| Ice Track | Almost no friction: carts glide on and on. |
| Mud Track | Very sticky: carts slow right down. |
| Teleport Track | Carts jump to the other Teleport Track with the same number. Right-click to set the number. |
| Music Track | Plays a note when a cart passes; right-click to change the note. |
| Signal Track | Block signal: a train waits here while another train is on the track ahead (up to the next Signal Track). Powered: always stop. |

### Switch tracks

Place one where the branch joins, so it curves into the branch. It curves from the side a train enters toward the branch.

| Track | What it does |
|---|---|
| Passenger Switch | Carts with riders take the curve; empty carts go straight. |
| Engine Switch | Trains with a mini engine take the curve; loose carts go straight. |
| Cargo Switch | Trains with a storage car take the curve; the rest go straight. |
| Tank Switch | Trains with a tank car take the curve; the rest go straight. |
| Redstone Switch | Powered: carts take the curve. Unpowered: straight on. |
| Random Switch | Each train flips a coin: curve or straight. |
| Alternating Switch | Every other train takes the curve. |
| Full Cargo Switch | Trains with a full storage car take the curve (sends full trains to unload). |
| Manual Turnout | A straight and a curve in one: right-click it (empty hand) to flip between them. Redstone flips it too. |

### Detector tracks

| Track | What it does |
|---|---|
| Detector Track+ | Redstone while any cart is on it (and comparators read how full a cargo car is). |
| Train Length Detector | Redstone as strong as the train is long: 1 car = 1, 10 cars = 15. |
| Engine Detector | Redstone only for mini engines (or trains pulled by one). |
| Passenger Detector | Redstone only for carts with someone riding. |
| Full Cargo Detector | Redstone only for storage cars that are full (or trains carrying one). |

### Crossing and wide curves

| Track | What it does |
|---|---|
| Crossing Track (X) | Two lines cross here: carts go straight on the way they came. |
| Wide Curve Track (radius 3, 5 or 8) | Right-click the ground where the curve should start, facing along the line: one item lays a whole quarter circle to the right (sneak: to the left). Needs a flat, clear area. Carts ride the true arc instead of zig-zag corners, and the rails are drawn as one smooth curve. Breaking any piece removes the whole curve and returns the item. |

## Block signals

- A **Signal Track** splits the line into sections (up to the next Signal Track, or 400 blocks). A train reaching it waits while another train is in the section ahead. Powered: always stop.
- A **Pride Signal** post placed beside a Signal Track, facing oncoming trains, shows **green** when the section ahead is free and **red** when it is occupied, and outputs redstone from its back.

## Cart Loader, Cart Unloader and Railway Observer

| Block | What it does |
|---|---|
| Cart Loader | Fills the minecart in front from the storage behind. |
| Cart Unloader | Empties the minecart in front into the storage behind. |
| Railway Observer | Redstone on while a minecart is on the rail in front. |

Loaders work on their own whenever a cart with an inventory stands in front. Their redstone output turns on when the job is done (cart full, storage empty, or cart empty), so a powered rail or launcher sign can send the cart on. Powering the block pauses it. A sign on its side with a cart's name limits it to that cart. Battery cars charge and discharge at Cart Loaders.

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

PrideCarts has no config file. Behaviour is set per train through properties and signs, per engine through its control panel and route, and per track by placement and redstone. Auto-routing at junctions without a switcher sign is always on.

To turn on debug logging, create an empty file named `pridecarts-debug` in the game (or server) folder. It logs every sign decision, plus engine route and teleport decisions.

## Requirements

- Minecraft 1.12.2
- Forge 14.23.5.2860 or newer, or Cleanroom
- Optional: **Railcraft**, for Railcraft linked trains and `/train link`/`unlink` (PrideCarts' own couplers work without it)
- Optional: **PrideQuests**, for station-arrival quest credit

Install on the server and on every client: PrideCarts adds blocks, items and entities.

## Install

1. Download `PrideCarts-1.12.2-<version>.jar`.
2. Put it in the `mods/` folder of your server and of every client (or just your client for single-player).
3. Start the game. The log shows `PrideCarts: N sign actions, /train ready`.

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/`. The build targets Java 8. The Blockbench sources for the car and signal models are in `blockbench/`. Railcraft, MixinBooter and Mixin jars in `libs/` are compile-only and are not bundled.

## License

MIT License. © 2026 crunkazcanbe.

Inspired by the TrainCarts plugin (MIT). No TrainCarts code is included.

## Compile-only jars

The build compiles against these jars in `libs/` (other authors' mods / APIs). They are not included in this repo — get them from their official pages and drop them in `libs/` before building:

- `mixinbooter-api.jar`
- `railcraft.jar`
- `sponge-mixin.jar`

## Credits

Made with [Claude Code](https://claude.com/claude-code) and [Blockbench](https://www.blockbench.net).

- Cart Loader, Cart Unloader and Railway Observer are ported from [Autowork](https://github.com/Ict00/autowork) by Ict00 (MIT).
- Inspired by the TrainCarts plugin (MIT). No TrainCarts code is included.
