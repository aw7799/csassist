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