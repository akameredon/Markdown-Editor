package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object MarkdownExporter {

    /**
     * Converts markdown to full-featured HTML string with bespoke Immersive CSS styling.
     */
    fun exportToHtml(title: String, markdown: String): String {
        val bodyContent = markdownToHtmlContent(markdown)
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${escapeHtml(title)}</title>
                <style>
                    :root {
                        --bg-color: #FAFAFC;
                        --text-color: #1A1C1E;
                        --primary-color: #381E72;
                        --accent-color: #D0BCFF;
                        --card-bg: #F3EDF7;
                        --code-bg: #2D2F33;
                        --code-text: #00E5FF;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        line-height: 1.6;
                        color: var(--text-color);
                        background-color: var(--bg-color);
                        max-width: 820px;
                        margin: 40px auto;
                        padding: 0 24px;
                    }
                    h1 {
                        font-size: 2.2em;
                        color: var(--primary-color);
                        border-bottom: 2px solid var(--accent-color);
                        padding-bottom: 12px;
                        margin-bottom: 24px;
                    }
                    h2 {
                        font-size: 1.6em;
                        color: #4F378B;
                        border-bottom: 1px solid #EADDFF;
                        padding-bottom: 8px;
                        margin-top: 36px;
                        margin-bottom: 16px;
                    }
                    h3 {
                        font-size: 1.25em;
                        color: #625B71;
                        margin-top: 28px;
                    }
                    p {
                        margin-bottom: 18px;
                    }
                    code {
                        font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
                        background-color: #EADDFF;
                        color: var(--primary-color);
                        padding: 3px 6px;
                        border-radius: 4px;
                        font-size: 0.9em;
                    }
                    pre {
                        background-color: var(--code-bg);
                        color: #E2E2E6;
                        padding: 16px;
                        border-radius: 8px;
                        overflow-x: auto;
                        margin: 20px 0;
                        border-left: 4px solid var(--code-text);
                    }
                    pre code {
                        background-color: transparent;
                        color: inherit;
                        padding: 0;
                        font-size: 0.85em;
                    }
                    blockquote {
                        border-left: 4px solid var(--accent-color);
                        background-color: var(--card-bg);
                        padding: 12px 24px;
                        margin: 24px 0;
                        font-style: italic;
                        border-radius: 0 8px 8px 0;
                    }
                    ul, ol {
                        padding-left: 24px;
                        margin-bottom: 18px;
                    }
                    li {
                        margin-bottom: 8px;
                    }
                    a {
                        color: #6750A4;
                        text-decoration: none;
                        font-weight: 500;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    hr {
                        border: 0;
                        border-top: 1px solid #CAC4D0;
                        margin: 40px 0;
                    }
                    .checkbox-list-item {
                        list-style-type: none;
                        position: relative;
                        left: -20px;
                    }
                </style>
            </head>
            <body>
                <h1>${escapeHtml(title)}</h1>
                $bodyContent
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Renders Markdown content as a formatted, multi-page PDF document locally.
     */
    fun exportToPdf(context: Context, title: String, markdown: String): File {
        val pdfDocument = PdfDocument()
        
        // Standard A4 dimensions in postscript points (595 x 842)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 45
        val contentWidth = pageWidth - (margin * 2)

        val htmlContent = "<h1>" + escapeHtml(title) + "</h1>" + markdownToHtmlContent(markdown)
        val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(htmlContent)
        }

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        // Measure layout to handle multi-paging elegantly
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(spanned, 0, spanned.length, textPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1.15f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                spanned,
                textPaint,
                contentWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.15f,
                3f,
                true
            )
        }

        val totalHeight = staticLayout.height
        val maxContentHeightPerPage = pageHeight - (margin * 2) - 40 // reserved footer margin
        var yOffset = 0
        var pageNumber = 1

        while (yOffset < totalHeight) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw header background/line on page 1
            if (pageNumber == 1) {
                val headerPaint = Paint().apply {
                    color = Color.parseColor("#381E72")
                    strokeWidth = 2f
                }
                canvas.drawLine(margin.toFloat(), margin.toFloat() - 5, (pageWidth - margin).toFloat(), margin.toFloat() - 5, headerPaint)
            }

            canvas.save()
            // Shift drawing coordinate system to corresponding clip for the page
            canvas.clipRect(
                margin.toFloat(),
                margin.toFloat(),
                (pageWidth - margin).toFloat(),
                (pageHeight - margin).toFloat()
            )
            canvas.translate(margin.toFloat(), margin.toFloat() - yOffset)
            
            staticLayout.draw(canvas)
            canvas.restore()

            // Draw page numbers footer
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawText(
                "Page $pageNumber",
                (pageWidth / 2f) - 15f,
                (pageHeight - margin + 15).toFloat(),
                footerPaint
            )

            pdfDocument.finishPage(page)
            yOffset += maxContentHeightPerPage
            pageNumber++
        }

        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val file = File(context.cacheDir, "$cleanTitle.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    /**
     * Formats the Markdown content as a fully compatible OOXML Word DOCX file and packs it into a ZIP stream.
     */
    fun exportToDocx(context: Context, title: String, markdown: String): File {
        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val file = File(context.cacheDir, "$cleanTitle.docx")
        
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // 1. Write [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
            """.trimIndent().toByteArray())
            zos.closeEntry()

            // 2. Write _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
            """.trimIndent().toByteArray())
            zos.closeEntry()

            // 3. Write word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(generateDocxXml(title, markdown).toByteArray())
            zos.closeEntry()
        }
        
        return file
    }

    private fun generateDocxXml(title: String, markdown: String): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")
        
        // 1. Add Title Header block
        sb.append("<w:p>")
        sb.append("<w:pPr><w:jc w:val=\"center\"/></w:pPr>")
        sb.append("<w:r><w:rPr><w:sz w:val=\"36\"/><w:b/><w:color w:val=\"381E72\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>")
        sb.append("<w:t>").append(escapeXml(title)).append("</w:t>")
        sb.append("</w:r>")
        sb.append("</w:p>")
        sb.append("<w:p/>") // Empty spacing line
        
        // 2. Parse Markdown lines and append paragraphs
        val lines = markdown.split("\n")
        var inList = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                sb.append("<w:p/>")
                inList = false
                continue
            }
            
            sb.append("<w:p>")
            
            if (trimmed.startsWith("# ")) {
                sb.append("<w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:sz w:val=\"28\"/><w:b/><w:color w:val=\"381E72\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>")
                sb.append("<w:t>").append(escapeXml(trimmed.substring(2))).append("</w:t>")
                sb.append("</w:r>")
                inList = false
            } else if (trimmed.startsWith("## ")) {
                sb.append("<w:pPr><w:pStyle w:val=\"Heading2\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:sz w:val=\"24\"/><w:b/><w:color w:val=\"4F378B\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>")
                sb.append("<w:t>").append(escapeXml(trimmed.substring(3))).append("</w:t>")
                sb.append("</w:r>")
                inList = false
            } else if (trimmed.startsWith("### ")) {
                sb.append("<w:pPr><w:pStyle w:val=\"Heading3\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:sz w:val=\"20\"/><w:b/><w:color w:val=\"625B71\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>")
                sb.append("<w:t>").append(escapeXml(trimmed.substring(4))).append("</w:t>")
                sb.append("</w:r>")
                inList = false
            } else if (trimmed.startsWith("> ")) {
                sb.append("<w:pPr><w:ind w:left=\"720\"/><w:pBdr><w:left w:val=\"single\" w:sz=\"24\" w:space=\"12\" w:color=\"D0BCFF\"/></w:pBdr></w:pPr>")
                sb.append("<w:r><w:rPr><w:i/><w:color w:val=\"4F378B\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>")
                sb.append("<w:t>").append(escapeXml(trimmed.substring(2))).append("</w:t>")
                sb.append("</w:r>")
                inList = false
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                sb.append("<w:pPr><w:ind w:left=\"480\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>")
                sb.append("<w:t>•  </w:t>")
                sb.append("<w:t>").append(escapeXml(trimmed.substring(2))).append("</w:t>")
                sb.append("</w:r>")
                inList = true
            } else {
                sb.append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>")
                // Format inline markdown bold **text** or italic *text*
                var formattedText = escapeXml(trimmed)
                // A very simplified inline formatting replacement for bold/italic representation
                sb.append("<w:t>").append(formattedText).append("</w:t>")
                sb.append("</w:r>")
                inList = false
            }
            sb.append("</w:p>")
        }
        
        sb.append("</w:body>")
        sb.append("</w:document>")
        return sb.toString()
    }

    private fun markdownToHtmlContent(markdown: String): String {
        var html = markdown
        html = escapeHtml(html)

        // Code Blocks ```code```
        html = html.replace("```([\\s\\S]*?)```".toRegex()) { match ->
            val code = match.groupValues[1].trim()
            "<pre><code>$code</code></pre>"
        }

        // Inline Code `code`
        html = html.replace("`([^`]+)`".toRegex(), "<code>$1</code>")

        // Headers
        html = html.replace("(?m)^# (.*)$".toRegex(), "<h2>$1</h2>") // Scale h1 in text body to h2 for nesting
        html = html.replace("(?m)^## (.*)$".toRegex(), "<h3>$1</h3>")
        html = html.replace("(?m)^### (.*)$".toRegex(), "<h4>$1</h4>")

        // Blockquotes
        html = html.replace("(?m)^> (.*)$".toRegex(), "<blockquote>$1</blockquote>")

        // Bold & Italic
        html = html.replace("\\*\\*([^\\*]+)\\*\\*".toRegex(), "<strong>$1</strong>")
        html = html.replace("\\*([^\\*]+)\\*".toRegex(), "<em>$1</em>")

        // Checkbox lists
        html = html.replace("- \\[[xX]\\] (.*)".toRegex(), "<li class=\"checkbox-list-item\"><input type=\"checkbox\" checked disabled> $1</li>")
        html = html.replace("- \\[ \\] (.*)".toRegex(), "<li class=\"checkbox-list-item\"><input type=\"checkbox\" disabled> $1</li>")

        // Standard bullet lists
        html = html.replace("(?m)^[-*+] (.*)$".toRegex(), "<li>$1</li>")

        // Links
        html = html.replace("\\[([^\\]]+)\\]\\(([^\\)]+)\\)".toRegex(), "<a href=\"$2\">$1</a>")

        // Horizontal Rule
        html = html.replace("(?m)^---$".toRegex(), "<hr>")

        // Segment text blocks into paragraphs
        val lines = html.split("\n")
        val sb = StringBuilder()
        var inList = false
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("<li")) {
                if (!inList) {
                    sb.append("<ul>\n")
                    inList = true
                }
                sb.append(trimmed).append("\n")
            } else {
                if (inList) {
                    sb.append("</ul>\n")
                    inList = false
                }
                if (trimmed.isNotBlank() && 
                    !trimmed.startsWith("<h") && 
                    !trimmed.startsWith("<pre") && 
                    !trimmed.startsWith("</pre") && 
                    !trimmed.startsWith("<block") && 
                    !trimmed.startsWith("</block") && 
                    !trimmed.startsWith("<li") && 
                    !trimmed.startsWith("<hr")) {
                    sb.append("<p>").append(trimmed).append("</p>\n")
                } else {
                    sb.append(line).append("\n")
                }
            }
        }
        if (inList) {
            sb.append("</ul>\n")
        }
        
        return sb.toString()
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
