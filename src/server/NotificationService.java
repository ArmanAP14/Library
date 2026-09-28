package server;

import com.google.gson.Gson;
import server.model.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationService {

    private static final Gson gson = new Gson();
    private final DatagramSocket socket;

    public NotificationService() throws IOException {
        this.socket = new DatagramSocket();
    }

    public void notifyUser(User user, String command, Map<String, Object> payload) {
        InetSocketAddress addr = user.getUdpAddress();
        if (addr == null || !user.isOnline()) return;
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("command", command);
        msg.put("payload", payload);
        send(addr, msg);
    }

    private void send(InetSocketAddress addr, Map<String, Object> msg) {
        try {
            byte[] data = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, addr.getAddress(), addr.getPort());
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("[notify] Failed to push UDP notification to " + addr + ": " + e.getMessage());
        }
    }

    public void close() {
        socket.close();
    }
}
