# Changelog

## 0.2.0 - 2026-10-07

### Added
- **33 mini cars** in four families, each with a Blockbench model, a 3D item icon, its cargo block shown inside, a recipe and a PrideCarts creative tab:
  - **Seat cars:** passenger, first-class (heals and feeds), sleeper (skips the night when everyone rides one), dining, observation and caboose (night vision), prisoner (locked while moving), ambulance (heals, clears poison), maintenance coach (repairs the held tool), crew (haste), subway, tram and high-speed.
  - **Storage cars:** baggage, mail, covered and open boxcar, container, warehouse (54 slots), refrigerated (food only), ore, log, hopper (picks up items), dump (empties beside the track on a powered activator rail), explosives, machinery, flatbed and heavy flatbed.
  - **Tank cars:** tanker, liquid tanker and gas tanker with Forge fluid tanks (buckets and pipes).
  - **Power cars:** battery car (2,000,000 FE) and generator car (burns fuel into FE).
- **Mini engines:** steam, electric and three solar tiers that pull coupled trains. Hold W/S while riding to change the throttle notch. Sneak + right-click with an empty hand opens a control panel with notches, reverse, emergency stop, fuel/FE gauge, solar status, speed, odometer, train list, horn, a smart-track toggle and uncouple.
- **Engine routes:** number Station Tracks by right-clicking them, then give an engine a no-code route in the Route screen (wait, skip, reverse, speed, horn, load, unload, hold at station N). Battery cars charge their train's engines.
- **Cart Chain and Cart Rope couplers:** right-click one cart, then the next, to couple trains of up to 10 carts with spring physics and visible 3D links; shears uncouple. Signs, properties and sensors treat coupled carts as one train, with or without Railcraft.
- **57 special tracks:**
  - Smart tracks: booster (three strengths), launcher, brake, slow and medium speed zones, high-speed, ice, mud, station, holding, delay, reverse, one-way, buffer stop, eject, embark, decoupler, auto-coupler, priming, refuel, engine start / faster / slower / full / stop, whistle, lamp, announcer, healing, music, teleport, rainbow and Pride fireworks.
  - Cargo tracks: pickup, drop-off, fluid fill, fluid drain and item vacuum, which work as carts roll past.
  - Switch tracks: passenger, engine, cargo, tank, redstone, random, alternating, full-cargo and a manual turnout.
  - Detector tracks: any cart (with comparator fill level), train length, engine, passenger and full cargo.
  - A crossing track (X) and **wide curve tracks** (radius 3, 5 or 8) that lay a whole quarter circle from one item; carts ride the true arc and the rails are drawn as a smooth curve.
- **Block signals:** a Signal Track holds a train while the section ahead (up to the next Signal Track) is occupied, and a Pride Signal post beside it shows green or red and outputs redstone.
- **Cart Loader, Cart Unloader and Railway Observer** (ported from Autowork, MIT): fill or empty the cart in front from the storage behind, with a redstone "done" signal, pause on redstone, and sign-based cart name filters. Battery cars charge and discharge at Cart Loaders.
- Route and teleport lines in the debug log (`pridecarts-debug` file).

### Fixed
- Engines follow the track after reversing, ignore sideways bumps and coupling jolts, and turn round on a Reverse Track.
- Couplings are stiff (no bouncing) and chain links are drawn between the couplers.
- A Railcraft single-cart result no longer hides PrideCarts couplings.
- The Signal post only powers out of its back, so it no longer holds its own track red.
- Item and block names load on Cleanroom (added `pack.mcmeta`).
- Switch tracks curve from the side a train enters toward the branch.

### Changed
- PrideCarts now adds blocks, items and entities, so it must be installed on both the server and the clients.
- PrideCarts menus follow the pack-wide menu theme chosen in PrideCanvas.

