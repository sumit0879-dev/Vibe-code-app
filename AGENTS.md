# AGENTS.md — Universal Guidelines for AI Coding Agents

**Purpose:** This file provides production-quality guidance for any AI coding agent working on VibeCode, including ChatGPT, Codex, OpenHands, Cursor, Claude Code, Gemini CLI, and future agents.

This document is **AI-agnostic** — it does not prescribe a specific LLM or provider. All constraints and workflows apply uniformly to any agent system.

> **Note:** For Claude-specific deep context and reasoning, see [CLAUDE.md](CLAUDE.md).

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture Summary](#architecture-summary)
3. [Repository Structure](#repository-structure)
4. [Branch Strategy](#branch-strategy)
5. [Commit Message Conventions](#commit-message-conventions)
6. [Pull Request Workflow](#pull-request-workflow)
7. [Code Style Expectations](#code-style-expectations)
8. [Security Rules](#security-rules)
9. [AI Behavior Rules](#ai-behavior-rules)
10. [What Agents Can Modify](#what-agents-can-modify)
11. [What Agents Must Never Modify](#what-agents-must-never-modify)
12. [Review Checklist](#review-checklist)
13. [Testing Checklist](#testing-checklist)
14. [Documentation Update Rules](#documentation-update-rules)

---

## Project Overview

**VibeCode AI IDE** is a mobile-first Android IDE with an on-device, provider-agnostic AI coding assistant.

### Core Features

- **Kotlin + Jetpack Compose + Material 3** architecture with MVVM, Hilt DI, Room database, and DataStore
- **Dynamic AI Provider Manager** — add unlimited OpenAI-compatible or custom API providers
- **Unlimited Model Management** — automatic discovery or manual entry per provider
- **AI Coding Agent** with file tools:
  - **Read-only:** `read_file`, `list_directory`, `search_project` (auto-execute)
  - **Mutating:** `create_file`, `update_file`, `delete_file` (always show diff, require user approval)
- **Provider-agnostic tool protocol** — tools are emitted in fenced ` ```tool ` ` blocks as JSON
- **No code execution** — agent only reads/writes source files; no terminal, build, or runtime integration
- **Mobile editor** — multi-tab, line numbers, syntax highlighting, 5 themes, user-configurable settings
- **Persistent local history** — Room database stores all chat/project state locally
- **Encrypted API keys** — stored via Android `EncryptedSharedPreferences` backed by hardware-backed Android Keystore

### Design Philosophy

- **Offline-first:** All data persisted locally; remote providers optional
- **Privacy-preserving:** Keys never stored in plaintext or database
- **Security-first:** File operations sandboxed to project root; path traversal blocked
- **User-controlled:** No automatic mutations; all changes require explicit approval

---

## Architecture Summary

### High-Level Layers

```
┌─────────────────────────────────────────────────────────┐
│ UI Layer (Jetpack Compose + Material 3)                │
│ ├─ Chat Screen (ChatViewModel, tool orchestration)     │
│ ├─ Editor Screen (EditorViewModel, multi-tab)          │
│ ├─ Provider/Model Management Screens                   │
│ ├─ Settings Screen (theme, editor prefs)               │
│ └─ Home/Project Selection Screen                       │
├─────────────────────────────────────���───────────────────┤
│ Domain Layer (Business Logic)                          │
│ ├─ AI Agent (system prompt, tool calling)              │
│ ├─ Tool Definitions & Parsers (ToolCallParser)         │
│ ├─ Diff Engine (LCS-based, show before save)           │
│ ├─ Domain Models (AiProvider, Project, ChatMessage)    │
│ └─ AI Provider Abstraction (provider-agnostic)         │
├─────────────────────────────────────────────────────────┤
│ Data Layer (Persistence & Network)                     │
│ ├─ Room Database (chat history, projects, settings)    │
│ ├─ DataStore (user preferences)                        │
│ ├─ Secure Key Store (EncryptedSharedPreferences)       │
│ ├─ Repositories (file, provider, project, chat)        │
│ ├─ Network Client (OkHttp + SSE for streaming)         │
│ └─ File System Operations (sandboxed FileRepository)   │
├─────────────────────────────────────────────────────────┤
│ DI Container (Hilt)                                    │
│ └─ Modules provide singletons & scoped instances      │
└─────────────────────────────────────────────────────────┘
```

### Key Design Patterns

- **Single-Activity Architecture:** One `MainActivity` hosts all Compose screens via Navigation
- **MVVM + Repository Pattern:** ViewModels orchestrate domain logic; repositories abstract data access
- **Provider-Agnostic Agent:** System prompt teaches tools in plain JSON; no hardcoded provider SDK
- **Diff-on-Save:** All mutating file operations show a diff before user approval

---

## Repository Structure

```
VibeCodeAI/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/vibecode/ide/
│       │   ├── data/                  # Room, DataStore, repositories, network, file I/O
│       │   │   ├── local/             # Room entities
│       │   │   ├── repository/        # Data access layer
│       │   │   ├── network/           # API clients, SSE handling
│       │   │   ├── security/          # SecureKeyStore (encrypted storage)
│       │   │   └── file/              # FileRepository (sandboxed operations)
│       │   ├── domain/                # Business logic, models, agent, tools, diff
│       │   │   ├── model/             # Domain objects (AiProvider, Project, ChatMessage, etc.)
│       │   │   ├── tools/             # Tool definitions, parsers, system prompt
│       │   │   ├── diff/              # DiffUtil (LCS-based diff algorithm)
│       │   │   └── agent/             # Agent orchestration (if separate from ViewModels)
│       │   ├── di/                    # Hilt modules
│       │   ├── ui/                    # Jetpack Compose screens & ViewModels
│       │   │   ├── home/              # HomeScreen (project selection)
│       │   │   ├── editor/            # EditorScreen (multi-tab IDE)
│       │   │   ├── chat/              # ChatScreen & ChatViewModel
│       │   │   ├── providers/         # ProviderManagementScreen
│       │   │   ├── models/            # ModelManagementScreen
│       │   │   ├── settings/          # SettingsScreen
│       │   │   └── theme/             # Theme.kt (colors, EditorColorTheme enum)
│       │   ├── util/                  # SyntaxHighlighter, utilities
│       │   └── MainActivity.kt        # Single-activity entry point
│       └── res/                       # Drawable, values (strings, colors, dimens)
│
├── build.gradle.kts                  # Root Gradle build config
├── settings.gradle.kts               # Gradle settings (plugins, repos, modules)
├── gradle.properties                 # Gradle JVM args, plugins, build flags
├── gradlew / gradlew.bat             # Gradle wrapper scripts
├── gradle/wrapper/                   # Gradle wrapper JAR & metadata
├── local.properties.example          # Template for local.properties (SDK path)
├── .gitignore                        # Ignored files (.gradle/, build/, .idea/, *.iml)
├── README.md                         # Build instructions, project overview
├── CLAUDE.md                         # Claude-specific deep guidance
└── AGENTS.md                         # This file — universal agent guidelines
```

### Key Directories Explained

- **`data/`** — All persistence, network, and file I/O. Repositories expose high-level APIs; implementation details are hidden.
- **`domain/`** — Pure business logic independent of Android. Models, tool definitions, diff algorithm, agent rules.
- **`ui/`** — Compose screens, ViewModels, theme. Should be mostly stateless; state management delegated to repositories/ViewModels.
- **`util/`** — Syntax highlighter (regex-based), utility functions.

---

## Branch Strategy

### Branch Naming

All feature/fix branches must follow this naming convention and originate from `develop`:

```
feature/<feature-name>      # New features
fix/<bug-name>              # Bug fixes
docs/<doc-topic>            # Documentation
refactor/<area>             # Refactoring
chore/<task>                # Maintenance tasks
```

Examples:
- `feature/add-llama-provider`
- `fix/editor-cursor-position`
- `docs/add-agents-md`
- `refactor/extract-tool-parser`
- `chore/update-dependencies`

### Merge Flow

```
┌─────────────────────────────────────────┐
│ feature/*, fix/*, docs/*, etc. branches │
│         (originate from develop)        │
└────────────────┬────────────────────────┘
                 │ Pull Request (reviewed)
                 ↓
          ┌─────────────┐
          │   develop   │  (integration branch)
          └──────┬──────┘
                 │ (when ready for release)
                 │ Tag + Release PR
                 ↓
            ┌────────┐
            │  main  │  (stable releases)
            └────────┘
```

**Rules:**
- Always branch from `develop`
- All PRs target `develop` (not `main`)
- `main` only receives commits from develop via explicit release PRs
- Tag releases on `main` with semantic versioning (e.g., `v1.0.0`)

---

## Commit Message Conventions

All commits must follow a structured format for automated parsing and clarity.

### Template

```
type(scope): short-summary

Optional body: Longer explanation of the change, rationale, and implementation details.

Optional footer: References (issues, breaking changes, co-authors)
```

### Rules

- **Header line:** Max 72 characters
- **Type:** One of `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `ci`
- **Scope:** Package or area affected (e.g., `ui/editor`, `domain/tools`, `data/network`)
- **Summary:** Present tense, lowercase, imperative mood (e.g., "add", "fix", "refactor")
- **Body:** Explain *why*, not *what* (commit diff shows what). Include test steps if applicable.
- **Footer:** Reference issue numbers (e.g., `Fixes #123`) or breaking changes

### Examples

✅ **Good:**
```
feat(domain): add format_file tool to ToolName enum

Add a new tool "format_file" that requests code formatting for a given file.
Implemented in domain/tools/ToolDefinition.kt and domain/tools/ToolDocs.kt.

Mutating tool — requires user approval before applying.

Test:
- Run domain unit tests: ./gradlew test
- Manually: open editor, select code, invoke format tool, verify diff appears
```

✅ **Good:**
```
fix(ui/editor): preserve cursor position when switching tabs

Editor was resetting cursor to (0,0) when switching between open files.
Now stored and restored via EditorState in EditorViewModel.

Fixes #42
```

✅ **Good:**
```
docs(AGENTS.md): add testing checklist section

Added comprehensive testing guidelines for all agents to ensure consistent
QA practices across different AI provider implementations.
```

❌ **Bad:**
```
Update stuff
```

❌ **Bad:**
```
feat: modified files
```

---

## Pull Request Workflow

### Before Opening

1. Ensure your branch is up-to-date with `develop`
2. Run local tests: `./gradlew test`
3. Build the APK locally: `./gradlew assembleDebug`
4. Verify your changes follow code style (see [Code Style Expectations](#code-style-expectations))

### PR Title & Description

**Title:** Same as commit message header
```
docs: add AGENTS.md for universal AI agents
```

**Description:** Use this template

````markdown
## Summary
Brief 1–2 sentence description of what this PR does.

## Why
Explain the motivation and context.

## Changes
- Bullet list of files modified/created
- Keep it concise

## How to Test
Step-by-step instructions (device/emulator/Termux):
1. Build: `./gradlew assembleDebug`
2. Install: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Open the app and verify [feature description]

## Notes
- Database migrations: (if any)
- Security considerations: (if any)
- Breaking changes: (if any)
- Backward compatibility: (if relevant)

## Checklist
- [ ] I have read AGENTS.md
- [ ] Code follows style guidelines
- [ ] Local tests pass
- [ ] Documentation updated (README, inline comments)
- [ ] No secrets committed
- [ ] Changes are minimal and focused
````

### Merge Requirements

- ✅ All tests pass
- ✅ Code review approved
- ✅ No merge conflicts
- ✅ Branch is up-to-date with develop
- ✅ Commit message is clear and follows conventions

### After Merge

Delete the feature branch (GitHub will prompt you).

---

## Code Style Expectations

### Language & Tooling

- **Language:** Kotlin
- **Android Framework:** Jetpack Compose, Material 3
- **Architecture:** MVVM + Repository pattern
- **DI:** Hilt
- **Async:** Kotlin Coroutines with proper Dispatcher scoping
- **Compiler:** Kotlin 1.9.24 (OpenJDK 17)

### Kotlin Style

Follow **official Kotlin style guide** (enforced via `kotlin.code.style=official` in `gradle.properties`):

- **Naming:** `camelCase` for variables/functions, `PascalCase` for classes/interfaces
- **Indentation:** 4 spaces
- **Line length:** Prefer ≤100 characters; wrap long lines
- **Visibility:** Use explicit `private`, `internal`, `public` modifiers
- **Nullability:** Prefer non-nullable types; use `?.` and `!!` sparingly

### Compose Guidelines

- **Stateless components:** Keep composables pure; delegate state to ViewModels
- **Lambda trailing syntax:** `Modifier.fillMaxWidth() { ... }` not `{ Modifier.fillMaxWidth() }`
- **Avoid magic numbers:** Use theme values (`LocalTextStyle.current`, `LocalDimensions.current`)
- **Performance:** Avoid recomposing large lists; use `remember` and `key` for stability

### Package Organization

```
com.vibecode.ide
├── data              # All persistence/network logic
├── domain            # Pure business logic
├── di                # Hilt modules
├── ui                # Compose screens, ViewModels, theme
└── util              # Helper functions, syntax highlighter
```

**Rule:** Do not cross-import between domain and UI without going through a ViewModel or repository.

### Comments & Documentation

- **KDoc for public APIs:**
  ```kotlin
  /**
   * Reads a file from the project root and returns its content.
   * 
   * @param filePath Relative path to the file (e.g., "src/Main.kt")
   * @return File content as String, or null if file not found
   * @throws SecurityException If [filePath] attempts path traversal outside project root
   */
  suspend fun readFile(filePath: String): String?
  ```
- **Inline comments:** Only for non-obvious logic; prefer clear naming over comments
- **TODO comments:** Include a reference to an issue if possible: `// TODO: #42 add caching for model discovery`

### Performance Rules

- **Main thread:** Never block on the main thread; use `Dispatchers.IO` for file/network I/O
- **Coroutines:** Always launch on appropriate dispatcher; avoid `GlobalScope`
- **Memory:** Be mindful of large file operations; consider streaming for large diffs
- **Regex:** The syntax highlighter is intentionally regex-based for speed; do not replace with a slower parser without benchmarking

### Security in Code

- **API Keys:** Never hardcode or log them; always use `SecureKeyStore`
- **File operations:** Always use `FileRepository.resolveSafe()` to validate paths
- **String concatenation:** Avoid dynamic SQL or command construction; use parameterized queries/APIs
- **Error messages:** Do not leak sensitive info in exception messages shown to users

---

## Security Rules

These rules are **mandatory** for any change involving data, networking, or user input.

### Rule 1: Never Commit Secrets

- ❌ Do not commit API keys, tokens, or private keystores
- ✅ Use `EncryptedSharedPreferences` via `SecureKeyStore` (see `data/security/SecureKeyStore.kt`)
- ✅ Store unencrypted metadata in Room; reference secrets only by alias

### Rule 2: Encrypt Sensitive Data at Rest

- API keys → `EncryptedSharedPreferences` (hardware-backed Android Keystore)
- Tokens → Same as above
- User credentials → `EncryptedSharedPreferences`
- Unencrypted data (provider names, model names) → Room or DataStore

**Code example:**
```kotlin
// ✅ Correct
val secureKeyStore: SecureKeyStore = hilt.inject()
secureKeyStore.setApiKey("openai", apiKey)  // Stored encrypted
val retrieved = secureKeyStore.getApiKey("openai")  // Decrypted

// ❌ Wrong
val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
sharedPrefs.edit().putString("api_key", secretKey).apply()  // Plain text!
```

### Rule 3: Sandbox File Operations

- All file I/O must go through `FileRepository.resolveSafe(projectRoot, userPath)`
- ❌ Do not allow `../`, absolute paths, or symlinks outside project root
- ✅ Validate all paths before reading or writing

**Code example:**
```kotlin
// ✅ Correct
val resolvedPath = fileRepository.resolveSafe(projectRoot, userPath)
// Throws SecurityException if path is outside projectRoot

// ❌ Wrong
File(userPath).readText()  // No validation!
```

### Rule 4: No Remote Execution

- ❌ Do not execute arbitrary commands on the device or server
- ❌ Do not enable eval(), runtime compilation, or code execution
- ✅ The agent reads and writes files only; no terminal/REPL/build integration

### Rule 5: Backup/Export Excludes Secrets

- When users export projects/settings, always exclude encrypted keys
- Backup should only contain: chat history, provider metadata (names, URLs), model list, user preferences
- After import, users must re-enter their API keys

**Code example:**
```kotlin
// ✅ Correct
fun exportSettings(includeKeys: Boolean = false): String {
    val providers = getAllProviders()  // Includes names, URLs
    val models = getAllModels()        // Includes names, model IDs
    
    if (includeKeys) {
        return error("Never export keys!")  // Reject this option
    }
    
    return json.encodeToString(providers)  // Keys are never in here
}
```

---

## AI Behavior Rules

These rules define how any AI agent must behave when modifying the repository.

### Rule 1: Read-First, Propose-Second

**Mandatory workflow:**
1. Read all relevant source files
2. Analyze the codebase structure and existing patterns
3. Cite file paths and line numbers in your explanation
4. Propose a solution with clear before/after diffs
5. Wait for human approval before committing

**Example (✅ Correct):**
```
I've read:
- domain/tools/ToolDefinition.kt (lines 10–45): ToolName enum + tool registry
- domain/tools/ToolDocs.kt (lines 50–100): tool descriptions
- ui/chat/ChatViewModel.kt (lines 200–250): tool execution logic

Proposal: Add a new tool "lint_file" for static analysis.

Changes:
1. Add LINT_FILE to ToolName enum
2. Add lint tool docs to ToolDocs
3. Add lint execution branch in ChatViewModel.executeReadOnlyTool()

Files to change:
- domain/tools/ToolDefinition.kt
- domain/tools/ToolDocs.kt
- ui/chat/ChatViewModel.kt

Should I proceed?
```

### Rule 2: Never Auto-Commit or Auto-Push

- ❌ Do not create branches without asking
- ❌ Do not commit without explicit human approval
- ✅ Always wait for "go ahead" or equivalent confirmation

**Example (✅ Correct):**
```
Ready to:
1. Create branch: feature/add-lint-tool
2. Commit changes with message: feat(domain): add lint_file tool
3. Open PR against develop

Approve? [yes/no]
```

### Rule 3: Minimal & Focused Changes

- ❌ Do not mass-reformat files unrelated to your change
- ❌ Do not upgrade dependencies or modify build config unless asked
- ✅ Keep diffs minimal; one logical change per commit

**Example (❌ Wrong):**
```
I reformatted all Kotlin files to match Ktlint style.
Then I added the new feature.
Then I upgraded Hilt to 2.52.
```

**Example (✅ Correct):**
```
Commit 1: feat(domain): add lint_file tool
- Only changes to ToolDefinition.kt, ToolDocs.kt, ChatViewModel.kt
- No formatting changes unrelated to the feature
```

### Rule 4: No Mutation Without Diff Approval

- ❌ Do not write files to the repository until approved
- ✅ Show the diff (before/after content) and wait for "approved" or "proceed"

**Example (✅ Correct):**
```
File: domain/tools/ToolDefinition.kt
Before:
```kotlin
enum class ToolName {
    READ_FILE, LIST_DIRECTORY, SEARCH_PROJECT
}
```

After:
```kotlin
enum class ToolName {
    READ_FILE, LIST_DIRECTORY, SEARCH_PROJECT, LINT_FILE
}
```

Approved to write this change?
```

### Rule 5: Respect Security & Sandboxing

- ❌ Do not bypass `SecureKeyStore` for key storage
- ❌ Do not disable file sandboxing in `FileRepository.resolveSafe()`
- ❌ Do not add features that allow remote code execution
- ✅ Always preserve and strengthen security boundaries

### Rule 6: Reference & Link to Existing Code

When proposing a change, always cite:
- Exact file paths: `app/src/main/java/com/vibecode/ide/domain/tools/ToolDefinition.kt`
- Line numbers: `lines 10–20`
- Function/class names: `ToolName enum`, `executeReadOnlyTool()` method

**Example (✅ Correct):**
```
In ui/chat/ChatViewModel.kt (lines 200–220), the executeReadOnlyTool() function currently handles:
- read_file
- list_directory
- search_project

I propose adding:
- lint_file (new)

The execution follows the pattern at lines 205–210 (read-only tool auto-execute).
```

---

## What Agents Can Modify

These are the areas where AI agents have freedom to make changes with proper review.

### ✅ Can Create/Modify

- **Source code** in `app/src/main/java/com/vibecode/ide/` (any package)
- **Resource files** in `app/src/main/res/` (strings, colors, drawables, layouts)
- **Test files** in `app/src/test/` and `app/src/androidTest/`
- **Documentation** — README.md, AGENTS.md, inline KDoc comments
- **App configuration** in `AndroidManifest.xml`
- **Proguard rules** in `app/proguard-rules.pro`

### ✅ With Caution (Needs Extra Review)

- **Domain models** (`domain/model/`) — changes affect serialization/database schema
- **Database entities** (`data/local/entity/`) — may require migrations
- **Room DAOs** (`data/local/dao/`) — risk of query errors
- **Hilt modules** (`di/`) — can break dependency injection if misconfigured
- **Security code** (`data/security/`) — any mistake exposes secrets

---

## What Agents Must Never Modify

These are **hard boundaries**. Any change requires explicit human approval.

### ❌ Never Auto-Modify

1. **Build infrastructure without approval:**
   - `build.gradle.kts` (root and app/)
   - `settings.gradle.kts`
   - `gradle.properties`
   - `gradle/wrapper/` and `gradle-wrapper.jar`
   - `gradlew` / `gradlew.bat`
   - `.github/workflows/` (CI/CD configuration)

2. **Secrets and key material:**
   - Never commit plaintext API keys, tokens, or keystores
   - Never write secrets to Room, DataStore, or SharedPreferences
   - Always use `SecureKeyStore` for sensitive data

3. **Security boundaries:**
   - Do not modify file sandboxing logic in `FileRepository.resolveSafe()`
   - Do not add features allowing path traversal outside project root
   - Do not disable encryption for stored keys

4. **License and attribution:**
   - Do not change license declaration
   - Do not remove or modify legal/attribution comments

5. **Breaking architecture decisions:**
   - Do not replace MVVM with a different pattern without approval
   - Do not add synchronous blocking operations on the main thread without justification
   - Do not introduce tight coupling between UI and domain logic

6. **Version numbers & versioning:**
   - Do not bump version codes/names in `build.gradle.kts` without human approval
   - Do not create release tags without explicit direction

7. **Critical configuration files:**
   - `local.properties.example` (template only; do not include actual SDK paths)
   - `AndroidManifest.xml` permission declarations (can modify, but needs care)

---

## Review Checklist

Use this checklist **before** opening a PR or requesting approval:

### Code Quality

- [ ] Code follows Kotlin style guide (enforced via `kotlin.code.style=official`)
- [ ] No magic numbers; use named constants or theme values
- [ ] Functions are small (<30 lines) and single-responsibility
- [ ] Null safety enforced: use non-nullable types by default
- [ ] Coroutines use appropriate Dispatchers (no blocking on main thread)
- [ ] No duplicate code; extract to shared utilities if needed
- [ ] All public functions have KDoc comments

### Architecture

- [ ] Domain logic in `domain/`, UI in `ui/`, data in `data/`
- [ ] ViewModels delegate to repositories; repositories hide implementation
- [ ] Compose components are stateless; state managed in ViewModels
- [ ] No cross-package imports without going through a ViewModel/repository
- [ ] Hilt modules properly scoped and provided

### Security

- [ ] No hardcoded API keys or secrets
- [ ] All sensitive data uses `SecureKeyStore` (no plain SharedPreferences)
- [ ] File operations use `FileRepository.resolveSafe()`
- [ ] No eval(), runtime compilation, or remote code execution
- [ ] Backup/export excludes encrypted keys

### Testing

- [ ] Unit tests cover critical domain logic (agent, tools, diff, file operations)
- [ ] Tests pass locally: `./gradlew test`
- [ ] New database migrations include test coverage
- [ ] Manual testing steps documented in PR description

### Documentation

- [ ] Inline comments explain non-obvious logic
- [ ] New features documented in README.md (if user-facing)
- [ ] KDoc comments added for public APIs
- [ ] CLAUDE.md or AGENTS.md updated (if relevant)
- [ ] Commit message is clear and follows conventions

### Performance

- [ ] No O(n²) algorithms without caching or streaming
- [ ] Syntax highlighter unchanged (regex-based, intentionally fast)
- [ ] Large file operations do not block the main thread
- [ ] Memory usage reasonable for mid-range devices (API 26+)

### Compatibility

- [ ] Targets Android API 26+ (minSdk = 26)
- [ ] Kotlin 1.9.24 compatible
- [ ] Changes backward-compatible where possible
- [ ] Database migrations provided (if schema changed)

---

## Testing Checklist

All changes must be tested. Use this checklist:

### Unit Tests

- [ ] Domain logic tests: `./gradlew test`
- [ ] Mock repositories and external dependencies
- [ ] Test both happy path and error cases
- [ ] Coverage ≥70% for new code (goal)

**Example test location:** `app/src/test/java/com/vibecode/ide/domain/tools/ToolCallParserTest.kt`

### UI Tests (Instrumented)

- [ ] If screen behavior changed: add instrumented tests
- [ ] Test with Material 3 theme active
- [ ] Test with accessibility features (screen reader, large text)

**Example test location:** `app/src/androidTest/java/com/vibecode/ide/ui/editor/EditorScreenTest.kt`

### Manual Testing

**Minimum steps (before pushing):**

1. **Build locally:**
   ```bash
   chmod +x ./gradlew
   ./gradlew assembleDebug
   ```

2. **Install on device/emulator:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Test the specific feature:**
   - Follow the "How to Test" steps in your PR description
   - Verify expected behavior
   - Check error cases (invalid input, network failure, etc.)
   - On multiple devices if possible (phone, tablet)

4. **Check for regressions:**
   - Verify existing features still work (editor, chat, provider management)
   - Verify performance (app does not stutter or hang)
   - Verify no crashes or ANRs

### Termux-Specific Testing (if applicable)

If your change affects build, performance, or file I/O:

1. **Test build in Termux:**
   ```bash
   cd ~/VibeCodeAI
   ./gradlew assembleDebug
   ```

2. **Verify AAPT2 compatibility:** If Gradle fails with AAPT2 error, check `gradle.properties` override

3. **Test file operations:** If modifying `FileRepository`, test on actual Termux paths

### Database Migration Testing

If you modified Room entities:

- [ ] Migration runs without data loss
- [ ] Old data correctly migrated to new schema
- [ ] Query results unchanged (where applicable)
- [ ] Foreign keys and constraints enforced

**Code example:**
```kotlin
// app/src/androidTest/java/com/vibecode/ide/data/local/MigrationTest.kt

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private lateinit var helper: MigrationTestHelper
    
    @Before
    fun setup() {
        helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
        )
    }
    
    @Test
    fun migrate1To2() {
        // Test that migration from schema v1 to v2 works
    }
}
```

---

## Documentation Update Rules

Whenever you make a code change, check if documentation needs updating:

### README.md

- [ ] New user-facing feature? Add to "What's included"
- [ ] Build process changed? Update build instructions
- [ ] New architecture component? Add to "Project layout"
- [ ] New limitation discovered? Add to "Known limitations"

### AGENTS.md (this file)

- [ ] Workflow or convention changed? Update the relevant section
- [ ] New restricted file or area? Add to "What Agents Must Never Modify"
- [ ] New best practice? Add to "Code Style Expectations" or "AI Behavior Rules"

### CLAUDE.md

- Only update if making changes specific to Claude integration or deep context.
- Most updates should go to AGENTS.md instead.

### Inline Code Documentation

- [ ] New public function? Add KDoc
- [ ] Non-obvious logic? Add inline comment
- [ ] Complex algorithm (e.g., diff, parser)? Explain the approach

**KDoc template:**
```kotlin
/**
 * Brief one-line summary.
 *
 * Longer explanation if needed (return behavior, side effects, etc.).
 *
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType Condition that causes this exception
 *
 * Example:
 * ```
 * val result = myFunction(value)
 * ```
 */
fun myFunction(paramName: String): String {
    // ...
}
```

### Comments in Code

- ✅ Explain *why*, not *what*: the code shows what it does
- ✅ Reference issue numbers: `// TODO: #42 optimize this loop`
- ✅ Warn about gotchas: `// Note: this is O(n²); only use for small inputs`
- ❌ Avoid obvious comments: `i = i + 1  // increment i`

---

## Summary: Agent Workflow

**Recommended process for any AI agent:**

1. **Analyze:** Read README.md, AGENTS.md, CLAUDE.md, and relevant source files
2. **Understand:** Map the architecture, identify affected files, check constraints
3. **Propose:** Cite file paths, describe changes, ask for approval
4. **Review:** Wait for human confirmation; adjust if needed
5. **Implement:** Create branch, make focused changes, add tests
6. **Verify:** Run `./gradlew test`, build locally, manual testing
7. **Document:** Update README/AGENTS.md if needed; add inline comments
8. **PR:** Open against `develop`, include detailed description and test steps
9. **Merge:** Wait for approval; delete branch after merge

---

## Quick Reference

| Item | Value |
|------|-------|
| **Language** | Kotlin |
| **Min SDK** | 26 (Android 8) |
| **Target SDK** | 34 (Android 14) |
| **Architecture** | MVVM + Repository + Hilt DI |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Async** | Kotlin Coroutines |
| **Database** | Room |
| **Settings** | DataStore |
| **Key Store** | EncryptedSharedPreferences (Android Keystore) |
| **Default Branch** | `develop` |
| **Release Branch** | `main` |
| **Commit Style** | Conventional Commits (`type(scope): summary`) |
| **Build Tool** | Gradle 8.7 + Gradle Wrapper |

---

## Further Reading

- **[README.md](README.md)** — Build instructions, project overview, Termux guidance
- **[CLAUDE.md](CLAUDE.md)** — Deep context and reasoning for Claude-specific workflows
- **Kotlin Style Guide:** https://kotlinlang.org/docs/coding-conventions.html
- **Jetpack Compose Best Practices:** https://developer.android.com/jetpack/compose/think-in-compose
- **Android Security Best Practices:** https://developer.android.com/privacy-and-security

---

**Last Updated:** 2026-07-15  
**Version:** 1.0
