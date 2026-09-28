package server.model;

import java.io.Serializable;
import java.util.*;

public class Club implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String ownerUsername;
    private final Set<String> members = new LinkedHashSet<>();
    private final Set<String> pendingJoinRequests = new LinkedHashSet<>();


    private Fundraiser activeFundraiser;
    private final List<Fundraiser> fundraiserHistory = new ArrayList<>();

    public Club(String id, String name, String ownerUsername) {
        this.id = id;
        this.name = name;
        this.ownerUsername = ownerUsername;
        this.members.add(ownerUsername);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getOwnerUsername() { return ownerUsername; }
    public Set<String> getMembers() { return members; }
    public Set<String> getPendingJoinRequests() { return pendingJoinRequests; }

    public boolean isOwner(String username) { return ownerUsername.equals(username); }
    public boolean isMember(String username) { return members.contains(username); }

    public Fundraiser getActiveFundraiser() { return activeFundraiser; }
    public void setActiveFundraiser(Fundraiser f) { this.activeFundraiser = f; }
    public List<Fundraiser> getFundraiserHistory() { return fundraiserHistory; }
}
