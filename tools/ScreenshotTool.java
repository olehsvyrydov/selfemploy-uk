import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ScreenshotTool {
    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Take full screenshot
        BufferedImage screenshot = robot.createScreenCapture(
            new Rectangle(0, 0, screenSize.width, screenSize.height));

        String path = args.length > 0 ? args[0] : "/tmp/screenshot.png";
        ImageIO.write(screenshot, "png", new File(path));

        System.out.println("Screenshot saved to: " + path);
        System.out.println("Screen size: " + screenSize.width + "x" + screenSize.height);

        // Get mouse position
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        System.out.println("Mouse position: " + mousePos.x + ", " + mousePos.y);
    }
}
