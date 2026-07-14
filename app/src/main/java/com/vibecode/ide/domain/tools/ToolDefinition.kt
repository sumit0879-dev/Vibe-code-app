package com.vibecode.ide.domain.tools

/** The fixed set of tools the AI coding agent is allowed to call. */
enum class ToolName(val id: String, val mutatesFiles: Boolean) {
    READ_FILE("read_file", false),
    CREATE_FILE("create_file", true),
    UPDATE_FILE("update_file", true),
    DELETE_FILE("delete_file", true),
    SEARCH_PROJECT("search_project", false),
    LIST_DIRECTORY("list_directory", false);

    companion object {
        fun fromId(id: String): ToolName? = entries.firstOrNull { it.id == id }
    }
}

/** A parsed tool invocation extracted from an assistant message. */
data class ParsedToolCall(
    val tool: ToolName,
    val path: String? = null,
    val content: String? = null,
    val query: String? = null,
    val rawArgsJson: String,
)

/** Human/AI-readable documentation of every tool, injected into the system prompt. */
object ToolDocs {
    val description: String = """
        You are an AI coding agent. The app hosting you happens to be a mobile IDE, but
        that describes the tool only — it says nothing about what kind of project you're
        working on. The project may be Android, a web app, a script, or anything else;
        always take its actual stack from the project structure, never from the host app.
        You can propose file operations using the following tools. To call a tool, output
        a fenced code block with the language tag `tool` containing a single JSON object,
        like:

        ```tool
        {"tool": "read_file", "path": "<a real path from the project structure above>"}
        ```

        Available tools:
        - read_file: {"tool": "read_file", "path": "<relative path>"}
        - list_directory: {"tool": "list_directory", "path": "<relative path or '.'>"}
        - search_project: {"tool": "search_project", "query": "<text to find>"}
        - create_file: {"tool": "create_file", "path": "<relative path>", "content": "<full file content>"}
        - update_file: {"tool": "update_file", "path": "<relative path>", "content": "<full NEW file content>"}
        - delete_file: {"tool": "delete_file", "path": "<relative path>"}

        Rules:
        - read_file, list_directory, and search_project run automatically and their
          results are sent back to you as a follow-up message so you can continue.
        - create_file, update_file, and delete_file are NEVER executed automatically.
          They are shown to the user as a diff for manual approval. Do not assume
          they succeeded until you are told the user approved them.
        - You may only call ONE tool per message. Explain what you're doing in plain
          text before the tool block.
        - Never invent file contents you haven't read — use read_file first when
          modifying an existing file you haven't seen in this conversation.
        - You do not have the ability to run code, execute builds, or use a terminal.
          You only read and write source files.
    """.trimIndent()
}
