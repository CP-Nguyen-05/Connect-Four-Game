package shared;

import java.io.IOException;
import java.util.UUID;

public class Client extends Thread {
	public String userName;
	private String serverHost;
	private int serverPort;
	private ClientConnection myConnection;

	public Client() {
		this("localhost", 12345);
	}

	// Constructor with host and port
	public Client(String host, int port) {
		serverHost = host;
		serverPort = port;
	}

	// Main thread logic
	@Override
	public void run() {
		try {
			myConnection = new ClientConnection();
			myConnection.connect(serverHost, serverPort);

			// Send login message
			Message loginMessage = new Message(
					MessageType.LOGIN,
					"SERVER",
					userName,
					null
			);
			myConnection.sendMessage(loginMessage);

			// Keep reading messages
			while (true) {
				Message receivedMessage = myConnection.receiveMessage();
				System.out.println(receivedMessage.toString());
			}
		} catch (IOException | ClassNotFoundException error) {
			System.out.println("Error: " + error.getMessage());
		} finally {
			try {
				if (myConnection != null) {
					myConnection.close();
				}
			} catch (IOException ignored) {}
		}
	}

	// Send a message to the server
	public void send(Message message) {
		try {
			if (myConnection != null) {
				myConnection.sendMessage(message);
			}
		} catch (IOException error) {
			System.out.println("Send error: " + error.getMessage());
		}
	}

	// Read the next message
	public Message readMessage() throws IOException, ClassNotFoundException {
		return myConnection.receiveMessage();
	}
}