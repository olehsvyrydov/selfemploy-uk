import java.awt.*;
import java.awt.event.KeyEvent;

public class KeyPressTool {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java KeyPressTool <key>");
            System.out.println("Keys: TAB, ENTER, DOWN, UP, LEFT, RIGHT, F1-F12");
            System.exit(1);
        }

        String key = args[0].toUpperCase();
        Robot robot = new Robot();
        robot.setAutoDelay(50);

        int keyCode = switch (key) {
            case "TAB" -> KeyEvent.VK_TAB;
            case "ENTER" -> KeyEvent.VK_ENTER;
            case "DOWN" -> KeyEvent.VK_DOWN;
            case "UP" -> KeyEvent.VK_UP;
            case "LEFT" -> KeyEvent.VK_LEFT;
            case "RIGHT" -> KeyEvent.VK_RIGHT;
            case "SPACE" -> KeyEvent.VK_SPACE;
            case "F1" -> KeyEvent.VK_F1;
            case "F2" -> KeyEvent.VK_F2;
            case "F6" -> KeyEvent.VK_F6;
            default -> throw new IllegalArgumentException("Unknown key: " + key);
        };

        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        System.out.println("Pressed: " + key);
    }
}
