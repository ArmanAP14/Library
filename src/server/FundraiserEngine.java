package server;

import common.Protocol;
import server.model.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FundraiserEngine {
    private FundraiserEngine() {}

    public static Map<String, Object> donate(ServerContext ctx, Club club, Fundraiser f, User donor, double requestedAmount) {
        ReentrantReadWriteLock.ReadLock backupLock = ctx.store.getBackupLock().readLock();
        backupLock.lock();
        try {
            synchronized (f) {
                if (f.getStatus() != Fundraiser.Status.ACTIVE) {
                    return errorResp("This fundraiser is no longer active.");
                }
                double remaining = f.getTargetAmount() - f.getCurrentAmount();
                if (remaining <= 0) {
                    return errorResp("This fundraiser's target has already been reached.");
                }
                double actualDonation = Math.min(requestedAmount, remaining);

                synchronized (donor) {
                    if (!donor.debit(actualDonation)) {
                        return errorResp("Insufficient balance.");
                    }
                }
                f.addToCurrentAmount(actualDonation);
                f.getDonations().add(new Donation(donor.getUsername(), actualDonation));

                broadcastToClub(ctx, club, Protocol.NOTIF_DONATION, donationNotif(f, donor, actualDonation));

                if (f.getCurrentAmount() >= f.getTargetAmount()) {
                    completeFundraiser(ctx, club, f);
                }

                Map<String, Object> data = toJson(ctx, f);
                data.put("actualDonation", actualDonation);
                return okResp("Donation recorded.", data);
            }
        } finally {
            backupLock.unlock();
        }
    }

    public static void onMemberAcquiredBookIndependently(ServerContext ctx, User buyer, String bookId, double price) {
        ReentrantReadWriteLock.ReadLock backupLock = ctx.store.getBackupLock().readLock();
        backupLock.lock();
        try {
            for (String clubId : buyer.getMemberClubIds()) {
                Club club = ctx.store.getClubsById().get(clubId);
                if (club == null) continue;
                Fundraiser f = club.getActiveFundraiser();
                if (f == null || f.getStatus() != Fundraiser.Status.ACTIVE) continue;
                if (!f.getBookId().equals(bookId)) continue;

                synchronized (f) {
                    if (f.getStatus() != Fundraiser.Status.ACTIVE) continue;
                    if (!f.getEligibleMembers().remove(buyer.getUsername())) continue;

                    f.setTargetAmount(f.getTargetAmount() - price);
                    if (f.getTargetAmount() < f.getCurrentAmount()) {
                        completeFundraiserWithRefund(ctx, club, f);
                    } else if (f.getCurrentAmount() >= f.getTargetAmount() && f.getTargetAmount() > 0) {
                        completeFundraiser(ctx, club, f);
                    }
                }
            }
        } finally {
            backupLock.unlock();
        }
    }

    private static void completeFundraiser(ServerContext ctx, Club club, Fundraiser f) {
        f.setStatus(Fundraiser.Status.COMPLETED);
        distributeBook(ctx, club, f);
    }

    private static void completeFundraiserWithRefund(ServerContext ctx, Club club, Fundraiser f) {
        double excess = f.getCurrentAmount() - f.getTargetAmount();
        f.setStatus(Fundraiser.Status.COMPLETED);
        if (excess > 0 && !f.getDonations().isEmpty()) {
            Donation last = f.getDonations().get(f.getDonations().size() - 1);
            User lastDonor = ctx.store.getUsersByName().get(last.getUsername());
            if (lastDonor != null) {
                synchronized (lastDonor) {
                    lastDonor.credit(excess);
                }
            }
            f.addToCurrentAmount(-excess);
        }
        distributeBook(ctx, club, f);
    }

    private static void distributeBook(ServerContext ctx, Club club, Fundraiser f) {
        Book book = ctx.catalog.get(f.getBookId());
        for (String username : new ArrayList<>(f.getEligibleMembers())) {
            User u = ctx.store.getUsersByName().get(username);
            if (u == null) continue;
            synchronized (u) {
                if (!u.ownsBook(f.getBookId())) {
                    u.getLibrary().put(f.getBookId(), new BookProgress(f.getBookId()));
                }
            }
            Map<String, Object> notif = new LinkedHashMap<>();
            notif.put("clubId", club.getId());
            notif.put("bookId", f.getBookId());
            notif.put("bookTitle", book != null ? book.getTitle() : f.getBookId());
            ctx.notifier.notifyUser(u, Protocol.NOTIF_FUNDRAISER_COMPLETED, notif);
        }
    }

    private static void broadcastToClub(ServerContext ctx, Club club, String command, Map<String, Object> payload) {
        for (String username : club.getMembers()) {
            User u = ctx.store.getUsersByName().get(username);
            if (u != null) ctx.notifier.notifyUser(u, command, payload);
        }
    }

    private static Map<String, Object> donationNotif(Fundraiser f, User donor, double amount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clubId", f.getClubId());
        m.put("bookId", f.getBookId());
        m.put("donor", donor.getUsername());
        m.put("amount", amount);
        m.put("currentAmount", f.getCurrentAmount());
        m.put("targetAmount", f.getTargetAmount());
        return m;
    }

    public static Map<String, Object> toJson(ServerContext ctx, Fundraiser f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fundraiserId", f.getId());
        m.put("bookId", f.getBookId());
        Book b = ctx.catalog.get(f.getBookId());
        m.put("bookTitle", b != null ? b.getTitle() : f.getBookId());
        m.put("status", f.getStatus().name());
        m.put("targetAmount", f.getTargetAmount());
        m.put("currentAmount", f.getCurrentAmount());
        List<Object> donations = new ArrayList<>();
        for (Donation d : f.getDonations()) {
            Map<String, Object> dm = new LinkedHashMap<>();
            dm.put("username", d.getUsername());
            dm.put("amount", d.getAmount());
            dm.put("timestamp", d.getTimestampEpochMillis());
            donations.add(dm);
        }
        m.put("donations", donations);
        return m;
    }

    private static Map<String, Object> okResp(String message, Map<String, Object> data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", Protocol.STATUS_OK);
        resp.put("message", message);
        resp.put("data", data != null ? data : new LinkedHashMap<>());
        return resp;
    }

    private static Map<String, Object> errorResp(String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", Protocol.STATUS_ERROR);
        resp.put("message", message);
        resp.put("data", new LinkedHashMap<>());
        return resp;
    }
}
