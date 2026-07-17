package uk.selfemploy.ui.help.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.help.markdown.HelpBlock.BulletList;
import uk.selfemploy.ui.help.markdown.HelpBlock.Heading;
import uk.selfemploy.ui.help.markdown.HelpBlock.Inline;
import uk.selfemploy.ui.help.markdown.HelpBlock.OrderedList;
import uk.selfemploy.ui.help.markdown.HelpBlock.Paragraph;
import uk.selfemploy.ui.help.markdown.HelpBlock.Rule;
import uk.selfemploy.ui.help.markdown.HelpBlock.Table;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HelpMarkdownParser")
class HelpMarkdownParserTest {

    private final HelpMarkdownParser parser = new HelpMarkdownParser();

    private String text(List<Inline> runs) {
        StringBuilder sb = new StringBuilder();
        for (Inline run : runs) {
            if (run instanceof Inline.TextRun t) {
                sb.append(t.text());
            } else if (run instanceof Inline.Link l) {
                sb.append(l.text());
            }
        }
        return sb.toString();
    }

    @Test
    @DisplayName("a heading becomes a Heading block with its level and text")
    void heading() {
        List<HelpBlock> blocks = parser.parseBody("## Getting started");

        assertThat(blocks).singleElement()
            .isInstanceOfSatisfying(Heading.class, h -> {
                assertThat(h.level()).isEqualTo(2);
                assertThat(text(h.content())).isEqualTo("Getting started");
            });
    }

    @Test
    @DisplayName("bold spans become bold text runs, leaving the rest plain")
    void boldRuns() {
        List<HelpBlock> blocks = parser.parseBody("Your **NINO** is required.");

        Paragraph p = (Paragraph) blocks.get(0);
        assertThat(p.content()).anySatisfy(run ->
            assertThat(run).isInstanceOfSatisfying(Inline.TextRun.class, t -> {
                if (t.text().equals("NINO")) {
                    assertThat(t.bold()).isTrue();
                }
            }));
        assertThat(text(p.content())).isEqualTo("Your NINO is required.");
    }

    @Test
    @DisplayName("each bullet is its own list item — not run together (fixes M17)")
    void bulletsAreSeparateRows() {
        List<HelpBlock> blocks = parser.parseBody("""
            - First point
            - Second point
            - Third point
            """);

        assertThat(blocks).singleElement()
            .isInstanceOfSatisfying(BulletList.class, list -> {
                assertThat(list.items()).hasSize(3);
                assertThat(text(list.items().get(0))).isEqualTo("First point");
                assertThat(text(list.items().get(1))).isEqualTo("Second point");
                assertThat(text(list.items().get(2))).isEqualTo("Third point");
            });
    }

    @Test
    @DisplayName("ordered lists keep their items separate")
    void orderedList() {
        List<HelpBlock> blocks = parser.parseBody("""
            1. Register
            2. Configure
            """);

        assertThat(blocks).singleElement()
            .isInstanceOfSatisfying(OrderedList.class, list ->
                assertThat(list.items()).hasSize(2));
    }

    @Test
    @DisplayName("a link becomes a Link run carrying text and destination")
    void link() {
        List<HelpBlock> blocks = parser.parseBody("See [gov.uk](https://www.gov.uk/mtd) for details.");

        Paragraph p = (Paragraph) blocks.get(0);
        assertThat(p.content()).anySatisfy(run ->
            assertThat(run).isInstanceOfSatisfying(Inline.Link.class, l -> {
                assertThat(l.text()).isEqualTo("gov.uk");
                assertThat(l.url()).isEqualTo("https://www.gov.uk/mtd");
            }));
    }

    @Test
    @DisplayName("a GFM table becomes a Table block with header and rows")
    void table() {
        List<HelpBlock> blocks = parser.parseBody("""
            | Date | Requirement |
            |------|-------------|
            | April 2026 | Income over 50k |
            | April 2027 | Income over 30k |
            """);

        assertThat(blocks).singleElement()
            .isInstanceOfSatisfying(Table.class, t -> {
                assertThat(text(t.header().get(0))).isEqualTo("Date");
                assertThat(text(t.header().get(1))).isEqualTo("Requirement");
                assertThat(t.rows()).hasSize(2);
                assertThat(text(t.rows().get(0).get(0))).isEqualTo("April 2026");
            });
    }

    @Test
    @DisplayName("a thematic break becomes a Rule")
    void rule() {
        List<HelpBlock> blocks = parser.parseBody("Above\n\n---\n\nBelow");
        assertThat(blocks).anySatisfy(b -> assertThat(b).isInstanceOf(Rule.class));
    }

    @Test
    @DisplayName("front matter is parsed into id / title / category / link fields")
    void frontMatter() {
        String md = """
            ---
            id: HMRC_CONNECTION
            title: Connecting to HMRC
            category: HMRC
            hmrcLink: https://www.gov.uk/guidance/mtd
            linkText: Read the HMRC guidance
            ---
            Body paragraph.
            """;

        HelpMarkdownParser.FrontMatter fm = parser.parseFrontMatter(md);

        assertThat(fm.id()).isEqualTo("HMRC_CONNECTION");
        assertThat(fm.title()).isEqualTo("Connecting to HMRC");
        assertThat(fm.category()).isEqualTo("HMRC");
        assertThat(fm.hmrcLink()).isEqualTo("https://www.gov.uk/guidance/mtd");
        assertThat(fm.linkText()).isEqualTo("Read the HMRC guidance");
        // The body parses without the front matter leaking in.
        assertThat(parser.parseBody(md)).noneMatch(b ->
            b instanceof Heading h && text(h.content()).contains("id:"));
    }
}
