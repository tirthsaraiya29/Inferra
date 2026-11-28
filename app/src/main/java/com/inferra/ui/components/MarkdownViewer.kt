package com.inferra.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun MarkdownViewer(
    markdown: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    SelectionContainer(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            3 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = block.text,
                                style = style,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (block.level <= 2) {
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }

                    is MarkdownBlock.CodeBlock -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = block.language.ifBlank { "code" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(block.code))
                                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Code",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    Text(
                                        text = block.code,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.Quote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is MarkdownBlock.ListItem -> {
                        Row(
                            modifier = Modifier.padding(start = (block.indentLevel * 12).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = renderInlineMarkdown(block.text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    is MarkdownBlock.Table -> {
                        TableBlockView(block = block)
                    }

                    is MarkdownBlock.Image -> {
                        AsyncImage(
                            model = block.url,
                            contentDescription = block.alt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 6.dp)
                        )
                    }

                    is MarkdownBlock.HorizontalRule -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = renderInlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableBlockView(block: MarkdownBlock.Table) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        Column {
            if (block.headers.isNotEmpty()) {
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    block.headers.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .width(120.dp)
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            block.rows.forEach { row ->
                Row {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .width(120.dp)
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))
            }
        }
    }
}

private fun renderInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
    }
}

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class ListItem(val text: String, val indentLevel: Int = 0) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class Image(val alt: String, val url: String) : MarkdownBlock
    data object HorizontalRule : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()

    var inCodeBlock = false
    var codeLang = ""
    val codeBuilder = StringBuilder()

    var inTable = false
    val tableHeaders = mutableListOf<String>()
    val tableRows = mutableListOf<List<String>>()

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuilder.toString().trimEnd()))
                codeBuilder.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuilder.append(line).append("\n")
            continue
        }

        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            val cells = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.all { it.all { ch -> ch == '-' || ch == ':' } }) {
                continue
            }
            if (!inTable) {
                inTable = true
                tableHeaders.clear()
                tableHeaders.addAll(cells)
            } else {
                tableRows.add(cells)
            }
            continue
        } else if (inTable) {
            blocks.add(MarkdownBlock.Table(tableHeaders.toList(), tableRows.toList()))
            inTable = false
            tableHeaders.clear()
            tableRows.clear()
        }

        when {
            trimmed.startsWith("# ") -> blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            trimmed.startsWith("## ") -> blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            trimmed.startsWith("### ") -> blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            trimmed.startsWith("#### ") -> blocks.add(MarkdownBlock.Heading(4, trimmed.removePrefix("#### ").trim()))
            trimmed.startsWith("> ") -> blocks.add(MarkdownBlock.Quote(trimmed.removePrefix("> ").trim()))
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> blocks.add(MarkdownBlock.ListItem(trimmed.drop(2).trim(), 0))
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> blocks.add(MarkdownBlock.HorizontalRule)
            trimmed.startsWith("![") && trimmed.contains("](") -> {
                val alt = trimmed.substringAfter("![").substringBefore("]")
                val url = trimmed.substringAfter("](").substringBefore(")")
                blocks.add(MarkdownBlock.Image(alt, url))
            }
            trimmed.isNotBlank() -> blocks.add(MarkdownBlock.Paragraph(trimmed))
        }
    }

    if (inTable) {
        blocks.add(MarkdownBlock.Table(tableHeaders.toList(), tableRows.toList()))
    }

    return blocks
}
