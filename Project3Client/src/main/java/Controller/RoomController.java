package Controller;

import shared.Message;
import shared.MessageType;
import shared.User;
import shared.ClientConnection;
import shared.ConnectFourApp;
import Controller.RoomView;
import javafx.application.Platform;
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
                        sb.append(String.format("%s  (%d/%d)%s\n",
                                rv.getRoomId(),
                                rv.getCurrentPlayerCount(),
                                rv.getMaxPlayerCapacity(),
                                rv.isOpen() ? "" : " [FULL]")
                        );
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

        new Thread(() -> {
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.CREATE_ROOM,
                        "",
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
                Platform.runLater(app::showWaitingScene);
                waitForGameStart();
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showError("Failed to create room: " + ex.getMessage());
                    app.showRoomScene();
                });
            }
        }, "CreateRoom-Thread").start();
    }

    /** Quick‑join: pick the first open room, or prompt to create one. */
    public void handleQuickJoin() {
        Optional<RoomView> open = rooms.stream()
                .filter(RoomView::isOpen)
                .findFirst();

        if (open.isPresent()) {
            joinRoom(open.get().getRoomId());
        } else {
            Platform.runLater(() -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("No Open Rooms");
                confirm.setHeaderText("No rooms available");
                confirm.setContentText("Would you like to create a new room instead?");
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
            joinRoom(id);
        }
    }

    /** Spectates a full room by ID. */
    public void handleJoinAsSpectator() {
        String id = roomIdField.getText().trim();
        if (id.isEmpty()) {
            messageLabel.setText("Enter a room ID.");
        } else {
            showWaiting("Joining as spectator…");
            new Thread(() -> {
                try {
                    conn.sendMessage(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.SPECTATE,
                            id,
                            currentUser.getUsername(),
                            null,
                            System.currentTimeMillis()
                    ));
                    waitForGameStart();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        showError("Spectate failed: " + ex.getMessage());
                        app.showRoomScene();
                    });
                }
            }, "Spectate-Thread").start();
        }
    }

    // ──────────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────────

    private void joinRoom(String roomId) {
        Platform.runLater(() -> {
            messageLabel.setText("Joining room " + roomId + "…");
            roomListArea.setDisable(true);
            roomIdField.setDisable(true);
        });

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

    private void waitForGameStart() throws Exception {
        Message m;
        do {
            m = conn.receiveMessage();
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
