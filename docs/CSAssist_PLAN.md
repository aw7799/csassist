# CSAssist — Build Plan (Mode 2, monorepo)

**What:** CSAssist ("Customer Support Assist") — a support-desk demo built to *practice Claude Code engineering workflow* while producing a portfolio piece that feels like a real system.
**How:** One monorepo, two folders. Built with Claude Code in VS Code (or Cursor).
**Primary goal:** learn Claude Code practice — CLAUDE.md, MCP (server + client), slash commands/skills, and hooks — applied to a working app.

> Drop this file in the repo root as `docs/PLAN.md`. In Claude Code you'll say `read docs/PLAN.md` to load it.

---

## 1. What this demonstrates

For your own learning:
- Authoring an MCP server **and** an MCP client (both sides of the protocol).
- The Claude Code extensibility stack: CLAUDE.md, hooks, slash commands/skills, MCP config.
- A design-first, plan-gated, test-verified workflow on a polyglot codebase.

For interviews (FDE / engineering):
- "I built a support-desk system, wrapped its knowledge base as an MCP server, and wired an LLM-driven enrichment component into the app via MCP — all using Claude Code."
- Differentiator vs. the Sentinel project: this MCP layer is *authored with Claude Code*, and includes a hand-built MCP **client**.

---

## 2. Scope (Mode 2)

A running product. A support agent (or the UI) creates/edits/deletes tickets. When a ticket is created, the app's **enrichment component** invokes an LLM that uses the Knowledge Base **over MCP** to find relevant articles, and writes suggestions back onto the ticket.

Two folders in one repo:

| Folder | Stack | Responsibility |
|---|---|---|
| `/service` | Java / Spring Boot | Ticketing API, thin UI, status-transition rules + audit trail, **enrichment component (MCP client + LLM)** |
| `/kb-mcp` | TypeScript / Node | Knowledge Base **MCP server** (tools: `search_kb`, `get_article`, `list_categories`), dual transport |

---

## 3. Architecture — two consumption paths

The KB MCP server is consumed in **two** ways, which is why it exposes **two transports**:

1. **Dev time — Claude Code consumes it (stdio).** While you build, Claude Code connects to the KB server over stdio (registered in `.mcp.json`) so *you* can test the tools: "search the KB for password resets." This proves MCP authoring + CC consumption.
2. **Run time — the app consumes it (HTTP).** The Spring `enrichment` component connects to the KB server over HTTP/Streamable-HTTP and drives an LLM that calls the tools automatically when a ticket is created. This is the Mode 2 product behavior. (HTTP is the now-preferred MCP transport over the older SSE.)

```
RUNTIME (the product)
  New ticket ─▶ CSAssist Service ─▶ Enrichment component ──MCP/HTTP──▶ CSAssist KB
                                     (LLM: Ollama/Groq)   ◀──articles──   (MCP server)
                                          └─▶ writes suggestions onto the ticket

DEV TIME (your workflow)
  You ─▶ Claude Code ──MCP/stdio──▶ CSAssist KB     (test the tools as you build)
         Claude Code ─▶ edits /service and /kb-mcp source
```

---

## 4. Repo structure (monorepo)

```
csassist/
├─ CLAUDE.md                      # project memory — loaded every CC session
├─ .mcp.json                      # registers the KB server for Claude Code (stdio)
├─ .claude/
│  ├─ settings.json               # hooks + permissions
│  ├─ hooks/verify.sh             # runs the right tests after edits
│  ├─ commands/ramp.md            # legacy-path slash command (still supported)
│  └─ skills/
│     └─ kb-article/SKILL.md      # canonical skill: author KB articles
├─ docs/
│  ├─ PLAN.md                     # this file
│  └─ DESIGN-status-transitions.md
├─ service/                       # Spring Boot
│  ├─ pom.xml
│  └─ src/main/java/... resources/templates (thin UI)
└─ kb-mcp/                        # TypeScript MCP server
   ├─ package.json
   ├─ src/
   └─ data/                       # ~12 mock KB articles + _schema.json
```

> Note on commands vs skills: as of Claude Code v2.1.101 (April 2026), custom slash commands were merged into **skills**. The canonical location is `.claude/skills/<name>/SKILL.md`; the old `.claude/commands/*.md` path still works. Both are invoked as `/name`; if both exist with the same name, the skill wins. Skills add auto-invocation (Claude can pull them in when relevant). We use one of each so you learn both.

---

## 5. The Claude Code feature surface (the learning core)

### CLAUDE.md — always-loaded project memory
Conventions, build/test commands, architecture in one short file. See seed in §7.

### MCP — both sides
- **Server** (`/kb-mcp`): author tools with the official `@modelcontextprotocol/sdk`; expose stdio + HTTP.
- **Register for Claude Code** (`.mcp.json`, stdio) so CC can use it at dev time.
- **Client** (`/service`): build an MCP client in Spring that connects over HTTP and drives an LLM. Recommended: **Spring AI**, which provides MCP client support and model integrations for Ollama and OpenAI-compatible endpoints (Groq). *Verify current Spring AI artifact versions when you scaffold.*

### Slash commands / skills
- `/ramp` (command): map an unfamiliar package before changing it.
- `kb-article` (skill): author KB articles in the correct schema, consistently.

### Hooks — automated verification
A `PostToolUse` hook (matcher `Edit|Write`) runs your tests/formatter automatically after every edit, turning the verification loop into something the tooling enforces. See seed in §8.

---

## 6. Tech choices (defaults — override as you like)

- **Service:** Spring Boot (Java 21), Maven. Thin UI via Thymeleaf server-rendered pages (simplest — no separate frontend build). Don't gold-plate the UI.
- **Enrichment:** Spring AI for the MCP client + LLM call.
- **Runtime model:** **Ollama** locally by default (free, no keys, mirrors Sentinel's local-model use); **Groq** as a swappable OpenAI-compatible alternative. The KB server doesn't care which.
- **KB server:** TypeScript, `@modelcontextprotocol/sdk`, stdio + Streamable-HTTP, ~12 mock articles as JSON.

---

## 7. CLAUDE.md seed (paste at repo root once it exists)

```markdown
# CSAssist — Project Memory

## What this repo is
A support-desk demo to practice Claude Code workflow. Monorepo, two folders:
- /service  — Java/Spring Boot: ticketing API, thin Thymeleaf UI, status rules,
              audit trail, and an enrichment component (MCP client + LLM).
- /kb-mcp   — TypeScript Knowledge Base MCP server (stdio + HTTP).

## Architecture (Mode 2)
On ticket creation, /service's enrichment component calls an LLM (Spring AI,
Ollama/Groq) that uses /kb-mcp tools over MCP/HTTP to find relevant articles,
then writes suggestions onto the ticket.

## Build & test
- Service:   cd service && ./mvnw test
- KB server: cd kb-mcp && npm run build && npm test

## Conventions
- Plan before non-trivial change; wait for approval (use plan mode).
- Test-first: write/adjust the test, implement, then run the suite.
- Conventional commits: feat/fix/chore(scope): summary.
- IMPORTANT: never mark a task done until its tests pass.
```

## 8. .claude/settings.json seed (hooks + permissions)

```json
{
  "permissions": {
    "allow": ["Bash(./mvnw *)", "Bash(npm *)", "Bash(git diff*)"],
    "deny": ["Read(./**/.env)"]
  },
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "bash .claude/hooks/verify.sh", "timeout": 180 }
        ]
      }
    ]
  }
}
```

`.claude/hooks/verify.sh` (runs the right suite based on what changed):
```bash
#!/usr/bin/env bash
set -e
if git diff --name-only | grep -q '^kb-mcp/'; then
  (cd kb-mcp && npm run -s build && npm test) || true
fi
if git diff --name-only | grep -q '^service/'; then
  (cd service && ./mvnw -q test) || true
fi
```

## 9. Example skill — .claude/skills/kb-article/SKILL.md

```markdown
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
```

## 10. .mcp.json seed (registers KB for Claude Code, stdio)

```json
{
  "mcpServers": {
    "kb": { "command": "node", "args": ["./kb-mcp/build/index.js"] }
  }
}
```
*(The runtime HTTP connection used by /service is configured separately in the Spring app, not here.)*

---

## 11. Phased build with kickoff prompts

Open the `csassist/` monorepo in VS Code, start Claude Code in the integrated terminal (or the extension). Use **plan mode** for any non-trivial phase: let CC propose a plan, review it, then approve before it writes code.

**Phase 0 — Setup (you, manually):** create the repo + `docs/PLAN.md` + `CLAUDE.md`, commit.

**Phase 1 — Scaffold the service**
> Read CLAUDE.md and docs/PLAN.md. Scaffold /service as a Spring Boot app: Ticket entity (id, title, description, status, assignee, timestamps), CRUD REST endpoints, an in-memory or H2 store, and a thin Thymeleaf UI listing tickets with create/edit/delete. Plan first, then build test-first.

**Phase 2 — Design → build status rules + audit**
1. Draft `docs/DESIGN-status-transitions.md` with me (the spec).
2. > Read docs/DESIGN-status-transitions.md. In plan mode, propose an implementation for status-transition rules (Open→In Progress→Resolved→Closed, no illegal jumps) and an audit trail recording who/what/when. Show the plan before coding, then implement test-first.

**Phase 3 — Author the KB MCP server**
> In /kb-mcp, scaffold a TypeScript MCP server with @modelcontextprotocol/sdk exposing search_kb, get_article, list_categories, backed by ~12 mock articles in /data. Support BOTH stdio and HTTP transport. Plan first, then build test-first.

**Phase 4 — Wire KB into Claude Code and test it (dev-time MCP)**
```bash
cd kb-mcp && npm run build
# from repo root, register the stdio server (project scope -> .mcp.json):
claude mcp add --transport stdio kb -s project -- node ./kb-mcp/build/index.js
claude mcp list
```
Start a new CC session, then `/mcp` to confirm "kb" is connected, then:
> Using the kb MCP tools, find articles about password resets and summarize the steps.

**Phase 5 — Build the enrichment component (runtime MCP client)**
> In /service, add an enrichment component: on ticket creation, use Spring AI to call the LLM (Ollama locally; Groq as an option) with the kb MCP server available over HTTP, retrieve relevant articles, and attach suggestions to the ticket. Plan first; build test-first; mock the LLM in tests.

**Phase 6 — Exercise the CC features deliberately**
> Add the PostToolUse verify hook (.claude/settings.json + .claude/hooks/verify.sh), the /ramp command, and the kb-article skill exactly as specified in docs/PLAN.md. Then use /ramp on the enrichment package and the kb-article skill to add 3 SSO articles.

---

## 12. Commit sequence (the demo narrative)

The history should read as the workflow:
1. `chore: scaffold csassist monorepo + plan + CLAUDE.md`
2. `feat(service): ticket CRUD + thin UI`
3. `docs: status-transition + audit design`
4. `feat(service): status rules (TDD)` → `feat(service): audit trail`
5. `feat(kb-mcp): MCP server + tools (stdio + http)`
6. `chore: register kb server (.mcp.json)`
7. `feat(service): LLM enrichment via MCP client`
8. `chore(cc): hooks, /ramp command, kb-article skill`

---

## 13. Open decisions to confirm

- [ ] **KB tool surface:** read-only (`search_kb`/`get_article`/`list_categories`) or add one write tool (`suggest_article`) to show side-effect design? *(Default: read-only.)*
- [ ] **Runtime model:** Ollama local (default) or Groq?
- [ ] **UI:** Thymeleaf (default) or minimal static HTML+JS?
- [ ] **Hosting:** public GitHub or private with granted access?

---

## 14. Chat → Claude Code handoff (recap)

This chat can't see your local files; Claude Code can't see this chat. The bridge is: design here → write `docs/PLAN.md` + `CLAUDE.md` → commit → open Claude Code in the repo → paste a kickoff prompt → plan mode → review → execute → verify. Durable decisions get written into CLAUDE.md by Claude Code itself (`#` to add a line, `/memory` to edit).

## 15. Sources (verified)
- Memory / CLAUDE.md: https://code.claude.com/docs/en/memory
- Hooks reference: https://code.claude.com/docs/en/hooks
- MCP setup & transports: https://code.claude.com/docs/en/mcp
- Settings (commands, skills, precedence): https://docs.anthropic.com/claude/docs/claude-code/settings
