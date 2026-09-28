package server.model;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.*;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String passwordHash;
    private final String salt;
    private double balance;

    private final Map<String, BookProgress> library = new LinkedHashMap<>();
    private final Set<String> ownedClubIds = new LinkedHashSet<>();
    private final Set<String> memberClubIds = new LinkedHashSet<>();

    private transient String currentToken;
    private transient InetSocketAddress udpAddress;
    private transient Object handlerRef;

    public User(String username, String passwordHash, String salt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.balance = 0.0;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public double getBalance() { return balance; }

    public synchronized void charge(double amount) { this.balance += amount; }
    public synchronized boolean debit(double amount) {
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }
    public synchronized void credit(double amount) { this.balance += amount; }

    public Map<String, BookProgress> getLibrary() { return library; }
    public Set<String> getOwnedClubIds() { return ownedClubIds; }
    public Set<String> getMemberClubIds() { return memberClubIds; }

    public boolean ownsBook(String bookId) { return library.containsKey(bookId); }

    public String getCurrentToken() { return currentToken; }
    public void setCurrentToken(String currentToken) { this.currentToken = currentToken; }

    public InetSocketAddress getUdpAddress() { return udpAddress; }
    public void setUdpAddress(InetSocketAddress udpAddress) { this.udpAddress = udpAddress; }

    public boolean isOnline() { return currentToken != null; }
}
