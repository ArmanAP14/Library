package server;

import server.model.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataStore implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, User> usersByName = new ConcurrentHashMap<>();
    private final Map<String, Club> clubsById = new ConcurrentHashMap<>();

    private final AtomicInteger clubIdCounter = new AtomicInteger(1);
    private final AtomicInteger fundraiserIdCounter = new AtomicInteger(1);

    private transient Map<String, String> tokenToUsername = new ConcurrentHashMap<>();
    private transient ReentrantReadWriteLock backupLock = new ReentrantReadWriteLock();

    public Map<String, User> getUsersByName() { return usersByName; }
    public Map<String, Club> getClubsById() { return clubsById; }

    public String nextClubId() { return "club" + clubIdCounter.getAndIncrement(); }
    public String nextFundraiserId() { return "fund" + fundraiserIdCounter.getAndIncrement(); }

    public Map<String, String> getTokenToUsername() {
        if (tokenToUsername == null) tokenToUsername = new ConcurrentHashMap<>();
        return tokenToUsername;
    }

    public ReentrantReadWriteLock getBackupLock() {
        if (backupLock == null) backupLock = new ReentrantReadWriteLock();
        return backupLock;
    }

    public void initTransients() {
        this.tokenToUsername = new ConcurrentHashMap<>();
        this.backupLock = new ReentrantReadWriteLock();
        for (String id : clubsById.keySet()) {
            try {
                int n = Integer.parseInt(id.replace("club", ""));
                if (n >= clubIdCounter.get()) clubIdCounter.set(n + 1);
            } catch (NumberFormatException ignored) {}
        }
    }
}
