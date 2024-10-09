package ru.panov;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.panov.util.PropertiesUtil.get;


/**
 * Класс TCPEchoServer реализует TCP-сервер, который принимает соединения от клиентов
 * и возвращает им те же данные, которые были получены.
 * <p>
 * Сервер прослушивает указанный порт и обрабатывает запросы от каждого клиента в отдельном потоке.
 */
public class TCPEchoServer {
    private static final Logger logger = Logger.getLogger(TCPEchoServer.class.getName());

    private final int port;

    private ServerSocket serverSocket;

    public TCPEchoServer(int port) {
        this.port = port;
    }

    /**
     * Запускает сервер, который принимает входящие соединения и обрабатывает их в отдельных потоках.
     * Когда сервер запущен, он прослушивает указанный порт и принимает новых клиентов, создавая для каждого
     * нового подключения поток {@link EchoClientHandler}.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            logger.log(Level.INFO, "Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new EchoClientHandler(clientSocket).start();
                logger.log(Level.INFO, "Client connected: " + clientSocket);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server exception: ", e);
        } finally {
            stop();
        }
    }

    /**
     * Останавливает сервер, закрывая все активные соединения и освобождая ресурсы.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                logger.log(Level.INFO, "Server stopped");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during server stop: ", e);
        }
    }

    /**
     * Класс EchoClientHandler представляет собой поток, который обрабатывает соединение с клиентом.
     * Он принимает сообщения от клиента и отправляет их обратно.
     */
    private static class EchoClientHandler extends Thread {
        private Socket socket;

        public EchoClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Запускает обработку клиентских запросов. Принимает сообщения от клиента и отправляет их обратно,
         * пока не будет получено сообщение "stop", после чего закрывает соединение.
         */
        public void run() {
            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream((socket.getInputStream()))) {
                String inputLine = in.readUTF();

                while (!"stop".equals(inputLine)) {
                    out.writeUTF(inputLine);
                    inputLine = in.readUTF();
                }

            } catch (IOException e) {
                logger.log(Level.WARNING, "Client handler exception: ", e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing client socket: ", e);
                }
            }
        }
    }

    /**
     * Точка входа программы. Создает и запускает сервер на порту, указанном в конфигурационном файле.
     */
    public static void main(String[] args) {
        TCPEchoServer server = new TCPEchoServer(Integer.parseInt(get("tcp.port")));
        server.start();
    }
}