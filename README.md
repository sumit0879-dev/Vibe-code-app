# VibeCode AI IDE

A mobile-first AI coding assistant for Android — inspired by Cursor, Windsurf, and
Claude Code, built to run entirely on-device and to be compiled entirely inside
**Termux**. No specific AI provider is hardcoded: you connect any OpenAI-compatible
(or custom) API, add unlimited models, and the app's built-in coding agent uses
file-read/write/search tools with mandatory diff review before anything is saved.

## What's included

- **Kotlin + Jetpack Compose + Material 3**, MVVM, Hilt DI, Room, DataStore.
- **Dynamic AI Provider Manager** — add unlimited providers (name, base URL, auth
  method, request format, headers) with no vendor hardcoded.
- **Model Manager** — automatic model discovery (`GET /models`) or manual entry,
  per provider, unlimited models.
- **AI coding agent** with tools: `read_file`, `list_directory`, `search_project`
  (auto-run), and `create_file` / `update_file` / `delete_file` (always shown as a
  diff and require explicit Approve/Reject — nothing is written without consent).
- **No code execution** — the agent only reads/writes source files. There is no
  terminal, build, or run integration, by design.
- **Mobile code editor** — multi-tab, dirty-state dots, line numbers, regex-based
  syntax highlighting for 10+ languages, 5 selectable color themes.
- **ChatGPT-style chat** — Markdown rendering, fenced code blocks with a copy
  button, streaming responses (SSE) with a stop button.
- **Persistent local history** — Room database. Closing the app, restarting the
  phone, or switching provider/model never loses a project's chat history;
  reopening a project resumes the latest session automatically.
- **Encrypted API keys** — stored via `EncryptedSharedPreferences` backed by the
  Android Keystore (AES-256-GCM). Keys are never written to the Room database or
  to backup/export files in plaintext.
- **Settings** — theme, editor font size/line numbers/word wrap/tab size,
  JSON export/import of providers (keys excluded) + models + preferences.

## Project layout

```
VibeCodeAI/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/vibecode/ide/
│       │   ├── data/            # Room, DataStore, secure storage, repositories, network
│       │   ├── domain/          # models, coding-agent tools, diff engine
│       │   ├── di/              # Hilt modules
│       │   ├── ui/              # Compose screens (home, editor, chat, providers, models, settings)
│       │   └── util/            # syntax highlighter
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── gradle/wrapper/
```

## Building the APK in Termux

### 1. Install dependencies

```bash
pkg update && pkg upgrade -y
pkg install -y openjdk-17 wget unzip git
```

Verify Java:

```bash
javac -version   # should print 17.x
```

### 2. Install the Android SDK command-line tools

Termux can't run the Android Studio installer, so grab the SDK command-line
tools directly:

```bash
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
rm commandlinetools-linux-11076708_latest.zip
```

Add the SDK to your shell profile (`~/.bashrc` or `~/.zshrc`):

```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

Reload and install the required SDK packages (accept licenses when prompted):

```bash
source ~/.bashrc
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

> Termux runs Android binaries via a Linux userland (proot-distro or native
> Termux), so the SDK's `aapt2` needs to match your device's CPU architecture
> (typically `arm64-v8a`). If Gradle fails with an AAPT2 "cannot execute binary
> file" error, add this to `gradle.properties`:
> `android.aapt2FromMavenOverride=<path to a termux-compatible aapt2 binary>`
> (search "termux android aapt2" for a prebuilt binary matching your Termux version).

### 3. Extract the project and set the SDK path

```bash
cd ~
unzip VibeCodeAI.zip
cd VibeCodeAI
cp local.properties.example local.properties
```

Edit `local.properties` so `sdk.dir` points at `~/android-sdk` (i.e. the actual
absolute path, e.g. `/data/data/com.termux/files/home/android-sdk`).

### 4. Generate the Gradle wrapper distribution (first run only)

The wrapper jar is already included in `gradle/wrapper/gradle-wrapper.jar`. The
first `./gradlew` invocation downloads Gradle 8.7 itself (~200MB) from
`services.gradle.org` — make sure Termux has network access and enough free
storage.

```bash
chmod +x gradlew
./gradlew --version
```

### 5. Build the debug APK

```bash
./gradlew assembleDebug
```

The output APK will be at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it directly on-device with:

```bash
pkg install -y android-tools   # provides adb, optional if building on-device
# or simply open the APK from a file manager / Termux:
termux-open app/build/outputs/apk/debug/app-debug.apk
```

### 6. Building a release APK (optional)

```bash
./gradlew assembleRelease
```

You'll need to configure signing (a keystore) for a release build to install
cleanly outside of debug mode — add a `signingConfigs` block to
`app/build.gradle.kts` pointing at your keystore, or use
`./gradlew assembleRelease` unsigned and sign manually with `apksigner` from
the SDK build-tools.

## Modifying the project

- **Add a new AI provider default field** → `data/local/entity/ProviderEntity.kt`,
  `domain/model/AiProvider.kt`, and the mapping in
  `data/repository/ProviderRepository.kt`.
- **Add a new coding-agent tool** → extend `ToolName` in
  `domain/tools/ToolDefinition.kt`, document it in `ToolDocs.description`, and
  handle it in `ChatViewModel.executeReadOnlyTool` (read-only) or the
  `finishAssistantTurn` mutating-tool branch (needs approval).
- **Add a new editor color theme** → `ui/theme/Theme.kt` (`EditorColorTheme` enum
  + `paletteFor`).
- **Add a new syntax-highlighted language** → `util/SyntaxHighlighter.kt`
  (`Language` enum, `KEYWORDS`, `COMMENT_REGEX`).
- **Change the coding-agent's system prompt** →
  `domain/tools/SystemPromptBuilder.kt`.
- All screens are plain Jetpack Compose + Hilt `ViewModel`s under `ui/`, so any
  screen can be edited independently — e.g. `ui/editor/EditorScreen.kt` for the
  main IDE layout, `ui/chat/ChatViewModel.kt` for the agent orchestration logic.

## How the AI coding agent works

There is no dependency on any provider's native function-calling schema — this
keeps the app compatible with literally any OpenAI-compatible or custom text
endpoint. Instead, the system prompt (see `SystemPromptBuilder`) teaches the
model a simple, provider-agnostic protocol: to call a tool, the assistant emits
a fenced ```` ```tool ```` code block containing a single JSON object, e.g.

```
```tool
{"tool": "update_file", "path": "app/src/main/MainActivity.kt", "content": "..."}
```
```

`ToolCallParser` extracts this block after each streamed turn. Read-only tools
(`read_file`, `list_directory`, `search_project`) execute immediately and their
output is fed back to the model as a follow-up turn (capped at a handful of
automatic continuations to avoid runaway loops). Mutating tools (`create_file`,
`update_file`, `delete_file`) are **never** executed automatically — a diff is
computed (`domain/diff/DiffUtil.kt`) and shown in a dialog; the file is only
written after the user taps **Approve**.

## Security notes

- API keys are stored via Android's `EncryptedSharedPreferences`
  (`data/security/SecureKeyStore.kt`), keyed by the hardware-backed Android
  Keystore. They are referenced from Room only by a random alias — the
  `providers` table itself contains no secrets and is safe to inspect/back up.
- Backup/export (`Settings → Backup & Restore`) intentionally excludes API
  keys; after importing a backup you'll need to re-enter each provider's key.
- File tools are sandboxed to the open project's root directory
  (`FileRepository.resolveSafe`) — path traversal outside the project (e.g.
  `../../etc`) is rejected.

## Known limitations / things to extend

- The syntax highlighter is regex-based (fast, good-enough visual highlighting),
  not a full incremental parser — it won't catch every edge case of every
  language grammar.
- The line-diff algorithm is a straightforward LCS implementation; it's fine
  for typical source-file edits but not optimized for huge files.
- There is intentionally no terminal, build, or code-execution integration.
- "Open folder" currently takes a typed absolute path rather than a Storage
  Access Framework picker — swap in `ActivityResultContracts.OpenDocumentTree`
  in `ui/home/HomeScreen.kt` if you'd prefer a native folder picker (note that
  SAF-returned `content://` URIs need `DocumentFile`, not `java.io.File`, so
  `FileRepository` would need a matching URI-based variant).

## License

Generated for personal use — adapt as needed for your own project.
