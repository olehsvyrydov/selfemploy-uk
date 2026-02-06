package uk.selfemploy.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

/**
 * MCP Server for automating JavaFX desktop applications.
 *
 * Provides tools for:
 * - Taking screenshots
 * - Clicking at coordinates
 * - Typing text
 * - Scrolling
 * - Getting screen dimensions
 */
public class JavaFxMcpServer {

    private static final Gson GSON = new Gson();  // No pretty-printing - MCP requires single-line JSON (NDJSON)
    private static final AtomicInteger requestId = new AtomicInteger(0);
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
                    sendError(-1, "Parse error: " + e.getMessage());
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
            // Notifications like "notifications/initialized" don't need a response
            return null;
        }

        int id = request.get("id").getAsInt();

        return switch (method) {
            case "initialize" -> handleInitialize(id);
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, request.getAsJsonObject("params"));
            default -> createErrorResponse(id, -32601, "Method not found: " + method);
        };
    }

    private static JsonObject handleInitialize(int id) {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2024-11-05");

        JsonObject capabilities = new JsonObject();
        JsonObject tools = new JsonObject();
        capabilities.add("tools", tools);
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "javafx-automation");
        serverInfo.addProperty("version", "1.0.0");
        result.add("serverInfo", serverInfo);

        return createSuccessResponse(id, result);
    }

    private static JsonObject handleToolsList(int id) {
        JsonArray tools = new JsonArray();

        // Screenshot tool
        JsonObject screenshot = new JsonObject();
        screenshot.addProperty("name", "screenshot");
        screenshot.addProperty("description", "Take a screenshot of the entire screen or a region");
        JsonObject screenshotSchema = new JsonObject();
        screenshotSchema.addProperty("type", "object");
        JsonObject screenshotProps = new JsonObject();
        addIntProperty(screenshotProps, "x", "X coordinate of the region (optional, default: 0)");
        addIntProperty(screenshotProps, "y", "Y coordinate of the region (optional, default: 0)");
        addIntProperty(screenshotProps, "width", "Width of the region (optional, default: full screen)");
        addIntProperty(screenshotProps, "height", "Height of the region (optional, default: full screen)");
        addStringProperty(screenshotProps, "savePath", "Optional path to save the screenshot as PNG");
        screenshotSchema.add("properties", screenshotProps);
        screenshot.add("inputSchema", screenshotSchema);
        tools.add(screenshot);

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

    private static JsonObject handleToolsCall(int id, JsonObject params) {
        String name = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();

        try {
            String result = switch (name) {
                case "screenshot" -> takeScreenshot(arguments);
                case "click" -> click(arguments);
                case "move_mouse" -> moveMouse(arguments);
                case "type_text" -> typeText(arguments);
                case "press_key" -> pressKey(arguments);
                case "scroll" -> scroll(arguments);
                case "get_screen_info" -> getScreenInfo();
                case "wait" -> wait(arguments);
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };

            JsonObject content = new JsonObject();
            content.addProperty("type", "text");
            content.addProperty("text", result);

            JsonArray contentArray = new JsonArray();
            contentArray.add(content);

            JsonObject resultObj = new JsonObject();
            resultObj.add("content", contentArray);

            return createSuccessResponse(id, resultObj);
        } catch (Exception e) {
            return createErrorResponse(id, -32000, "Tool error: " + e.getMessage());
        }
    }

    private static String takeScreenshot(JsonObject args) throws Exception {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int x = args.has("x") ? args.get("x").getAsInt() : 0;
        int y = args.has("y") ? args.get("y").getAsInt() : 0;
        int width = args.has("width") ? args.get("width").getAsInt() : screenSize.width - x;
        int height = args.has("height") ? args.get("height").getAsInt() : screenSize.height - y;

        Rectangle captureRect = new Rectangle(x, y, width, height);
        BufferedImage screenshot = robot.createScreenCapture(captureRect);

        // Save to file if path provided
        if (args.has("savePath")) {
            String savePath = args.get("savePath").getAsString();
            File outputFile = new File(savePath);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(screenshot, "png", outputFile);
            return "Screenshot saved to: " + outputFile.getAbsolutePath() + " (size: " + width + "x" + height + ")";
        }

        // Return as base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(screenshot, "png", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        return "Screenshot taken (size: " + width + "x" + height + ")\nBase64 (first 100 chars): " + base64.substring(0, Math.min(100, base64.length())) + "...";
    }

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

        return GSON.toJson(info);
    }

    private static String wait(JsonObject args) {
        int ms = args.get("ms").getAsInt();
        robot.delay(ms);
        return "Waited " + ms + " milliseconds";
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

    private static JsonObject createSuccessResponse(int id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", id);
        response.add("result", result);
        return response;
    }

    private static JsonObject createErrorResponse(int id, int code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return response;
    }

    private static void sendError(int id, String message) {
        out.println(GSON.toJson(createErrorResponse(id, -32000, message)));
        out.flush();
    }
}
