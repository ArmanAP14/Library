package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LibraryClient {

    private static final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private Socket tcp;
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private DatagramSocket udpSocket;
    private NotificationListener listener;

    private String token;
    private String currentClubId;
    private Consumer<String> notificationHandler = msg -> {};

    public synchronized void connect(String host, int port) throws IOException {
        if (isConnected()) disconnect();
        tcp = new Socket(host, port);
        tcpIn = new BufferedReader(new InputStreamReader(tcp.getInputStream(), "UTF-8"));
        tcpOut = new PrintWriter(new OutputStreamWriter(tcp.getOutputStream(), "UTF-8"), true);
        udpSocket = new DatagramSocket(0);
        listener = new NotificationListener(udpSocket, notificationHandler);
        listener.start();
    }

    public synchronized void disconnect() {
        try { if (listener != null) listener.stopListening(); } catch (Exception ignored) {}
        try { if (udpSocket != null) udpSocket.close(); } catch (Exception ignored) {}
        try { if (tcp != null) tcp.close(); } catch (Exception ignored) {}
        listener = null;
        udpSocket = null;
        tcpIn = null;
        tcpOut = null;
        tcp = null;
        token = null;
        currentClubId = null;
    }

    public boolean isConnected() {
        return tcp != null && tcp.isConnected() && !tcp.isClosed();
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public String getCurrentClubId() {
        return currentClubId;
    }

    public void setNotificationHandler(Consumer<String> handler) {
        notificationHandler = handler != null ? handler : msg -> {};
        if (listener != null) listener.setHandler(notificationHandler);
    }

    public Map<String, Object> register(String username, String password) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", PasswordHasher.sha256Hex(password));
        return send("register", payload);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> login(String username, String password) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", PasswordHasher.sha256Hex(password));
        payload.put("udpPort", udpSocket.getLocalPort());
        Map<String, Object> resp = send("login", payload);
        if ("ok".equals(resp.get("status"))) {
            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            token = getString(data, "token");
        }
        return resp;
    }

    public Map<String, Object> logout() throws IOException {
        Map<String, Object> resp = send("logout", new LinkedHashMap<>());
        token = null;
        currentClubId = null;
        return resp;
    }

    public Map<String, Object> chargeAccount(double amount) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amount);
        return send("account_charge", payload);
    }

    public Map<String, Object> showBalance() throws IOException {
        return send("balance_show", new LinkedHashMap<>());
    }

    public Map<String, Object> listMarketBooks() throws IOException {
        return send("books_market_list", new LinkedHashMap<>());
    }

    public Map<String, Object> buyBook(String bookId) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookId", bookId);
        return send("book_buy", payload);
    }

    public Map<String, Object> submitProgress(String bookId, int page) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookId", bookId);
        payload.put("page", page);
        return send("progress_submit", payload);
    }

    public Map<String, Object> listLibraryUnread() throws IOException {
        return send("library_read_not_list", new LinkedHashMap<>());
    }

    public Map<String, Object> listLibraryReading() throws IOException {
        return send("library_reading_list", new LinkedHashMap<>());
    }

    public Map<String, Object> listLibraryRead() throws IOException {
        return send("library_read_list", new LinkedHashMap<>());
    }

    public Map<String, Object> createClub(String name) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        return send("club_create", payload);
    }

    public Map<String, Object> listMyClubs() throws IOException {
        return send("clubs_list", new LinkedHashMap<>());
    }

    public Map<String, Object> listAllClubs() throws IOException {
        return send("clubs_list_total", new LinkedHashMap<>());
    }

    public Map<String, Object> joinClub(String clubId) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clubId", clubId);
        return send("join", payload);
    }

    public Map<String, Object> acceptJoinRequest(String userId, String clubId) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("clubId", clubId);
        return send("accept_join_request", payload);
    }

    public Map<String, Object> denyJoinRequest(String userId, String clubId) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("clubId", clubId);
        return send("deny_join_request", payload);
    }

    public Map<String, Object> viewClub(String clubId) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clubId", clubId);
        Map<String, Object> resp = send("club_view", payload);
        if ("ok".equals(resp.get("status"))) currentClubId = clubId;
        return resp;
    }

    public void exitClubView() {
        currentClubId = null;
    }

    public Map<String, Object> listClubMembers() throws IOException {
        return send("members_club_list", new LinkedHashMap<>());
    }

    public Map<String, Object> removeMember(String userId) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        return send("member_remove", payload);
    }

    public Map<String, Object> createFundraiser(String bookId) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookId", bookId);
        return send("fundraiser_create", payload);
    }

    public Map<String, Object> viewFundraiserProgress() throws IOException {
        return send("progress_fundraiser_view", new LinkedHashMap<>());
    }

    public Map<String, Object> donate(double amount) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amount);
        return send("donate", payload);
    }

    public synchronized Map<String, Object> send(String command, Map<String, Object> payload) throws IOException {
        if (!isConnected()) throw new IOException("Not connected to server.");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("command", command);
        if (token != null) request.put("token", token);
        request.put("payload", payload);

        tcpOut.println(gson.toJson(request));
        String responseLine = tcpIn.readLine();
        if (responseLine == null) {
            disconnect();
            throw new IOException("Connection closed by server.");
        }
        return gson.fromJson(responseLine, MAP_TYPE);
    }

    public static String formatResponse(Map<String, Object> resp) {
        String status = getString(resp, "status");
        String message = getString(resp, "message");
        Object data = resp.get("data");
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(status).append("] ").append(message);
        if (data instanceof Map && !((Map<String, Object>) data).isEmpty()) {
            sb.append("\n").append(gson.toJson(data));
        }
        return sb.toString();
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
