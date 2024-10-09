package ru.panov;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.panov.util.PropertiesUtil.get;


/**
 * Класс TCPEchoClient реализует TCP-клиент, который подключается к TCP-серверу
 * и отправляет ему сообщения, получая ответы.
 * <p>
 * Клиент считывает вводимые пользователем сообщения в консоль и передает их серверу.
 */
public class TCPEchoClient {
    private static final Logger logger = Logger.getLogger(TCPEchoClient.class.getName());
    private final String host;
    private final int port;
    private Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;
    private Scanner scanner;

    public TCPEchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Устанавливает соединение с сервером и обрабатывает ввод пользователя.
     * Клиент ожидает ввода от пользователя и отправляет каждую введенную строку на сервер,
     * а затем выводит ответ сервера на экран.
     */
    public void startConnection() {
        try {
            clientSocket = new Socket(host, port);
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());
            scanner = new Scanner(System.in);
            logger.log(Level.INFO, "Connected to server at " + host + ":" + port);

            while (scanner.hasNext()) {
                System.out.println("Response from server: " + sendMessage(scanner.nextLine()));
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Client connection error: ", e);
        } finally {
            stopConnection();
        }
    }

    /**
     * Отправляет сообщение на сервер и получает ответ от него.
     *
     * @param msg сообщение, которое будет отправлено на сервер.
     * @return ответ сервера на отправленное сообщение.
     */
    public String sendMessage(String msg) throws IOException {
        out.writeUTF(msg);
        return in.readUTF();
    }

    /**
     * Закрывает соединение с сервером и освобождает ресурсы.
     */
    private void stopConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (scanner != null) scanner.close();
            if (clientSocket != null) clientSocket.close();
            logger.log(Level.INFO, "Connection closed");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing connection: ", e);
        }
    }

    /**
     * Точка входа программы. Создает клиент и запускает соединение с сервером,
     * используя параметры, указанные в конфигурационном файле.
     */
    public static void main(String[] args) {
        TCPEchoClient tcpEchoClient = new TCPEchoClient(get("tcp.host"),
                Integer.parseInt(get("tcp.port")));
        tcpEchoClient.startConnection();
    }

    /**
     * Класс NewClient1 представляет собой простой запускной класс для создания
     * экземпляра клиента {@link TCPEchoClient} и установления соединения с TCP-сервером.
     */
    public static class NewClient1 {
        public static void main(String[] args) {
            TCPEchoClient tcpEchoClient = new TCPEchoClient(get("tcp.host"),
                    Integer.parseInt(get("tcp.port")));
            tcpEchoClient.startConnection();
        }
    }

    /**
     * Класс NewClient2 представляет собой простой запускной класс для создания
     * экземпляра клиента {@link TCPEchoClient} и установления соединения с TCP-сервером.
     */
    public static class NewClient2 {
        public static void main(String[] args) {
            TCPEchoClient tcpEchoClient = new TCPEchoClient(get("tcp.host"),
                    Integer.parseInt(get("tcp.port")));
            tcpEchoClient.startConnection();
        }
    }
}