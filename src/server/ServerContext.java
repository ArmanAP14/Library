package server;

public class ServerContext {
    public final DataStore store;
    public final BookCatalog catalog;
    public final NotificationService notifier;
    public final BackupManager backupManager;

    public ServerContext(DataStore store, BookCatalog catalog, NotificationService notifier, BackupManager backupManager) {
        this.store = store;
        this.catalog = catalog;
        this.notifier = notifier;
        this.backupManager = backupManager;
    }
}
