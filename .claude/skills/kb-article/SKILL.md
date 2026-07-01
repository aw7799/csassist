---
name: kb-article
description: Create Knowledge Base articles in the CSAssist KB format. Use when adding KB content.
---
# KB article authoring
1. Read kb-mcp/data/_schema.json for the article shape.
2. Write one JSON file per article under kb-mcp/data/articles/.
3. Fields: id, title, category, body (markdown), tags.
4. Titles are action-oriented (e.g. "Reset your password").
5. After writing, run: cd kb-mcp && npm test
