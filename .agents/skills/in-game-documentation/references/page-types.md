# Book page types — writing reference

This is a **writing reference only**. There is no hand-maintained page-type
catalog in this project: the Modonomicon datagen/compiler is the single source of
truth for valid page types and fields, and an invalid type or field fails the
datagen compile. This document describes how to choose between page types when
writing Light Book content.

## Choosing a page type

- **Text** (`modonomicon:text`, `BookTextPageModel`) — the default and preferred
  page for prose. Use it for explanations, guides, lists and tips. Supports
  markdown-like formatting, forced line breaks, lists, `<t>key</t>` translatable
  inline tags, markdown links (including book/category/entry/command/item/
  patchouli links) and macros. Page title and text are separate lang keys.
- **Recipe** (`modonomicon:recipe…`, `BookRecipePageModel` and subclasses such
  as crafting, smelting, blasting, smoking, campfire cooking, smithing and viewer
  recipe pages) — use when the entry is about producing an item; the page renders
  the recipe and can hold title/text plus recipe references. Prefer over plain
  text once a crafting/production step exists in code.
- **Multiblock** (`BookMultiblockPageModel`) — use when the content describes a
  multiblock structure; renders the structure name, text and a visualize button.
- **Entity** (`BookEntityPageModel`) — use when the content is about an entity
  (behaviour, properties, notes).
- **Image** (`BookImagePageModel`) — use when the content needs pictures; supports
  display modes, border, image scale mode and text.
- **Spotlight** (`BookSpotlightPageModel`) — use when the entry should highlight a
  single item/block with accompanying text.
- **Empty** (`BookEmptyPageModel`) — reserved for placeholders; avoid unless a page
  legitimately renders nothing.

## Rules of thumb

- Write the simplest page that conveys the information; do not add image or
  multiblock pages for content that is plain prose.
- Every page needs a resolvable title and text (lang keys generated from the
  ModLanguageProvider) and a valid condition or no condition; conditions must match
  real gameplay unlocks or stay unused until they do.
- Links (`entry://…`, `category://…`, `command://…`) must reference pages/entries
  that exist; the reference integrity check catches broken links.

## Drift detection (optional)

An optional jar-based drift check can compare the compiled-in page types against
the API used by the datagen; it is not required while the datagen/compiler remains
the catalog.
