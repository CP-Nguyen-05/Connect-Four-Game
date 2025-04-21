package shared;
import java.io.IOException;
import java.util.UUID;

/**
 * High‑level client logic: holds username, does LOGIN handshake,
 * and reads/sends Message objects on a background thread.
 */
public class Client extends Thread {
	public String username;
	private final String host;
	private final int    port;
	private ClientConnection conn;

	public Client() {
		this("localhost", 12345);
	}
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		try {
			// 1) Establish low‑level connection
			conn = new ClientConnection();
			conn.connect(host, port);

			// 2) Send LOGIN
			Message login = new Message(
					UUID.randomUUID().toString(),
					MessageType.LOGIN,
					"SERVER",            // no payload
					username,
					null,
					System.currentTimeMillis()
			);
			conn.sendMessage(login);

			// 3) Enter read loop
			while (true) {
				Message msg = conn.receiveMessage();
				// Here you might dispatch to a UI listener, e.g.:
				System.out.println(msg.toString());
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (IOException ignored) {}
		}
	}

	/**
	 * Convenience method for sending any Message.
	 */
	public void send(Message msg) {
		try {
			if (conn != null) {
				conn.sendMessage(msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Blocking read; returns the next incoming Message.
	 */
	public Message readMessage() throws IOException, ClassNotFoundException {
		return conn.receiveMessage();
	}
}