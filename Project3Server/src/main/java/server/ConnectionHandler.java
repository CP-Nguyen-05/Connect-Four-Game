package server;
import shared.Message;
import shared.MessageType;
import shared.User;
import shared.Room;
import shared.RoomManager;
import server.UserService;


import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;


/**
 * Handles one client socket: LOGIN handshake, then chat/move routing.
 */
public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final Server server;

    private final UserService userService = new UserService();

    private ObjectInputStream  in;
    private ObjectOutputStream out;
    private String username;

    public ConnectionHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    /** Thread‐safe send */
    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException ignored) { }
    }

    /** Blocking receive */
    public Message readMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            // 1) set up streams (out first!)
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            // 2) LOGIN handshake
            while (true) {
                Message msg = readMessage();
                switch (msg.getType()) {
                    case REGISTER:
                        User newUser = new User(
                                msg.getSender(),    // displayName
                                msg.getSender(),    // username
                                msg.getContent(),   // password
                                0, 0, 0, 0, 0       // all stats start at zero
                        );
                        boolean created = userService.registerUser(newUser);
                        if (created) {
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.CHAT,
                                    "Registration successful – you may now log in.",
                                    "SERVER",
                                    msg.getSender(),
                                    System.currentTimeMillis()
                            ));
                        } else {
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Username already exists.",
                                    "SERVER",
                                    msg.getSender(),
                                    System.currentTimeMillis()
                            ));
                        }
                        break;

                    case LOGIN:
                        String uname = msg.getSender();
                        String pwd   = msg.getContent();

                        if (!userService.usernameExists(uname)) {
                            // user not found
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Account \"" + uname + "\" does not exist. Please register.",
                                    "SERVER",
                                    uname,
                                    System.currentTimeMillis()
                            ));
                        }
                        else if (userService.validateCredentials(uname, pwd) == null) {
                            // wrong password
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Password is incorrect.",
                                    "SERVER",
                                    uname,
                                    System.currentTimeMillis()
                            ));
                        }
                        else {
                            // success!
                            this.username = uname;
                            server.broadcast(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.CHAT,
                                    uname + " joined the lobby.",
                                    "SERVER",
                                    null,
                                    System.currentTimeMillis()
                            ));
                        }
                        break;

                    default:
                        // unexpected during handshake
                        sendMessage(new Message(
                                UUID.randomUUID().toString(),
                                MessageType.ERROR,
                                "Please REGISTER or LOGIN first.",
                                "SERVER",
                                msg.getSender(),
                                System.currentTimeMillis()
                        ));
                }
                if (username != null) {
                    // LOGIN succeeded
                    System.out.println(username + " connected to the server.");
                    break;
                }
            }

            // 3) Main loop: route CHAT and MOVE
            while (true) {
                Message msg = readMessage();
                switch (msg.getType()) {
                    case CHAT:
                        handleChat(msg);
                        break;
                    case MOVE:
                        server.broadcast(msg);
                        break;
                    // TODO: handle CREATE_ROOM, JOIN_ROOM, etc.
                    default:
                        // ignore unknown types
                }
            }
        } catch (EOFException eof) {
            // client disconnected
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // cleanup
            server.removeClient(this);
            if (username != null) {
                System.out.println(username + " disconnected to the server.");
                server.broadcast(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.CHAT,
                        username + " left the lobby.",
                        "SERVER",
                        null,
                        System.currentTimeMillis()
                ));
            }
            try { socket.close(); } catch (IOException ignore) {}
        }
    }

    private void handleChat(Message msg) {
        String target = msg.getRecipient();
        if (target != null && !target.isEmpty()) {
            // private message
            if (target.equals(username)) {
                // can't PM yourself
                sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.ERROR,
                        "You cannot send a private message to yourself.",
                        "SERVER",
                        username,
                        System.currentTimeMillis()
                ));
                return;
            }

            ConnectionHandler recipientHandler = server.findByUsername(target);
            if (recipientHandler != null) {
                // deliver to recipient and echo to sender
                recipientHandler.sendMessage(msg);
                sendMessage(msg);
            } else {
                // user not found
                sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.ERROR,
                        "User \"" + target + "\" does not exist.",
                        "SERVER",
                        username,
                        System.currentTimeMillis()
                ));
            }
        } else {
            // public broadcast
            server.broadcast(msg);
        }
    }
}