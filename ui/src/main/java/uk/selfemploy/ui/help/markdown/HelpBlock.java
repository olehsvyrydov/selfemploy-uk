package uk.selfemploy.ui.help.markdown;

import java.util.List;

/**
 * The structured, view-independent representation of a rendered help document: an ordered list of
 * blocks, each carrying inline runs. {@link HelpMarkdownParser} produces this from markdown; a thin
 * JavaFX builder turns it into nodes. Keeping the structure separate from JavaFX lets the parsing be
 * unit-tested without the FX toolkit.
 */
public sealed interface HelpBlock
        permits HelpBlock.Heading, HelpBlock.Paragraph, HelpBlock.BulletList,
                HelpBlock.OrderedList, HelpBlock.Table, HelpBlock.Rule {

    /** A section heading; {@code level} is 1-6. */
    record Heading(int level, List<Inline> content) implements HelpBlock {
    }

    /** A paragraph of inline content. */
    record Paragraph(List<Inline> content) implements HelpBlock {
    }

    /** An unordered list; each item is its own inline content, rendered as a hang-indented row. */
    record BulletList(List<List<Inline>> items) implements HelpBlock {
    }

    /** An ordered list; each item is its own inline content. Numbering starts at 1. */
    record OrderedList(List<List<Inline>> items) implements HelpBlock {
    }

    /** A simple table: a header row and body rows, each cell being inline content. */
    record Table(List<List<Inline>> header, List<List<List<Inline>>> rows) implements HelpBlock {
    }

    /** A horizontal rule / thematic break. */
    record Rule() implements HelpBlock {
    }

    /** A run of inline content: either plain/bold text or a link. */
    sealed interface Inline permits Inline.TextRun, Inline.Link {

        /** A run of text, optionally bold. */
        record TextRun(String text, boolean bold) implements Inline {
        }

        /** A hyperlink with its visible text and destination. */
        record Link(String text, String url) implements Inline {
        }
    }
}
