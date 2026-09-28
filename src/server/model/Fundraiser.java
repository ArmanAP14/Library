package server.model;

import java.io.Serializable;
import java.util.*;

public class Fundraiser implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { ACTIVE, COMPLETED, CLOSED }

    private final String id;
    private final String clubId;
    private final String bookId;
    private final String creatorUsername;
    private double targetAmount;
    private double currentAmount;
    private Status status;
    private final List<Donation> donations = new ArrayList<>();

    private final Set<String> eligibleMembers = new LinkedHashSet<>();

    public Fundraiser(String id, String clubId, String bookId, String creatorUsername, double targetAmount, Set<String> eligibleMembers) {
        this.id = id;
        this.clubId = clubId;
        this.bookId = bookId;
        this.creatorUsername = creatorUsername;
        this.targetAmount = targetAmount;
        this.currentAmount = 0.0;
        this.status = Status.ACTIVE;
        this.eligibleMembers.addAll(eligibleMembers);
    }

    public String getId() { return id; }
    public String getClubId() { return clubId; }
    public String getBookId() { return bookId; }
    public String getCreatorUsername() { return creatorUsername; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public double getCurrentAmount() { return currentAmount; }
    public void addToCurrentAmount(double delta) { this.currentAmount += delta; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<Donation> getDonations() { return donations; }
    public Set<String> getEligibleMembers() { return eligibleMembers; }
}
