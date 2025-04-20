import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class Client {
	private static final String HOST = "localhost";
	private static final int    PORT = 12345;

	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream  in;
	public String username;

	/** Call this to start the network thread */
	public void start() {
		try {
			socket = new Socket(HOST, PORT);

			// IMPORTANT: create ObjectOutputStream before ObjectInputStream
			out = new ObjectOutputStream(socket.getOutputStream());
			in  = new ObjectInputStream(socket.getInputStream());

			// send LOGIN message
			Message login = new Message(
					UUID.randomUUID().toString(),
					MessageType.LOGIN,
					"",             // no content needed for login
					username,
					null,
					System.currentTimeMillis()
			);
			out.writeObject(login);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Send any Message to the server */
	public void send(Message msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Blocking read; returns next Message from server */
	public Message readMessage() throws IOException, ClassNotFoundException {
		return (Message) in.readObject();
	}
}