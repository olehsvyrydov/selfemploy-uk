import java.awt.*;
import java.awt.event.InputEvent;

public class ClickTool {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java ClickTool <x> <y>");
            System.exit(1);
        }

        int x = Integer.parseInt(args[0]);
        int y = Integer.parseInt(args[1]);

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        // Move and click
        robot.mouseMove(x, y);
        robot.delay(100);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        System.out.println("Clicked at: " + x + ", " + y);
    }
}
