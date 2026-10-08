package com.example.git

enum class DiffLineType {
    ADDED,
    DELETED,
    UNCHANGED
}

data class DiffLine(
    val type: DiffLineType,
    val oldLineNum: Int?,
    val newLineNum: Int?,
    val text: String
)

data class FileDiff(
    val relativePath: String,
    val lines: List<DiffLine>,
    val addedCount: Int,
    val deletedCount: Int
)

object GitDiffEngine {

    /**
     * Computes unified line-by-line diff using Longest Common Subsequence (LCS) algorithm.
     */
    fun computeDiff(oldText: String, newText: String, relativePath: String): FileDiff {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()

        val lcsMatrix = computeLcsMatrix(oldLines, newLines)
        val diffLines = mutableListOf<DiffLine>()

        var i = oldLines.size
        var j = newLines.size

        val backtrack = mutableListOf<DiffLine>()

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
                backtrack.add(DiffLine(DiffLineType.UNCHANGED, i, j, oldLines[i - 1]))
                i--
                j--
            } else if (j > 0 && (i == 0 || lcsMatrix[i][j - 1] >= lcsMatrix[i - 1][j])) {
                backtrack.add(DiffLine(DiffLineType.ADDED, null, j, newLines[j - 1]))
                j--
            } else if (i > 0 && (j == 0 || lcsMatrix[i][j - 1] < lcsMatrix[i - 1][j])) {
                backtrack.add(DiffLine(DiffLineType.DELETED, i, null, oldLines[i - 1]))
                i--
            }
        }

        diffLines.addAll(backtrack.reversed())

        val added = diffLines.count { it.type == DiffLineType.ADDED }
        val deleted = diffLines.count { it.type == DiffLineType.DELETED }

        return FileDiff(
            relativePath = relativePath,
            lines = diffLines,
            addedCount = added,
            deletedCount = deleted
        )
    }

    private fun computeLcsMatrix(oldLines: List<String>, newLines: List<String>): Array<IntArray> {
        val n = oldLines.size
        val m = newLines.size
        val dp = Array(n + 1) { IntArray(m + 1) }

        for (i in 1..n) {
            for (j in 1..m) {
                if (oldLines[i - 1] == newLines[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        return dp
    }
}
