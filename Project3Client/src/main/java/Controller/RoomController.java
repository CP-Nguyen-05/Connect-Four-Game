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

                // parse into your plain List<RoomView>
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

                // update UI on FX thread
                Platform.runLater(() -> {
                    rooms.clear();
                    rooms.addAll(parsed);

                    // rebuild the text area
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
        }).start();
    }

    /** Creates a new room and waits for GAME_START. */
    public void handleCreateRoom() {
        // 1) immediately switch to a “waiting” UI
        Platform.runLater(() -> {
            messageLabel.setText("Waiting for an opponent to join…");
            roomListArea.setDisable(true);
            roomIdField.setDisable(true);
        });

        // 2) send CREATE_ROOM and then block until GAME_START
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
                Platform.runLater(() -> app.showWaitingScene());
                waitForGameStart();   // loops until GAME_START
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
        // look in your plain List<RoomView>
        Optional<RoomView> open = rooms.stream()
                .filter(RoomView::isOpen)
                .findFirst();

        if (open.isPresent()) {
            joinRoom(open.get().getRoomId());
        } else {
            // must be on FX thread to show alerts
            Platform.runLater(() -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("No Open Rooms");
                confirm.setHeaderText("No rooms available");
                confirm.setContentText("Would you like to create a new room instead?");
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    handleCreateRoom();
                } else {
                    app.showRoomScene();
                }
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

    private List<RoomView> parseRoomList(String payload) {
        if (payload.isBlank()) return Collections.emptyList();
        return Arrays.stream(payload.split(";", -1))
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
    }

    private void joinRoom(String roomId) {
        showWaiting("Joining room " + roomId + "…");
        new Thread(() -> {
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.JOIN_ROOM,
                        roomId,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
                waitForGameStart();
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
            // 1) tell the App which room we’re in
            app.setCurrentRoomId(roomId);
            // 2) switch into the actual game scene
            app.showGameScene();
        });
    }

    private void showWaiting(String text) {
        Platform.runLater(() -> {
            messageLabel.setText(text);
            roomListArea.setDisable(true);   // disable your TextArea
            roomIdField.setDisable(true);
        });
    }

    private void showError(String text) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, text).showAndWait());
    }
}
