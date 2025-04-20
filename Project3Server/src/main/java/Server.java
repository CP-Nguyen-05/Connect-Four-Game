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
				// Must create ObjectOutputStream before ObjectInputStream
				out = new ObjectOutputStream(socket.getOutputStream());
				in  = new ObjectInputStream(socket.getInputStream());

				// Expect LOGIN first
				Message login = (Message) in.readObject();
				if (login.getType() == MessageType.LOGIN) {
					username = login.getSender();
					System.out.println(username + " connected.");
					// announce join as CHAT from SERVER
					broadcast(new Message(
							UUID.randomUUID().toString(),
							MessageType.CHAT,
							username + " joined the game lobby.",
							"SERVER",
							null,
							System.currentTimeMillis()
					));
				} else {
					socket.close();
					return;
				}

				// Main loop
				while (true) {
					Message msg = (Message) in.readObject();
					switch (msg.getType()) {
						case CHAT:
							String target = msg.getRecipient();
							if (target != null && !target.isEmpty()) {
								boolean found = false;
								for (ClientHandler ch : clients) {
									if (ch.username.equals(target)) {
										// send to recipient
										ch.send(msg);
										found = true;
										break;
									}
								}
								if (found) {
									if (target.equals(username)) {
										this.send(new Message(
												UUID.randomUUID().toString(),
												MessageType.CHAT,
												"Cannot send to your username",
												"SERVER",
												username,
												System.currentTimeMillis()
										));
									}
									else{
										// echo back to sender
										this.send(msg);
									}

								} else {
									// recipient not found → notify sender
									this.send(new Message(
											UUID.randomUUID().toString(),
											MessageType.CHAT,
											"User \"" + target + "\" does not exist.",
											"SERVER",
											username,
											System.currentTimeMillis()
									));
								}
							} else {
								// public chat → broadcast
								broadcast(msg);
							}
							break;

						case MOVE:
							// In a real game you'd route only to the opponent
							broadcast(msg);
							break;

						// handle other types (CREATE_ROOM, JOIN_ROOM, etc.) as needed...
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