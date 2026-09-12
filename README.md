# LightCraft

A Minecraft mod for NeoForge 26.1.x.

## Agentic Approach

I've used this project to try out agentic coding approaches.
From local LLMs to OpenRouter, I want to try out how modern heavily AI-assisted programming looks like.

### Why a minecraft mod?

On the one hand, because I had the idea for it for a long time and never the time to implement it.
On the other hand, because it seems niche enough to find out how AI deals with something less common than simply
creating a website (which I've seen it do hundreds of times before), while at the same time also offering a lot
of documentation and one of the most common programming languages.

## Project stack

- **Minecraft:** 26.1.2
- **Mod loader:** NeoForge 26.1.2.107
- **Java:** JDK 25
- **Build system:** Gradle 9.2.1

## Getting started

Import the project into IntelliJ IDEA with the MinecraftDev plugin.

Common Gradle tasks:

```bash
./gradlew build      # Build the mod jar
./gradlew runClient  # Run the Minecraft client
./gradlew runServer  # Run a dedicated server
./gradlew runData    # Generate data-driven resources
```

## Project decisions

See [research/0_basic_setup.md](research/0_basic_setup.md) for the source-of-truth project configuration.

