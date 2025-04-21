package server;
import shared.Message;
import shared.MessageType;
import shared.User;
import shared.Room;
import shared.RoomManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Top‐level server: accepts connections and keeps track of all handlers.
 */
public class Server {
	private static final int PORT = 12345;
	private final Set<ConnectionHandler> clients = ConcurrentHashMap.newKeySet();

	public static void main(String[] args) throws IOException {
		new Server().start();
	}

	public void start() throws IOException {
		System.out.println("Server starting on port " + PORT + "...");
		try (ServerSocket ss = new ServerSocket(PORT)) {
			while (true) {
				Socket sock = ss.accept();
				ConnectionHandler handler = new ConnectionHandler(sock, this);
				clients.add(handler);
				new Thread(handler).start();
			}
		}
	}

	/** Broadcast to everyone _except_ the origin handler. */
	public void broadcastExcept(Message msg, ConnectionHandler exclude) {
		for (ConnectionHandler ch : clients) {
			if (ch != exclude) {
				ch.sendMessage(msg);
			}
		}
	}

	/** Broadcast to absolutely everyone. */
	public void broadcast(Message msg) {
		broadcastExcept(msg, null);
	}

	/**
	 * Remove a handler when its client disconnects.
	 */
	public void removeClient(ConnectionHandler ch) {
		clients.remove(ch);
	}

	/**
	 * Find a handler by username (or return null if not connected).
	 */
	public ConnectionHandler findByUsername(String username) {
		return clients.stream()
				.filter(ch -> ch.getUsername().equals(username))
				.findFirst()
				.orElse(null);
	}
}