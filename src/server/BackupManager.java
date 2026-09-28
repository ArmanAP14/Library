package server;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class BackupManager {

    public static final long BACKUP_INTERVAL_MINUTES = 5;

    private final Path backupFile;
    private final Path tmpFile;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "backup-thread");
        t.setDaemon(true);
        return t;
    });

    public BackupManager(String backupFilePath) {
        this.backupFile = Paths.get(backupFilePath);
        this.tmpFile = Paths.get(backupFilePath + ".tmp");
    }

    /** Loads state from disk, or returns a brand-new empty DataStore if no backup exists yet. */
    public DataStore loadOrCreate() {
        if (Files.exists(backupFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(backupFile)))) {
                DataStore store = (DataStore) ois.readObject();
                store.initTransients();
                System.out.println("[backup] Restored previous state from " + backupFile.toAbsolutePath());
                return store;
            } catch (Exception e) {
                System.err.println("[backup] Failed to read existing backup (" + e.getMessage() + "). Starting with empty state.");
            }
        }
        System.out.println("[backup] No backup file found. Starting with empty state.");
        DataStore fresh = new DataStore();
        fresh.initTransients();
        return fresh;
    }

    public void startScheduledBackups(DataStore store) {
        scheduler.scheduleAtFixedRate(() -> safeBackup(store), BACKUP_INTERVAL_MINUTES, BACKUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
        System.out.println("[backup] Scheduled automatic backup every " + BACKUP_INTERVAL_MINUTES + " minute(s).");
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /** Performs one backup pass, taking the exclusive write lock so nothing mutates state mid-snapshot. */
    public void safeBackup(DataStore store) {
        ReentrantReadWriteLock.WriteLock lock = store.getBackupLock().writeLock();
        lock.lock();
        try {
            writeAtomically(store);
            System.out.println("[backup] Snapshot written at " + java.time.LocalDateTime.now());
        } catch (IOException e) {
            System.err.println("[backup] Backup failed: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void writeAtomically(DataStore store) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpFile)))) {
            oos.writeObject(store);
            oos.flush();
        }
        try {
            Files.move(tmpFile, backupFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmpFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
