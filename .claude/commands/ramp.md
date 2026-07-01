---
description: Map an unfamiliar package before changing it — structure, key files, conventions, tests.
---
Before changing code in an unfamiliar package or directory, build a concise map of it first: $ARGUMENTS

1. List the files and subpackages under the given path.
2. Identify the primary types/classes and their responsibilities (one line each).
3. Skim 2-3 representative files to note conventions in use (naming, error handling, DI/construction style, test style).
4. Identify the corresponding test files and how they're structured (unit vs integration, mocking approach).
5. Report a concise summary: package purpose, key files with one-line descriptions, conventions to follow, and anything surprising or risky to know before editing.

This command is read-only — do not edit any files while ramping up. Keep the summary scannable, not an essay.
