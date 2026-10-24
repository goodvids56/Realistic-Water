# NeoForge and Forge port

Builds upstream's Fabric source (sskaffy/Realistic-Water) for NeoForge and Forge without
forking it. The nightly workflow runs these builds against whatever upstream's current branch
targets.

- `compat/` holds the parts of Fabric API that upstream uses (events, networking, client
  commands, creative tab and fluid rendering hooks, `FabricLoader.getConfigDir`), written
  against plain Minecraft classes. Upstream compiles against it unchanged. The shipped jars
  relocate it to `me.skaffy.port.fabricmc` so it can't clash with a real Fabric API.
- `neoforge/` and `forge/` are standalone Gradle builds that compile upstream's source, `compat/`
  and a small bridge. The bridge runs upstream's entrypoints from its `fabric.mod.json`, fires
  the compat events from the loader's events, registers its payloads, and gives its fluids a
  `FluidType` that matches their water/lava tags.
- `build-logic/` holds the steps both builds share:
  - generating `mods.toml` from `fabric.mod.json`
  - turning the Fabric API access wideners upstream relies on into an access transformer
  - applying `upstream-patches.json`

## When a nightly NeoForge or Forge build fails

Upstream compiles almost as-is. The exceptions are in each loader's `upstream-patches.json`,
exact find/replace rules for code that conflicts with the loader's own patches to Minecraft.
Every rule has a "why". If upstream changes the patched code, the rule stops matching and the
build fails with a message naming it; update the rule to the new code.

The other usual cause is upstream starting to use a Fabric API class that `compat/` doesn't have
yet, which shows up as a missing `net.fabricmc...` symbol. Add it to `compat/`, mirroring Fabric
API's signatures.

## Building locally

Java 25 is needed. With a checkout of upstream next to this repository:

```sh
port/gradlew -p port/neoforge build -Pupstream_dir=/path/to/upstream
port/gradlew -p port/forge build -Pupstream_dir=/path/to/upstream
```

The jars land in `port/<loader>/build/libs/realistic-water-<loader>-<version>.jar`; the `-slim`
jar is the unrelocated intermediate. `runClient` and `runServer` work for testing.
