package com.yourorg

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.util.Alarm
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import java.awt.Font
import java.awt.datatransfer.DataFlavor

class ApplyDiffAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val clipboardText =
            CopyPasteManager.getInstance().contents?.getTransferData(DataFlavor.stringFlavor) as? String
        if (clipboardText.isNullOrBlank()) {
            Messages.showInfoMessage(project, "Clipboard is empty or not text.", "Apply Diff")
            return
        }

        val lines = clipboardText.lines()
        val hunks = parseDiff(lines)
        val results = mutableListOf<String>()

        try {
            WriteCommandAction.runWriteCommandAction(project) {
                for ((index, hunk) in hunks.withIndex()) {
                    try {
                        applyHunkSafely(project, editor, hunk)
                        results.add("Hunk #${index + 1}: ✅ Applied")
                    } catch (ex: Exception) {
                        results.add("Hunk #${index + 1}: ❌ ${ex.message}")
                    }
                }
            }
        } catch (ex: Exception) {
            results.add("Global error: ${ex.message}")
        }

        // Non-blocking result feedback
        ApplicationManager.getApplication().invokeLater {
            val summary = results.joinToString("\n")
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DiffApplier")
                .createNotification(summary, NotificationType.INFORMATION)
                .notify(project)
        }
    }

    private fun parseDiff(lines: List<String>): List<List<String>> {
        val hunks = mutableListOf<List<String>>()
        val current = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("***") || line.startsWith("---") ||
                line.startsWith("+++") || line.startsWith("index ") ||
                line.startsWith("diff --git")
            ) continue

            if (line.startsWith("@@")) {
                if (current.isNotEmpty()) hunks.add(current.toList())
                current.clear()
            } else current.add(line)
        }

        if (current.isNotEmpty()) hunks.add(current.toList())
        return hunks
    }

    private fun applyHunkSafely(project: Project, editor: Editor, hunk: List<String>) {
        val document = editor.document
        val caretOffset = editor.caretModel.offset
        val text = document.text

        val content = hunk.filter { it.isNotBlank() }
        if (content.isEmpty()) return

        val isAddOnly = content.all { it.startsWith("+") }
        val isDeleteOnly = content.all { it.startsWith("-") }

        val addedText = content.filter { it.startsWith("+") }.joinToString("\n") { it.removePrefix("+") }
        val removedText = content.filter { it.startsWith("-") }.joinToString("\n") { it.removePrefix("-") }

        val anchorOffset = findAnchorOffset(text, content, caretOffset)

        if (isAddOnly) {
            val lineEndOffset = document.getLineEndOffset(document.getLineNumber(caretOffset))
            document.insertString(lineEndOffset, "\n$addedText")
            blinkLines(editor, document.getLineNumber(lineEndOffset),
                document.getLineNumber(lineEndOffset) + addedText.lines().size)
        } else if (isDeleteOnly) {
            val idx = text.indexOf(removedText, caretOffset)
            if (idx >= 0) {
                document.deleteString(idx, idx + removedText.length)
                blinkLines(editor, document.getLineNumber(idx), document.getLineNumber(idx))
            }
        } else {
            val windowSize = 200
            val start = (caretOffset - windowSize).coerceAtLeast(0)
            val end = (caretOffset + windowSize).coerceAtMost(text.length)
            val contextWindow = text.substring(start, end)
            val removeIdx = contextWindow.indexOf(removedText)

            if (removeIdx >= 0) {
                val replaceStart = start + removeIdx
                val replaceEnd = replaceStart + removedText.length
                document.replaceString(replaceStart, replaceEnd, addedText)
                blinkLines(editor, document.getLineNumber(replaceStart),
                    document.getLineNumber(replaceStart) + addedText.lines().size)
            } else {
                document.insertString(anchorOffset, "\n$addedText")
                blinkLines(editor, document.getLineNumber(anchorOffset),
                    document.getLineNumber(anchorOffset) + addedText.lines().size)
            }
        }

        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    private fun findAnchorOffset(text: String, hunk: List<String>, caretOffset: Int): Int {
        val contextLines = hunk.filter { !it.startsWith("+") && !it.startsWith("-") }
        if (contextLines.isEmpty()) return caretOffset

        val contextText = contextLines.joinToString("\n")
        val idx = text.indexOf(contextText, caretOffset)
        if (idx != -1) return idx

        val windowSize = 1000
        val start = (caretOffset - windowSize).coerceAtLeast(0)
        val end = (caretOffset + windowSize).coerceAtMost(text.length)
        val nearby = text.substring(start, end)
        val found = nearby.indexOf(contextText)
        return if (found != -1) start + found else caretOffset
    }

    private fun blinkLines(editor: Editor, startLine: Int, endLine: Int) {
        val markup = editor.markupModel
        val startOffset = editor.document.getLineStartOffset(startLine)
        val endOffset = editor.document.getLineEndOffset(endLine.coerceAtMost(editor.document.lineCount - 1))

        val highlight = markup.addRangeHighlighter(
            startOffset, endOffset, HighlighterLayer.SELECTION - 1,
            TextAttributes(null, JBColor.YELLOW, null, null, Font.PLAIN),
            HighlighterTargetArea.EXACT_RANGE
        )

        Alarm().addRequest({ markup.removeHighlighter(highlight) }, 800)
    }
}
