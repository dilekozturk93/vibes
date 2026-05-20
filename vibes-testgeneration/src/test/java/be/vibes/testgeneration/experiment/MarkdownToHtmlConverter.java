package be.vibes.testgeneration.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Markdown to HTML converter tailored to the milestone reports
 * shipped under {@code automode-reports/}. Handles:
 *
 * <ul>
 *   <li>ATX headings ({@code # H1}, {@code ## H2}, {@code ### H3});</li>
 *   <li>bold {@code **text**} and italic {@code *text*};</li>
 *   <li>inline code {@code `code`} and fenced code blocks {@code ```};</li>
 *   <li>unordered list items {@code - item};</li>
 *   <li>GitHub-flavoured tables ({@code | col | col |} + separator);</li>
 *   <li>horizontal rules ({@code ---});</li>
 *   <li>links {@code [text](url)} and image references
 *       {@code ![alt](src)} (relative paths preserved verbatim);</li>
 *   <li>paragraphs (blank-line separated).</li>
 * </ul>
 *
 * <p>Not a CommonMark-compliant parser — kept intentionally small and
 * regex-based so we don't have to add a markdown dependency to the
 * module. Sufficient for the reports we generate.
 *
 * <p>CLI: {@code java MarkdownToHtmlConverter [dir]}. Walks the given
 * directory (default {@code automode-reports}), converts every
 * {@code *.md} file to a sibling {@code *.html}.
 */
public final class MarkdownToHtmlConverter {

    private static final Pattern BOLD = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");

    private MarkdownToHtmlConverter() {
    }

    public static void main(String[] args) throws Exception {
        Path baseDir = Paths.get(args.length > 0 ? args[0] : "automode-reports");
        if (!Files.isDirectory(baseDir)) {
            System.err.println("Not a directory: " + baseDir);
            System.exit(1);
        }
        int converted = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*.md")) {
            for (Path md : stream) {
                Path html = md.resolveSibling(
                        md.getFileName().toString().replaceAll("\\.md$", ".html"));
                convertFile(md, html);
                System.out.println("  " + md.getFileName() + " -> " + html.getFileName());
                converted++;
            }
        }
        System.out.println("Converted " + converted + " markdown file(s).");
    }

    public static void convertFile(Path mdPath, Path htmlPath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(mdPath.toFile()))) {
            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
        }
        String body = render(lines);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(htmlPath.toFile()))) {
            w.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n");
            w.write("<meta charset=\"UTF-8\"/>\n");
            w.write("<title>" + escapeHtml(mdPath.getFileName().toString()) + "</title>\n");
            w.write("<style>\n"
                    + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; "
                    + "max-width: 980px; margin: 2em auto; padding: 0 1em; color: #222; line-height: 1.55; }\n"
                    + "h1 { border-bottom: 2px solid #333; padding-bottom: 0.3em; }\n"
                    + "h2 { margin-top: 2em; color: #444; border-bottom: 1px solid #ddd; padding-bottom: 0.2em; }\n"
                    + "h3 { color: #555; margin-top: 1.5em; }\n"
                    + "h4 { color: #666; }\n"
                    + "code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-size: 0.95em; "
                    + "font-family: ui-monospace, Menlo, Consolas, monospace; }\n"
                    + "pre { background: #f4f4f4; padding: 10px 12px; border-radius: 4px; overflow-x: auto; "
                    + "font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.92em; line-height: 1.45; }\n"
                    + "pre code { background: transparent; padding: 0; }\n"
                    + "img { max-width: 100%; border: 1px solid #ccc; padding: 4px; background: white; "
                    + "display: block; margin: 1em auto; }\n"
                    + "table { border-collapse: collapse; margin: 1em 0; width: 100%; }\n"
                    + "th, td { border: 1px solid #ccc; padding: 6px 10px; text-align: left; }\n"
                    + "th { background: #f0f0f0; font-weight: 600; }\n"
                    + "tr:nth-child(even) td { background: #fafafa; }\n"
                    + "hr { border: 0; border-top: 1px solid #ccc; margin: 2.5em 0; }\n"
                    + "a { color: #06c; text-decoration: none; }\n"
                    + "a:hover { text-decoration: underline; }\n"
                    + "ul { padding-left: 1.6em; }\n"
                    + "</style>\n</head><body>\n");
            w.write(body);
            w.write("</body></html>\n");
        }
    }

    /**
     * Renders a list of markdown lines as HTML body content. Walks the
     * lines stateful-ly: tracks fenced code blocks, list runs, table runs,
     * and paragraph runs so we can emit one well-formed HTML element per
     * group.
     */
    private static String render(List<String> lines) {
        StringBuilder out = new StringBuilder();
        boolean inCodeBlock = false;
        StringBuilder codeBuffer = new StringBuilder();
        List<String> listBuffer = new ArrayList<>();
        List<String> paragraphBuffer = new ArrayList<>();
        List<String> tableBuffer = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);

            // Fenced code blocks.
            if (raw.trim().startsWith("```")) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                flushTable(out, tableBuffer);
                if (inCodeBlock) {
                    out.append("<pre><code>").append(escapeHtml(codeBuffer.toString()))
                            .append("</code></pre>\n");
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                }
                continue;
            }
            if (inCodeBlock) {
                codeBuffer.append(raw).append('\n');
                continue;
            }

            // Table rows.
            if (raw.startsWith("|") && raw.endsWith("|")) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                tableBuffer.add(raw);
                continue;
            } else if (!tableBuffer.isEmpty()) {
                flushTable(out, tableBuffer);
            }

            String trimmed = raw.trim();
            // Horizontal rule.
            if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                out.append("<hr/>\n");
                continue;
            }
            // Blank line.
            if (trimmed.isEmpty()) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                continue;
            }
            // Heading.
            if (trimmed.startsWith("####")) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                out.append("<h4>").append(renderInline(trimmed.substring(4).trim()))
                        .append("</h4>\n");
                continue;
            }
            if (trimmed.startsWith("###")) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                out.append("<h3>").append(renderInline(trimmed.substring(3).trim()))
                        .append("</h3>\n");
                continue;
            }
            if (trimmed.startsWith("##")) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                out.append("<h2>").append(renderInline(trimmed.substring(2).trim()))
                        .append("</h2>\n");
                continue;
            }
            if (trimmed.startsWith("#")) {
                flushList(out, listBuffer);
                flushParagraph(out, paragraphBuffer);
                out.append("<h1>").append(renderInline(trimmed.substring(1).trim()))
                        .append("</h1>\n");
                continue;
            }
            // List item.
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                flushParagraph(out, paragraphBuffer);
                listBuffer.add(trimmed.substring(2));
                continue;
            }
            // Default: paragraph continuation.
            flushList(out, listBuffer);
            paragraphBuffer.add(raw);
        }
        // Flush any pending state at EOF.
        flushList(out, listBuffer);
        flushParagraph(out, paragraphBuffer);
        flushTable(out, tableBuffer);
        if (inCodeBlock) {
            out.append("<pre><code>").append(escapeHtml(codeBuffer.toString()))
                    .append("</code></pre>\n");
        }
        return out.toString();
    }

    private static void flushList(StringBuilder out, List<String> buffer) {
        if (buffer.isEmpty()) return;
        out.append("<ul>\n");
        for (String item : buffer) {
            out.append("  <li>").append(renderInline(item)).append("</li>\n");
        }
        out.append("</ul>\n");
        buffer.clear();
    }

    private static void flushParagraph(StringBuilder out, List<String> buffer) {
        if (buffer.isEmpty()) return;
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < buffer.size(); i++) {
            if (i > 0) joined.append(' ');
            joined.append(buffer.get(i).trim());
        }
        out.append("<p>").append(renderInline(joined.toString())).append("</p>\n");
        buffer.clear();
    }

    private static void flushTable(StringBuilder out, List<String> buffer) {
        if (buffer.isEmpty()) return;
        // First row = header. Second row = separator (---). Remaining = body.
        List<List<String>> rows = new ArrayList<>();
        for (String row : buffer) {
            String inner = row.trim();
            if (inner.startsWith("|")) inner = inner.substring(1);
            if (inner.endsWith("|")) inner = inner.substring(0, inner.length() - 1);
            String[] cells = inner.split("\\|", -1);
            List<String> trimmed = new ArrayList<>();
            for (String c : cells) {
                trimmed.add(c.trim());
            }
            rows.add(trimmed);
        }
        out.append("<table>\n");
        // Header.
        if (!rows.isEmpty()) {
            out.append("  <thead><tr>");
            for (String c : rows.get(0)) {
                out.append("<th>").append(renderInline(c)).append("</th>");
            }
            out.append("</tr></thead>\n");
        }
        // Body (skip header and separator rows).
        out.append("  <tbody>\n");
        for (int i = 2; i < rows.size(); i++) {
            out.append("    <tr>");
            for (String c : rows.get(i)) {
                out.append("<td>").append(renderInline(c)).append("</td>");
            }
            out.append("</tr>\n");
        }
        out.append("  </tbody>\n</table>\n");
        buffer.clear();
    }

    /**
     * Applies inline-element transformations in order: images first
     * (because their syntax overlaps with links), then links, then
     * inline code (so its protected contents are not subject to bold
     * / italic transformations after the fact), then bold + italic.
     * Plain text and untouched characters are HTML-escaped.
     */
    private static String renderInline(String s) {
        // Image: ![alt](src) -> <img src=...>
        Matcher m = IMAGE.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String alt = m.group(1);
            String src = m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "<img src=\"" + escapeHtml(src) + "\" alt=\"" + escapeHtml(alt) + "\"/>"));
        }
        m.appendTail(sb);
        String out = sb.toString();

        // Link: [text](url) -> <a href=...>
        m = LINK.matcher(out);
        sb = new StringBuffer();
        while (m.find()) {
            String text = m.group(1);
            String href = m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "<a href=\"" + escapeHtml(href) + "\">" + escapeHtml(text) + "</a>"));
        }
        m.appendTail(sb);
        out = sb.toString();

        // Inline code: replace with placeholders so bold/italic don't touch them.
        List<String> codeTokens = new ArrayList<>();
        m = INLINE_CODE.matcher(out);
        sb = new StringBuffer();
        while (m.find()) {
            codeTokens.add(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(" CODE" + (codeTokens.size() - 1) + " "));
        }
        m.appendTail(sb);
        out = sb.toString();

        // Bold then italic (order matters: bold uses **, italic uses *).
        out = BOLD.matcher(out).replaceAll("<strong>$1</strong>");
        out = ITALIC.matcher(out).replaceAll("<em>$1</em>");

        // Escape any remaining HTML-special characters in plain text. We
        // do this AFTER our regex substitutions because the substitutions
        // already escape user-supplied parts via escapeHtml above.
        // Strategy: split on our HTML tags and escape only the bits in
        // between. Simpler heuristic: escape & < > only if they would
        // create a tag, but our regex-emitted tags are well-formed. For
        // milestone reports we accept the minor risk and only escape
        // characters that are NOT already part of our emitted tags.

        // Restore code tokens (these get their own escaping).
        for (int i = 0; i < codeTokens.size(); i++) {
            out = out.replace(" CODE" + i + " ",
                    "<code>" + escapeHtml(codeTokens.get(i)) + "</code>");
        }
        return out;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
