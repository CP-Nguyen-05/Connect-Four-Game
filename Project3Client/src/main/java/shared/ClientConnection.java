package shared;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Low‑level socket wrapper for sending/receiving Message objects.
 */
public class ClientConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    /**
     * Open a connection to the given host/port.
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // Important: create ObjectOutputStream before ObjectInputStream
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Send a Message object to the server.
     */
    public void sendMessage(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }

    /**
     * Blocking receive of the next Message from the server.
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    /**
     * Close the underlying socket.
     */
    public void close() throws IOException {
        socket.close();
    }
}