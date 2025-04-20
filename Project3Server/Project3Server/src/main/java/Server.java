import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
	private static final int PORT = 12345;
	private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

	public static void main(String[] args) throws IOException {
		System.out.println("Server starting on port " + PORT + "...");
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			while (true) {
				Socket sock = serverSocket.accept();
				ClientHandler handler = new ClientHandler(sock);
				clients.add(handler);
				new Thread(handler).start();
			}
		}
	}

	private static void broadcast(Message msg) {
		for (ClientHandler ch : clients) {
			ch.send(msg);
		}
	}

	private static class ClientHandler implements Runnable {
		private Socket socket;
		private ObjectInputStream in;
		private ObjectOutputStream out;
		private String username;

		public ClientHandler(Socket socket) {
			this.socket = socket;
		}

		public void send(Message msg) {
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException e) {
				// ignore or log
			}
		}

		@Override
		public void run() {
			try {
				// Streams: output first
				out = new ObjectOutputStream(socket.getOutputStream());
				in  = new ObjectInputStream(socket.getInputStream());

				// --- LOGIN HANDSHAKE ---
				Message login = (Message) in.readObject();
				if (login.getType() != MessageType.LOGIN) {
					socket.close();
					return;
				}

				String requested = login.getSender();
				// Check for duplicate among already‐connected users
				boolean exists = false;
				for (ClientHandler ch : clients) {
					if (ch != this && requested.equals(ch.username)) {
						exists = true;
						break;
					}
				}

				if (exists) {
					// Inform the client that the username is taken
					send(new Message(
							UUID.randomUUID().toString(),
							MessageType.ERROR,
							"Username \"" + requested + "\" is already in use.",
							"SERVER",
							requested,
							System.currentTimeMillis()
					));
					socket.close();
					return;
				}

				// Accept the login
				username = requested;
				System.out.println(username + " connected.");
				broadcast(new Message(
						UUID.randomUUID().toString(),
						MessageType.CHAT,
						username + " joined the game lobby.",
						"SERVER",
						null,
						System.currentTimeMillis()
				));

				// --- MAIN LOOP ---
				while (true) {
					Message msg = (Message) in.readObject();
					switch (msg.getType()) {
						case CHAT:
							String target = msg.getRecipient();
							if (target != null && !target.isEmpty()) {
								// Private chat
								boolean found = false;
								for (ClientHandler ch : clients) {
									if (ch.username.equals(target)) {
										ch.send(msg);
										found = true;
										break;
									}
								}
								if (found) {
									// Echo to sender
									this.send(msg);
								} else {
									// Recipient not found
									this.send(new Message(
											UUID.randomUUID().toString(),
											MessageType.ERROR,
											"User \"" + target + "\" does not exist.",
											"SERVER",
											username,
											System.currentTimeMillis()
									));
								}
							} else {
								// Public chat
								broadcast(msg);
							}
							break;

						case MOVE:
							// Game‐move routing (example: broadcast to all)
							broadcast(msg);
							break;

						// handle other types...
						default:
							break;
					}
				}
			} catch (EOFException eof) {
				// client closed connection
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				clients.remove(this);
				if (username != null) {
					System.out.println(username + " disconnected.");
					broadcast(new Message(
							UUID.randomUUID().toString(),
							MessageType.CHAT,
							username + " left the game lobby.",
							"SERVER",
							null,
							System.currentTimeMillis()
					));
				}
				try { socket.close(); } catch (IOException ignored) {}
			}
		}
	}
}