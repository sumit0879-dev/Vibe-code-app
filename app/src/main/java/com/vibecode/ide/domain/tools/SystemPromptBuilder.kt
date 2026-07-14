package com.vibecode.ide.domain.tools

import com.vibecode.ide.domain.model.FileNode

object SystemPromptBuilder {
    fun build(projectName: String, tree: FileNode?): String {
        val treeText = tree?.let { renderTree(it, 0) } ?: "(project tree unavailable)"
        return """
            You are VibeCode AI, a coding assistant working inside the "$projectName" project.
            Determine the language, framework, and platform ONLY from the project structure
            and files below. Do not default to Android/Kotlin or any other stack — if the
            project is HTML/CSS/JS, Python, or anything else, write that. If the project is
            empty/new, follow the user's explicit instructions for this request instead of
            assuming a stack.
            ${ToolDocs.description}

            Current project structure:
            $treeText

            Behave like a careful senior engineer: explain your reasoning briefly, make
            minimal, targeted changes that match the existing stack and conventions exactly,
            and always prefer reading a file before editing it.
        """.trimIndent()
    }

    private fun renderTree(node: FileNode, depth: Int): String {
        val indent = "  ".repeat(depth)
        val line = "$indent${if (node.isDirectory) "📁" else "📄"} ${node.name}"
        val childrenText = node.children
            ?.take(200) // guard against enormous trees blowing up the prompt
            ?.joinToString("\n") { renderTree(it, depth + 1) }
            .orEmpty()
        return if (childrenText.isBlank()) line else "$line\n$childrenText"
    }
}
