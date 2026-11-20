---
name: in-game-documentation
description: >-
  Author and audit LightCraft's in-game documentation for the Light Book
  (Modonomicon). Use when writing book categories, entries and pages, when
  checking documentation against actual gameplay code, or when running
  documentation consistency / correctness audits. Covers the generated book
  data, the authoring workflow, the knowledge/doc-structure.md reading order
  map, and the documentation scripts. Search keywords: guide, guidebook, book,
  Modonomicon, page, entry, category, docs, documentation.
---

# In-Game Documentation

## Purpose

Maintain LightCraft's in-game documentation: the **Light Book** (`lightcraft:light_book`), the item added by the player outside this skill and rendered in-game by Modonomicon.

For each documentation effort the deliverable is the documentation files plus a
quick overview of what was added.

## Golden rules

1. **Behaviour is the truth.** Documentation describes behaviour; it never drives gameplay changes, and gameplay is never changed to make documentation accurate. Never change behaviour to match docs, and never change docs to cover behaviour that has not been properly read in code. This skill covers documentation only; no functionality changes outside documentation functionality.
2. **No guessing.** Read the relevant code properly before writing or judging anything. If unsure what something does, or whether to include something, ask the user. Otherwise decide inclusion yourself.
3. **Highest documentation quality over speed.** Only the pre-push check may be quick and may be wrong; the manual audits are the source of truth for quality.

## Authoring workflow

- Book content is authored as Modonomicon datagen providers under
  `src/main/java/dev/ablinger/lightcraft/data/book/` (the existing
  `dev.ablinger.lightcraft.data.book` package; e.g. `LightBook`,
  category/entry providers).
- The book item itself is a normal new item added by the user outside this
  skill; the skill assumes it already exists and must not recreate it.
- Generated book JSON is written to
  `src/generated/resources/data/lightcraft/modonomicon/books/light_book/` and all
  English strings come from the single `ModLanguageProvider`
  (`assets/lightcraft/lang/en_us.json`) through a shared `LanguageProviderCache`
  that the Modonomicon book provider fills. The book provider must be registered
  before the language provider; wrong order silently produces raw translation keys.
- Generated resources are tracked in git. After running data generation, inspect
  `git status` and keep only book-owned generated changes; report unrelated
  regeneration instead of committing it.

Regenerate with:

```bash
./gradlew runData
```

## Page types

- A writing reference for choosing page types lives at
  `references/page-types.md`. There is no hand-maintained page-type catalog in
  this project: the Modonomicon datagen/compiler is the catalog; an invalid type
  or field fails the datagen compile. The writing reference describes page
  choices only, with optional jar-based drift detection.

## Reading order map

- `knowledge/doc-structure.md` is the source of truth for the Light Book's
  reading order / information architecture.
- The skill creates and maintains this file, but must not prescribe a structure
  of its own; the skill describes where the file is, how to use it, and when/how
  to update it.
- Audits read the book in the order defined there and ignore extra information in
  the map that is not part of the book structure.

## Scripts

All scripts live in `.agents/skills/in-game-documentation/scripts/` and are plain
Java (JDK only, no runtime dependencies).

Compile once into a temporary classes directory:

```bash
javac -d /tmp/in-game-doc-classes \
  .agents/skills/in-game-documentation/scripts/Json.java \
  .agents/skills/in-game-documentation/scripts/Report.java \
  .agents/skills/in-game-documentation/scripts/Tool.java \
  .agents/skills/in-game-documentation/scripts/BookTree.java \
  .agents/skills/in-game-documentation/scripts/Render.java \
  .agents/skills/in-game-documentation/scripts/ReferenceIntegrity.java \
  .agents/skills/in-game-documentation/scripts/LayoutCollision.java \
  .agents/skills/in-game-documentation/scripts/DocStructureSync.java
```

Scripts (all take `--root <project-root>`):

| Script | Purpose |
| --- | --- |
| `Render` | Renders all pages of a book into one output file (used by checks and the consistency audit). |
| `ReferenceIntegrity` | Validates entry/command/book/category/item links and inline translations. |
| `LayoutCollision` | Detects two entries using the same layout cell within a category. |
| `DocStructureSync` | Compares book structure against `knowledge/doc-structure.md`. |

Example runs for the Light Book:

```bash
java -cp /tmp/in-game-doc-classes Render \
  --root <project-root> --book lightcraft:light_book --out /tmp/light-book-render.txt
java -cp /tmp/in-game-doc-classes ReferenceIntegrity \
  --root <project-root> --book lightcraft:light_book
java -cp /tmp/in-game-doc-classes LayoutCollision \
  --root <project-root> --book lightcraft:light_book
java -cp /tmp/in-game-doc-classes DocStructureSync \
  --root <project-root> --book lightcraft:light_book --doc knowledge/doc-structure.md
```

Shared options: `--root <path>` (required), `--book <book-id>` (optional, defaults
to the only book found), `--out <path>` (optional file output, default stdout),
`--doc <path>` (optional doc-structure path, default `knowledge/doc-structure.md`).

Exit codes: `0` = pass / help, `1` = check failed (findings reported), `2` = tool
error (bad arguments, missing inputs).

## Audits

### Consistency audit (manual, required for quality decisions)

1. Read all documentation at once and nothing else.
2. Read the book in `knowledge/doc-structure.md` order; ignore extra information
   in the map that is not part of the book structure.
3. Act like a player who knows nothing and must learn the mod purely through the
   documentation.
4. Flag any inconsistency between documentation, layout and player-facing
   behaviour.

### Correctness audit (manual, required for quality decisions)

1. Enumerate all pages of the book.
2. Check one page at a time together with only the relevant code; no other
   documentation pages and no unrelated code.
3. Report a green check per page; for issues give a detailed breakdown.

### Audit reports

- Reports are chat-only by default; use a temp file only when the output is too
  long; never commit audit output files.

### Pre-push gate (quick, may be wrong)

- Run `Render`, `ReferenceIntegrity`, `LayoutCollision`, and `DocStructureSync`.
- The gate must flag **both** new documentation that is needed and stale existing
  pages; it must fail when new documentation is needed or when existing pages are
  stale.
- The pre-push check is allowed to be quick and may be wrong; it is not a substitute
  for the manual audits.

## Authoring checklist

When adding a category, entry or page:

1. Read the relevant gameplay code; write documentation that matches actual behaviour.
2. Add the provider under `src/main/java/dev/ablinger/lightcraft/data/book/`.
3. Run `./gradlew runData` and inspect `git status`; keep only book-owned generated changes.
4. Run the four scripts; fix findings.
5. Update `knowledge/doc-structure.md` if the IA changed.
6. Run the manual consistency and correctness audits.
