package server.model;

import java.io.Serializable;
import java.time.Instant;

public class Donation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final double amount;
    private final long timestampEpochMillis;

    public Donation(String username, double amount) {
        this.username = username;
        this.amount = amount;
        this.timestampEpochMillis = Instant.now().toEpochMilli();
    }

    public String getUsername() { return username; }
    public double getAmount() { return amount; }
    public long getTimestampEpochMillis() { return timestampEpochMillis; }
}
