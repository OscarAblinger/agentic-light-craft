---
name: conventional-commit
description: 'Guides agents to generate conventional commit messages in line with the Conventional Commits v1.0.0 specification, including YouTrack issue references (LIGHT-XXXX) when applicable.'
---

### Purpose

This skill guides agents to create standardized, descriptive commit messages for git commits following the [Conventional Commits v1.0.0 specification](https://www.conventionalcommits.org/en/v1.0.0/#specification). It also covers how to reference YouTrack issues for this project.

When creating git commits in this project, agents MUST use this skill to ensure commits follow the conventional commit format.

### Commit Message Format

All commit messages MUST follow this format:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Structural Elements

- **fix:** A commit of the type `fix` patches a bug in your codebase (correlates with PATCH in Semantic Versioning).
- **feat:** A commit of the type `feat` introduces a new feature to the codebase (correlates with MINOR in Semantic Versioning).
- **chore:** A commit of the type `chore` is used for maintenance tasks, dependencies, build tooling, etc.
- **docs:** A commit of the type `docs` is used for documentation changes only.
- **BREAKING CHANGE:** In this project, breaking changes are indicated by appending a `!` after the type/scope in the commit message (e.g., `feat!:` or `feat(api)!:`). A breaking change indicates an API change that breaks backward compatibility (correlates with MAJOR in Semantic Versioning). When using `!`, the breaking nature should be explained in the description or body as needed.
- **types used in this project:** `feat`, `fix`, `chore`, `docs`.
- **scope:** Optional. A noun describing a section of the codebase surrounded by parentheses, e.g., `feat(parser): add ability to parse arrays`.
- **description:** REQUIRED. A short, imperative summary of the change immediately following the colon and space.
- **body:** Optional. Additional contextual information about the code changes. Must begin one blank line after the description. Free-form, may consist of any number of newline-separated paragraphs.
- **footers:** Optional. One or more footers may be provided one blank line after the body. Each footer MUST consist of a word token, followed by either a `:<space>` or `<space>#` separator, followed by a string value. Use `-` instead of whitespace in token names (except `BREAKING CHANGE`).
- **Breaking change indicator:** This project uses the `!` syntax exclusively. Append `!` immediately before the `:` in the type/scope prefix to indicate a breaking change. The description SHALL describe the breaking change when appropriate.

### Key Rules

From the [specification](https://www.conventionalcommits.org/en/v1.0.0/#specification):
- Commits MUST be prefixed with a type, followed by optional scope, optional `!`, and REQUIRED terminal colon and space.
- The type `feat` MUST be used when a commit adds a new feature to your application or library.
- The type `fix` MUST be used when a commit represents a bug fix for your application.
- A longer commit body MAY be provided after the short description. The body MUST begin one blank line after the description.
- One or more footers MAY be provided one blank line after the body.
- Breaking changes MUST be indicated in the type/scope prefix or as an entry in the footer.
- In this project, only the types `feat`, `fix`, `chore`, and `docs` are used.
- Breaking changes MUST be indicated using the `!` syntax in the type/scope prefix (e.g., `feat!:`).

### YouTrack Issue References

This project uses YouTrack at [https://light-craft.youtrack.cloud/projects/LIGHT](https://light-craft.youtrack.cloud/projects/LIGHT).

When referencing issues:
- **Format:** Use `LIGHT-XXXX` (e.g., `LIGHT-1`, `LIGHT-42`). Do **NOT** prefix with `#` (YouTrack does not allow `#LIGHT-1`).
- **Placement:** Include issue references in the footer section when applicable.
- **Optional:** Not every commit needs an associated issue. Only reference issues if there is one relevant to the change.
- **Multiple issues:** Multiple issue references can be included if appropriate, each as part of footer(s).
- **Examples of footer usage with issues:**
  - `Refs: LIGHT-123` (using `Refs:` token as in git trailer convention)
  - `Fixes: LIGHT-123`
  - `Closes: LIGHT-123`

### Examples

```text
feat(parser): add ability to parse arrays
```

```text
fix(ui): correct button alignment
```

```text
docs: update README with usage instructions
```

```text
chore: update dependencies
```

```text
feat!: send email on registration
```

```text
fix: prevent racing of requests

Introduce a request id and a reference to latest request. Dismiss
incoming responses other than from latest request.

Remove timeouts which were used to mitigate the racing issue but are
obsolete now.

Reviewed-by: Z
Refs: LIGHT-123
```

```text
feat(api)!: send an email to the customer when a product is shipped
```

### Validation Checklist

- [ ] Type is one of: `feat`, `fix`, `chore`, `docs`.
- [ ] Scope is in parentheses if used (e.g., `feat(api): ...`).
- [ ] Description is in imperative mood (e.g., "add", "fix", "update", not "added", "fixed", "updated").
- [ ] Body is separated by a blank line from description if present.
- [ ] Footers are separated by a blank line from body if present.
- [ ] If referencing YouTrack issues, use format `LIGHT-XXXX` without `#`.
- [ ] If breaking change, indicated by `!` after type/scope (e.g., `feat!:` or `feat(api)!:`).
