import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class clientHandler implements Runnable {
    private Socket clientSocket;
    private JButton button;
    private JLabel imageLabel;
    private JFrame frame;
    private static boolean captureFromCamera = false;
    private DataOutputStream dos;
    private boolean isCamera = false;

    public clientHandler(Socket clientSocket,JButton button, JLabel imageLabel, JFrame frame) {
        this.clientSocket = clientSocket;
        this.imageLabel = imageLabel;
        this.frame = frame;
        this.button = button;
        try {
            this.dos = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error constructor DataOutputStream: " + e.getMessage());
        }
    }


    @Override
    public void run() {

        try (InputStream in = clientSocket.getInputStream();
             DataInputStream dis = new DataInputStream(in)) {
            button.addActionListener(e -> {
                try {
                    if (!isCamera) {
                        isCamera = true;
                        dos.writeUTF("startCamera");
                    } else {
                        isCamera = false;
                        dos.writeUTF("stopCamera");
                    }

                    dos.flush();
                } catch (IOException ex) {
                    System.err.println("Error required Camera: " + ex.getMessage());
                }
            });
            while (true) {
                // Đọc kích thước ảnh
                int size = dis.readInt();
                if (size <= 0) break; // Kiểm tra kích thước hợp lệ

                byte[] imageBytes = new byte[size];
                dis.readFully(imageBytes);

                // Chuyển đổi byte[] thành BufferedImage
                BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

                // Scale image
                Image scaledImage = originalImage.getScaledInstance(
                        imageLabel.getWidth(), imageLabel.getHeight(), Image.SCALE_SMOOTH);
                // Thumbnailator có thể scale ảnh tốt hơn bằng thư viện này
                SwingUtilities.invokeLater(() -> {
                    ImageIcon imageIcon = new ImageIcon(scaledImage);
                    imageLabel.setIcon(imageIcon);
                    frame.repaint();
                });

            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
