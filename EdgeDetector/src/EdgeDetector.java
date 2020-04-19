import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class EdgeDetector {
    //Canny parameters
    private static final double CANNY_THRESHOLD_RATIO = .2; //Suggested range .2 - .4
    private static final int CANNY_STD_DEV = 1;             //Range 1-3

    //I/O parameters
    private static String imgFileName;
    private static String imgOutFile = "";
    private static String imgExt;

    public static void main(String[] args) {
        //Podawanie obrazu do przetworzenia oraz rozszerzenia pliku wyjściowego
        imgFileName ="imag.jpg"; //args[0];
        imgExt = "jpg"; //args[1];
        String[] arr = imgFileName.split("\\.");

        for (int i = 0; i < arr.length - 1; i++) {
            imgOutFile += arr[i];
        }

        imgOutFile += "EdgeDetected.";
        imgOutFile += imgExt;

        //Sample JCanny usage
        try {
            BufferedImage input = ImageIO.read(new File(imgFileName));
            BufferedImage output = JCanny.CannyEdges(input, CANNY_STD_DEV, CANNY_THRESHOLD_RATIO);
            ImageIO.write(output, imgExt, new File(imgOutFile));
        } catch (Exception ex) {
            System.out.println("ERROR ACCESING IMAGE FILE:\n" + ex.getMessage());
        }
    }
}
