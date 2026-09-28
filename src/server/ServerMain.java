package server;

import common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        int tcpPort = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_TCP_PORT;
        String booksPath = args.length > 1 ? args[1] : "books.json";
        String backupPath = args.length > 2 ? args[2] : "data/backup.ser";

        System.out.println("=== Book Club Server starting up ===");

        BookCatalog catalog;
        try {
            catalog = BookCatalog.loadFrom(booksPath);
            System.out.println("[catalog] Loaded " + catalog.all().size() + " books from " + booksPath);
        } catch (IOException e) {
            System.err.println("[catalog] Could not read " + booksPath + ": " + e.getMessage());
            System.err.println("[catalog] Starting with an EMPTY catalog. Place your books.json next to the server and restart.");
            catalog = BookCatalog.loadFrom(writeEmptyCatalogFallback(booksPath));
        }

        new java.io.File("data").mkdirs();
        BackupManager backupManager = new BackupManager(backupPath);
        DataStore store = backupManager.loadOrCreate();

        NotificationService notifier = new NotificationService();
        backupManager.startScheduledBackups(store);

        ServerContext ctx = new ServerContext(store, catalog, notifier, backupManager);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[shutdown] Taking final backup before exit...");
            backupManager.safeBackup(store);
            notifier.close();
            System.out.println("[shutdown] Done. Bye!");
        }));

        Thread adminConsole = new Thread(() -> {
            try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = console.readLine()) != null) {
                    if (line.trim().equalsIgnoreCase("shutdown")) {
                        System.out.println("[admin] Shutdown requested.");
                        System.exit(0);
                    } else if (line.trim().equalsIgnoreCase("backup")) {
                        backupManager.safeBackup(store);
                    }
                }
            } catch (IOException ignored) {}
        }, "admin-console");
        adminConsole.setDaemon(true);
        adminConsole.start();

        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("[net] Listening for TCP connections on port " + tcpPort);
            System.out.println("=== Server ready. Type 'shutdown' to stop, 'backup' to force a backup now. ===");
            while (true) {
                Socket client = serverSocket.accept();
                pool.submit(new ClientHandler(client, ctx));
            }
        }
    }

    private static String writeEmptyCatalogFallback(String originalPath) throws IOException {
        String fallback = originalPath + ".empty-fallback.json";
        java.nio.file.Files.write(java.nio.file.Paths.get(fallback), "[]".getBytes("UTF-8"));
        return fallback;
    }
}
