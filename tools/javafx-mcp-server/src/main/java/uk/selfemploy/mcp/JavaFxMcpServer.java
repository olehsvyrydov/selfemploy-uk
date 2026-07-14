package uk.selfemploy.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * MCP Server for automating JavaFX desktop applications.
 *
 * Provides tools for:
 * - Taking screenshots (returned as MCP image content so the model can see them)
 * - Listing application windows
 * - Clicking at coordinates
 * - Typing text
 * - Scrolling
 * - Getting screen dimensions
 *
 * Wayland note: java.awt.Robot screen capture is unreliable in Wayland sessions
 * (the JDK routes it through the desktop portal, which blocks on a permission
 * dialog; the plain X11 path only sees a black root window). JavaFX apps run as
 * XWayland clients, so on Wayland this server captures the target application
 * window directly via `xwd` instead. Robot input (XTest) reaches XWayland
 * windows, so clicks and typing work either way.
 */
public class JavaFxMcpServer {

    private static final Gson GSON = new Gson();  // No pretty-printing - MCP requires single-line JSON (NDJSON)
    private static final int MAX_IMAGE_WIDTH = 1568;
    private static final long EXEC_TIMEOUT_MS = 10_000;
    private static Robot robot;
    private static PrintWriter out;

    public static void main(String[] args) {
        try {
            robot = new Robot();
            robot.setAutoDelay(50); // Small delay between actions

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            out = new PrintWriter(System.out, true);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    JsonObject request = JsonParser.parseString(line).getAsJsonObject();
                    JsonObject response = handleRequest(request);
                    // Only send response if it's not a notification (response is not null)
                    if (response != null) {
                        out.println(GSON.toJson(response));
                        out.flush();
                    }
                } catch (Exception e) {
                    sendError(JsonNull.INSTANCE, "Parse error: " + e.getMessage());
                }
            }
        } catch (AWTException e) {
            System.err.println("Failed to create Robot: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static JsonObject handleRequest(JsonObject request) {
        String method = request.get("method").getAsString();

        // Handle notifications (no id, no response expected)
        if (!request.has("id")) {
            return null;
        }

        JsonElement id = request.get("id");

        return switch (method) {
            case "initialize" -> handleInitialize(id, request.getAsJsonObject("params"));
            case "ping" -> createSuccessResponse(id, new JsonObject());
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, request.getAsJsonObject("params"));
            case "resources/list" -> emptyListResponse(id, "resources");
            case "prompts/list" -> emptyListResponse(id, "prompts");
            default -> createErrorResponse(id, -32601, "Method not found: " + method);
        };
    }

    private static JsonObject emptyListResponse(JsonElement id, String field) {
        JsonObject result = new JsonObject();
        result.add(field, new JsonArray());
        return createSuccessResponse(id, result);
    }

    private static JsonObject handleInitialize(JsonElement id, JsonObject params) {
        JsonObject result = new JsonObject();
        String protocolVersion = "2024-11-05";
        if (params != null && params.has("protocolVersion")) {
            protocolVersion = params.get("protocolVersion").getAsString();
        }
        result.addProperty("protocolVersion", protocolVersion);

        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "javafx-automation");
        serverInfo.addProperty("version", "1.1.0");
        result.add("serverInfo", serverInfo);

        return createSuccessResponse(id, result);
    }

    private static JsonObject handleToolsList(JsonElement id) {
        JsonArray tools = new JsonArray();

        // Screenshot tool
        JsonObject screenshot = new JsonObject();
        screenshot.addProperty("name", "screenshot");
        screenshot.addProperty("description",
                "Take a screenshot and return it as an image. On Wayland this captures the target "
                + "application window (default: the largest visible window, or use windowTitle); on X11 "
                + "it captures the screen. Coordinates x/y/width/height select a region in SCREEN coordinates. "
                + "The response text explains how to map image pixels back to screen coordinates for clicking.");
        JsonObject screenshotSchema = new JsonObject();
        screenshotSchema.addProperty("type", "object");
        JsonObject screenshotProps = new JsonObject();
        addIntProperty(screenshotProps, "x", "X coordinate of the region in screen coordinates (optional)");
        addIntProperty(screenshotProps, "y", "Y coordinate of the region in screen coordinates (optional)");
        addIntProperty(screenshotProps, "width", "Width of the region (optional, default: full capture)");
        addIntProperty(screenshotProps, "height", "Height of the region (optional, default: full capture)");
        addStringProperty(screenshotProps, "windowTitle", "Capture the window whose title contains this text (case-insensitive, optional)");
        addStringProperty(screenshotProps, "savePath", "Optional path to also save the screenshot as PNG");
        screenshotSchema.add("properties", screenshotProps);
        screenshot.add("inputSchema", screenshotSchema);
        tools.add(screenshot);

        // List windows tool
        JsonObject listWindows = new JsonObject();
        listWindows.addProperty("name", "list_windows");
        listWindows.addProperty("description", "List visible application windows with their titles, positions and sizes (screen coordinates)");
        JsonObject listWindowsSchema = new JsonObject();
        listWindowsSchema.addProperty("type", "object");
        listWindowsSchema.add("properties", new JsonObject());
        listWindows.add("inputSchema", listWindowsSchema);
        tools.add(listWindows);

        // Click tool
        JsonObject click = new JsonObject();
        click.addProperty("name", "click");
        click.addProperty("description", "Click at the specified screen coordinates");
        JsonObject clickSchema = new JsonObject();
        clickSchema.addProperty("type", "object");
        JsonObject clickProps = new JsonObject();
        addIntProperty(clickProps, "x", "X coordinate to click");
        addIntProperty(clickProps, "y", "Y coordinate to click");
        addStringProperty(clickProps, "button", "Mouse button: left, right, middle (default: left)");
        addIntProperty(clickProps, "clicks", "Number of clicks (default: 1)");
        clickSchema.add("properties", clickProps);
        JsonArray clickRequired = new JsonArray();
        clickRequired.add("x");
        clickRequired.add("y");
        clickSchema.add("required", clickRequired);
        click.add("inputSchema", clickSchema);
        tools.add(click);

        // Move mouse tool
        JsonObject move = new JsonObject();
        move.addProperty("name", "move_mouse");
        move.addProperty("description", "Move the mouse to the specified screen coordinates");
        JsonObject moveSchema = new JsonObject();
        moveSchema.addProperty("type", "object");
        JsonObject moveProps = new JsonObject();
        addIntProperty(moveProps, "x", "X coordinate");
        addIntProperty(moveProps, "y", "Y coordinate");
        moveSchema.add("properties", moveProps);
        JsonArray moveRequired = new JsonArray();
        moveRequired.add("x");
        moveRequired.add("y");
        moveSchema.add("required", moveRequired);
        move.add("inputSchema", moveSchema);
        tools.add(move);

        // Type text tool
        JsonObject type = new JsonObject();
        type.addProperty("name", "type_text");
        type.addProperty("description", "Type text using the keyboard");
        JsonObject typeSchema = new JsonObject();
        typeSchema.addProperty("type", "object");
        JsonObject typeProps = new JsonObject();
        addStringProperty(typeProps, "text", "Text to type");
        typeSchema.add("properties", typeProps);
        JsonArray typeRequired = new JsonArray();
        typeRequired.add("text");
        typeSchema.add("required", typeRequired);
        type.add("inputSchema", typeSchema);
        tools.add(type);

        // Press key tool
        JsonObject pressKey = new JsonObject();
        pressKey.addProperty("name", "press_key");
        pressKey.addProperty("description", "Press a keyboard key (e.g., Enter, Tab, Escape, F1-F12)");
        JsonObject pressKeySchema = new JsonObject();
        pressKeySchema.addProperty("type", "object");
        JsonObject pressKeyProps = new JsonObject();
        addStringProperty(pressKeyProps, "key", "Key name: Enter, Tab, Escape, Space, Backspace, Delete, Up, Down, Left, Right, F1-F12, etc.");
        pressKeySchema.add("properties", pressKeyProps);
        JsonArray pressKeyRequired = new JsonArray();
        pressKeyRequired.add("key");
        pressKeySchema.add("required", pressKeyRequired);
        pressKey.add("inputSchema", pressKeySchema);
        tools.add(pressKey);

        // Scroll tool
        JsonObject scroll = new JsonObject();
        scroll.addProperty("name", "scroll");
        scroll.addProperty("description", "Scroll the mouse wheel at the current or specified position");
        JsonObject scrollSchema = new JsonObject();
        scrollSchema.addProperty("type", "object");
        JsonObject scrollProps = new JsonObject();
        addIntProperty(scrollProps, "amount", "Scroll amount (positive = down, negative = up)");
        addIntProperty(scrollProps, "x", "X coordinate (optional, uses current position if not specified)");
        addIntProperty(scrollProps, "y", "Y coordinate (optional, uses current position if not specified)");
        scrollSchema.add("properties", scrollProps);
        JsonArray scrollRequired = new JsonArray();
        scrollRequired.add("amount");
        scrollSchema.add("required", scrollRequired);
        scroll.add("inputSchema", scrollSchema);
        tools.add(scroll);

        // Get screen info tool
        JsonObject screenInfo = new JsonObject();
        screenInfo.addProperty("name", "get_screen_info");
        screenInfo.addProperty("description", "Get screen dimensions and mouse position");
        JsonObject screenInfoSchema = new JsonObject();
        screenInfoSchema.addProperty("type", "object");
        screenInfoSchema.add("properties", new JsonObject());
        screenInfo.add("inputSchema", screenInfoSchema);
        tools.add(screenInfo);

        // Wait tool
        JsonObject wait = new JsonObject();
        wait.addProperty("name", "wait");
        wait.addProperty("description", "Wait for the specified number of milliseconds");
        JsonObject waitSchema = new JsonObject();
        waitSchema.addProperty("type", "object");
        JsonObject waitProps = new JsonObject();
        addIntProperty(waitProps, "ms", "Milliseconds to wait");
        waitSchema.add("properties", waitProps);
        JsonArray waitRequired = new JsonArray();
        waitRequired.add("ms");
        waitSchema.add("required", waitRequired);
        wait.add("inputSchema", waitSchema);
        tools.add(wait);

        JsonObject result = new JsonObject();
        result.add("tools", tools);
        return createSuccessResponse(id, result);
    }

    private static JsonObject handleToolsCall(JsonElement id, JsonObject params) {
        String name = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();

        try {
            JsonArray contentArray = switch (name) {
                case "screenshot" -> takeScreenshot(arguments);
                case "list_windows" -> textContent(listWindowsJson());
                case "click" -> textContent(click(arguments));
                case "move_mouse" -> textContent(moveMouse(arguments));
                case "type_text" -> textContent(typeText(arguments));
                case "press_key" -> textContent(pressKey(arguments));
                case "scroll" -> textContent(scroll(arguments));
                case "get_screen_info" -> textContent(getScreenInfo());
                case "wait" -> textContent(wait(arguments));
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };

            JsonObject resultObj = new JsonObject();
            resultObj.add("content", contentArray);
            return createSuccessResponse(id, resultObj);
        } catch (Exception e) {
            return createErrorResponse(id, -32000, "Tool error: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Screenshot
    // ------------------------------------------------------------------

    /** A captured image plus the screen coordinates of its top-left corner. */
    private record Capture(BufferedImage image, int originX, int originY, String source) {}

    private record WindowInfo(String id, String title, int x, int y, int width, int height) {}

    private static JsonArray takeScreenshot(JsonObject args) throws Exception {
        Capture capture = isWayland() ? captureWindow(args) : captureX11Screen(args);

        BufferedImage image = capture.image();
        // Crop to the requested region (screen coordinates), if any
        if (args.has("x") || args.has("y") || args.has("width") || args.has("height")) {
            int rx = args.has("x") ? args.get("x").getAsInt() : capture.originX();
            int ry = args.has("y") ? args.get("y").getAsInt() : capture.originY();
            int rw = args.has("width") ? args.get("width").getAsInt() : image.getWidth();
            int rh = args.has("height") ? args.get("height").getAsInt() : image.getHeight();
            Rectangle requested = new Rectangle(rx, ry, rw, rh);
            Rectangle available = new Rectangle(capture.originX(), capture.originY(), image.getWidth(), image.getHeight());
            Rectangle region = requested.intersection(available);
            if (region.isEmpty()) {
                throw new IllegalArgumentException("Requested region " + rectString(requested)
                        + " does not overlap the captured area " + rectString(available));
            }
            image = image.getSubimage(region.x - capture.originX(), region.y - capture.originY(), region.width, region.height);
            capture = new Capture(image, region.x, region.y, capture.source());
        }

        // Save to file if requested
        String savedNote = "";
        if (args.has("savePath")) {
            File outputFile = new File(args.get("savePath").getAsString());
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            ImageIO.write(image, "png", outputFile);
            savedNote = " Saved to: " + outputFile.getAbsolutePath() + ".";
        }

        // Downscale large captures so responses stay small; report the mapping
        int capturedWidth = image.getWidth();
        int capturedHeight = image.getHeight();
        BufferedImage outputImage = image;
        if (capturedWidth > MAX_IMAGE_WIDTH) {
            double scale = (double) MAX_IMAGE_WIDTH / capturedWidth;
            outputImage = scaleImage(image, MAX_IMAGE_WIDTH, (int) Math.round(capturedHeight * scale));
        }

        double factorX = (double) capturedWidth / outputImage.getWidth();
        double factorY = (double) capturedHeight / outputImage.getHeight();
        String mapping = (factorX == 1.0 && factorY == 1.0)
                ? String.format("screenX = %d + imageX, screenY = %d + imageY.", capture.originX(), capture.originY())
                : String.format(Locale.ROOT, "screenX = %d + round(imageX * %.3f), screenY = %d + round(imageY * %.3f).",
                        capture.originX(), factorX, capture.originY(), factorY);

        String text = String.format("Screenshot of %s: %dx%d at screen position (%d, %d).%s To click on something in this image: %s",
                capture.source(), capturedWidth, capturedHeight, capture.originX(), capture.originY(), savedNote, mapping);

        JsonArray content = textContent(text);
        content.add(imageContent(outputImage));
        return content;
    }

    /** X11 session: capture the screen with Robot; fall back to window capture on a blank result. */
    private static Capture captureX11Screen(JsonObject args) throws Exception {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        BufferedImage screenshot = robot.createScreenCapture(new Rectangle(0, 0, screenSize.width, screenSize.height));
        if (!looksUniform(screenshot)) {
            return new Capture(screenshot, 0, 0, "the screen");
        }
        // Blank capture (e.g. XWayland root window) - try window capture instead
        try {
            return captureWindow(args);
        } catch (Exception e) {
            return new Capture(screenshot, 0, 0, "the screen (warning: capture looks blank and window capture failed: " + e.getMessage() + ")");
        }
    }

    /** Capture one application window via xwd (works for XWayland clients such as JavaFX apps). */
    private static Capture captureWindow(JsonObject args) throws Exception {
        String titleFilter = args.has("windowTitle") ? args.get("windowTitle").getAsString() : null;
        List<WindowInfo> candidates = selectWindows(titleFilter);
        Exception lastError = null;
        // A window that is partially offscreen makes xwd fail with BadMatch - try the next candidate
        for (WindowInfo window : candidates) {
            try {
                byte[] xwdData = exec("xwd", "-id", window.id(), "-silent");
                BufferedImage image = decodeXwd(xwdData);
                return new Capture(image, window.x(), window.y(), "window \"" + window.title() + "\" (" + window.id() + ")");
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw new IllegalStateException("Could not capture any window ("
                + candidates.stream().map(WindowInfo::title).toList() + "): "
                + (lastError != null ? lastError.getMessage() : "no candidates"), lastError);
    }

    /** Candidate windows for capture, largest first, optionally filtered by title. */
    private static List<WindowInfo> selectWindows(String titleFilter) throws Exception {
        List<WindowInfo> windows = new ArrayList<>(listWindows());
        if (windows.isEmpty()) {
            throw new IllegalStateException("No visible application windows found (xwininfo)");
        }
        if (titleFilter != null) {
            String needle = titleFilter.toLowerCase(Locale.ROOT);
            List<String> allTitles = windows.stream().map(WindowInfo::title).toList();
            windows.removeIf(w -> !w.title().toLowerCase(Locale.ROOT).contains(needle));
            if (windows.isEmpty()) {
                throw new IllegalArgumentException("No visible window with title containing \"" + titleFilter + "\". Visible: " + allTitles);
            }
        }
        windows.sort(Comparator.comparingLong((WindowInfo w) -> (long) w.width() * w.height()).reversed());
        return windows;
    }

    private static final Pattern WINDOW_LINE = Pattern.compile(
            "^\\s*(0x[0-9a-fA-F]+)\\s+\"(.*)\":\\s+\\(([^)]*)\\)\\s+(\\d+)x(\\d+)\\+(-?\\d+)\\+(-?\\d+)\\s+\\+(-?\\d+)\\+(-?\\d+)\\s*$");

    /** Visible, reasonably sized, titled top-level windows from xwininfo. */
    private static List<WindowInfo> listWindows() throws Exception {
        String tree = new String(exec("xwininfo", "-root", "-tree"), StandardCharsets.UTF_8);
        List<WindowInfo> windows = new ArrayList<>();
        for (String line : tree.split("\n")) {
            Matcher m = WINDOW_LINE.matcher(line);
            if (!m.matches()) {
                continue;
            }
            String id = m.group(1);
            String title = m.group(2);
            String wmClass = m.group(3);
            int width = Integer.parseInt(m.group(4));
            int height = Integer.parseInt(m.group(5));
            int absX = Integer.parseInt(m.group(8));
            int absY = Integer.parseInt(m.group(9));
            if (title.isBlank() || width < 100 || height < 50) {
                continue;
            }
            // Windows without a WM class are desktop infrastructure (mutter guard
            // window, GNOME Shell, ...); mutter-x11-frames are decoration wrappers.
            if (wmClass.isBlank() || wmClass.contains("mutter-x11-frames")) {
                continue;
            }
            if (isViewable(id)) {
                windows.add(new WindowInfo(id, title, absX, absY, width, height));
            }
        }
        return windows;
    }

    private static boolean isViewable(String windowId) {
        try {
            String stats = new String(exec("xwininfo", "-id", windowId, "-stats"), StandardCharsets.UTF_8);
            return stats.contains("IsViewable");
        } catch (Exception e) {
            return false;
        }
    }

    private static String listWindowsJson() throws Exception {
        JsonArray array = new JsonArray();
        for (WindowInfo w : listWindows()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", w.id());
            o.addProperty("title", w.title());
            o.addProperty("x", w.x());
            o.addProperty("y", w.y());
            o.addProperty("width", w.width());
            o.addProperty("height", w.height());
            array.add(o);
        }
        return GSON.toJson(array);
    }

    /**
     * Decode an XWD (X Window Dump) file into a BufferedImage.
     * Supports the common case: ZPixmap, depth 24, 32 or 24 bits per pixel.
     */
    private static BufferedImage decodeXwd(byte[] data) {
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        int headerSize = header.getInt(0);
        int pixmapFormat = header.getInt(8);
        int width = header.getInt(16);
        int height = header.getInt(20);
        int byteOrder = header.getInt(28); // 0 = LSBFirst, 1 = MSBFirst
        int bitsPerPixel = header.getInt(44);
        int bytesPerLine = header.getInt(48);
        int redMask = header.getInt(56);
        int greenMask = header.getInt(60);
        int blueMask = header.getInt(64);
        int ncolors = header.getInt(76);

        if (pixmapFormat != 2 || (bitsPerPixel != 32 && bitsPerPixel != 24)) {
            throw new IllegalStateException("Unsupported XWD format: pixmapFormat=" + pixmapFormat + ", bitsPerPixel=" + bitsPerPixel);
        }

        int pixelOffset = headerSize + ncolors * 12;
        int bytesPerPixel = bitsPerPixel / 8;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < height; row++) {
            int rowStart = pixelOffset + row * bytesPerLine;
            for (int col = 0; col < width; col++) {
                int p = rowStart + col * bytesPerPixel;
                int pixel = 0;
                if (byteOrder == 0) { // LSBFirst
                    for (int b = bytesPerPixel - 1; b >= 0; b--) {
                        pixel = (pixel << 8) | (data[p + b] & 0xFF);
                    }
                } else { // MSBFirst
                    for (int b = 0; b < bytesPerPixel; b++) {
                        pixel = (pixel << 8) | (data[p + b] & 0xFF);
                    }
                }
                int r = extractChannel(pixel, redMask);
                int g = extractChannel(pixel, greenMask);
                int b = extractChannel(pixel, blueMask);
                image.setRGB(col, row, (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    private static int extractChannel(int pixel, int mask) {
        if (mask == 0) {
            return 0;
        }
        return (pixel & mask) >>> Integer.numberOfTrailingZeros(mask);
    }

    private static boolean isWayland() {
        return System.getenv("WAYLAND_DISPLAY") != null
                || "wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"));
    }

    /** True if a sparse sample of the image is a single uniform color (blank/black capture). */
    private static boolean looksUniform(BufferedImage image) {
        int first = image.getRGB(0, 0);
        int stepX = Math.max(1, image.getWidth() / 16);
        int stepY = Math.max(1, image.getHeight() / 16);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                if (image.getRGB(x, y) != first) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private static String rectString(Rectangle r) {
        return r.width + "x" + r.height + " at (" + r.x + ", " + r.y + ")";
    }

    /** Run an external command and return its stdout; fails on non-zero exit or timeout. */
    private static byte[] exec(String... command) throws Exception {
        Process process = new ProcessBuilder(command).start();
        try (InputStream stdout = process.getInputStream()) {
            byte[] output = stdout.readAllBytes();
            if (!process.waitFor(EXEC_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException(command[0] + " timed out after " + EXEC_TIMEOUT_MS + " ms");
            }
            if (process.exitValue() != 0) {
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                throw new IOException(command[0] + " failed (exit " + process.exitValue() + "): " + stderr);
            }
            return output;
        }
    }

    // ------------------------------------------------------------------
    // Input tools
    // ------------------------------------------------------------------

    private static String click(JsonObject args) {
        int x = args.get("x").getAsInt();
        int y = args.get("y").getAsInt();
        String button = args.has("button") ? args.get("button").getAsString() : "left";
        int clicks = args.has("clicks") ? args.get("clicks").getAsInt() : 1;

        int buttonMask = switch (button.toLowerCase()) {
            case "right" -> InputEvent.BUTTON3_DOWN_MASK;
            case "middle" -> InputEvent.BUTTON2_DOWN_MASK;
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };

        robot.mouseMove(x, y);
        robot.delay(50);

        for (int i = 0; i < clicks; i++) {
            robot.mousePress(buttonMask);
            robot.mouseRelease(buttonMask);
            if (i < clicks - 1) {
                robot.delay(50);
            }
        }

        return "Clicked " + button + " button " + clicks + " time(s) at (" + x + ", " + y + ")";
    }

    private static String moveMouse(JsonObject args) {
        int x = args.get("x").getAsInt();
        int y = args.get("y").getAsInt();

        robot.mouseMove(x, y);

        return "Mouse moved to (" + x + ", " + y + ")";
    }

    private static String typeText(JsonObject args) {
        String text = args.get("text").getAsString();

        for (char c : text.toCharArray()) {
            typeCharacter(c);
        }

        return "Typed: " + text;
    }

    private static void typeCharacter(char c) {
        boolean shift = Character.isUpperCase(c) || "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;

        int keyCode = getKeyCode(c);
        if (keyCode == -1) {
            return; // Unknown character
        }

        if (shift) {
            robot.keyPress(KeyEvent.VK_SHIFT);
        }
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        if (shift) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }

    private static int getKeyCode(char c) {
        // Handle lowercase letters
        if (c >= 'a' && c <= 'z') {
            return KeyEvent.VK_A + (c - 'a');
        }
        // Handle uppercase letters
        if (c >= 'A' && c <= 'Z') {
            return KeyEvent.VK_A + (c - 'A');
        }
        // Handle digits
        if (c >= '0' && c <= '9') {
            return KeyEvent.VK_0 + (c - '0');
        }
        // Handle special characters
        return switch (c) {
            case ' ' -> KeyEvent.VK_SPACE;
            case '\n' -> KeyEvent.VK_ENTER;
            case '\t' -> KeyEvent.VK_TAB;
            case '.' -> KeyEvent.VK_PERIOD;
            case ',' -> KeyEvent.VK_COMMA;
            case '-' -> KeyEvent.VK_MINUS;
            case '=' -> KeyEvent.VK_EQUALS;
            case '[' -> KeyEvent.VK_OPEN_BRACKET;
            case ']' -> KeyEvent.VK_CLOSE_BRACKET;
            case '\\' -> KeyEvent.VK_BACK_SLASH;
            case ';' -> KeyEvent.VK_SEMICOLON;
            case '\'' -> KeyEvent.VK_QUOTE;
            case '/' -> KeyEvent.VK_SLASH;
            case '`' -> KeyEvent.VK_BACK_QUOTE;
            default -> -1;
        };
    }

    private static String pressKey(JsonObject args) {
        String key = args.get("key").getAsString().toUpperCase();

        int keyCode = switch (key) {
            case "ENTER", "RETURN" -> KeyEvent.VK_ENTER;
            case "TAB" -> KeyEvent.VK_TAB;
            case "ESCAPE", "ESC" -> KeyEvent.VK_ESCAPE;
            case "SPACE" -> KeyEvent.VK_SPACE;
            case "BACKSPACE" -> KeyEvent.VK_BACK_SPACE;
            case "DELETE" -> KeyEvent.VK_DELETE;
            case "UP" -> KeyEvent.VK_UP;
            case "DOWN" -> KeyEvent.VK_DOWN;
            case "LEFT" -> KeyEvent.VK_LEFT;
            case "RIGHT" -> KeyEvent.VK_RIGHT;
            case "HOME" -> KeyEvent.VK_HOME;
            case "END" -> KeyEvent.VK_END;
            case "PAGEUP" -> KeyEvent.VK_PAGE_UP;
            case "PAGEDOWN" -> KeyEvent.VK_PAGE_DOWN;
            case "F1" -> KeyEvent.VK_F1;
            case "F2" -> KeyEvent.VK_F2;
            case "F3" -> KeyEvent.VK_F3;
            case "F4" -> KeyEvent.VK_F4;
            case "F5" -> KeyEvent.VK_F5;
            case "F6" -> KeyEvent.VK_F6;
            case "F7" -> KeyEvent.VK_F7;
            case "F8" -> KeyEvent.VK_F8;
            case "F9" -> KeyEvent.VK_F9;
            case "F10" -> KeyEvent.VK_F10;
            case "F11" -> KeyEvent.VK_F11;
            case "F12" -> KeyEvent.VK_F12;
            default -> throw new IllegalArgumentException("Unknown key: " + key);
        };

        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);

        return "Pressed key: " + key;
    }

    private static String scroll(JsonObject args) {
        int amount = args.get("amount").getAsInt();

        if (args.has("x") && args.has("y")) {
            int x = args.get("x").getAsInt();
            int y = args.get("y").getAsInt();
            robot.mouseMove(x, y);
            robot.delay(50);
        }

        robot.mouseWheel(amount);

        return "Scrolled " + (amount > 0 ? "down" : "up") + " by " + Math.abs(amount) + " units";
    }

    private static String getScreenInfo() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point mousePos = MouseInfo.getPointerInfo().getLocation();

        JsonObject info = new JsonObject();
        info.addProperty("screenWidth", screenSize.width);
        info.addProperty("screenHeight", screenSize.height);
        info.addProperty("mouseX", mousePos.x);
        info.addProperty("mouseY", mousePos.y);
        info.addProperty("sessionType", isWayland() ? "wayland" : "x11");

        return GSON.toJson(info);
    }

    private static String wait(JsonObject args) {
        int ms = Math.min(args.get("ms").getAsInt(), 60_000); // Robot.delay caps at 60s
        robot.delay(ms);
        return "Waited " + ms + " milliseconds";
    }

    // ------------------------------------------------------------------
    // JSON-RPC / MCP plumbing
    // ------------------------------------------------------------------

    private static JsonArray textContent(String text) {
        JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", text);
        JsonArray array = new JsonArray();
        array.add(content);
        return array;
    }

    private static JsonObject imageContent(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        JsonObject content = new JsonObject();
        content.addProperty("type", "image");
        content.addProperty("data", Base64.getEncoder().encodeToString(baos.toByteArray()));
        content.addProperty("mimeType", "image/png");
        return content;
    }

    private static void addStringProperty(JsonObject props, String name, String description) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        prop.addProperty("description", description);
        props.add(name, prop);
    }

    private static void addIntProperty(JsonObject props, String name, String description) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "integer");
        prop.addProperty("description", description);
        props.add(name, prop);
    }

    private static JsonObject createSuccessResponse(JsonElement id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        response.add("result", result);
        return response;
    }

    private static JsonObject createErrorResponse(JsonElement id, int code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return response;
    }

    private static void sendError(JsonElement id, String message) {
        out.println(GSON.toJson(createErrorResponse(id, -32000, message)));
        out.flush();
    }
}
