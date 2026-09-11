# Research notes

## Sources reviewed

- Official NeoForge MDK organization: https://github.com/NeoForgeMDKs
- Official 26.1 ModDevGradle template: https://github.com/NeoForgeMDKs/MDK-26.1-ModDevGradle
- Official NeoForge MDK mirror source: https://github.com/neoforged/MDK
- NeoForge 26.1 release notes: https://github.com/neoforged/websites/blob/main/content/news/26.1release.md
- Riloox/minecraft-mod-builder: https://github.com/Riloox/minecraft-mod-builder
- chouzz/minecraft-mod-dev: https://github.com/chouzz/minecraft-mod-dev
- Jahrome907/minecraft-agent-skills: https://github.com/Jahrome907/minecraft-agent-skills
- xingluo01/neoforge-dev-1.21.1-skill: https://github.com/xingluo01/neoforge-dev-1.21.1-skill

## Findings

There are already useful agent skills. The closest bases are `minecraft-mod-builder` for an end-to-end Claude Code workflow and `minecraft-agent-skills` for Codex/Claude skill packaging. `minecraft-mod-dev` is useful for documentation lookup and mod integrations.

However, none of the reviewed projects should be treated as the project's version authority: their documented examples primarily target Minecraft 1.21.x or provide multi-version/multi-loader workflows.

The official NeoForge MDK organization currently publishes 26.1.x templates, including ModDevGradle and NeoGradle variants. The 26.1 ModDevGradle template is therefore the correct project bootstrap reference for this skill.

NeoForge's 26.1 release guidance states that Minecraft 26.1 uses Java 25 and requires Gradle 9.1.0 or newer. It also documents the removal of obfuscation and the resulting availability of official parameter names. These are important reasons not to blindly reuse 1.21.x agent instructions.

The project-specific `minecraft-mod-setup.md` remains the highest-priority local source of truth. It explicitly fixes Minecraft 26.1.x, NeoForge, JDK 25, Gradle, the official MDK/project generator, and the client/server verification commands.
