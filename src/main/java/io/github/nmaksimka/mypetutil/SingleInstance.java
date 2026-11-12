package io.github.nmaksimka.mypetutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Не даёт запустить программу дважды. Второй запуск просто просит первый
 * открыть окно настроек и завершается.
 */
final class SingleInstance {

    private static final Logger LOG = Logger.getLogger(SingleInstance.class.getName());

    // держим ссылки, иначе блокировка файла может пропасть вместе со сборкой мусора
    private static FileChannel channel;
    private static FileLock lock;
    private static ServerSocket server;

    private SingleInstance() {
    }

    /**
     * @param onShowRequest что сделать, когда кто-то запустил программу ещё раз
     * @return true — мы первые и работаем дальше; false — программа уже запущена
     */
    static boolean acquire(Path dir, Runnable onShowRequest) {
        try {
            Files.createDirectories(dir);
            channel = FileChannel.open(dir.resolve("instance.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null) {
                askRunningToShow(dir);
                return false;
            }
            server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
            Files.writeString(dir.resolve("instance.port"), Integer.toString(server.getLocalPort()));
            Thread thread = new Thread(() -> listen(onShowRequest), "single-instance");
            thread.setDaemon(true);
            thread.start();
            return true;
        } catch (IOException e) {
            // если что-то пошло не так — лучше запуститься, чем не запуститься совсем
            LOG.log(Level.WARNING, "single instance check failed", e);
            return true;
        }
    }

    private static void listen(Runnable onShowRequest) {
        while (!server.isClosed()) {
            try (Socket socket = server.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                if ("show".equals(reader.readLine())) {
                    onShowRequest.run();
                }
            } catch (IOException e) {
                if (!server.isClosed()) {
                    LOG.log(Level.FINE, "single instance socket error", e);
                }
            }
        }
    }

    private static void askRunningToShow(Path dir) {
        try {
            int port = Integer.parseInt(Files.readString(dir.resolve("instance.port")).trim());
            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
                 OutputStream out = socket.getOutputStream()) {
                out.write("show\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | NumberFormatException e) {
            LOG.log(Level.FINE, "cannot reach running instance", e);
        }
    }

    static void release() {
        try {
            if (server != null) {
                server.close();
            }
            if (lock != null) {
                lock.release();
            }
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "release failed", e);
        }
    }
}
