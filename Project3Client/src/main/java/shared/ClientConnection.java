package shared;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientConnection {
    private Socket mySocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    // Connect to the server
    public void connect(String host, int port) throws IOException {
        mySocket = new Socket(host, port);
        outputStream = new ObjectOutputStream(mySocket.getOutputStream());
        inputStream = new ObjectInputStream(mySocket.getInputStream());
    }

    // Send a message to the server
    public void sendMessage(Message message) throws IOException {
        outputStream.writeObject(message);
        outputStream.flush();
    }

    // Receive a message from the server
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) inputStream.readObject();
    }

    // Close the connection
    public void close() throws IOException {
        mySocket.close();
    }
}