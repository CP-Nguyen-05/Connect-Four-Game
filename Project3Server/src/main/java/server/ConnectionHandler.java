package server;
import shared.Message;
import shared.MessageType;
import shared.User;
import shared.Room;
import shared.RoomManager;
import server.UserService;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Optional;
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
    private User user;
    private String currentRoomId;

    public String getCurrentRoomId() {
        return currentRoomId;
    }

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
                                    MessageType.REGISTER_SUCCESS,
                                    "Registration successful – you may now log in.",
                                    "SERVER",
                                    msg.getSender()
                            ));
                        } else {
                            sendMessage(new Message(
                                    MessageType.ERROR,
                                    "Username already exists.",
                                    "SERVER",
                                    msg.getSender()
                            ));
                        }
                        break;

                    case LOGIN:
                        String uname = msg.getSender();
                        String pwd = msg.getContent();

                        if (!userService.usernameExists(uname)) {
                            sendMessage(new Message(
                                    MessageType.ERROR,
                                    "Account \"" + uname + "\" does not exist. Please register.",
                                    "SERVER",
                                    uname
                            ));
                        } else if (userService.validateCredentials(uname, pwd) == null) {
                            sendMessage(new Message(
                                    MessageType.ERROR,
                                    "Password is incorrect.",
                                    "SERVER",
                                    uname
                            ));
                        } else {
                            // 1) Mark this handler as logged‐in
                            this.username = uname;
                            // 2) Load the real User (with stats) from disk
                            User realUser = userService.validateCredentials(uname, pwd);
                            this.user = realUser;
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
                                    MessageType.LOGIN_SUCCESS,
                                    payload,
                                    "SERVER",
                                    uname
                            ));


                        }
                        break;

                    default:
                        sendMessage(new Message(
                                MessageType.ERROR,
                                "Please REGISTER or LOGIN first.",
                                "SERVER",
                                msg.getSender()
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
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                                break;
                            }
                        }
                        break;
                    case MOVE:
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                                break;
                            }
                        }
                        break;
                    case SURRENDER:
                        System.out.println(msg.getSender()+" surrendered in "+ currentRoomId);
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                                break;
                            }
                        }
                        break;
                    case REMATCH_REQUEST:
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                                break;
                            }
                        }
                        break;
                    case REMATCH_REJECT:
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                                break;
                            }
                        }
                        sendMessage(new Message(
                                MessageType.ERROR,
                                "No active game to rematch/reject.",
                                "SERVER",
                                username
                        ));
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
                                    MessageType.ERROR,
                                    "Server error: unable to delete account.",
                                    "SERVER",
                                    user
                            ));
                            break;
                        }
                        if (ok) {
                            sendMessage(new Message(
                                    MessageType.DELETE_ACCOUNT_SUCCESS,
                                    "Account deleted successfully.",
                                    "SERVER",
                                    user
                            ));
                            // Log the deletion
                            System.out.println(user + " has deleted their account.");
                            // Close the socket and exit the loop
                            try {
                                socket.close();
                            } catch (IOException ignore) {
                            }
                            throw new SocketException("Connection closed after DELETE_ACCOUNT");
                        } else {
                            sendMessage(new Message(
                                    MessageType.ERROR,
                                    "Password incorrect; cannot delete account.",
                                    "SERVER",
                                    user
                            ));
                        }
                        return;
                    case LIST_ROOMS:
                        List<Room> rooms = server.getRoomManager().getOpenRooms();
                        int userCount = server.getClients().size();

                        String roomsPart = rooms.stream()
                                .map(r -> String.join("|",
                                        r.getRoomId(),
                                        String.valueOf(r.getPlayers().size()),
                                        String.valueOf(r.getMaxPlayerCapacity()),
                                        String.valueOf(r.isOpenForPlayers())
                                ))
                                .collect(Collectors.joining(";"));

                        String payload = userCount + ";" + roomsPart;
                        sendMessage(new Message(
                                MessageType.ROOM_LIST,
                                payload,
                                "SERVER",
                                msg.getSender()
                        ));
                        break;
                    case LIST_LEADERBOARD:
                        // get every user, drop anyone with zero points, sort desc by score
                        List<User> top = userService.getLeaderboard();
                        // build a payload string, e.g. "Alice|4100;Bob|4000;…"
                        payload = top.stream()
                                .map(u -> u.getUsername() + "|" + u.getScore())
                                .collect(Collectors.joining(";"));
                        sendMessage(new Message(
                                MessageType.LEADERBOARD,
                                payload,
                                "SERVER",
                                msg.getSender()
                        ));
                        break;
                    case JOIN_ROOM:
                        String roomId = msg.getContent();
                        Optional<Room> opt = server.getRoomManager().findRoomById(roomId);
                        if (opt.isEmpty()) {
                            sendMessage(new Message(
                                    MessageType.ERROR,
                                    "Room not found: " + roomId,
                                    "SERVER",
                                    username
                            ));
                        } else {
                            Room room = opt.get();
                            if (!room.isOpenForPlayers()) {
                                sendMessage(new Message(
                                        MessageType.ERROR,
                                        "Room is full: " + roomId,
                                        "SERVER",
                                        username
                                ));
                            } else {
                                // 1) join
                                room.addPlayer(this.user);
                                this.currentRoomId = roomId;      // ← mark “in this room”
                                System.out.println(username + " joined " + roomId);

                                // 2) if it's now full, start the game:
                                if (!room.isOpenForPlayers()) {
                                    User u1 = room.getPlayers().get(0);
                                    User u2 = room.getPlayers().get(1);
                                    ConnectionHandler ch1 = server.findByUsername(u1.getUsername());
                                    ConnectionHandler ch2 = server.findByUsername(u2.getUsername());

                                    payload=roomId+"|"+u1.getUsername()+","+u2.getUsername();
                                    // tell both to switch to game UI
                                    for (ConnectionHandler ch : List.of(ch1, ch2)) {
                                        ch.sendMessage(new Message(
                                                MessageType.GAME_START,
                                                payload,
                                                "SERVER",
                                                ch.getUsername()
                                        ));
                                    }
                                    System.out.println(roomId + " started game");
                                    GameSession session = new GameSession(ch1, ch2,server, roomId);
                                    server.addGameSession(roomId, session);
                                    new Thread(session, "GameSession-" + roomId).start();
                                }
                            }
                        }
                        break;
                    case CREATE_ROOM:
                        // make a new room and add this user
                        Room newRoom = server.getRoomManager().createRoom();
                        newRoom.addPlayer(this.user);
                        System.out.println(username + " created " + newRoom.getRoomId());
                        this.currentRoomId = newRoom.getRoomId();
                        sendMessage(new Message(
                                MessageType.ROOM_CREATED,
                                currentRoomId,
                                "SERVER",
                                username
                        ));
                        break;
                    case CANCEL_ROOM:
                        String rid = msg.getContent();
                        username = msg.getSender();
                        server.getRoomManager().findRoomById(rid).ifPresent(room -> {
                            server.getRoomManager().removeRoom(rid);
                            System.out.println(username + " has been cancelled "+rid);
                        });
                        sendMessage(new Message(
                                MessageType.ROOM_CANCELLED,
                                rid,
                                "SERVER",
                                username
                        ));
                        break;

                    default:
                        sendMessage(new Message(
                                MessageType.ERROR,
                                "Invalid message type: " + msg.getType(),
                                "SERVER",
                                msg.getSender()
                        ));
                }
            }
        } catch (EOFException eof) {
        } catch (SocketException se) {
        } catch (Exception e) {
        } finally {

            if (currentRoomId != null) {
                // treat as surrender
                if (currentRoomId != null) {
                    GameSession gs = server.getGameSession(currentRoomId);
                    if (gs != null) {
                        // notify opponent of win
                        gs.handleMessage(new Message(
                                MessageType.SURRENDER,
                                currentRoomId,
                                username,
                                null
                        ));
                    }
                }
                server.removeClient(this);
                if (username != null) {
                    System.out.println(username + " disconnected");
                    try { socket.close(); } catch (IOException ignore) {}
                }
            }

            // Cleanup
            server.removeClient(this);
            if (username != null) {
                System.out.println(username + " disconnected from the server.");

                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void roomJoinAndStartSession(Message msg) throws IOException {
        String roomId = msg.getContent();
        Optional<Room> opt = server.getRoomManager().findRoomById(roomId);
        if (opt.isEmpty()) {
            sendMessage(errorResponse("Room not found: " + roomId));
        } else {
            Room room = opt.get();
            if (!room.isOpenForPlayers()) {
                sendMessage(errorResponse("Room is full: " + roomId));
            } else {
                room.addPlayer(this.user);
                this.currentRoomId = roomId;
                if (!room.isOpenForPlayers()) {
                    // start game session
                    ConnectionHandler ch1 = server.findByUsername(room.getPlayers().get(0).getUsername());
                    ConnectionHandler ch2 = server.findByUsername(room.getPlayers().get(1).getUsername());
                    GameSession gs = new GameSession(ch1, ch2,server, roomId);
                    server.addGameSession(roomId, gs);
                    new Thread(gs, "GameSession-"+roomId).start();
                    // notify both clients
                    ch1.sendMessage(new Message(MessageType.GAME_START,
                            roomId,
                            "SERVER",
                            ch1.getUsername()
                    ));
                    ch2.sendMessage(new Message(MessageType.GAME_START,
                            roomId,
                            "SERVER",
                            ch2.getUsername()
                    ));
                }
            }
        }
    }

    private Message errorResponse(String text) {
        return new Message(
                MessageType.ERROR,
                text,
                "SERVER",
                username
        );
    }
}