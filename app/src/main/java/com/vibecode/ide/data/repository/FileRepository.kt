package com.vibecode.ide.data.repository

import com.vibecode.ide.domain.model.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Names always skipped when listing/searching a project tree. */
private val IGNORED_DIR_NAMES = setOf(
    ".git", ".gradle", "build", ".idea", "node_modules", ".kotlin", "gradle",
)

class PathEscapeException(message: String) : Exception(message)

@Singleton
class FileRepository @Inject constructor() {

    /** Lists the immediate children of [dirPath], guarding against escaping [projectRoot]. */
    suspend fun listChildren(projectRoot: String, dirPath: String): List<FileNode> = withContext(Dispatchers.IO) {
        val dir = resolveSafe(projectRoot, dirPath)
        if (!dir.exists()) {
            throw java.io.IOException("'$dirPath' does not exist on disk. If you just created this project, storage access may not be granted yet.")
        }
        if (!dir.isDirectory) return@withContext emptyList()
        val listed = dir.listFiles()
            ?: throw java.io.IOException("Could not read '$dirPath' — the app may not have storage permission.")
        listed
            .filter { !IGNORED_DIR_NAMES.contains(it.name) }
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { it.toNode() }
    }

    /** Recursively builds the full tree — used for giving the AI project-structure context. */
    suspend fun buildTree(projectRoot: String, maxDepth: Int = 6): FileNode = withContext(Dispatchers.IO) {
        val root = File(projectRoot)
        buildTreeRecursive(root, 0, maxDepth)
    }

    private fun buildTreeRecursive(file: File, depth: Int, maxDepth: Int): FileNode {
        if (!file.isDirectory || depth >= maxDepth) return file.toNode()
        val children = file.listFiles()
            ?.filter { !IGNORED_DIR_NAMES.contains(it.name) }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { buildTreeRecursive(it, depth + 1, maxDepth) }
            ?: emptyList()
        return file.toNode().copy(children = children)
    }

    suspend fun readFile(projectRoot: String, relativeOrAbsolutePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = resolveSafe(projectRoot, relativeOrAbsolutePath)
            if (!file.exists()) return@withContext Result.failure(IllegalArgumentException("File not found: $relativeOrAbsolutePath"))
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFile(projectRoot: String, relativeOrAbsolutePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = resolveSafe(projectRoot, relativeOrAbsolutePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(projectRoot: String, relativeOrAbsolutePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = resolveSafe(projectRoot, relativeOrAbsolutePath)
            if (file.exists()) return@withContext Result.failure(IllegalStateException("File already exists: $relativeOrAbsolutePath"))
            file.parentFile?.mkdirs()
            file.writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(projectRoot: String, relativeOrAbsolutePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = resolveSafe(projectRoot, relativeOrAbsolutePath)
            if (!file.exists()) return@withContext Result.failure(IllegalArgumentException("File not found: $relativeOrAbsolutePath"))
            val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (ok) Result.success(Unit) else Result.failure(IllegalStateException("Could not delete $relativeOrAbsolutePath"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(projectRoot: String, relativeOrAbsolutePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = resolveSafe(projectRoot, relativeOrAbsolutePath)
            if (dir.exists() || dir.mkdirs()) Result.success(Unit)
            else Result.failure(IllegalStateException("Could not create directory $relativeOrAbsolutePath"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Simple substring / filename search across the project (skipping ignored + binary-ish files). */
    suspend fun searchProject(projectRoot: String, query: String, maxResults: Int = 50): List<SearchHit> = withContext(Dispatchers.IO) {
        val root = File(projectRoot)
        val hits = mutableListOf<SearchHit>()
        val lowerQuery = query.lowercase()

        fun walk(dir: File) {
            if (hits.size >= maxResults) return
            val children = dir.listFiles() ?: return
            for (child in children.sortedBy { it.name }) {
                if (hits.size >= maxResults) return
                if (IGNORED_DIR_NAMES.contains(child.name)) continue
                if (child.isDirectory) {
                    walk(child)
                } else {
                    if (child.name.lowercase().contains(lowerQuery)) {
                        hits.add(SearchHit(child.relativePathFrom(root), lineNumber = null, lineText = "(filename match)"))
                    }
                    if (child.length() < 2_000_000 && isProbablyText(child)) {
                        runCatching {
                            child.readLines().forEachIndexed { idx, line ->
                                if (hits.size >= maxResults) return@forEachIndexed
                                if (line.lowercase().contains(lowerQuery)) {
                                    hits.add(SearchHit(child.relativePathFrom(root), idx + 1, line.trim().take(160)))
                                }
                            }
                        }
                    }
                }
            }
        }
        walk(root)
        hits
    }

    private fun isProbablyText(file: File): Boolean {
        val textExtensions = setOf(
            "kt", "kts", "java", "xml", "json", "gradle", "md", "txt", "py", "js", "ts",
            "jsx", "tsx", "html", "css", "yaml", "yml", "properties", "pro", "cfg", "toml",
            "c", "cpp", "h", "hpp", "rs", "go", "rb", "sh", "sql",
        )
        return textExtensions.contains(file.extension.lowercase())
    }

    private fun File.relativePathFrom(root: File): String =
        this.absolutePath.removePrefix(root.absolutePath).trimStart('/')

    private fun File.toNode() = FileNode(
        name = name,
        path = absolutePath,
        isDirectory = isDirectory,
        children = null,
        sizeBytes = if (isFile) length() else 0L,
    )

    /**
     * Resolves a path against the project root and verifies the canonical result
     * still lives inside the project root, preventing path traversal (e.g. "../../etc/passwd").
     */
    private fun resolveSafe(projectRoot: String, path: String): File {
        val rootCanonical = File(projectRoot).canonicalFile
        val candidate = if (File(path).isAbsolute) File(path) else File(rootCanonical, path)
        val candidateCanonical = candidate.canonicalFile
        if (!candidateCanonical.path.startsWith(rootCanonical.path)) {
            throw PathEscapeException("Path '$path' escapes the project root")
        }
        return candidateCanonical
    }
}

data class SearchHit(val filePath: String, val lineNumber: Int?, val lineText: String)
