# Minecraft Mod Development — Project Decisions

> **Source of truth for agentic coding work.**
> **Decision date:** 2026-09-10

## Fixed project configuration

| Setting | Decision |
|---|---|
| Mod name | LightCraft |
| Mod id | `lightcraft` |
| Root Package Name | dev.ablinger.lightcraft |
| Minecraft | **26.1.2** |
| Minecraft version policy | **26.1 only initially** |
| NeoForge | **26.1.2.107** |
| Mod loader | **NeoForge** |
| Loader policy | **NeoForge only initially** |
| Java | **JDK 25** |
| Gradle | **9.2.1** |
| Build system | **Gradle** |
| Modding toolchain | **Official NeoForge MDK / project generator** |
| IDE | **IntelliJ IDEA + MinecraftDev** |
| Source control | **Git** |

## Version policy

- Use **Minecraft 26.1.x** only.
- Pin the exact Minecraft patch version in Gradle.
- Pin the exact NeoForge version in Gradle.
- Do not target snapshots.
- Do not add support for other Minecraft versions unless explicitly decided later.
- Do not upgrade Minecraft automatically.

## Loader policy

- Use **NeoForge**.
- Do not support Fabric initially.
- Do not support legacy Forge.
- Do not add a multi-loader abstraction initially.
- Re-evaluate multi-loader support as a separate project decision if needed later.

## Java / Gradle

- Use **JDK 25** for development and Gradle.
- Use the **Gradle wrapper** committed to the repository.
- Run builds and development tasks through the wrapper.

Common commands:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

## Project template

Start from the **official NeoForge MDK / project generator for Minecraft 26.1.x**.

Do not base the project on setup files or tutorials for another Minecraft version.

## IDE

Use:

- **IntelliJ IDEA**
- **MinecraftDev** plugin

The free IntelliJ edition is sufficient.

## Repository policy

Commit:

- `src/`
- project resources
- `build.gradle`
- `gradle.properties`
- `settings.gradle`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/`
- mod metadata
- project documentation

Do not commit:

- `build/`
- `.gradle/`
- IDE state
- run directories
- logs and crash reports
- local configuration
- secrets
- local caches

Use the repository `.gitignore`.

## Code architecture

### Common/server code

Must not reference client-only classes.

### Client-only code

Keep the following client-only:

- rendering
- screens
- keybindings
- HUD code
- client-only event handlers

### NeoForge APIs

Prefer, in this order:

1. NeoForge APIs
2. Vanilla Minecraft APIs
3. Mixins only where required

Do not use Mixins when an appropriate NeoForge or vanilla API exists.

### Data generation

Use data generation for generated resources, including:

- recipes
- loot tables
- tags
- models

### Versions and dependencies

Keep version declarations centralized in the Gradle configuration.

Every external dependency must have an explicit version and a documented purpose.

Do not add a dependency solely for convenience when the required functionality is already available through NeoForge or vanilla Minecraft.

## Compatibility assumptions

The project targets **Minecraft 26.1.x / NeoForge**.

Do not make compatibility with another Minecraft version a requirement unless explicitly requested.

For integrations with other mods:

- check that the dependency has a **26.1-compatible release** before implementing hard integration;
- keep optional integrations optional where practical;
- isolate integration code from the core implementation.

Current compatibility notes:

| Mod | Project assumption |
|---|---|
| Create | **Do not require for 26.1** |
| Mekanism | **Do not require for 26.1** |
| Applied Energistics 2 | **26.1-compatible** |
| JEI | **26.1-compatible** |
| Sodium | **26.1-compatible** |
| Iris | **26.1-compatible** |
| Xaero's Minimap | **26.1-compatible** |
| Jade | **26.1-compatible** |
| AppleSkin | **26.1-compatible** |
| Waystones | **26.1-compatible** |
| CC: Tweaked | **26.1-compatible** |
| Twilight Forest | **Do not require for 26.1** |

These entries are dependency constraints, not requirements to include every mod.

## Testing

At minimum, verify:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

To test against other mods (e.g. JEI), declare their Maven coordinates in the `modpackMods`
configuration in `build.gradle`; Gradle copies them into `run-modpack/mods/` before
`./gradlew runServerModpack` / `./gradlew runClientModpack` (or via `./gradlew syncModpackMods`;
`./gradlew cleanModpackMods`, which `./gradlew clean` also runs, empties the folder). The two
*Modpack runs share the
`run-modpack` game directory, so they always load the same mods. Pin Modrinth version IDs or
CurseForge file IDs: plain version numbers are ambiguous across mod loaders and Minecraft
versions. Jars can also still be placed in the folder manually.

Test dedicated-server compatibility for every feature that is intended to work on servers.

## Performance

- Do not optimize without a measurable problem.
- Profile before making performance-sensitive changes.
- Do not perform heavy work on the render thread.

## Upgrade policy

Minecraft and NeoForge upgrades require an explicit project decision.

When upgrading:

1. update the pinned versions;
2. update affected dependencies;
3. fix compilation/runtime incompatibilities;
4. run client and dedicated-server tests;
5. update this document.

## Initial setup sequence

1. Generate the official **NeoForge 26.1.x** project.
2. Set the project and Gradle JVM to **JDK 25**.
3. Import the project into **IntelliJ IDEA**.
4. Install/configure **MinecraftDev**.
5. Verify `./gradlew build`.
6. Verify `./gradlew runClient`.
7. Verify `./gradlew runServer`.
8. Initialize Git and commit the clean template.
9. Begin implementation.

## Final decision

**Minecraft 26.1.x + NeoForge + JDK 25 + Gradle + official NeoForge MDK + IntelliJ IDEA/MinecraftDev.**
