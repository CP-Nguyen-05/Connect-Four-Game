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
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
	private static final int SERVER_PORT = 12345;
	private Set<ConnectionHandler> connectedClients = ConcurrentHashMap.newKeySet();
	private Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
	private RoomManager myRoomManager = new RoomManager();

	// Add a new game session
	public void addGameSession(String roomId, GameSession gameSession) {
		activeGames.put(roomId, gameSession);
		new Thread(gameSession, "GameSession-" + roomId).start();
	}

	public GameSession getGameSession(String roomId) {
		return activeGames.get(roomId);
	}

	public void removeGameSession(String roomId) {
		activeGames.remove(roomId);
	}

	// Start the server
	public static void main(String[] args) throws IOException {
		new Server().start();
	}

	public void start() throws IOException {
		System.out.println("Starting server on port " + SERVER_PORT);
		try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				ConnectionHandler clientHandler = new ConnectionHandler(clientSocket, this);
				connectedClients.add(clientHandler);
				new Thread(clientHandler).start();
			}
		}
	}

	public RoomManager getRoomManager() {
		return myRoomManager;
	}

	// Send message to all clients except one
	public void broadcastExcept(Message message, ConnectionHandler excludeClient) {
		for (ConnectionHandler client : connectedClients) {
			if (client != excludeClient) {
				client.sendMessage(message);
			}
		}
	}

	// Send message to all clients
	public void broadcast(Message message) {
		broadcastExcept(message, null);
	}

	// Remove a disconnected client
	public void removeClient(ConnectionHandler clientHandler) {
		connectedClients.remove(clientHandler);
	}

	// Find client by username
	public ConnectionHandler findByUsername(String username) {
		for (ConnectionHandler client : connectedClients) {
			if (client.getUsername().equals(username)) {
				return client;
			}
		}
		return null;
	}

	public Set<ConnectionHandler> getClients() {
		return Collections.unmodifiableSet(connectedClients);
	}
}