package server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.Protocol;
import server.model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientHandler implements Runnable {

    private static final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final Socket socket;
    private final ServerContext ctx;

    private User currentUser;
    private String viewingClubId;

    public ClientHandler(Socket socket, ServerContext ctx) {
        this.socket = socket;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Map<String, Object> response;
                try {
                    Map<String, Object> request = gson.fromJson(line, MAP_TYPE);
                    response = dispatch(request);
                } catch (Exception e) {
                    response = error("Malformed request: " + e.getMessage());
                }
                out.println(gson.toJson(response));
            }
        } catch (IOException e) {
        } finally {
            handleDisconnect();
        }
    }

    private void handleDisconnect() {
        if (currentUser != null) {
            invalidateSession(currentUser);
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void invalidateSession(User u) {
        String token = u.getCurrentToken();
        if (token != null) ctx.store.getTokenToUsername().remove(token);
        u.setCurrentToken(null);
        u.setUdpAddress(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dispatch(Map<String, Object> request) {
        String command = getString(request, "command");
        String token = getString(request, "token");
        Map<String, Object> payload = (Map<String, Object>) request.get("payload");
        if (payload == null) payload = new LinkedHashMap<>();
        if (command == null) return error("Missing 'command' field.");

        if (!command.equals("register") && !command.equals("login")) {
            User sessionUser = resolveToken(token);
            if (sessionUser == null) {
                return unauthorized();
            }
            currentUser = sessionUser;
        }

        try {
            switch (command) {
                case "register": return cmdRegister(payload);
                case "login": return cmdLogin(payload);
                case "logout": return cmdLogout();

                case "account_charge": return cmdAccountCharge(payload);
                case "balance_show": return cmdBalanceShow();

                case "books_market_list": return cmdBooksMarketList();
                case "book_buy": return cmdBookBuy(payload);
                case "progress_submit": return cmdProgressSubmit(payload);

                case "library_read_not_list": return cmdLibraryList(BookProgress.Status.NOT_READ);
                case "library_reading_list": return cmdLibraryList(BookProgress.Status.READING);
                case "library_read_list": return cmdLibraryList(BookProgress.Status.READ);

                case "club_create": return cmdClubCreate(payload);
                case "clubs_list": return cmdClubsList();
                case "clubs_list_total": return cmdClubsListTotal();
                case "join": return cmdJoin(payload);
                case "accept_join_request": return cmdAcceptJoin(payload);
                case "deny_join_request": return cmdDenyJoin(payload);

                case "club_view": return cmdClubView(payload);
                case "club_exit": viewingClubId = null; return ok("Left club view mode.", null);

                case "members_club_list": return cmdMembersClubList();
                case "member_remove": return cmdMemberRemove(payload);
                case "fundraiser_create": return cmdFundraiserCreate(payload);
                case "progress_fundraiser_view": return cmdProgressFundraiserView();
                case "donate": return cmdDonate(payload);

                default: return error("Unknown command: " + command);
            }
        } catch (BusinessException be) {
            return error(be.getMessage());
        } catch (Exception e) {
            return error("Internal server error: " + e.getMessage());
        }
    }

    private User resolveToken(String token) {
        if (token == null) return null;
        String username = ctx.store.getTokenToUsername().get(token);
        if (username == null) return null;
        return ctx.store.getUsersByName().get(username);
    }

    private Map<String, Object> cmdRegister(Map<String, Object> payload) {
        String username = getString(payload, "username");
        String clientHash = getString(payload, "password");
        if (username == null || username.trim().isEmpty() || clientHash == null || clientHash.trim().isEmpty()) {
            return error("username and password are required.");
        }
        ReentrantReadWriteLock.ReadLock lock = ctx.store.getBackupLock().readLock();
        lock.lock();
        try {
            if (ctx.store.getUsersByName().containsKey(username)) {
                return error("Username already taken.");
            }
            String salt = Security.generateSalt();
            String storedHash = Security.saltAndHash(clientHash, salt);
            User user = new User(username, storedHash, salt);
            ctx.store.getUsersByName().put(username, user);
            return ok("Registration successful.", null);
        } finally {
            lock.unlock();
        }
    }

    private Map<String, Object> cmdLogin(Map<String, Object> payload) {
        String username = getString(payload, "username");
        String clientHash = getString(payload, "password");
        Integer udpPort = getInt(payload, "udpPort");
        User user = ctx.store.getUsersByName().get(username);
        if (user == null) return unauthorized("invalid credentials");
        String check = Security.saltAndHash(clientHash, user.getSalt());
        if (!check.equals(user.getPasswordHash())) return unauthorized("invalid credentials");

        String token = Security.generateToken();
        user.setCurrentToken(token);
        ctx.store.getTokenToUsername().put(token, username);
        if (udpPort != null) {
            InetAddress addr = socket.getInetAddress();
            user.setUdpAddress(new InetSocketAddress(addr, udpPort));
        }
        currentUser = user;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("balance", user.getBalance());
        return ok("Login successful.", data);
    }

    private Map<String, Object> cmdLogout() {
        invalidateSession(currentUser);
        currentUser = null;
        viewingClubId = null;
        return ok("Logged out.", null);
    }

    private Map<String, Object> cmdAccountCharge(Map<String, Object> payload) {
        Double amount = getDouble(payload, "amount");
        if (amount == null || amount <= 0) return error("Charge amount must be a positive number.");
        currentUser.charge(amount);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("balance", currentUser.getBalance());
        return ok("Account charged.", data);
    }

    private Map<String, Object> cmdBalanceShow() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("balance", currentUser.getBalance());
        return ok("ok", data);
    }

    private Map<String, Object> cmdBooksMarketList() {
        List<Object> arr = new ArrayList<>();
        for (Book b : ctx.catalog.all()) arr.add(bookToJson(b));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("books", arr);
        return ok("ok", data);
    }

    private Map<String, Object> bookToJson(Book b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("title", b.getTitle());
        m.put("author", b.getAuthor());
        m.put("pages", b.getPages());
        m.put("genre", b.getGenre());
        m.put("year", b.getYear());
        m.put("price", b.getPrice());
        return m;
    }

    private Map<String, Object> cmdBookBuy(Map<String, Object> payload) {
        String bookId = getString(payload, "bookId");
        if (bookId == null) return error("bookId is required.");
        Book book = ctx.catalog.get(bookId);
        if (book == null) return error("No such book in the market.");

        ReentrantReadWriteLock.ReadLock lock = ctx.store.getBackupLock().readLock();
        lock.lock();
        try {
            synchronized (currentUser) {
                if (currentUser.ownsBook(bookId)) return error("You already own this book.");
                if (!currentUser.debit(book.getPrice())) return error("Insufficient balance.");
                BookProgress progress = new BookProgress(bookId);
                currentUser.getLibrary().put(bookId, progress);
            }
        } finally {
            lock.unlock();
        }

        FundraiserEngine.onMemberAcquiredBookIndependently(ctx, currentUser, bookId, book.getPrice());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("balance", currentUser.getBalance());
        return ok("Purchase successful. Book added to your unread library.", data);
    }

    private Map<String, Object> cmdProgressSubmit(Map<String, Object> payload) {
        String bookId = getString(payload, "bookId");
        Integer page = getInt(payload, "page");
        if (bookId == null || page == null) return error("bookId and page are required.");

        BookProgress progress = currentUser.getLibrary().get(bookId);
        if (progress == null) return error("This book is not in your library.");

        Book book = ctx.catalog.get(bookId);
        if (book == null) return error("Book no longer exists in the catalog.");
        if (page < 0 || page > book.getPages()) {
            return error("Invalid page number. Must be between 0 and " + book.getPages() + ".");
        }
        if (page == book.getPages() && book.isUnfinishable()) {
            return error("This book (Faust) can never be marked as fully read.");
        }

        synchronized (currentUser) {
            progress.setLastPage(page);
            if (page == 0) progress.setStatus(BookProgress.Status.NOT_READ);
            else if (page == book.getPages()) progress.setStatus(BookProgress.Status.READ);
            else progress.setStatus(BookProgress.Status.READING);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", progress.getStatus().name());
        return ok("Progress updated.", data);
    }

    private Map<String, Object> cmdLibraryList(BookProgress.Status status) {
        List<Object> arr = new ArrayList<>();
        for (BookProgress p : currentUser.getLibrary().values()) {
            if (p.getStatus() != status) continue;
            Book b = ctx.catalog.get(p.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bookId", p.getBookId());
            m.put("title", b != null ? b.getTitle() : "(unknown book)");
            m.put("lastPage", p.getLastPage());
            arr.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("books", arr);
        return ok("ok", data);
    }

    private Map<String, Object> cmdClubCreate(Map<String, Object> payload) {
        String name = getString(payload, "name");
        if (name == null || name.trim().isEmpty()) return error("Club name is required.");
        Club club = new Club(ctx.store.nextClubId(), name, currentUser.getUsername());
        ctx.store.getClubsById().put(club.getId(), club);
        currentUser.getOwnedClubIds().add(club.getId());
        currentUser.getMemberClubIds().add(club.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clubId", club.getId());
        return ok("Club created.", data);
    }

    private Map<String, Object> cmdClubsList() {
        List<Object> arr = new ArrayList<>();
        for (String id : currentUser.getMemberClubIds()) {
            Club c = ctx.store.getClubsById().get(id);
            if (c != null) arr.add(clubSummary(c));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clubs", arr);
        return ok("ok", data);
    }

    private Map<String, Object> cmdClubsListTotal() {
        List<Object> arr = new ArrayList<>();
        for (Club c : ctx.store.getClubsById().values()) arr.add(clubSummary(c));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clubs", arr);
        return ok("ok", data);
    }

    private Map<String, Object> clubSummary(Club c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("owner", c.getOwnerUsername());
        m.put("memberCount", c.getMembers().size());
        return m;
    }

    private Map<String, Object> cmdJoin(Map<String, Object> payload) {
        String clubId = getString(payload, "clubId");
        Club club = requireClub(clubId);
        if (club.isMember(currentUser.getUsername())) return error("You are already a member of this club.");
        club.getPendingJoinRequests().add(currentUser.getUsername());

        User owner = ctx.store.getUsersByName().get(club.getOwnerUsername());
        if (owner != null) {
            Map<String, Object> notif = new LinkedHashMap<>();
            notif.put("clubId", club.getId());
            notif.put("clubName", club.getName());
            notif.put("username", currentUser.getUsername());
            ctx.notifier.notifyUser(owner, Protocol.NOTIF_JOIN_REQUEST, notif);
        }
        return ok("Join request sent.", null);
    }

    private Map<String, Object> cmdAcceptJoin(Map<String, Object> payload) {
        String clubId = getString(payload, "clubId");
        String requestingUser = getString(payload, "userId");
        Club club = requireClub(clubId);
        requireOwner(club);
        if (!club.getPendingJoinRequests().remove(requestingUser)) {
            return error("No pending join request from this user.");
        }
        club.getMembers().add(requestingUser);
        User u = ctx.store.getUsersByName().get(requestingUser);
        if (u != null) {
            u.getMemberClubIds().add(clubId);
            Map<String, Object> notif = new LinkedHashMap<>();
            notif.put("clubId", clubId);
            notif.put("clubName", club.getName());
            notif.put("accepted", true);
            ctx.notifier.notifyUser(u, Protocol.NOTIF_JOIN_RESULT, notif);
        }
        return ok("User accepted into the club.", null);
    }

    private Map<String, Object> cmdDenyJoin(Map<String, Object> payload) {
        String clubId = getString(payload, "clubId");
        String requestingUser = getString(payload, "userId");
        Club club = requireClub(clubId);
        requireOwner(club);
        if (!club.getPendingJoinRequests().remove(requestingUser)) {
            return error("No pending join request from this user.");
        }
        User u = ctx.store.getUsersByName().get(requestingUser);
        if (u != null) {
            Map<String, Object> notif = new LinkedHashMap<>();
            notif.put("clubId", clubId);
            notif.put("clubName", club.getName());
            notif.put("accepted", false);
            ctx.notifier.notifyUser(u, Protocol.NOTIF_JOIN_RESULT, notif);
        }
        return ok("Join request denied.", null);
    }

    private Map<String, Object> cmdClubView(Map<String, Object> payload) {
        String clubId = getString(payload, "clubId");
        Club club = requireClub(clubId);
        if (!club.isMember(currentUser.getUsername())) return error("You must be a member of this club to view it.");
        viewingClubId = clubId;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clubId", club.getId());
        data.put("name", club.getName());
        data.put("owner", club.getOwnerUsername());
        return ok("Entered club view mode.", data);
    }

    private Club requireViewedClub() {
        if (viewingClubId == null) {
            throw new BusinessException("You must run club_view(clubId) first.");
        }
        return requireClub(viewingClubId);
    }

    private Map<String, Object> cmdMembersClubList() {
        Club club = requireViewedClub();
        List<Object> arr = new ArrayList<>();
        for (String username : club.getMembers()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", username);
            m.put("owner", club.isOwner(username));
            arr.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("members", arr);
        return ok("ok", data);
    }

    private Map<String, Object> cmdMemberRemove(Map<String, Object> payload) {
        Club club = requireViewedClub();
        requireOwner(club);
        String userId = getString(payload, "userId");
        if (club.isOwner(userId)) return error("The owner cannot be removed from their own club.");
        if (!club.getMembers().remove(userId)) return error("This user is not a member of the club.");
        User u = ctx.store.getUsersByName().get(userId);
        if (u != null) {
            u.getMemberClubIds().remove(club.getId());
            Map<String, Object> notif = new LinkedHashMap<>();
            notif.put("clubId", club.getId());
            notif.put("clubName", club.getName());
            ctx.notifier.notifyUser(u, Protocol.NOTIF_MEMBER_REMOVED, notif);
        }
        return ok("Member removed.", null);
    }

    private Map<String, Object> cmdFundraiserCreate(Map<String, Object> payload) {
        Club club = requireViewedClub();
        String bookId = getString(payload, "bookId");
        Book book = ctx.catalog.get(bookId);
        if (book == null) return error("No such book in the market.");
        if (club.getActiveFundraiser() != null && club.getActiveFundraiser().getStatus() == Fundraiser.Status.ACTIVE) {
            return error("This club already has an active fundraiser.");
        }

        Set<String> eligible = new LinkedHashSet<>();
        for (String username : club.getMembers()) {
            User u = ctx.store.getUsersByName().get(username);
            if (u != null && !u.ownsBook(bookId)) eligible.add(username);
        }
        double target = eligible.size() * book.getPrice();
        Fundraiser f = new Fundraiser(ctx.store.nextFundraiserId(), club.getId(), bookId, currentUser.getUsername(), target, eligible);
        club.setActiveFundraiser(f);
        club.getFundraiserHistory().add(f);

        Map<String, Object> notif = new LinkedHashMap<>();
        notif.put("clubId", club.getId());
        notif.put("bookTitle", book.getTitle());
        notif.put("creator", currentUser.getUsername());
        notif.put("targetAmount", target);
        for (String username : club.getMembers()) {
            User u = ctx.store.getUsersByName().get(username);
            if (u != null) ctx.notifier.notifyUser(u, Protocol.NOTIF_FUNDRAISER_CREATED, notif);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fundraiserId", f.getId());
        data.put("targetAmount", target);
        return ok("Fundraiser created.", data);
    }

    private Map<String, Object> cmdProgressFundraiserView() {
        Club club = requireViewedClub();
        Fundraiser f = club.getActiveFundraiser();
        if (f == null) return error("There is no active fundraiser in this club.");
        Map<String, Object> data = FundraiserEngine.toJson(ctx, f);
        return ok("ok", data);
    }

    private Map<String, Object> cmdDonate(Map<String, Object> payload) {
        Club club = requireViewedClub();
        Double amount = getDouble(payload, "amount");
        if (amount == null || amount <= 0) return error("Donation amount must be a positive number.");
        Fundraiser f = club.getActiveFundraiser();
        if (f == null || f.getStatus() != Fundraiser.Status.ACTIVE) return error("There is no active fundraiser in this club.");

        return FundraiserEngine.donate(ctx, club, f, currentUser, amount);
    }

    private Club requireClub(String clubId) {
        if (clubId == null) throw new BusinessException("clubId is required.");
        Club c = ctx.store.getClubsById().get(clubId);
        if (c == null) throw new BusinessException("No such club.");
        return c;
    }

    private void requireOwner(Club club) {
        if (!club.isOwner(currentUser.getUsername())) {
            throw new BusinessException("Only the club owner can perform this action.");
        }
    }

    private static Map<String, Object> ok(String message, Map<String, Object> data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", Protocol.STATUS_OK);
        resp.put("message", message);
        resp.put("data", data == null ? new LinkedHashMap<>() : data);
        return resp;
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", Protocol.STATUS_ERROR);
        resp.put("message", message);
        resp.put("data", new LinkedHashMap<>());
        return resp;
    }

    private static Map<String, Object> unauthorized() {
        return unauthorized("invalid or expired token");
    }

    private static Map<String, Object> unauthorized(String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", Protocol.STATUS_UNAUTHORIZED);
        resp.put("message", message);
        resp.put("data", new LinkedHashMap<>());
        return resp;
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Double getDouble(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    static class BusinessException extends RuntimeException {
        BusinessException(String msg) { super(msg); }
    }
}
