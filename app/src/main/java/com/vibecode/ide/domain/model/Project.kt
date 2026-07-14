package com.vibecode.ide.domain.model

data class Project(
    val id: String,
    val name: String,
    val rootPath: String,
    val createdAt: Long,
    val lastOpenedAt: Long,
    val selectedProviderId: String? = null,
    val selectedModelId: String? = null,
)

/** A node in the project's file tree, lazily expandable. */
data class FileNode(
    val name: String,
    val path: String,           // absolute path on disk
    val isDirectory: Boolean,
    val children: List<FileNode>? = null, // null until expanded
    val sizeBytes: Long = 0L,
)
