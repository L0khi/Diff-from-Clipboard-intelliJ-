package com.yourorg

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import java.awt.datatransfer.DataFlavor

class ApplyDiffAction : AnAction() {
    private val logger = Logger.getInstance(ApplyDiffAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            showError("No active editor found", project)
            return
        }

        val clipboardText = try {
            CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor) as? String
        } catch (ex: Exception) {
            null
        }

        if (clipboardText.isNullOrBlank()) {
            showError("Clipboard is empty or not text", project)
            return
        }

        val hunks = try {
            parseDiff(clipboardText)
        } catch (ex: Exception) {
            showError("Invalid diff format: ${ex.message}", project)
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            hunks.asReversed().forEach { hunk ->
                applyHunk(editor, hunk, project)
            }
        }
    }

    /** Diff hunk structure */
    data class DiffHunk(
        val startLine: Int,
        val original: List<String>,
        val modified: List<String>
    )

    /** Parse unified diff, tolerant to malformed ones */
    private fun parseDiff(text: String): List<DiffHunk> {
        val hunks = mutableListOf<DiffHunk>()
        var startLine = 1
        var currentOriginal = mutableListOf<String>()
        var currentModified = mutableListOf<String>()

        text.lines().forEach { line ->
            when {
                line.startsWith("@@") -> {
                    if (currentOriginal.isNotEmpty() || currentModified.isNotEmpty()) {
                        hunks.add(DiffHunk(startLine, currentOriginal, currentModified))
                        currentOriginal = mutableListOf()
                        currentModified = mutableListOf()
                    }
                    val regex = "@@ -(\\d+),?\\d* \\+(\\d+),?\\d* @@".toRegex()
                    val match = regex.find(line)
                    startLine = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
                }
                line.startsWith("-") -> currentOriginal.add(line.drop(1))
                line.startsWith("+") -> currentModified.add(line.drop(1))
                line.startsWith(" ") -> {
                    val ctx = line.drop(1)
                    currentOriginal.add(ctx)
                    currentModified.add(ctx)
                }
                else -> {
                    // Treat garbage as context
                    currentOriginal.add(line)
                    currentModified.add(line)
                }
            }
        }

        if (currentOriginal.isNotEmpty() || currentModified.isNotEmpty()) {
            hunks.add(DiffHunk(startLine, currentOriginal, currentModified))
        }

        return hunks
    }

    /** Apply hunk with alignment + fuzzy fallback */
    private fun applyHunk(editor: Editor, hunk: DiffHunk, project: com.intellij.openapi.project.Project?) {
        val document = editor.document
        val lineCount = document.lineCount

        val startLineIndex = (hunk.startLine - 1).coerceAtLeast(0)
        val endLineIndex = (startLineIndex + hunk.original.size - 1).coerceAtMost(lineCount - 1)

        val existingText = document.getText(
            TextRange(
                document.getLineStartOffset(startLineIndex),
                document.getLineEndOffset(endLineIndex)
            )
        ).lines()

        // Context check
        if (!existingText.containsAll(hunk.original.take(1))) {
            val fuzzyIndex = (0 until lineCount).find { idx ->
                val line = document.getText(
                    TextRange(
                        document.getLineStartOffset(idx),
                        document.getLineEndOffset(idx)
                    )
                )
                line.trim() == hunk.original.firstOrNull()?.trim()
            }

            if (fuzzyIndex != null) {
                applyHunk(editor, hunk.copy(startLine = fuzzyIndex + 1), project)
                return
            } else {
                showError("Hunk could not be aligned, skipping", project)
                return
            }
        }

        val startOffset = document.getLineStartOffset(startLineIndex)
        val endOffset = document.getLineEndOffset(endLineIndex)

        document.replaceString(startOffset, endOffset, hunk.modified.joinToString("\n"))
    }

    /** Simple error popup */
    private fun showError(message: String, project: com.intellij.openapi.project.Project?) {
        Messages.showErrorDialog(project, message, "Apply Diff")
        logger.warn(message)
    }
}
