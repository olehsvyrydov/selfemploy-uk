package uk.selfemploy.ui.help.markdown;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import uk.selfemploy.ui.help.markdown.HelpBlock.Inline;
import uk.selfemploy.ui.util.BrowserUtil;

import java.util.List;

/**
 * Renders the {@link HelpBlock} model (from {@link HelpMarkdownParser}) into JavaFX nodes: headings
 * as styled labels, paragraphs and list items as {@link TextFlow}s with bold runs and clickable
 * links, bullet/numbered lists as hang-indented rows (so bullets never run together), and tables as a
 * two-column grid. Styling is carried by {@code help-md-*} classes in help.css.
 */
public final class HelpMarkdownRenderer {

    private final HelpMarkdownParser parser = new HelpMarkdownParser();

    /**
     * Parses and renders help markdown into a single container node.
     *
     * @param markdown the topic body markdown (front matter, if present, is ignored)
     * @return a VBox of rendered blocks
     */
    public VBox render(String markdown) {
        VBox container = new VBox(12);
        container.getStyleClass().add("help-md-body");
        for (HelpBlock block : parser.parseBody(markdown)) {
            Region node = renderBlock(block);
            if (node != null) {
                container.getChildren().add(node);
            }
        }
        return container;
    }

    private Region renderBlock(HelpBlock block) {
        return switch (block) {
            case HelpBlock.Heading h -> heading(h);
            case HelpBlock.Paragraph p -> paragraph(p.content());
            case HelpBlock.BulletList list -> list(list.items(), null);
            case HelpBlock.OrderedList list -> list(list.items(), list.start());
            case HelpBlock.Table t -> table(t);
            case HelpBlock.Rule ignored -> {
                Region rule = new Region();
                rule.getStyleClass().add("help-md-rule");
                rule.setMinHeight(1);
                yield rule;
            }
        };
    }

    private Label heading(HelpBlock.Heading h) {
        Label label = new Label(plainText(h.content()));
        label.setWrapText(true);
        label.getStyleClass().addAll("help-md-heading", "help-md-heading-" + Math.min(h.level(), 6));
        return label;
    }

    private TextFlow paragraph(List<Inline> content) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("help-md-paragraph");
        addInlines(flow, content);
        return flow;
    }

    private VBox list(List<List<Inline>> items, Integer startNumber) {
        VBox box = new VBox(6);
        box.getStyleClass().add("help-md-list");
        int number = startNumber == null ? 0 : startNumber;
        for (List<Inline> item : items) {
            Label gutter = new Label(startNumber == null ? "•" : (number++ + "."));
            gutter.getStyleClass().add("help-md-list-marker");
            gutter.setMinWidth(startNumber == null ? 16 : 24);

            TextFlow flow = new TextFlow();
            flow.getStyleClass().add("help-md-list-text");
            addInlines(flow, item);
            HBox.setHgrow(flow, Priority.ALWAYS);

            HBox row = new HBox(6, gutter, flow);
            row.setAlignment(Pos.TOP_LEFT); // hang-indent: marker stays at the top of a wrapped item
            box.getChildren().add(row);
        }
        return box;
    }

    private GridPane table(HelpBlock.Table table) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("help-md-table");
        int columns = Math.max(table.header().size(),
            table.rows().stream().mapToInt(List::size).max().orElse(0));
        for (int c = 0; c < columns; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        int row = 0;
        for (int c = 0; c < table.header().size(); c++) {
            grid.add(cell(table.header().get(c), true), c, row);
        }
        row++;
        for (List<List<Inline>> bodyRow : table.rows()) {
            for (int c = 0; c < bodyRow.size(); c++) {
                grid.add(cell(bodyRow.get(c), false), c, row);
            }
            row++;
        }
        return grid;
    }

    private TextFlow cell(List<Inline> content, boolean header) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add(header ? "help-md-th" : "help-md-td");
        GridPane.setValignment(flow, VPos.TOP);
        addInlines(flow, content);
        return flow;
    }

    private void addInlines(TextFlow flow, List<Inline> content) {
        for (Inline run : content) {
            switch (run) {
                case Inline.TextRun t -> {
                    Text text = new Text(t.text());
                    text.getStyleClass().add(t.bold() ? "help-md-bold" : "help-md-text");
                    flow.getChildren().add(text);
                }
                case Inline.Link l -> {
                    Hyperlink link = new Hyperlink(l.text());
                    link.getStyleClass().add("help-md-link");
                    link.setOnAction(e -> BrowserUtil.openUrl(l.url()));
                    flow.getChildren().add(link);
                }
            }
        }
    }

    private String plainText(List<Inline> content) {
        StringBuilder sb = new StringBuilder();
        for (Inline run : content) {
            switch (run) {
                case Inline.TextRun t -> sb.append(t.text());
                case Inline.Link l -> sb.append(l.text());
            }
        }
        return sb.toString();
    }
}
