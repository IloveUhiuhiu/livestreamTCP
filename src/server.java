import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;
import javax.swing.*;

public class server {
    private final static int PORT = 9999;
    private final static Logger audit = Logger.getLogger("requests");
    private final static Logger errors = Logger.getLogger("errors");


    public static void main(String[] args) throws UnknownHostException {
        JFrame frame = new JFrame("Live Stream");
        JLabel[] subFrames = new JLabel[4];
        JButton[] buttons = new JButton[4];
        JLabel[] labels = new JLabel[4];
        for (int i = 0; i < 4; i++) {
            subFrames[i] = new JLabel();
            subFrames[i].setLayout(new BorderLayout());
            buttons[i] = new JButton("Yêu cầu mở/tắt camera");
            labels[i] = new JLabel();
            subFrames[i].add(buttons[i], BorderLayout.SOUTH);
            subFrames[i].add(labels[i], BorderLayout.CENTER);
            frame.add(subFrames[i]);
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 800);
        frame.setLayout(new GridLayout(2,2));

        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        int clientCount = 0;
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.print("Client " + clientCount);
                    System.out.println(" connected: " + clientSocket.getInetAddress());
                    clientHandler client = new clientHandler(clientSocket,buttons[clientCount],subFrames[clientCount],frame);
                    ++clientCount;
                    pool.submit(client);
                } catch (IOException ex) {
                    errors.log(Level.SEVERE, "accept error", ex);
                } catch (RuntimeException ex) {
                    errors.log(Level.SEVERE, "unexpected error " + ex.getMessage(), ex);
                }
            }
        } catch (IOException ex) {
            errors.log(Level.SEVERE, "Couldn't start server", ex);
        } catch (RuntimeException ex) {
            errors.log(Level.SEVERE, "Couldn't start server: " + ex.getMessage(), ex);
        }
    }
}