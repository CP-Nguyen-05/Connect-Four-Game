package server;
import shared.Message;
import shared.MessageType;
import shared.User;
import shared.Room;
import shared.RoomManager;
import server.UserService;

import java.net.SocketException;
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
            // 1) Set up streams (out first!)
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
                        String pwd = msg.getContent();

                        if (!userService.usernameExists(uname)) {
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Account \"" + uname + "\" does not exist. Please register.",
                                    "SERVER",
                                    uname,
                                    System.currentTimeMillis()
                            ));
                        } else if (userService.validateCredentials(uname, pwd) == null) {
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Password is incorrect.",
                                    "SERVER",
                                    uname,
                                    System.currentTimeMillis()
                            ));
                        } else {
                            // 1) Mark this handler as logged‐in
                            this.username = uname;

                            // 2) Load the real User (with stats) from disk
                            User realUser = userService.validateCredentials(uname, pwd);

                            // 3) Build a CSV payload matching your players.txt format
                            String payload = String.join(",",
                                    realUser.getDisplayName(),
                                    realUser.getUsername(),
                                    realUser.getPassword(),
                                    String.valueOf(realUser.getScore()),
                                    String.valueOf(realUser.getGamesPlayed()),
                                    String.valueOf(realUser.getWinCount()),
                                    String.valueOf(realUser.getLossCount()),
                                    String.valueOf(realUser.getDrawCount())
                            );

                            // 4) Send it back as LOGIN_SUCCESS
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.LOGIN_SUCCESS,
                                    payload,
                                    "SERVER",
                                    uname,
                                    System.currentTimeMillis()
                            ));

                            // 5) (Optional) announce the lobby join
                            Message joinedMsg = new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.CHAT,
                                    uname + " joined the lobby.",
                                    "SERVER",
                                    null,
                                    System.currentTimeMillis()
                            );
                            server.broadcastExcept(joinedMsg, this);
                        }
                        break;

                    default:
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
                    System.out.println(username + " connected to the server.");
                    break;
                }
            }

            // 3) Main loop: route CHAT, MOVE, DELETE_ACCOUNT, and other messages
            while (true) {
                Message msg = readMessage();
                switch (msg.getType()) {
                    case CHAT:
                        handleChat(msg);
                        break;
                    case MOVE:
                        server.broadcast(msg);
                        break;
                    case DELETE_ACCOUNT:
                        String user = msg.getSender();
                        String pass = msg.getContent();
                        boolean ok = false;
                        try {
                            ok = userService.deleteUser(user, pass);
                        } catch (Exception ex) {
                            System.err.println("Error deleting user " + user + ": " + ex.getMessage());
                            ex.printStackTrace();
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Server error: unable to delete account.",
                                    "SERVER",
                                    user,
                                    System.currentTimeMillis()
                            ));
                            break;
                        }
                        if (ok) {
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.DELETE_ACCOUNT_SUCCESS,
                                    "Account deleted successfully.",
                                    "SERVER",
                                    user,
                                    System.currentTimeMillis()
                            ));
                            // Log the deletion
                            System.out.println(user + " has deleted their account.");
                            // Close the socket and exit the loop
                            try {
                                socket.close();
                            } catch (IOException ignore) {}
                            throw new SocketException("Connection closed after DELETE_ACCOUNT");
                        } else {
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Password incorrect; cannot delete account.",
                                    "SERVER",
                                    user,
                                    System.currentTimeMillis()
                            ));
                        }
                        return;
                        //break;
                    //TO DO
                    case LIST_ROOMS:
                        //List<Room> rooms = server.getRoomManager().getOpenRooms();
                        break;
                    case JOIN_ROOM:
                        break;
                    case CREATE_ROOM:
                        break;

                    default:
                        sendMessage(new Message(
                                UUID.randomUUID().toString(),
                                MessageType.ERROR,
                                "Invalid message type: " + msg.getType(),
                                "SERVER",
                                msg.getSender(),
                                System.currentTimeMillis()
                        ));
                }
            }
        } catch (EOFException eof) {
        } catch (SocketException se) {
        } catch (Exception e) {
        } finally {
            // Cleanup
            server.removeClient(this);
            if (username != null) {
                System.out.println(username + " disconnected from the server.");
                server.broadcastExcept(
                        new Message(
                                UUID.randomUUID().toString(),
                                MessageType.CHAT,
                                username + " left the lobby.",
                                "SERVER",
                                null,
                                System.currentTimeMillis()
                        ),this);
            }
            try {
                socket.close();
            } catch (IOException ignore) {}
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