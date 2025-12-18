package ohmry.github.io.captcha;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class CaptchaUtil {
    // 캡차에서 생성할 수 있는 텍스트 모음
    private final String CHAR_PRESET = "0123456789";
    // 텍스트 생성 시 사용이 가능한 색상 모음
    private final String[] COLOR_PRESET = {"#351f66", "#59bec9", "#525659", "#fed143", "#ffd442"};

    // 캡차로 만드는 도형에 대한 정보
    class RotateShape {
        public String value;
        public Shape shape;
        public double x;
    }

    private String getRandomText(int size) {
        List<Character> characters = new ArrayList<>();
        for (char c : CHAR_PRESET.toCharArray()) {
            characters.add(c);
        }
        Collections.shuffle(characters);

        StringBuilder randomText = new StringBuilder();
        for (int index = 0; index < size; index++) {
            randomText.append(characters.get(index));
        }
        return randomText.toString();
    }

    private String createSingleNoise(int width, int height, String color, double scale) {
        Random rand = new Random();

        int x1 = 0;
        int y1 = rand.nextInt(height);
        int x2 = width;
        int y2 = rand.nextInt(height);
        int cx1 = rand.nextInt(width);
        int cy1 = rand.nextInt(height);
        int cx2 = rand.nextInt(width);
        int cy2 = rand.nextInt(height);
        int strokeWidth = 1 + rand.nextInt(10);

        return String.format(Locale.US, "<path d=\"M %.2f %.2f C %.2f %.2f, %.2f %.2f, %.2f %.2f\" stroke=\"%s\" stroke-width=\"%d\" fill=\"rgba(0,0,0,0)\" />\n", x1 * scale, y1 * scale, cx1 * scale, cy1 * scale, cx2 * scale, cy2 * scale, x2 * scale, y2 * scale, color + "55", strokeWidth);
    }

    private RotateShape createRotatedShape(int width, int height, double x, char ch) {
        Random random = new Random();
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Font font = new Font("Arial", Font.BOLD, height);
        GlyphVector gv = font.createGlyphVector(frc, new char[]{ch});
        Shape shape = gv.getGlyphOutline(0);
        Rectangle2D rect = shape.getBounds2D();

        // -30 ~ 30
        double rotateAngle = Math.toRadians(random.nextInt(60) - 30);
        AffineTransform rotation = AffineTransform.getRotateInstance(rotateAngle, rect.getCenterX(), rect.getCenterY());
        Shape rotatedShape = rotation.createTransformedShape(shape);
        Rectangle2D rotatedBounds = rotatedShape.getBounds2D();

        AffineTransform transform = new AffineTransform();
        double centerY = Math.floor((height - rotatedShape.getBounds2D().getHeight()) / 2);
        transform.translate(x + -rotatedShape.getBounds2D().getX(), -rotatedShape.getBounds2D().getY() + centerY);

        Shape finalShape = transform.createTransformedShape(rotatedShape);
        StringBuilder svgPath = new StringBuilder();
        PathIterator pi = finalShape.getPathIterator(null);
        double[] coords = new double[6];

        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    svgPath.append(String.format("M %.2f %.2f ", coords[0], coords[1]));
                    break;
                case PathIterator.SEG_LINETO:
                    svgPath.append(String.format("L %.2f %.2f ", coords[0], coords[1]));
                    break;
                case PathIterator.SEG_QUADTO:
                    svgPath.append(String.format("Q %.2f %.2f %.2f %.2f ", coords[0], coords[1], coords[2], coords[3]));
                    break;
                case PathIterator.SEG_CUBICTO:
                    svgPath.append(String.format("C %.2f %.2f %.2f %.2f %.2f %.2f ", coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]));
                    break;
                case PathIterator.SEG_CLOSE:
                    svgPath.append("Z ");
                    break;
            }
            pi.next();
        }

        RotateShape returnShape = new RotateShape();
        returnShape.value = String.format(Locale.US, "  <path d='%s' fill='%s' />\n", svgPath, COLOR_PRESET[random.nextInt(COLOR_PRESET.length - 1)]);
        returnShape.shape = rotatedShape;
        returnShape.x = x + rotatedShape.getBounds2D().getWidth();
        return returnShape;
    }

    public Map<String, Object> createCaptchaImage(int width, int height, int size) {
        Map<String, Object> response = new HashMap<>();

        Random random = new Random();
        String randomText = this.getRandomText(size);

        StringBuilder svgHeader = new StringBuilder();
        svgHeader.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svgHeader.append(String.format(Locale.US, "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d'>\n", width, height));

        StringBuilder svgShapes = new StringBuilder();
        double totalWidth = 0;
        double x = 20;

        for (int index = 0; index < size; index++) {
            RotateShape rotateShape = createRotatedShape(width, height, x, randomText.charAt(index));
            x = rotateShape.x;
            svgShapes.append(rotateShape.value);

            if (index == size - 1) {
                totalWidth = rotateShape.x;
            }
        }

        StringBuilder svg = new StringBuilder();
        if (totalWidth + 20 > width) {
            double scale = width / (totalWidth + 20);
            svg.append(String.format("<g transform='scale(%.2f, 1)'>", scale));
            svg.append(svgShapes);
            svg.append("</g>");
        } else {
            svg.append(svgShapes);
        }

        svgHeader.append(svg);

        for (int index = 0; index < size; index++) {
            // 노이즈 추가
            svgHeader.append(createSingleNoise(width, height, COLOR_PRESET[random.nextInt(COLOR_PRESET.length - 1)], 1));
        }

        svgHeader.append("</svg>");
        response.put("text", randomText);
        response.put("svg", svgHeader.toString());
        return response;
    }
}
