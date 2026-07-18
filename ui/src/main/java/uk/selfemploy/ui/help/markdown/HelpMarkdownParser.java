package uk.selfemploy.ui.help.markdown;

import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a help markdown document into the view-independent {@link HelpBlock} model. Uses
 * commonmark-java for the markdown itself (with the GFM tables and YAML front-matter extensions);
 * this class only maps the parsed tree onto the app's block model, so it is testable without JavaFX.
 */
public final class HelpMarkdownParser {

    private static final Parser PARSER = Parser.builder()
        .extensions(List.of(TablesExtension.create(), YamlFrontMatterExtension.create()))
        .build();

    /** The metadata from a topic file's YAML front matter. Any field may be null. */
    public record FrontMatter(String id, String title, String category, String hmrcLink, String linkText) {
    }

    /**
     * Parses the document body (ignoring any front matter) into blocks.
     *
     * @param markdown the topic markdown, optionally with a leading front-matter block
     * @return the ordered blocks; never null
     */
    public List<HelpBlock> parseBody(String markdown) {
        Node document = PARSER.parse(markdown == null ? "" : markdown);
        List<HelpBlock> blocks = new ArrayList<>();
        collectBlocks(document, blocks);
        return blocks;
    }

    private void collectBlocks(Node parent, List<HelpBlock> out) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof BlockQuote) {
                collectBlocks(node, out); // render quoted content as ordinary blocks, never drop it
                continue;
            }
            HelpBlock block = toBlock(node);
            if (block != null) {
                out.add(block);
            }
        }
    }

    /**
     * Parses the YAML front matter. Missing keys become null fields; a document with no front matter
     * yields a record of all-null fields.
     *
     * @param markdown the topic markdown
     * @return the front-matter fields
     */
    public FrontMatter parseFrontMatter(String markdown) {
        Document document = (Document) PARSER.parse(markdown == null ? "" : markdown);
        YamlFrontMatterVisitor visitor = new YamlFrontMatterVisitor();
        document.accept(visitor);
        Map<String, List<String>> data = visitor.getData();
        return new FrontMatter(
            first(data, "id"),
            first(data, "title"),
            first(data, "category"),
            first(data, "hmrcLink"),
            first(data, "linkText"));
    }

    private static String first(Map<String, List<String>> data, String key) {
        List<String> values = data.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private HelpBlock toBlock(Node node) {
        if (node instanceof Heading heading) {
            return new HelpBlock.Heading(heading.getLevel(), inlines(heading));
        }
        if (node instanceof Paragraph paragraph) {
            return new HelpBlock.Paragraph(inlines(paragraph));
        }
        if (node instanceof BulletList list) {
            return new HelpBlock.BulletList(listItems(list));
        }
        if (node instanceof OrderedList list) {
            Integer start = list.getMarkerStartNumber();
            return new HelpBlock.OrderedList(start != null ? start : 1, listItems(list));
        }
        if (node instanceof TableBlock table) {
            return toTable(table);
        }
        if (node instanceof ThematicBreak) {
            return new HelpBlock.Rule();
        }
        return null; // unsupported block types are skipped rather than rendered raw
    }

    private List<List<HelpBlock.Inline>> listItems(ListBlock list) {
        List<List<HelpBlock.Inline>> items = new ArrayList<>();
        for (Node item = list.getFirstChild(); item != null; item = item.getNext()) {
            if (item instanceof ListItem) {
                items.add(inlines(item));
            }
        }
        return items;
    }

    private HelpBlock toTable(TableBlock table) {
        List<List<HelpBlock.Inline>> header = new ArrayList<>();
        List<List<List<HelpBlock.Inline>>> rows = new ArrayList<>();
        for (Node section = table.getFirstChild(); section != null; section = section.getNext()) {
            if (section instanceof TableHead) {
                TableRow row = firstRow(section);
                if (row != null) {
                    header.addAll(cells(row));
                }
            } else if (section instanceof TableBody) {
                for (Node r = section.getFirstChild(); r != null; r = r.getNext()) {
                    if (r instanceof TableRow tableRow) {
                        rows.add(cells(tableRow));
                    }
                }
            }
        }
        return new HelpBlock.Table(header, rows);
    }

    private TableRow firstRow(Node section) {
        for (Node r = section.getFirstChild(); r != null; r = r.getNext()) {
            if (r instanceof TableRow row) {
                return row;
            }
        }
        return null;
    }

    private List<List<HelpBlock.Inline>> cells(TableRow row) {
        List<List<HelpBlock.Inline>> cells = new ArrayList<>();
        for (Node cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
            if (cell instanceof TableCell) {
                cells.add(inlines(cell));
            }
        }
        return cells;
    }

    /** Collects the inline runs under a block node (paragraph, heading, list item or table cell). */
    private List<HelpBlock.Inline> inlines(Node parent) {
        List<HelpBlock.Inline> runs = new ArrayList<>();
        collectInlines(parent, false, runs);
        return runs;
    }

    private void collectInlines(Node parent, boolean bold, List<HelpBlock.Inline> out) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof Text text) {
                out.add(new HelpBlock.Inline.TextRun(text.getLiteral(), bold));
            } else if (node instanceof StrongEmphasis) {
                collectInlines(node, true, out);
            } else if (node instanceof Emphasis) {
                collectInlines(node, bold, out); // no italic in the model — render as the current weight
            } else if (node instanceof Link link) {
                out.add(new HelpBlock.Inline.Link(linkText(link), link.getDestination()));
            } else if (node instanceof Code code) {
                out.add(new HelpBlock.Inline.TextRun(code.getLiteral(), bold)); // inline `code` as text
            } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
                out.add(new HelpBlock.Inline.TextRun(" ", bold));
            } else if (node.getFirstChild() != null) {
                collectInlines(node, bold, out); // descend through paragraphs inside list items, etc.
            }
        }
    }

    private String linkText(Link link) {
        StringBuilder sb = new StringBuilder();
        appendText(link, sb);
        return sb.toString();
    }

    /** Collects visible text from a link label, descending through emphasis/strong/code wrappers. */
    private void appendText(Node parent, StringBuilder sb) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof Text text) {
                sb.append(text.getLiteral());
            } else if (node instanceof Code code) {
                sb.append(code.getLiteral());
            } else {
                appendText(node, sb);
            }
        }
    }
}
