# CLAUDE.md — Single Source of Truth for AI Assistants working on VibeCode

Purpose
-------
This file is the authoritative, code-first guide for automated or human AI assistants that will read, modify, or propose changes to the VibeCode codebase. It contains:
- the project vision and long-term architectural goals,
- operational rules for AI-driven changes,
- development workflows (including Termux),
- repository and commit/PR conventions,
- coding philosophy, performance and UX rules,
- explicit things the AI must never change automatically.

All factual claims below are either directly supported by code and project files in this repository (referenced by path) or clearly marked as a project goal / roadmap item.

Quick navigator
---------------
- Evidence-backed code pointers: MainActivity, VibeCodeApplication, ui/, domain/, data/, util/
  - app/src/main/java/com/vibecode/ide/MainActivity.kt
  - app/src/main/java/com/vibecode/ide/VibeCodeApplication.kt
  - app/src/main/java/com/vibecode/ide/ui/
  - app/src/main/java/com/vibecode/ide/domain/
  - app/src/main/java/com/vibecode/ide/data/
  - app/src/main/java/com/vibecode/ide/util/SyntaxHighlighter.kt
  - domain/diff/DiffUtil.kt
  - data/security/SecureKeyStore.kt
  - README.md (for existing build/Termux details and developer notes)
- Build artifacts & config:
  - build.gradle.kts (root & app/)
  - gradle.properties
  - local.properties.example
  - gradlew / gradlew.bat

Project vision (supported + goals)
----------------------------------
Vision (supported by repository)
- VibeCode is a mobile-first Android IDE with an on-device, provider-agnostic AI coding assistant. The app uses a single-activity Compose architecture with Hilt DI and local persistence (Room/DataStore). The agent emits provider-agnostic tool calls (fenced ```tool``` JSON blocks) and mutating changes are shown as diffs for user approval before writing files. (See MainActivity.kt, VibeCodeApplication.kt, domain/tool and domain/diff, README.md.)

Long-term product & technical goals (project goals — not yet fully implemented)
- Offline-first, privacy-preserving IDE with optional local LLM support (run locally when available).
- Modular plugin system that allows third-party or internal extensions (e.g., additional analysis tools, formatters, language servers, adapters to local LLMs).
- Multi-CPU / multi-device compatibility including Termux-first packaging for devices without Android Studio.
- MCP (Model and Capability Provider) integration plans: abstract provider capabilities (model discovery, streaming, function-calling) behind a clear adapter interface so new providers or local LLM runtimes can be added with minimal core changes.
Note: the goals above are future-oriented; where features are currently present they are noted elsewhere.

Repository architecture overview (evidence-based)
-----------------------------------------------
- Single-activity Compose app:
  - Main entry: app/src/main/java/com/vibecode/ide/MainActivity.kt
  - Application-level DI: app/src/main/java/com/vibecode/ide/VibeCodeApplication.kt (Hilt)
- High-level packages:
  - data/ — Room entities, DataStore, repositories, secure-keystore wrapper (see data/security/SecureKeyStore.kt)
  - domain/ — domain models, the AI agent tooling, diff algorithm (domain/diff/DiffUtil.kt), SystemPromptBuilder
  - ui/ — Compose screens and ViewModels (chat, editor, home, providers, models, settings, theme)
  - util/ — utilities such as the regex-based syntax highlighter
- Agent runtime pattern:
  - Provider-agnostic tool protocol: tools are emitted by the model inside fenced ```tool``` blocks and parsed via ToolCallParser (referenced in README).
  - Read-only tools execute automatically, mutating tools produce a diff and must be approved by the user (DiffUtil, ChatViewModel flows described in README).

Termux development workflow (existing + clarified)
--------------------------------------------------
What is supported now (from README and repo):
- Termux-focused build guidance exists in README.md. The project is designed to be buildable on-device via Termux using the Android SDK command-line tools and the Gradle wrapper.
- Local properties: copy local.properties.example → local.properties and set sdk.dir correctly.

Recommended (single-source workflow — explicit instructions)
1. On a Termux device:
   - Ensure Java 17 (OpenJDK 17) is installed.
   - Install Android SDK command-line tools and platform/build-tools appropriate for the targeted platform (README recommends android-34 / build-tools 34.0.0).
   - Copy `local.properties.example` to `local.properties` and set `sdk.dir`.
   - Run `chmod +x ./gradlew` and `./gradlew assembleDebug`.
2. If an aapt2 binary error occurs on Termux, follow the README advice to override `android.aapt2FromMavenOverride` in `gradle.properties`.

AI coding rules (explicit, must be enforced)
-------------------------------------------
These rules are mandatory for any AI assistant modifying this repository:

A. Read-only analysis first
   - Always read the relevant files and README excerpts directly before proposing a change. Cite file paths and lines where you base decisions.

B. No automatic mutating writes without review
   - Mutating tool outputs (create_file/update_file/delete_file) must result in a diff that is shown to a human and require explicit human approval before being written to the repository or working copy. This rule mirrors the app behavior and is strictly required.

C. Secrets & key management
   - Never write API keys, tokens, credentials, or private keystore information into version control, Room, or plain-text files. Use `data/security/SecureKeyStore.kt` patterns and EncryptedSharedPreferences when persisting keys.

D. Maintain sandboxing constraints
   - Do not change file access rules that allow the app to operate only inside the project root (see FileRepository.resolveSafe behavior; path traversal must be rejected).

E. Minimal-diff principle
   - When changing code, keep diffs minimal and localized to the feature or fix. Avoid mass reformatting or unrelated changes.

Repository conventions (recommended, enforceable)
-----------------------------------------------
- Package boundaries:
  - UI code: ui/*
  - Business/agent logic: domain/*
  - Data/persistence/network: data/*
  - DI: di/*
  - Utilities: util/*
- Files to never modify automatically:
  - build.gradle.kts files, gradle/wrapper, gradle.properties, settings.gradle.kts, CI/workflows, and other build infrastructure unless the change is explicitly requested and approved by a human.
- Database migrations:
  - If schema changes are required, include proper Room migration objects and tests (or clear migration notes).

Commit message conventions
--------------------------
Follow a short, consistent style to make automated parsing and release notes simple.

Template:
- header: type(scope): short-summary
- body (optional): one or more paragraphs explaining rationale
- footer (optional): references (issue numbers, breaking changes)

Examples:
- feat(domain): add ToolName enum entry for format_file
- fix(ui/editor): preserve cursor position when switching tabs
- docs: add CLAUDE.md — single-source guidance for AI assistants

Types to prefer:
- feat, fix, docs, refactor, chore, test, ci

Pull request conventions
------------------------
- Branch from develop: feature/* or fix/* or docs/*
- PR title: same as the commit message header (one-line)
- PR description should include:
  - Summary of changes
  - Why it’s needed
  - How to test (device/emulator steps; Termux variants when applicable)
  - Any DB migration notes
- Target branch: develop
- Do not merge to main without an explicit release process; follow the repository's develop → PR → main flow described in this file.

Coding philosophy
-----------------
- Single responsibility & small functions: prefer small, focused classes and ViewModels.
- Declarative UI: Compose components should be stateless where possible; side-effects handled by ViewModels.
- Separation of concerns: agent logic belongs in domain/, storage in data/, and UI in ui/.
- Explicitness over magic: prefer clear, testable domain code over implicit, brittle parsers.
- Backwards-compatible changes: prefer additive changes and migration paths for persisted data.

Performance requirements (project-specific)
------------------------------------------
Minimum expectations for any change:
- UI must remain responsive on mid-range Android devices; avoid blocking the main thread — use coroutines with proper Dispatchers (IO/default/main).
- The regex-based syntax highlighter (util/SyntaxHighlighter.kt) is intentionally fast; do not replace it with an obviously slower algorithm without performance testing.
- The diff algorithm (domain/diff/DiffUtil.kt) is LCS-based and expected to handle typical source edits; do not introduce O(n^2) operations on very large files without caching or streaming strategies.

UI/UX rules
-----------
- Single-activity navigation: Keep the current Compose Navigation architecture intact.
- Editor behavior:
  - Preserve multi-tab behavior and "dirty" state markers.
  - Show line numbers when enabled, do not remove user-configurable editor settings.
- Agent interactions:
  - Tool calls should be visible to the user when a mutating operation is proposed, with an explicit approve/reject flow.
  - Keep streaming responses and stop functionality where present (Chat UI expectations).
- Theme & accessibility:
  - Respect system dark/light preferences and Material 3 theming; preserve font-size and word-wrap user settings.

Offline-first design goals (project goals — partly implemented)
---------------------------------------------------------------
- The app already persists chat history locally (Room) and stores provider metadata without plaintext secrets; continue to prioritize:
  - Local-first data availability (chat/history) even when remote providers are unreachable.
  - Graceful degradation for offline mode: read-only tools and local file operations should continue to function.
- Goal: Expand caching and local queueing of agent tool requests for intermittent connectivity (future work).

MCP integration plans (project goals)
------------------------------------
- Define a Model/Capability Provider (MCP) adapter interface that encapsulates:
  - model discovery (GET /models), streaming responses, authentication methods, and optional function-calling.
- Keep the core agent provider-agnostic by routing all provider interactions through the MCP interface. The repo already uses a dynamic provider model — formalizing as MCP is a roadmap item.

Local LLM support goals (project goals)
---------------------------------------
- Aim: Provide an adapter that can connect to local LLM runtimes (on-device or LAN-hosted) and expose the same provider interface used for remote providers.
- Consider lightweight quantized runtimes or a remote-on-LAN helper process for constrained devices (Termux support is key).

Future plugin architecture (project goals)
------------------------------------------
- Design a plugin API that allows:
  - additional tools (formatters, linters, language servers),
  - UI extensions (panels, custom editor features),
  - provider adapters (local LLMs, private endpoints).
- Plugins should be sandboxed and operate under the same approval/diff model as agent-suggested changes.

Things AI must never change (explicit, enforceable)
---------------------------------------------------
These are non-negotiable safeguards for any automated change:
1. Never commit API keys, secrets, or keystore material into the repository (files or DB). Always use or preserve `data/security/SecureKeyStore.kt` patterns.
2. Never bypass the diff/approve workflow for mutating agent changes. Mutations must be human-approved before being persisted.
3. Do not update or alter Gradle wrapper files, `gradle.properties`, `build.gradle.kts`, `settings.gradle.kts`, or CI/workflow files without explicit human approval.
4. Do not change the file sandboxing rules that prevent path-traversal outside the project root.
5. Do not auto-enable remote-execution features or anything that causes code execution on-device/server without explicit human review.
6. Do not modify license attribution or change the public license declaration without human approval.

Operational checklist for AI assistants
--------------------------------------
Before making a change:
- Read the involved files and reference them in the PR (file paths + brief quotes).
- Confirm the change adheres to the "Things AI must never change".
- Ensure the change is small and reversible; include test or manual verification steps.
- Prepare a single commit or a small set of logically separated commits with clear messages.
- Open a PR against develop with an explicit description and testing steps.

Suggested commit & PR templates (recommended)
---------------------------------------------
Commit header:
- 72-character max summary: type(scope): short summary
- Blank line
- Longer explanation with rationale and test steps (if required)
Example:
```
feat(domain): add FormatTool to ToolName enum

Add a new tool "format_file" that requests formatting for a given file path.
Implementation touches domain/tools/ToolDefinition.kt and domain/agent/FormatToolHandler.kt.

Test:
- Run unit tests for diff module
- Manually trigger formatting on sample file in editor and confirm diff appears
```

PR description checklist:
- Summary
- Files changed (list)
- How to test (explicit device/Termux steps)
- Migration notes (if any)
- Security considerations

Appendix: evidence pointers (what I read)
-----------------------------------------
- app/src/main/java/com/vibecode/ide/MainActivity.kt — Compose nav root
- app/src/main/java/com/vibecode/ide/VibeCodeApplication.kt — Hilt app entry
- app/src/main/java/com/vibecode/ide/ui/ — UI screen packages present
- app/src/main/java/com/vibecode/ide/util/SyntaxHighlighter.kt — syntax-highlighting approach (regex)
- domain/diff/DiffUtil.kt — diff/LCS-based strategy referenced in README
- data/security/SecureKeyStore.kt — encrypted key storage is implemented (README reference)
- README.md — build/Termux guidance and developer notes (used as evidence only, not reproduced)

How I will behave when asked to modify repository files
------------------------------------------------------
- I will ask for explicit permission before creating branches, making commits, or opening PRs.
- I will create a focused branch from develop and a single logical commit for this documentation change when you say "commit".
- I will not change code or build files — only add or update documentation unless you explicitly request otherwise.

Change log for this file
------------------------
- v1 — initial CLAUDE.md (draft)
- v1.1 — current: production-quality single-source guidance for AI assistants, includes vision and goals; clarifies rules and workflows.

If you want this committed
--------------------------
Reply "commit" and I will:
1. Create a branch from develop named `docs/claude-md-improve`.
2. Add CLAUDE.md at repository root.
3. Commit with message: `docs: add CLAUDE.md — single-source guidance for AI assistants`.
4. Open a Pull Request targeting develop with a concise description and the checklist above.

(If you prefer edits to the document first, tell me what to change and I will update the draft before committing.)
