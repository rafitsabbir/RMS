# Handbooks

Purpose: Word documents for people outside the code: the business analyst, the IT team, and new developers.
Last updated: 2026-10-02 (updated for the roles plan, Phase 1: four roles, Users and Roles, passwords, migration 002; sources moved into `src/`). 2026-10-02 (first versions, describing `dev` as of 2026-10-02)
Read this when: you need to hand RMS documentation to a non-developer, or check whether these handbooks are out of date.

| File | For | Covers |
|---|---|---|
| `RMS Business Analyst Guide.docx` | Business analysts | Roles, who can open which screen, how permissions are changed, features, business flows, how to use RMS, business data and rules, missing features, open business questions, feature ideas |
| `RMS Technical Operations Guide.docx` | IT / operations | Stack and versions, architecture, production prerequisites, hardware sizing (estimates, not measured), deployment and rollback, configuration, security, operations, release status, decisions for IT |
| `RMS Developer Handbook.docx` | Developers | First-day setup, repository map, request flow, security model, conventions, UI rules, recipes for common changes, database work, tests, pitfalls, Git and CI |

- They are snapshots written from the code and the other files in `docs/`. When facts change, the Markdown docs are updated first; these handbooks may lag behind, and the Markdown docs win if the two differ.
- No credentials, hostnames or connection strings are in them.

## Regenerating them
The sources are in `src/`: one text file per handbook (`ba.txt`, `it.txt`, `dev.txt`, in a small line-based markup described at the top of `make-docx.ps1`) and the diagrams (`src/diagrams/*.mmd`, with their rendered `.png` files). On Windows, from this folder:

```powershell
powershell -ExecutionPolicy Bypass -File src\make-docx.ps1 -Source src\ba.txt -Output "RMS Business Analyst Guide.docx" -ImageDir src\diagrams
```

Use `it.txt` → `RMS Technical Operations Guide.docx` and `dev.txt` → `RMS Developer Handbook.docx` the same way. Word isn't needed.

To redraw a diagram after editing its `.mmd`, run `src\diagrams\render.ps1`. It needs Microsoft Edge and loads Mermaid 11.4.1 from cdn.jsdelivr.net; it renders every `.mmd` in the folder and crops each PNG.
