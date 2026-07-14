package com.vibecode.ide.domain.diff

enum class DiffLineType { EQUAL, ADDED, REMOVED }

data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
)

/**
 * Simple LCS-based line diff. Good enough for reviewing typical source-file
 * edits on a phone screen without pulling in an external diff library.
 */
object DiffUtil {

    fun diff(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.split("\n")
        val newLines = newText.split("\n")
        return diffLines(oldLines, newLines)
    }

    private fun diffLines(a: List<String>, b: List<String>): List<DiffLine> {
        val n = a.size
        val m = b.size
        // LCS length table
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1
                else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }

        val result = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        var oldLineNo = 1
        var newLineNo = 1
        while (i < n && j < m) {
            when {
                a[i] == b[j] -> {
                    result.add(DiffLine(DiffLineType.EQUAL, a[i], oldLineNo, newLineNo))
                    i++; j++; oldLineNo++; newLineNo++
                }
                lcs[i + 1][j] >= lcs[i][j + 1] -> {
                    result.add(DiffLine(DiffLineType.REMOVED, a[i], oldLineNo, null))
                    i++; oldLineNo++
                }
                else -> {
                    result.add(DiffLine(DiffLineType.ADDED, b[j], null, newLineNo))
                    j++; newLineNo++
                }
            }
        }
        while (i < n) { result.add(DiffLine(DiffLineType.REMOVED, a[i], oldLineNo, null)); i++; oldLineNo++ }
        while (j < m) { result.add(DiffLine(DiffLineType.ADDED, b[j], null, newLineNo)); j++; newLineNo++ }
        return result
    }

    /** Collapses long unchanged runs to a handful of context lines around each change, VS Code style. */
    fun withContext(lines: List<DiffLine>, contextSize: Int = 3): List<DiffLine?> {
        if (lines.none { it.type != DiffLineType.EQUAL }) return lines
        val keep = BooleanArray(lines.size)
        lines.forEachIndexed { idx, line ->
            if (line.type != DiffLineType.EQUAL) {
                for (k in maxOf(0, idx - contextSize)..minOf(lines.lastIndex, idx + contextSize)) keep[k] = true
            }
        }
        val out = mutableListOf<DiffLine?>()
        var lastKept = false
        keep.forEachIndexed { idx, k ->
            if (k) {
                out.add(lines[idx])
            } else if (lastKept) {
                out.add(null) // marker for "..." collapsed section
            }
            lastKept = k
        }
        return out
    }
}
