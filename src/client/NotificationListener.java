package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.Protocol;

import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.function.Consumer;

public class NotificationListener extends Thread {

    private static final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final DatagramSocket socket;
    private volatile boolean running = true;
    private volatile Consumer<String> handler;

    public NotificationListener(DatagramSocket socket, Consumer<String> handler) {
        super("udp-notification-listener");
        setDaemon(true);
        this.socket = socket;
        this.handler = handler != null ? handler : msg -> {};
    }

    public void setHandler(Consumer<String> handler) {
        this.handler = handler != null ? handler : msg -> {};
    }

    public void stopListening() { running = false; }

    @Override
    public void run() {
        byte[] buffer = new byte[8192];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String text = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                handle(text);
            } catch (Exception e) {
                if (running) {

                }
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handle(String text) {
        try {
            Map<String, Object> msg = gson.fromJson(text, MAP_TYPE);
            String command = getString(msg, "command");
            Map<String, Object> payload = (Map<String, Object>) msg.get("payload");
            handler.accept(describe(command, payload));
        } catch (Exception e) {
            handler.accept("(unparsable notification received)");
        }
    }

    private String describe(String command, Map<String, Object> p) {
        if (command == null) return text_raw(p);
        switch (command) {
            case Protocol.NOTIF_JOIN_REQUEST:
                return getString(p, "username") + " requested to join club '" + getString(p, "clubName") + "' (" + getString(p, "clubId") + ")";
            case Protocol.NOTIF_JOIN_RESULT:
                boolean accepted = Boolean.TRUE.equals(p.get("accepted"));
                return "Your request to join '" + getString(p, "clubName") + "' was " + (accepted ? "ACCEPTED" : "DENIED") + ".";
            case Protocol.NOTIF_MEMBER_REMOVED:
                return "You have been removed from club '" + getString(p, "clubName") + "'.";
            case Protocol.NOTIF_FUNDRAISER_CREATED:
                return "New fundraiser in your club for '" + getString(p, "bookTitle") + "' by " + getString(p, "creator")
                        + " (target: " + getString(p, "targetAmount") + ")";
            case Protocol.NOTIF_DONATION:
                return "User " + getString(p, "donor") + " donated " + getString(p, "amount") + " to the fundraiser for book " + getString(p, "bookId")
                        + " (" + getString(p, "currentAmount") + "/" + getString(p, "targetAmount") + ")";
            case Protocol.NOTIF_FUNDRAISER_COMPLETED:
                return "Book '" + getString(p, "bookTitle") + "' has been added to your library!";
            default:
                return command + " " + text_raw(p);
        }
    }

    private String text_raw(Map<String, Object> p) {
        return p == null ? "" : gson.toJson(p);
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
