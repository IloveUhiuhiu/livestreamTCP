import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class client {
    private static final int PORT = 9999;
    private static final String HOSTNAME = "localhost";
    private static volatile boolean captureFromCamera = false;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        VideoCapture camera = new VideoCapture(0);
        try (Socket socket = new Socket(HOSTNAME, PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            if (!camera.isOpened()) {
                System.err.println("Error: Doesn't open camera!");
                return;
            }

            new Thread(() -> listenForRequests(socket)).start();
            captureImages(dos, camera);
        } catch (IOException | AWTException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            System.out.println("Server is closing...");
        } finally {
            camera.release();
        }
    }

    private static void listenForRequests(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            while (true) {
                String request = dis.readUTF();
                captureFromCamera = request.equals("startCamera");
            }
        } catch (IOException e) {
            System.err.println("Error listening required: " + e.getMessage());
        }
    }

    private static void captureImages(DataOutputStream dos, VideoCapture camera) throws AWTException, IOException, InterruptedException {
        Robot robot = new Robot();
        while (true) {
            if (captureFromCamera) {
                Mat frame = new Mat();
                if (camera.read(frame) && !frame.empty()) {
                    sendImage(dos, frame);
                }
            } else {
                BufferedImage screenCapture = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                sendImage(dos, screenCapture);
            }
            Thread.sleep(100);
        }
    }

    private static void sendImage(DataOutputStream dos, Mat frame) throws IOException {
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB); // Đảm bảo đúng định dạng màu
        sendImage(dos, matToBufferedImage(frame));
    }

    private static void sendImage(DataOutputStream dos, BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos); // Sử dụng định dạng JPEG
            byte[] imageBytes = baos.toByteArray();

            dos.writeInt(imageBytes.length);
            dos.write(imageBytes);
            dos.flush();
            System.out.println("Đã gửi ảnh kích thước: " + imageBytes.length + " bytes");
        }
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = new byte[mat.width() * mat.height() * (int) mat.elemSize()];
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, mat.width(), mat.height(), data);
        return image;
    }
}