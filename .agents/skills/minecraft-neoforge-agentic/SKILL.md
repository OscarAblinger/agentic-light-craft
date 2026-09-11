---
name: minecraft-neoforge-agentic
description: "Agentic coding workflow for implementing Minecraft Java Edition mods with NeoForge. Use for creating, implementing, debugging, testing, reviewing, or extending a Minecraft mod. This skill is intentionally constrained to the project's chosen stack: Minecraft 26.1.x, NeoForge, JDK 25, Gradle, and the official NeoForge MDK/project generator. Read the repository's minecraft-mod-setup.md before making architectural or version decisions."
---

# Minecraft NeoForge Agentic Coding

## Mission

Act as an implementation agent, not a tutorial writer.

For each task:

1. inspect the repository and establish the current state;
2. read `minecraft-mod-setup.md` when present and treat it as the project source of truth;
3. turn the requested feature into a small implementation plan;
4. implement the feature using current NeoForge/vanilla APIs;
5. run the narrowest useful checks first, then the required project verification;
6. inspect failures and iterate;
7. leave the repository in a buildable, testable state.

Do not replace working project decisions with generic Minecraft-mod examples from older versions.

## Fixed project contract

Unless the repository's own `minecraft-mod-setup.md` explicitly changes it:

- Minecraft: `26.1.x` only
- Exact Minecraft patch: pinned in Gradle/project configuration
- Loader: NeoForge only
- Java: JDK 25
- Build: Gradle wrapper
- Starting template: official NeoForge MDK / project generator
- IDE: IntelliJ IDEA + MinecraftDev
- Source control: Git

Do not add Fabric, legacy Forge, multi-loader abstractions, snapshots, or automatic Minecraft upgrades.

If the exact Minecraft/NeoForge patch is not pinned yet, determine it from the existing project or the current official NeoForge MDK/project generator before implementing version-sensitive code. Never invent a version.

## Source and API authority

Prefer, in order:

1. the existing project's implementation and conventions;
2. the project's `minecraft-mod-setup.md`;
3. official NeoForge documentation and the matching official NeoForge MDK;
4. vanilla Minecraft APIs for the pinned version;
5. community examples only as secondary references.

Use Mixins only when an appropriate NeoForge or vanilla extension point cannot implement the requirement.

Do not copy code from an older Minecraft version merely because its structure looks familiar. Confirm names, signatures, lifecycle events, registration APIs, data formats, and client/server boundaries for the pinned version.

## Repository reconnaissance

Before editing:

- inspect `settings.gradle`, `build.gradle`, `gradle.properties`;
- inspect `gradle/wrapper/gradle-wrapper.properties`;
- inspect `src/main/java`, `src/main/resources`, and generated-resource configuration;
- identify the mod id, package structure, entrypoints, registrations, and existing tests;
- check Git status;
- locate `minecraft-mod-setup.md`.

If the repository is empty or is not a NeoForge 26.1.x project, do not silently convert it. For a new mod, start from the official NeoForge MDK/project generator and then apply the project's configuration.

## Agentic workflow

### Phase 1 — Understand

Classify the task:

- content: items, blocks, recipes, loot, tags, models, sounds;
- gameplay: events, interactions, effects, inventories, entities;
- worldgen: features, biomes, structures, dimensions;
- networking: payloads, synchronization, client/server state;
- client: rendering, screens, keybindings, HUD;
- integration: another mod/API;
- infrastructure: Gradle, data generation, tests, CI;
- bug: compile failure, startup crash, runtime behavior, dedicated-server issue.

For ambiguous requirements, infer only what is necessary from the repository. Ask a focused question when a wrong assumption would materially change the implementation.

### Phase 2 — Plan

Write a compact plan before making substantial edits.

The plan should identify:

- files to add/change;
- registrations and APIs involved;
- data-generation changes;
- client/server boundaries;
- tests or manual verification;
- build/run commands.

Prefer the smallest design that fits existing architecture.

### Phase 3 — Implement

Implement vertically rather than creating disconnected scaffolding.

Typical order:

1. core Java/domain code;
2. NeoForge registrations/events;
3. server/common behavior;
4. client-only behavior;
5. generated resources/data;
6. tests and verification.

Keep common/server code free of client-only references.

Keep rendering, screens, keybindings, HUD code, and client-only event handlers in client-only code.

Use data generation for generated resources such as recipes, loot tables, tags, and models where the project is configured for it.

Centralize versions in Gradle configuration. Every external dependency must have an explicit version and a documented purpose.

### Phase 4 — Verify

Run the smallest relevant check after each meaningful change.

At minimum, for a completed feature:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

If the project exposes more targeted tests, run those too.

For features intended to work on dedicated servers, explicitly verify dedicated-server compatibility. Do not treat successful client startup as proof of server compatibility.

When possible, verify the actual behavior in-game rather than stopping at compilation.

### Phase 5 — Debug

When a check fails:

1. classify the failure as configuration, compilation, data/resource loading, startup, runtime, client-only, server-only, or integration;
2. read the relevant error and surrounding stack trace;
3. inspect the project's actual API usage;
4. make the smallest targeted correction;
5. rerun the failed check;
6. rerun broader verification after the local failure is fixed.

Do not paper over errors by disabling checks, suppressing exceptions, adding arbitrary dependencies, or changing versions.

For crashes, preserve the useful root-cause information and inspect the first relevant mod-owned frame rather than guessing from the final exception line.

## Version discipline

Minecraft 26.1 has a changed toolchain: Mojang removed obfuscation and Minecraft uses Java 25. Gradle 9.1.0+ is required by the NeoForge 26.1 release guidance.

Do not assume 1.21.x conventions apply.

Before using a version-sensitive API, verify it against the pinned 26.1.x project.

If an upgrade is explicitly requested:

1. update the pinned versions;
2. update affected dependencies/tooling;
3. fix compilation and runtime incompatibilities;
4. run client and dedicated-server verification;
5. update `minecraft-mod-setup.md`.

Do not upgrade Minecraft or NeoForge merely to fix an implementation problem.

## Architecture rules

### Registration

Follow the registration mechanism already used by the project. Do not introduce a second registration style without a reason.

### Events

Prefer NeoForge events when an event hook exists. Keep handlers narrow and avoid global work when a more specific lifecycle or event is available.

### Networking

Treat client and server as separate execution environments. Validate authority on the server and synchronize only the state the client actually needs.

### Persistence

Use the appropriate vanilla/NeoForge persistence mechanism for the state being stored. Avoid static mutable state for world/player data.

### Performance

Do not optimize speculatively.

Avoid heavy work on the render thread. If a change is performance-sensitive, measure or profile before optimizing.

### Dependencies

Do not add a library for functionality already provided by NeoForge or vanilla Minecraft.

When adding a dependency, document:

- exact version;
- purpose;
- required side/environment;
- why NeoForge/vanilla is insufficient.

### Mixins

Mixins are a last resort.

Before adding one, explicitly check for:

- a NeoForge event;
- a NeoForge extension point;
- a vanilla API;
- a registration hook;
- a supported capability/data mechanism.

If a Mixin is unavoidable, keep its scope minimal and document why.

## Data and resources

For generated content:

- keep source definitions in the expected generator locations;
- regenerate instead of manually maintaining generated files when the project uses data generation;
- verify generated paths and identifiers;
- test loading in the game.

Always check namespace/id consistency across Java registrations, resource paths, tags, recipes, models, and localization.

## Client/server safety

Before finalizing a feature, ask:

- Can this class be loaded on a dedicated server?
- Does any common code import client-only classes?
- Is client state being used as authoritative game state?
- Does the feature need networking?
- Are client-only registrations isolated?
- Does `runServer` start successfully?

A feature that works in `runClient` but crashes or classloads incorrectly on a dedicated server is incomplete.

## Testing strategy

Use layered verification:

1. static/project inspection;
2. compile/build;
3. targeted automated tests, if present;
4. client startup;
5. dedicated-server startup;
6. feature-specific in-game behavior;
7. regression check for adjacent behavior.

Do not create elaborate test infrastructure for trivial content unless the repository already uses it.

For complex gameplay, prefer deterministic tests around logic plus a small number of GameTest/in-game checks where practical.

## Git discipline

Do not commit:

- `build/`
- `.gradle/`
- IDE state
- run directories
- logs/crash reports
- local configuration
- secrets
- local caches

Do commit source, resources, Gradle configuration, wrapper files, mod metadata, and project documentation.

Before completion, inspect `git diff` and `git status`.

Do not reset or discard unrelated user changes.

## Completion checklist

A task is complete only when applicable items are true:

- [ ] Requirement is implemented, not merely scaffolded.
- [ ] Existing project conventions are followed.
- [ ] No unnecessary dependency was introduced.
- [ ] No unnecessary Mixin was introduced.
- [ ] Common/server code has no client-only references.
- [ ] Generated resources are correctly generated or maintained.
- [ ] `./gradlew build` passes.
- [ ] `./gradlew runClient` starts successfully.
- [ ] `./gradlew runServer` starts successfully when relevant.
- [ ] Feature behavior has been verified at the appropriate level.
- [ ] No unrelated files were changed.
- [ ] `git diff` contains only intentional changes.
- [ ] Documentation is updated when project decisions or setup changed.

## Research workflow

When API behavior is uncertain:

1. identify the exact pinned Minecraft and NeoForge versions;
2. consult the matching official NeoForge MDK/docs;
3. inspect the project's own dependency/API usage;
4. search for the exact class/method/event name;
5. prefer current official examples over old tutorials;
6. only then implement.

When web access is available, favor the official NeoForge sources and the version-matched MDK repositories.

## New-project bootstrap

For a brand-new mod:

1. obtain the official NeoForge MDK/project generator output for the selected 26.1.x patch;
2. initialize the repository from that clean template;
3. configure JDK 25 and the Gradle wrapper;
4. set mod metadata/package identifiers;
5. run `./gradlew build`;
6. run `./gradlew runClient`;
7. run `./gradlew runServer`;
8. establish Git and `.gitignore`;
9. record the exact pinned versions in `minecraft-mod-setup.md`;
10. begin feature implementation.

Do not build a new project by reconstructing Gradle files from a tutorial.

## Existing skills: use as references, not as authority

There are community skills that can provide useful workflow ideas, including:

- `Riloox/minecraft-mod-builder`: end-to-end Claude Code mod-building workflow for Fabric/NeoForge.
- `chouzz/minecraft-mod-dev`: NeoForge/Fabric workflow plus integrations and dynamic documentation.
- `Jahrome907/minecraft-agent-skills`: broader Codex/Claude Minecraft skill bundle with a `minecraft-modding` skill.
- `xingluo01/neoforge-dev-1.21.1-skill`: orchestration, QA, debugging, and release patterns, but it targets Minecraft 1.21.1 and therefore is not a version authority for this project.

Use these for workflow patterns only. This skill deliberately narrows the target to the project's 26.1.x + NeoForge + JDK 25 contract.

## Key principle

The agent should optimize for **correctness against the actual repository and pinned 26.1.x APIs**, not for producing plausible Minecraft code quickly.
