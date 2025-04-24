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
                                    UUID.randomUUID().toString(),
                                    MessageType.REGISTER_SUCCESS,
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
                                    UUID.randomUUID().toString(),
                                    MessageType.LOGIN_SUCCESS,
                                    payload,
                                    "SERVER",
                                    uname,
                                    System.currentTimeMillis()
                            ));


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
//                        handleChat(msg);
//                        break;
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                                break;    // do NOT fall through to lobby broadcast
                            }
                        }
                        // otherwise it’s lobby chat (or pre‐game), fall back to your existing handler
                        //handleChat(msg);
                        break;
                    case MOVE:
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                            }
                        }
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
                            } catch (IOException ignore) {
                            }
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
                        List<Room> rooms = server.getRoomManager().getOpenRooms();
                        // payload format example: room1|0|2|true;room2|1|2|true;room3|2|2|false
                        String payload = rooms.stream()
                                .map(r -> String.join("|",
                                        r.getRoomId(),
                                        Integer.toString(r.getPlayers().size()),
                                        Integer.toString(r.getMaxPlayerCapacity()),
                                        Boolean.toString(r.isOpenForPlayers())))
                                .collect(Collectors.joining(";"));
                        sendMessage(new Message(
                                UUID.randomUUID().toString(),
                                MessageType.ROOM_LIST,
                                payload,
                                "SERVER",
                                msg.getSender(),
                                System.currentTimeMillis()
                        ));
                        break;
                    case JOIN_ROOM:
                        String roomId = msg.getContent();
                        Optional<Room> opt = server.getRoomManager().findRoomById(roomId);
                        if (opt.isEmpty()) {
                            sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ERROR,
                                    "Room not found: " + roomId,
                                    "SERVER",
                                    username,
                                    System.currentTimeMillis()
                            ));
                        } else {
                            Room room = opt.get();
                            if (!room.isOpenForPlayers()) {
                                sendMessage(new Message(
                                        UUID.randomUUID().toString(),
                                        MessageType.ERROR,
                                        "Room is full: " + roomId,
                                        "SERVER",
                                        username,
                                        System.currentTimeMillis()
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

                                    // tell both to switch to game UI
                                    for (ConnectionHandler ch : List.of(ch1, ch2)) {
                                        ch.sendMessage(new Message(
                                                UUID.randomUUID().toString(),
                                                MessageType.GAME_START,
                                                roomId,
                                                "SERVER",
                                                ch.getUsername(),
                                                System.currentTimeMillis()
                                        ));
                                    }
                                    System.out.println(roomId + " started game");
                                    // hand off to your GameSession runner
                                   // new Thread(new GameSession(ch1, ch2), "GameSession-" + roomId).start();
                                    GameSession session = new GameSession(ch1, ch2,server);
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
                                UUID.randomUUID().toString(),
                                MessageType.ROOM_CREATED,
                                currentRoomId,
                                "SERVER",
                                username,
                                System.currentTimeMillis()
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
                                UUID.randomUUID().toString(),
                                MessageType.ROOM_CANCELLED,
                                rid,
                                "SERVER",
                                username,
                                System.currentTimeMillis()
                        ));
                        break;
                    case SURRENDER:
//                        String loser = msg.getSender();
//                        roomId = msg.getContent();      // client sent the roomId
//                        server.getRoomManager().findRoomById(roomId).ifPresent(room -> {
//                            // figure out who won/lost
//                            List<User> players = room.getPlayers();
//                            User loserUser  = players.stream()
//                                    .filter(u -> u.getUsername().equals(loser))
//                                    .findFirst().orElse(null);
//                            User winnerUser = players.stream()
//                                    .filter(u -> !u.getUsername().equals(loser))
//                                    .findFirst().orElse(null);
//
//                            if (loserUser != null && winnerUser != null) {
//                                // 1) notify both clients
//                                for (User u : players) {
//                                    ConnectionHandler ch = server.findByUsername(u.getUsername());
//                                    if (ch == null) continue;
//                                    boolean youWin = u.getUsername().equals(winnerUser.getUsername());
//                                    String result = youWin ? "YOU_WIN" : "YOU_LOSE";
//                                    ch.sendMessage(new Message(
//                                            UUID.randomUUID().toString(),
//                                            MessageType.GAME_END,
//                                            result,
//                                            "SERVER",
//                                            u.getUsername(),
//                                            System.currentTimeMillis()
//                                    ));
//                                }
//
//                                // 2) persist updated stats back to players.txt
//                                UserDataStore ds = new UserDataStore();
//                                List<User> all = ds.loadUsers();
//                                for (User u : all) {
//                                    if (u.getUsername().equals(winnerUser.getUsername())) {
//                                        u.setWinCount(   u.getWinCount()   + 1);
//                                        u.setGamesPlayed(u.getGamesPlayed()+ 1);
//                                    } else if (u.getUsername().equals(loserUser.getUsername())) {
//                                        u.setLossCount(  u.getLossCount()  + 1);
//                                        u.setGamesPlayed(u.getGamesPlayed()+ 1);
//                                    }
//                                }
//                                ds.saveUsers(all);
//
//                                server.getRoomManager().removeRoom(roomId);
//                                System.out.println(loser + " surrendered in " + roomId);
//                                System.out.println(winnerUser.getUsername() + " won in "+ roomId);
//                            }
//                        });
//                        break;
                        if (currentRoomId != null) {
                            GameSession gs = server.getGameSession(currentRoomId);
                            if (gs != null) {
                                gs.handleMessage(msg);
                                break;  // handled by game session
                            }
                        }
                        // fallback to broadcast in lobby
                        server.broadcast(msg);
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

            if (currentRoomId != null) {
                // treat as surrender
//                Optional<Room> o = server.getRoomManager().findRoomById(currentRoomId);
//                if (o.isPresent()) {
//                    Room room = o.get();
//                    // notify the other player
//                    for (User u : room.getPlayers()) {
//                        if (u.getUsername().equals(username)) continue;
//                        ConnectionHandler ch = server.findByUsername(u.getUsername());
//                        if (ch != null) {
//                            ch.sendMessage(new Message(
//                                    UUID.randomUUID().toString(),
//                                    MessageType.GAME_END,
//                                    "YOU_WIN",
//                                    "SERVER",
//                                    u.getUsername(),
//                                    System.currentTimeMillis()
//                            ));
//                        }
//                    }
//                    // remove the room so it vanishes from the lobby
//                    server.getRoomManager().removeRoom(currentRoomId);
//                    System.out.println(username + " disconnected, treated as surrender in " + currentRoomId);
//                }
                if (currentRoomId != null) {
                    GameSession gs = server.getGameSession(currentRoomId);
                    if (gs != null) {
                        // notify opponent of win
                        gs.handleMessage(new Message(
                                UUID.randomUUID().toString(),
                                MessageType.SURRENDER,
                                currentRoomId,
                                username,
                                null,
                                System.currentTimeMillis()
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

//    private void handleChat(Message msg) {
//        String target = msg.getRecipient();
//
//        // 1) Private message (PM)
//        if (target != null && !target.isEmpty()) {
//            // can’t PM yourself
//            if (target.equals(username)) {
//                sendMessage(new Message(
//                        UUID.randomUUID().toString(),
//                        MessageType.ERROR,
//                        "You cannot send a private message to yourself.",
//                        "SERVER",
//                        username,
//                        System.currentTimeMillis()
//                ));
//                return;
//            }
//
//            ConnectionHandler recipientHandler = server.findByUsername(target);
//            if (recipientHandler != null) {
//                // deliver to recipient and echo to sender
//                recipientHandler.sendMessage(msg);
//                sendMessage(msg);
//            } else {
//                sendMessage(new Message(
//                        UUID.randomUUID().toString(),
//                        MessageType.ERROR,
//                        "User \"" + target + "\" does not exist.",
//                        "SERVER",
//                        username,
//                        System.currentTimeMillis()
//                ));
//            }
//            return;
//        }
//
//        // 2) No recipient → either lobby chat or in‑game chat
//        else if (currentRoomId != null) {
//            // room chat: only send to handlers in the same room
//            server.getRoomManager()
//                    .findRoomById(currentRoomId)
//                    .ifPresent(room -> {
//                        for (User u : room.getPlayers()) {
//                            ConnectionHandler ch = server.findByUsername(u.getUsername());
//                            if (ch != null) ch.sendMessage(msg);
//                        }
//                    });
//        } else {
//            // still in the lobby
//            server.broadcast(msg);
//        }
//    }
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
                    GameSession gs = new GameSession(ch1, ch2,server);
                    server.addGameSession(roomId, gs);
                    new Thread(gs, "GameSession-"+roomId).start();
                    // notify both clients
                    ch1.sendMessage(new Message(UUID.randomUUID().toString(), MessageType.GAME_START, roomId, "SERVER", ch1.getUsername(), System.currentTimeMillis()));
                    ch2.sendMessage(new Message(UUID.randomUUID().toString(), MessageType.GAME_START, roomId, "SERVER", ch2.getUsername(), System.currentTimeMillis()));
                }
            }
        }
    }

    private Message errorResponse(String text) {
        return new Message(UUID.randomUUID().toString(), MessageType.ERROR, text, "SERVER", username, System.currentTimeMillis());
    }
}