package uk.selfemploy.ui.e2e;

import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Screenshot capture utility for SE-1001: Capture Real App Screenshots.
 *
 * <p>This utility test captures high-quality screenshots of all main application views
 * for documentation and marketing purposes.</p>
 *
 * <h3>Usage:</h3>
 * <pre>
 * mvn test -pl ui -Dtest=ScreenshotCaptureUtilityTest -DexcludedGroups=none -Dgroups=screenshot
 * </pre>
 *
 * <h3>Requirements:</h3>
 * <ul>
 *   <li>Requires a graphical display environment (not headless)</li>
 *   <li>Window size is set to 1280x720 for consistent screenshots</li>
 *   <li>Screenshots are saved to docs/screenshots/</li>
 * </ul>
 *
 * <h3>Captured Views:</h3>
 * <ol>
 *   <li>Dashboard - Main dashboard with summary cards</li>
 *   <li>Income - Income list view</li>
 *   <li>Expenses - Expense tracking table</li>
 *   <li>Tax Summary - Tax calculation breakdown</li>
 *   <li>HMRC Submission - Annual submission view</li>
 *   <li>Help - Help and support view</li>
 * </ol>
 *
 * @see docs/screenshots/CAPTURE-GUIDE.md
 */
@Tag("screenshot")
@DisplayName("SE-1001: Screenshot Capture Utility")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScreenshotCaptureUtilityTest extends BaseE2ETest {

    private static final String SCREENSHOTS_DIR = "docs/screenshots";
    private static final int SCREENSHOT_WIDTH = 1280;
    private static final int SCREENSHOT_HEIGHT = 720;

    private Path screenshotsPath;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        // Set window size for consistent screenshots
        stage.setWidth(SCREENSHOT_WIDTH);
        stage.setHeight(SCREENSHOT_HEIGHT);
        stage.centerOnScreen();

        WaitForAsyncUtils.waitForFxEvents();
    }

    @BeforeEach
    void setupScreenshotsDirectory() throws IOException {
        // Create screenshots directory relative to project root
        screenshotsPath = Paths.get(System.getProperty("user.dir"))
                .getParent() // Go up from ui module to project root
                .resolve(SCREENSHOTS_DIR);

        // If we're already at project root (running from there)
        if (!Files.exists(screenshotsPath.getParent())) {
            screenshotsPath = Paths.get(System.getProperty("user.dir"))
                    .resolve(SCREENSHOTS_DIR);
        }

        Files.createDirectories(screenshotsPath);
    }

    @Test
    @Order(1)
    @DisplayName("01 - Capture Dashboard Screenshot")
    void captureDashboardScreenshot() throws IOException {
        // Navigate to Dashboard (default view)
        if (!lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()) {
            clickOn("#navDashboard");
            waitForFxEvents();
        }

        // Verify Dashboard is displayed
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Dashboard");

        // Wait for any animations to complete
        shortSleep();
        shortSleep();

        // Capture screenshot
        captureScreenshot("01-dashboard.png");
    }

    @Test
    @Order(2)
    @DisplayName("02 - Capture Income List Screenshot")
    void captureIncomeListScreenshot() throws IOException {
        // Navigate to Income
        clickOn("#navIncome");
        waitForFxEvents();

        // Verify Income view is displayed
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Income");

        // Wait for any animations to complete
        shortSleep();
        shortSleep();

        // Capture screenshot
        captureScreenshot("02-income.png");
    }

    @Test
    @Order(3)
    @DisplayName("03 - Capture Expenses List Screenshot")
    void captureExpensesListScreenshot() throws IOException {
        // Navigate to Expenses
        clickOn("#navExpenses");
        waitForFxEvents();

        // Verify Expenses view is displayed
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Expenses");

        // Wait for any animations to complete
        shortSleep();
        shortSleep();

        // Capture screenshot
        captureScreenshot("03-expenses.png");
    }

    @Test
    @Order(4)
    @DisplayName("04 - Capture Tax Summary Screenshot")
    void captureTaxSummaryScreenshot() throws IOException {
        // Navigate to Tax Summary
        clickOn("#navTax");
        waitForFxEvents();

        // Verify Tax Summary view is displayed
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Tax Summary");

        // Wait for any animations to complete
        shortSleep();
        shortSleep();

        // Capture screenshot
        captureScreenshot("04-tax-summary.png");
    }

    @Test
    @Order(5)
    @DisplayName("05 - Capture HMRC Submission Screenshot")
    void captureHmrcSubmissionScreenshot() throws IOException {
        // Navigate to HMRC Submission
        clickOn("#navHmrc");
        waitForFxEvents();

        // Verify HMRC Submission view is displayed
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("HMRC Submission");

        // Wait for any animations to complete
        shortSleep();
        shortSleep();

        // Capture screenshot
        captureScreenshot("05-annual-submission.png");
    }

    @Test
    @Order(6)
    @DisplayName("06 - Capture Help & Support Screenshot")
    void captureHelpScreenshot() throws IOException {
        // Navigate to Help
        clickOn("#helpButton");
        waitForFxEvents();

        // Verify Help view is displayed
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Help & Support");

        // Wait for any animations to complete
        shortSleep();
        shortSleep();

        // Capture screenshot
        captureScreenshot("06-help.png");
    }

    @Test
    @Order(7)
    @DisplayName("07 - Capture Settings Screenshot")
    void captureSettingsScreenshot() throws IOException {
        // Navigate to Settings
        clickOn("#settingsButton");
        waitForFxEvents();

        // Verify Settings view is displayed
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Settings");

        // Wait for any animations to complete
        shortSleep();
        shortSleep();

        // Capture screenshot
        captureScreenshot("07-settings.png");
    }

    /**
     * Captures a screenshot of the current application window and saves it as a PNG file.
     * Uses pure JavaFX without requiring javafx.swing module.
     *
     * @param filename the filename for the screenshot (e.g., "01-dashboard.png")
     * @throws IOException if the screenshot cannot be saved
     */
    private void captureScreenshot(String filename) throws IOException {
        Scene scene = getPrimaryStage().getScene();
        int width = (int) scene.getWidth();
        int height = (int) scene.getHeight();

        // Create a writable image with the scene dimensions
        WritableImage image = new WritableImage(width, height);

        // Take snapshot on FX thread
        interact(() -> {
            scene.getRoot().snapshot(new SnapshotParameters(), image);
        });

        // Save as PNG using pure Java (no javafx.swing required)
        File outputFile = screenshotsPath.resolve(filename).toFile();
        writePng(image, outputFile);

        assertThat(outputFile)
                .as("Screenshot file should exist: " + filename)
                .exists();

        assertThat(outputFile.length())
                .as("Screenshot file should not be empty: " + filename)
                .isGreaterThan(0);

        System.out.println("Screenshot saved: " + outputFile.getAbsolutePath());
    }

    /**
     * Writes a JavaFX WritableImage to a PNG file without requiring javafx.swing.
     * This is a minimal PNG encoder that handles RGBA images.
     */
    private void writePng(WritableImage image, File outputFile) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             DataOutputStream dos = new DataOutputStream(fos)) {

            // PNG signature
            dos.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

            // IHDR chunk
            writeChunk(dos, "IHDR", createIhdrData(width, height));

            // IDAT chunk (compressed image data)
            byte[] rawImageData = createRawImageData(reader, width, height);
            byte[] compressedData = compress(rawImageData);
            writeChunk(dos, "IDAT", compressedData);

            // IEND chunk
            writeChunk(dos, "IEND", new byte[0]);
        }
    }

    private byte[] createIhdrData(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(13);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.put((byte) 8);  // Bit depth
        buffer.put((byte) 6);  // Color type (RGBA)
        buffer.put((byte) 0);  // Compression method
        buffer.put((byte) 0);  // Filter method
        buffer.put((byte) 0);  // Interlace method
        return buffer.array();
    }

    private byte[] createRawImageData(PixelReader reader, int width, int height) {
        // Each row starts with filter byte (0 = no filter), then RGBA pixels
        byte[] data = new byte[height * (1 + width * 4)];
        int index = 0;

        for (int y = 0; y < height; y++) {
            data[index++] = 0; // Filter type: None
            for (int x = 0; x < width; x++) {
                int argb = reader.getArgb(x, y);
                data[index++] = (byte) ((argb >> 16) & 0xFF); // R
                data[index++] = (byte) ((argb >> 8) & 0xFF);  // G
                data[index++] = (byte) (argb & 0xFF);         // B
                data[index++] = (byte) ((argb >> 24) & 0xFF); // A
            }
        }
        return data;
    }

    private byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[data.length + 1024];
        int compressedLength = deflater.deflate(buffer);
        deflater.end();

        byte[] result = new byte[compressedLength];
        System.arraycopy(buffer, 0, result, 0, compressedLength);
        return result;
    }

    private void writeChunk(DataOutputStream dos, String type, byte[] data) throws IOException {
        dos.writeInt(data.length);
        byte[] typeBytes = type.getBytes("US-ASCII");
        dos.write(typeBytes);
        dos.write(data);

        // CRC32 of type + data
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        dos.writeInt((int) crc.getValue());
    }
}
