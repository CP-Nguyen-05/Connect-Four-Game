package Controller;

import shared.Message;
import shared.MessageType;
import shared.User;
import shared.ClientConnection;
import shared.ConnectFourApp;
import Controller.RoomView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import java.util.*;
import java.util.stream.*;
import java.util.UUID;

/**
 * Handles room lobby logic: fetching rooms, creating, joining, spectating.
 * UI controls are passed in from ConnectFourApp.showRoomScene().
 */
public class RoomController {
    private final ClientConnection conn;
    private final User currentUser;
    private final ConnectFourApp app;

    private Thread createThread;
    private String lastCreatedRoomId;
    private boolean hostRoom = false;

    private final List<RoomView> rooms;       // plain List
    private final TextArea roomListArea;      // where we show them
    private final TextField roomIdField;
    private final Label messageLabel;

    public RoomController(ClientConnection conn,
                          User currentUser,
                          ConnectFourApp app,
                          List<RoomView> rooms,
                          TextArea roomListArea,
                          TextField roomIdField,
                          Label messageLabel) {
        this.conn           = conn;
        this.currentUser    = currentUser;
        this.app            = app;
        this.rooms          = rooms;
        this.roomListArea   = roomListArea;
        this.roomIdField    = roomIdField;
        this.messageLabel   = messageLabel;
    }

    /** Fetches open rooms from the server and updates the list. */
    public void fetchAvailableRooms() {
        messageLabel.setText("Loading rooms...");
        new Thread(() -> {
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.LIST_ROOMS,
                        "",
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));

                Message reply;
                do {
                    reply = conn.receiveMessage();
                } while (reply.getType() != MessageType.ROOM_LIST);

                List<RoomView> parsed = Arrays.stream(reply.getContent().split(";", -1))
                        .filter(s -> !s.isBlank())
                        .map(chunk -> {
                            String[] p = chunk.split("\\|", -1);
                            return new RoomView(
                                    p[0],
                                    Integer.parseInt(p[1]),
                                    Integer.parseInt(p[2]),
                                    Boolean.parseBoolean(p[3])
                            );
                        })
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    rooms.clear();
                    rooms.addAll(parsed);

                    StringBuilder sb = new StringBuilder();
                    for (RoomView rv : rooms) {
                        String status;
                        if (rv.isOpen()) {
                            status = String.format("(%d/%d)", rv.getCurrentPlayerCount(), rv.getMaxPlayerCapacity());
                        } else {
                            status = "[FULL]";
                        }

                        sb.append(String.format("%-40s %10s\n", rv.getRoomId(), status));
                    }
                    roomListArea.setText(sb.toString());
                    messageLabel.setText("");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        messageLabel.setText("Error: " + ex.getMessage())
                );
            }
        }, "FetchRooms-Thread").start();
    }

    /** Creates a new room and waits for GAME_START. */
    public void handleCreateRoom() {
        Platform.runLater(() -> {
            messageLabel.setText("Waiting for an opponent to join…");
            roomListArea.setDisable(true);
            roomIdField.setDisable(true);
        });
        hostRoom = true;
        app.setMyTurn(hostRoom);
        createThread = new Thread(() -> {
            try {
                // 1) ask server to make a room
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.CREATE_ROOM,
                        "",                           // no content
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));

                // 2) server will respond ROOM_CREATED
                Message created = conn.receiveMessage();
                if (created.getType() == MessageType.ROOM_CREATED) {
                    lastCreatedRoomId = created.getContent();
                }

                // 3) switch UI
                Platform.runLater(app::showWaitingScene);

                // 4) now block until GAME_START
                waitForGameStart();

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showError("Failed to create room: " + ex.getMessage());
                    app.showRoomScene();
                });
            }
        }, "CreateRoom-Thread");

        createThread.start();
    }

    public void cancelCreateRoom() {
        // 1) kill the background thread (it’ll likely block in receiveMessage, so we just drop it)
        if (createThread != null) {
            createThread.interrupt();
            createThread = null;
        }

        // 2) tell server to remove that room
        if (lastCreatedRoomId != null) {
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.CANCEL_ROOM,
                        lastCreatedRoomId,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
            } catch (Exception ignored) {}
        }
        lastCreatedRoomId = null;
        hostRoom = false;
        app.setMyTurn(hostRoom);
        // 3) go back to the lobby
        app.showRoomScene();
    }

    /** Quick‑join: pick the first open room, or prompt to create one. */
    public void handleQuickJoin() {
        hostRoom = true;
        app.setMyTurn(hostRoom);
        Optional<RoomView> open = rooms.stream()
                .filter(RoomView::isOpen)
                .findFirst();

        if (open.isPresent()) {
            joinRoom(open.get().getRoomId());
        } else {
            Platform.runLater(() -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Announcement");
                confirm.setHeaderText("NO ROOM AVAILABLE");
                confirm.setContentText("CREATE A NEW ROOM?");

                DialogPane pane = confirm.getDialogPane();

                pane.setStyle(
                        "-fx-background-color: #1D2529;" +
                        "-fx-font-family: '" + ConnectFourApp.globalFontFamily + "';" +
                        "-fx-font-size: 18px;" +
                        "-fx-text-fill: #FFF;"
                );

                pane.lookupButton(ButtonType.OK).setStyle(
                        "-fx-background-color: #F24339;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;"
                );
                pane.lookupButton(ButtonType.CANCEL).setStyle(
                        "-fx-background-color: #F98C02;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;"
                );

                confirm.showAndWait()
                        .filter(ButtonType.OK::equals)
                        .ifPresent(__ -> handleCreateRoom());
            });
        }
    }

    /** Joins the room ID entered by the user. */
    public void handleJoinRoomById() {

        String id = roomIdField.getText().trim();
        if (id.isEmpty()) {
            messageLabel.setText("Enter a room ID.");
        } else {
            joinRoom("ROOM " +id);
        }
    }

//    /** Spectates a full room by ID. */
//    public void handleJoinAsSpectator() {
//        String id = roomIdField.getText().trim();
//        if (id.isEmpty()) {
//            messageLabel.setText("Enter a room ID.");
//        } else {
//            showWaiting("Joining as spectator…");
//            new Thread(() -> {
//                try {
//                    conn.sendMessage(new Message(
//                            UUID.randomUUID().toString(),
//                            MessageType.SPECTATE,
//                            id,
//                            currentUser.getUsername(),
//                            null,
//                            System.currentTimeMillis()
//                    ));
//                    waitForGameStart();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    Platform.runLater(() -> {
//                        showError("Spectate failed: " + ex.getMessage());
//                        app.showRoomScene();
//                    });
//                }
//            }, "Spectate-Thread").start();
//        }
//    }

    // ──────────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────────

    private void joinRoom(String roomId) {
        Platform.runLater(() -> {
            messageLabel.setText("Joining room " + roomId + "…");
            roomListArea.setDisable(true);
            roomIdField.setDisable(true);
        });
        hostRoom = false;
        app.setMyTurn(hostRoom);

        new Thread(() -> {
            try {
                // 1) send one JOIN_ROOM
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.JOIN_ROOM,
                        roomId,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));

                // 2) wait for exactly one reply
                Message reply = conn.receiveMessage();

                // 3a) if it's an ERROR, show and go back to lobby
                if (reply.getType() == MessageType.ERROR) {
                    Platform.runLater(() -> {
                        showError(reply.getContent());
                        app.showRoomScene();
                    });
                    return;
                }

                // 3b) otherwise we expect GAME_START
                //     the roomId is carried in reply.getContent()
                String joinedId = reply.getContent();
                Platform.runLater(() -> {
                    app.setCurrentRoomId(joinedId);
                    app.showGameScene();
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showError("Join failed: " + ex.getMessage());
                    app.showRoomScene();
                });
            }
        }, "JoinRoom-Thread").start();
    }
    public void waitForGameRematch() throws Exception{
        if (hostRoom) {
            hostRoom = false;
            app.setMyTurn(false);
        }
        else{
            hostRoom = true;
            app.setMyTurn(true);
        }
        Message m;
        do {
            m = conn.receiveMessage();
            if (m.getType() == MessageType.ROOM_CANCELLED) {
                Platform.runLater(() -> {
                    new Alert(AlertType.INFORMATION, "Opponent declined rematch")
                            .showAndWait();
                    app.showOptionMenuScene();
                });
                return;
            }
        } while (m.getType() != MessageType.GAME_START);

        String roomId = m.getContent();
        Platform.runLater(() -> {
            app.setCurrentRoomId(roomId);
            app.showGameScene();
        });
    }

    public void waitForGameStart() throws Exception {
        Message m;
        do {
            m = conn.receiveMessage();
            if (m.getType() == MessageType.ROOM_CANCELLED) {
                Platform.runLater(() -> app.showRoomScene());
                return;
            }
        } while (m.getType() != MessageType.GAME_START);

        String roomId = m.getContent();
        Platform.runLater(() -> {
            app.setCurrentRoomId(roomId);
            app.showGameScene();
        });
    }


    private void showError(String text) {
        new Alert(Alert.AlertType.ERROR, text).showAndWait();
    }

    private void showWaiting(String text) {
        Platform.runLater(() -> {
            messageLabel.setText(text);
            roomListArea.setDisable(true);
            roomIdField.setDisable(true);
        });
    }
}
