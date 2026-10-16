# Agent Instructions

After each change in code, make sure to run verification and commit the changes to git.

## Verification

Before considering any change complete, agents must verify that it works by running several relevant Gradle tasks.

At minimum, run gradle tasks appropriate to the change, such as:

- `build` if any source code was changed.
- `check` if any tests or tested code was changed.
- `runData` once a build is successful if any resources were changed or large changes were done.

Additionally, if bigger features were implemented or gameplay bugs

you can also ensure that `runServer` does not throw an error, though be aware that the task runs forever
if successful.
Request user feedback if you do this as they will have to manually test it.

If the change only affects a specific module, feature, or test area, prefer targeted Gradle tasks first, then run broader validation when practical.

After running verification, summarize:

- which Gradle tasks were run
- whether they passed or failed
- any notable warnings, failures, or skipped checks

Agents must also ask the user for feedback before committing changes.

## Textures

Any new or changed pixel-art texture must follow the project texture style guide
and pass its checker before committing.

- Load the `lightcraft-texture-style` skill; it defines the workflow and rules.
- Locked palette: `.agents/skills/lightcraft-texture-style/palette.txt`.
- Style rules: `.agents/skills/lightcraft-texture-style/references/style-spec.md`.
- Run the checker (`.agents/skills/lightcraft-texture-style/scripts/TextureCheck.java`)
  with the matching category file from `references/categories/`, and report
  hard failures and warnings.
- The palette and style spec are `LOCKED`; `plants` and `ores` are deliberately
  palette-exempt. Do not invent colours or rules; propose changes and ask the
  user.

## Commit Policy

After verification passes and the user confirms the result, agents should commit the changes.

Commits must follow the Conventional Commits format.
Load the relevant skill for that.

Do not commit until:

1. the relevant Gradle verification tasks have been run,
2. the result has been reported to the user,
3. if user feedback was requested, until the feedback was provided.