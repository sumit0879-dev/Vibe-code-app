# GEMINI.md — Guide for Google Gemini CLI and Coding Agents

## Overview

This document provides Gemini (Google's coding agent and LLM tools) with everything needed to understand, modify, and propose changes to the VibeCode codebase. It is a **Gemini-specific companion** to CLAUDE.md and should be read in conjunction with it.

**For all core operational rules, AI safety guidelines, and general repository conventions, refer to [CLAUDE.md](./CLAUDE.md).**

---

## Quick Start for Gemini

- **Project**: VibeCode — a mobile-first Android IDE with a provider-agnostic AI coding assistant.
- **Tech Stack**: Kotlin, Jetpack Compose, Material 3, Hilt DI, Room, DataStore, OkHttp (SSE streaming).
- **Architecture**: Single-activity Compose app with MVVM + reactive ViewModels.
- **Default Branch**: `develop` (not `main`; see [CLAUDE.md](./CLAUDE.md) for branching rules).
- **Build System**: Gradle 8.7 (wrapper included).
- **Min/Target SDK**: 26 / 34 (Kotlin 1.9.24, Java 17).

---

## Understanding the Project

### What VibeCode Does

- **Mobile IDE** — A full Kotlin/Compose editor with multi-tab support, syntax highlighting (regex-based, 10+ languages), and line numbers.
- **AI Coding Agent** — Powered by any OpenAI-compatible provider (configurable at runtime).
  - Tools: `read_file`, `list_directory`, `search_project` (auto-execute), `create_file`, `update_file`, `delete_file` (diff-based approval).
  - Protocol: Tools are emitted as fenced ```` ```tool ```` JSON blocks in streaming responses.
  - No code execution—read/write files only.
- **On-Device Persistence** — Chat history, provider config, and models stored locally (Room + DataStore).
- **Encrypted Secrets** — API keys stored via `EncryptedSharedPreferences` + Android Keystore (AES-256-GCM).
- **Offline-First Design** — Local-first with graceful fallback when providers are unreachable.

### Architecture Layers

```
UI (Compose Screens + ViewModels)
   ↓
Domain (Models, Agent Logic, Diff Engine, Tool Definitions)
   ↓
Data (Room, DataStore, Repositories, SecureKeyStore, Network)
   ↓
Util (Syntax Highlighting, Parsers)
```

**Key Packages** (from [CLAUDE.md](./CLAUDE.md)):
- `app/src/main/java/com/vibecode/ide/ui/` — Compose screens (home, editor, chat, providers, models, settings, theme).
- `app/src/main/java/com/vibecode/ide/domain/` — Domain models, agent tools, `DiffUtil` (LCS-based), `SystemPromptBuilder`, `ToolCallParser`.
- `app/src/main/java/com/vibecode/ide/data/` — Room entities, repositories, network (OkHttp + SSE), `SecureKeyStore`.
- `app/src/main/java/com/vibecode/ide/util/` — `SyntaxHighlighter` (regex-based).

---

## Kotlin + Compose Conventions

### 1. Kotlin Version & Targets
- **Version**: 1.9.24
- **JVM Target**: Java 17 (set in `kotlinOptions { jvmTarget = "17" }`)
- **Compiler Args**: `-opt-in=kotlin.RequiresOptIn` enabled (for experimental APIs)

### 2. Compose & Material 3
- **BOM Version**: 2024.06.00
- **Material 3 Version**: 1.2.1
- **Compose Compiler**: 1.5.14
- **Navigation**: Jetpack Compose Navigation (`androidx.navigation:navigation-compose:2.7.7`)

### 3. Code Style & Patterns
- **MVVM + ViewModels**: State is managed in `ViewModel`s; Compose screens are stateless where possible.
- **Coroutines**: Use `viewModelScope` for async work; ensure proper `Dispatcher` usage (main, IO, default).
- **Immutability**: Prefer `data class` + `copy()` for state; avoid mutable collections in published state.
- **Lambdas**: Use trailing lambda syntax; avoid nested callbacks (prefer coroutines or `LaunchedEffect`).
- **Recomposition**: Keep Compose lambdas simple; move complex logic to ViewModels.

### 4. Common Compose Patterns in VibeCode
- **State Hoisting**: Editor state (tabs, selection, dirty flags) is in `EditorViewModel`, not in the Compose tree.
- **Side Effects**: `LaunchedEffect`, `DisposableEffect`, and `rememberCoroutineScope` for one-time or lifecycle-bound actions.
- **Navigation**: Use Compose Navigation `NavController` for screen switching; do **not** hardcode screen composition.
- **Bottom Sheets & Dialogs**: Use Compose's native `ModalBottomSheet` and `AlertDialog` with Material 3 styling.

---

## Hilt Conventions

### 1. Setup & Versions
- **Hilt Version**: 2.51.1
- **KSP (Kotlin Symbol Processing)**: 1.9.24-1.0.20
- **Hilt Navigation Compose**: 1.2.0

### 2. Application-Level DI
- **Entry Point**: `VibeCodeApplication extends Application` with `@HiltAndroidApp` annotation (see [CLAUDE.md](./CLAUDE.md) reference).
- **Modules**: DI modules are in `app/src/main/java/com/vibecode/ide/di/`.
- **Provide Pattern**: Use `@Provides` methods in `@Module` classes; prefer singleton scope for repositories and database instances.

### 3. ViewModel Injection
- All `ViewModel` subclasses in `ui/*` are annotated with `@HiltViewModel`.
- **Constructor Injection**: Dependencies injected via constructor (enabled by `@HiltViewModel`).
- **Navigation Args**: If passing arguments to a screen, decode them in the ViewModel factory (or use Compose Navigation argument serialization).

### 4. Repository Injection
- Repositories are singletons (scoped to `@Singleton`).
- They depend on Room DAOs, DataStore, OkHttp clients, and `SecureKeyStore`.
- Example pattern:
  ```kotlin
  @Singleton
  class ChatRepository(
      @ApplicationContext val context: Context,
      val chatDao: ChatDao,
      val secureKeyStore: SecureKeyStore
  ) { ... }
  ```

### 5. Service & API Client Injection
- OkHttp clients (with interceptors) are provided once and shared.
- API service wrappers (e.g., `OpenAIService`, `CustomProviderService`) are either singletons or scoped to the provider lifecycle.

---

## Repository Workflow (from CLAUDE.md)

**Always reference [CLAUDE.md](./CLAUDE.md) for:**
- Commit message conventions (`type(scope): summary`)
- PR conventions (branch naming, target branch, PR description checklist)
- AI safety rules (no automatic secrets, diff approval required, sandboxing)
- Things AI must never change (build files, secrets, CI/workflows without approval)
- Operational checklist before making changes

**Key Points for Gemini**:
1. Read involved files directly before proposing a change.
2. Branch from `develop` (not `main`).
3. Use commit header: `type(scope): short-summary` (max 72 chars).
4. Open PR against `develop` with summary, files changed, testing steps, and any migration notes.

---

## Development Workflow

### 1. Local Setup (Desktop or Termux)
Refer to [README.md](./README.md) for full Termux build instructions. Quick summary:
- Java 17 (OpenJDK)
- Android SDK 34 + build-tools 34.0.0
- `local.properties` with `sdk.dir` set
- `./gradlew assembleDebug` to build

### 2. Common Development Tasks

**Adding a new tool**:
- Extend `ToolName` enum in `domain/tools/ToolDefinition.kt`
- Document in `ToolDocs.description`
- Implement handler in `ChatViewModel.executeReadOnlyTool` (read-only) or `finishAssistantTurn` (mutating)

**Adding a new editor theme**:
- Add entry to `EditorColorTheme` enum in `ui/theme/Theme.kt`
- Implement `paletteFor()` function

**Adding a syntax-highlighted language**:
- Add entry to `Language` enum in `util/SyntaxHighlighter.kt`
- Define `KEYWORDS` and `COMMENT_REGEX` patterns

**Changing the system prompt**:
- Edit `domain/tools/SystemPromptBuilder.kt`

**UI Screen Changes**:
- All screens are Compose + ViewModel in `ui/*` subdirectories
- Edit independently (e.g., `ui/editor/EditorScreen.kt`, `ui/chat/ChatScreen.kt`)

### 3. Testing
- Unit tests for domain logic (tools, diff, syntax highlighting)
- Manual testing on emulator or physical device
- Termux: build APK on-device and sideload via `termux-open` or `adb install`

---

## Termux Workflow

VibeCode is **designed to be built entirely in Termux** (on an Android device without Android Studio).

**Key Termux Details** (see [README.md](./README.md) for full steps):
- **SDK Installation**: Download command-line tools manually; add to `~/.bashrc`
- **AAPT2 Issue**: If `aapt2` fails (architecture mismatch), override in `gradle.properties`:
  ```properties
  android.aapt2FromMavenOverride=<path-to-termux-compatible-aapt2>
  ```
- **First Run**: `./gradlew --version` downloads ~200 MB of Gradle; ensure network access
- **Build Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Install**: `termux-open app-debug.apk` or `adb install app-debug.apk` (if USB debugging enabled)

**Gemini's Termux Responsibilities**:
- When suggesting changes, include Termux-specific test steps if relevant
- Do not assume Android Studio is available; all builds must work via Gradle CLI
- Be aware of `arm64-v8a` architecture constraints (most common on Termux devices)

---

## Android SDK Notes

- **Compile SDK**: 34
- **Min SDK**: 26 (Android 8.0+, API level 26)
- **Target SDK**: 34 (Android 14)
- **Build Tools**: 34.0.0
- **Kotlin**: 1.9.24
- **Gradle**: 8.7 (managed by wrapper)

**Permissions** (check `AndroidManifest.xml`):
- File access (if using SAF or legacy file APIs)
- Network (for remote providers)
- Internet (for API calls)

---

## Performance Expectations

From [CLAUDE.md](./CLAUDE.md), maintain these performance standards:

1. **UI Responsiveness**: Keep the main thread unblocked.
   - Use `viewModelScope.launch(Dispatchers.IO)` for network/disk I/O
   - Use `withContext(Dispatchers.Main)` to update UI state
   - Avoid long-running operations in Compose lambdas

2. **Syntax Highlighter** (`util/SyntaxHighlighter.kt`):
   - Regex-based and intentionally fast
   - Do not replace with a slower algorithm without performance testing
   - Acceptable for visual highlighting; full grammar coverage not required

3. **Diff Algorithm** (`domain/diff/DiffUtil.kt`):
   - LCS (Longest Common Subsequence) implementation
   - Expected to handle typical source-file edits
   - Do not introduce O(n²) operations on large files without caching/streaming
   - If optimizing, benchmark before/after

4. **Network**: SSE streaming for real-time responses
   - OkHttp with `okhttp-sse` (4.12.0)
   - Handle stream errors gracefully
   - Provide user-visible stop button (already implemented in chat UI)

---

## AI Safety Rules

**These are non-negotiable.** See [CLAUDE.md](./CLAUDE.md) for full details; key points for Gemini:

1. **No Automatic Secrets**: Never commit API keys, tokens, or credentials. Use `data/security/SecureKeyStore.kt`.
2. **Diff Approval Required**: Mutating file changes must be shown as diffs and require explicit user approval.
3. **File Sandboxing**: Do not bypass `FileRepository.resolveSafe()` — path traversal outside the project root must be rejected.
4. **No Remote Execution**: Do not auto-enable features that cause code execution on-device or server without explicit human review.
5. **Build File Protection**: Do not modify `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`, or CI/workflow files without human approval.
6. **License Attribution**: Do not modify license or attribution without approval.

---

## Rules for Editing Code

1. **Read First**: Always read involved files directly (line counts, imports, existing patterns).
2. **Minimal Diffs**: Keep changes localized and small; avoid mass reformatting or unrelated modifications.
3. **Backwards Compatibility**: Prefer additive changes and migration paths for persisted data (Room schema).
4. **Small Functions**: Favor small, focused classes and ViewModels with clear responsibilities.
5. **Explicitness**: Prefer clear, testable code over implicit or "magical" behavior.
6. **Test Impact**: Consider edge cases, especially in domain logic (tools, diff, syntax highlighting).

---

## Rules for Proposing Changes

1. **Reference Files**: Cite file paths and line numbers in PR descriptions.
2. **Rationale**: Explain **why** a change is needed, not just **what** it does.
3. **Test Steps**: Provide explicit device/Termux steps to verify the change (e.g., "Build in Termux, open editor, add new provider, run agent").
4. **Migration Notes**: If database schema changes, include Room migration objects or clear migration steps.
5. **Security Checklist**: If touching auth, secrets, or file access, explicitly confirm safety.
6. **PR Title**: Match the commit message header: `type(scope): short-summary`.
7. **PR Description**: Include summary, files changed, testing steps, migration notes, and security notes.

---

## Preferred Commit Style

From [CLAUDE.md](./CLAUDE.md):

**Header** (max 72 characters):
```
type(scope): short-summary
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `ci`

**Examples**:
```
feat(domain): add FormatTool to ToolName enum
fix(ui/editor): preserve cursor position when switching tabs
docs: add GEMINI.md for Gemini CLI agents
```

**Body** (optional):
- One or more paragraphs explaining rationale
- Reference file paths and line numbers
- Include test steps if necessary

**Footer** (optional):
- Issue or PR references
- Breaking change notes

---

## Preferred PR Style

1. **Branch Naming**: `feature/*`, `fix/*`, or `docs/*` (from `develop`)
2. **PR Title**: Same as commit header
3. **PR Description**:
   ```
   ## Summary
   Brief description of changes.
   
   ## Why
   Why this change is needed.
   
   ## Files Changed
   - ui/chat/ChatScreen.kt — updated streaming UI
   - domain/tools/ToolCallParser.kt — improved JSON parsing
   
   ## How to Test (Device)
   1. Build: ./gradlew assembleDebug
   2. Install: adb install app/build/outputs/apk/debug/app-debug.apk
   3. Open app → Chat screen → send message → observe streaming response
   
   ## How to Test (Termux)
   1. ./gradlew assembleDebug
   2. termux-open app/build/outputs/apk/debug/app-debug.apk
   3. Grant storage permissions, test as above
   
   ## Migration Notes
   (If applicable)
   
   ## Security Considerations
   (If touching secrets, network, or file access)
   ```
4. **Target Branch**: `develop` (never `main` without explicit release process)

---

## When Gemini Should Ask Before Changing Build Files

**Always ask before modifying:**
- `build.gradle.kts` (root or app/)
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `.github/workflows/*` (CI/CD)
- `AndroidManifest.xml` (unless only adding permissions)

**Exception**: If the user explicitly requests a build change (e.g., "add this dependency"), proceed with clear explanation.

---

## Local LLM Support & Future Integration

VibeCode has a **long-term roadmap** for local LLM and MCP support. See [CLAUDE.md](./CLAUDE.md) for vision and goals.

### Current State
- Provider-agnostic architecture (any OpenAI-compatible endpoint)
- Dynamic provider management (name, base URL, auth, headers)
- Modular tool protocol (fenced JSON blocks)

### Future: Local LLM Adapter
When implementing local LLM support:
1. Create a new provider adapter (e.g., `OllamaProviderAdapter`)
2. Extend `ProviderEntity` and `AiProvider` domain model with local-only fields
3. Implement SSE streaming wrapper (or sync mode) for on-device models
4. Handle offline gracefully (cache recent completions)
5. Security: Local models bypass encryption (already on-device); ensure no secrets are leaked to logs

### Future: MCP (Model and Capability Provider) Integration
When formalizing MCP:
1. Define `ProviderCapabilities` interface:
   - Model discovery (`GET /models`)
   - Streaming response handler
   - Authentication (API key, OAuth, none)
   - Optional function-calling schema
2. Route all provider interactions through MCP adapter
3. Keep agent logic provider-agnostic
4. Support both remote and local adapters via same interface

**Gemini's Role**: When asked to implement local LLM or MCP features, follow the roadmap in [CLAUDE.md](./CLAUDE.md) and maintain backwards compatibility with existing providers.

---

## MCP Roadmap

From [CLAUDE.md](./CLAUDE.md):

1. **Current**: Provider management is dynamic but not formally abstracted
2. **Phase 1**: Define `ProviderAdapter` interface (model discovery, streaming, auth)
3. **Phase 2**: Refactor existing remote providers through adapter
4. **Phase 3**: Add local LLM adapter (Ollama, LM Studio, Llama.cpp)
5. **Phase 4**: Add plugin system for extensible tools and providers
6. **Future**: Remote on-LAN helper process for Termux-only devices

**Gemini Action**: When proposing provider or agent changes, design with MCP abstraction in mind (even if not implemented yet).

---

## Documentation Rules

1. **README.md**: Maintain as the single source for build, Termux, and architectural overview.
2. **CLAUDE.md**: Core guide for all AI assistants; Gemini supplements with Gemini-specific details.
3. **GEMINI.md** (this file): Gemini-specific workflow, conventions, and safety rules.
4. **Code Comments**: Use for **why**, not **what**; code should be self-documenting.
5. **Docstrings**: Document public classes, functions, and complex domain logic (tool definitions, diff algorithm).
6. **Examples**: Include before/after code snippets in commit messages for non-obvious changes.

**When Adding New Docs**:
- Keep scope focused (e.g., GEMINI.md for Gemini, CLAUDE.md for Claude)
- Link across documents (e.g., "See CLAUDE.md for X")
- Version and date major documentation changes
- Update this file (GEMINI.md) if Gemini workflow or conventions change

---

## How to Use This Guide

1. **Before any change**: Read the relevant section of CLAUDE.md or GEMINI.md.
2. **Before reading code**: Understand the architecture (see _Understanding the Project_).
3. **Before committing**: Check commit style and AI safety rules.
4. **Before opening a PR**: Use the PR template above.
5. **When unsure**: Reference CLAUDE.md (core rules) or ask for clarification.

---

## Appendix: File Inventory

**Key Files** (from [CLAUDE.md](./CLAUDE.md) & [README.md](./README.md)):

**UI & Navigation**:
- `app/src/main/java/com/vibecode/ide/MainActivity.kt` — Compose navigation root
- `app/src/main/java/com/vibecode/ide/ui/editor/EditorScreen.kt` — main editor
- `app/src/main/java/com/vibecode/ide/ui/chat/ChatScreen.kt` — chat & agent UI
- `app/src/main/java/com/vibecode/ide/ui/chat/ChatViewModel.kt` — agent orchestration
- `app/src/main/java/com/vibecode/ide/ui/theme/Theme.kt` — Material 3 theming

**Domain (Agent Logic)**:
- `app/src/main/java/com/vibecode/ide/domain/tools/ToolDefinition.kt` — tool enumeration
- `app/src/main/java/com/vibecode/ide/domain/tools/ToolCallParser.kt` — fenced block parsing
- `app/src/main/java/com/vibecode/ide/domain/tools/SystemPromptBuilder.kt` — agent prompt
- `app/src/main/java/com/vibecode/ide/domain/diff/DiffUtil.kt` — LCS-based diff

**Data (Persistence & Network)**:
- `app/src/main/java/com/vibecode/ide/data/security/SecureKeyStore.kt` — encrypted key storage
- `app/src/main/java/com/vibecode/ide/data/repository/ChatRepository.kt` — chat history
- `app/src/main/java/com/vibecode/ide/data/repository/ProviderRepository.kt` — provider config
- `app/src/main/java/com/vibecode/ide/data/repository/FileRepository.kt` — file I/O with sandboxing

**Utils**:
- `app/src/main/java/com/vibecode/ide/util/SyntaxHighlighter.kt` — regex-based highlighting

**Build & Config**:
- `build.gradle.kts` (root) — plugin versions
- `app/build.gradle.kts` — app dependencies, Android config
- `gradle.properties` — build flags
- `local.properties.example` — SDK path template (Termux)
- `README.md` — Termux build, architecture, and modification guide
- `CLAUDE.md` — core AI guidance

---

## Changelog for GEMINI.md

- **v1.0** — Initial GEMINI.md for Google Gemini CLI and coding agents; complements CLAUDE.md with Gemini-specific workflows, conventions, and safety rules.

---

**For all core repository rules, commit conventions, and operational guidelines, refer to [CLAUDE.md](./CLAUDE.md).**

**Questions? Check CLAUDE.md first, then GEMINI.md, then the code itself.**
