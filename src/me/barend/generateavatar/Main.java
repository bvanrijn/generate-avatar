package me.barend.generateavatar;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static Color fillColor = getColor("color.fill", new Color(0x48a9a6));
    private static Color backgroundColor = getColor("color.background", Color.white);
    private static final String inputPath = System.getProperty("image.input", "man-tipping-hand.png");
    private static final String outputPath = System.getProperty("image.output", "avatar.png");

    private static final double threshold;

    static {
        String thresholdProp = System.getProperty("color.threshold", "0.7");

        try {
            threshold = Double.parseDouble(thresholdProp);

            if (threshold < 0.0 || threshold > 1.0) {
                System.err.println("Threshold must be between 0.0 and 1.0");
                System.exit(2);
            }
        } catch (NumberFormatException e) {
            System.err.println(String.format("Threshold must be a number, got %s", thresholdProp));

            System.exit(1);
            throw e;
        }
    }

    public static void main(String[] args) throws IOException {
        if (Boolean.parseBoolean(System.getProperty("debug"))) {
            printProperties();
        }

        BufferedImage image = readImage(inputPath);
        BufferedImage opaqueImage = transparentToOpaque(image);
        BufferedImage greyscaleImage = imageToGreyscale(opaqueImage);
        BufferedImage recoloredImage = recolorImage(greyscaleImage);

        saveImage(outputPath, recoloredImage);
    }

    private static void printProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("color.threshold", threshold);
        properties.put("color.fill", fillColor);
        properties.put("color.background", backgroundColor);
        properties.put("image.input", inputPath);
        properties.put("image.output", outputPath);
        properties.put("debug", Boolean.parseBoolean(System.getProperty("debug")));

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            System.err.println(String.format("%s: %s", property.getKey(), property.getValue()));
        }
    }

    private static Color getColor(String key, Color defaultColor) {
        List<String> namedColors = getNamedColors();
        String prop = System.getProperty(key);

        if (prop == null || !namedColors.contains(prop.toUpperCase())) {
            return defaultColor;
        }

        try {
            return (Color) Color.class.getField(prop).get(Color.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            System.err.println(String.format("Attempted to use an unknown color %s, using %s instead", prop, defaultColor));

            e.printStackTrace();

            return defaultColor;
        }
    }

    /**
     * Returns a list of named colors (that is, the fields on the {@link Color} class).
     */
    private static List<String> getNamedColors() {
        Field[] colorFields = Color.class.getFields();
        List<String> colorNames = new ArrayList<>(colorFields.length);

        for (Field colorField : colorFields) {
            colorNames.add(colorField.getName().toUpperCase());
        }

        return colorNames;
    }

    /**
     * Create a new image using clamped colors.
     */
    private static BufferedImage recolorImage(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        List<Color> newColors = clampImageColors(img);
        BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                newImg.setRGB(w, h, newColors.get(width * w + h).getRGB());
            }
        }

        return newImg;
    }

    /**
     * Gets the "coordinates" of an image, that is, a list like <code>[Point(0, 0), Point(0, 1), ... Point(w - 1, h - 1)]</code>.
     */
    private static List<Point> getCoordinates(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        List<Point> coordinates = new ArrayList<>(width * height);

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                coordinates.add(new Point(w, h));
            }
        }

        return coordinates;
    }

    /**
     * Gets the pixel values of the image.
     */
    private static List<Color> getPixelValues(BufferedImage img) {
        List<Point> points = getCoordinates(img);
        List<Color> colors = new ArrayList<>(points.size());

        for (Point point : points) {
            colors.add(new Color(img.getRGB((int) point.getX(), (int) point.getY())));
        }

        return colors;
    }

    /**
     * Clamps the colors of the image using {@link Main#clamp}.
     */
    private static List<Color> clampImageColors(BufferedImage img) {
        List<Color> pixelValues = getPixelValues(img);
        List<Color> colors = new ArrayList<>(pixelValues.size());

        for (Color color : pixelValues) {
            colors.add(clamp(color));
        }

        return colors;
    }

    /**
     * Converts the image to opaque.
     */
    private static BufferedImage transparentToOpaque(BufferedImage img) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D gfx = result.createGraphics();

        gfx.drawImage(img, 0, 0, Color.white, null);

        return result;
    }

    /**
     * Reads the image at the given path.
     */
    private static BufferedImage readImage(@SuppressWarnings("SameParameterValue") String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    /**
     * Save the image to the given path.
     */
    private static void saveImage(@SuppressWarnings("SameParameterValue") String path, RenderedImage img) throws IOException {
        ImageIO.write(img, "png", new File(path));
    }

    /**
     * Clamps the color to either {@link Main#fillColor} or {@link Main#backgroundColor}.
     * The exact algorithm is subject to change.
     *
     * @param color the color
     * @return the clamped color; either {@link Main#fillColor} or {@link Main#backgroundColor}
     */
    private static Color clamp(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        double avg = (r + g + b) / 3.0 / 255.0;

        if (avg > threshold) {
            return backgroundColor;
        }

        return fillColor;
    }

    /**
     * Converts the image to greyscale. Returns a new {@link BufferedImage} with the colors converted to greyscale.
     *
     * @param img the image to convert to greyscale.
     * @return a new image, with the colors converted to greyscale.
     */
    private static BufferedImage imageToGreyscale(BufferedImage img) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);

        return op.filter(img, null);
    }
}
