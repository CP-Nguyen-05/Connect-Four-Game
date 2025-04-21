package Controller;

import shared.Message;
import shared.MessageType;
import shared.User;
import shared.ClientConnection;
import shared.ConnectFourApp;
import Controller.RoomView;
import javafx.application.Platform;
import javafx.collections.ObservableList;
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

    private final ObservableList<RoomView> availableRooms;
    private final ListView<RoomView> roomListView;
    private final TextField roomIdField;
    private final Label messageLabel;

    public RoomController(ClientConnection conn,
                          User currentUser,
                          ConnectFourApp app,
                          ObservableList<RoomView> availableRooms,
                          ListView<RoomView> roomListView,
                          TextField roomIdField,
                          Label messageLabel) {
        this.conn            = conn;
        this.currentUser     = currentUser;
        this.app             = app;
        this.availableRooms  = availableRooms;
        this.roomListView    = roomListView;
        this.roomIdField     = roomIdField;
        this.messageLabel    = messageLabel;

        // bind model to view
        this.roomListView.setItems(this.availableRooms);
        this.roomListView.setCellFactory(lv -> new ListCell<RoomView>() {
            @Override
            protected void updateItem(RoomView rv, boolean empty) {
                super.updateItem(rv, empty);
                if (empty || rv == null) {
                    setText(null);
                } else {
                    setText(String.format("%s  (%d/%d)%s",
                            rv.getRoomId(),
                            rv.getCurrentPlayerCount(),
                            rv.getMaxPlayerCapacity(),
                            rv.isOpen() ? "" : " [FULL]"));
                }
            }
        });
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

                List<RoomView> rooms = parseRoomList(reply.getContent());
                Platform.runLater(() -> {
                    availableRooms.setAll(rooms);
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
        showWaiting("Creating room…");
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
                waitForGameStart();
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showError("Create failed: " + ex.getMessage());
                    app.showRoomScene();
                });
            }
        }, "CreateRoom-Thread").start();
    }

    /** Quick‐join: joins first open room or prompts to create if none. */
    public void handleQuickJoin() {
        Optional<RoomView> open = availableRooms.stream()
                .filter(RoomView::isOpen)
                .findFirst();

        if (open.isPresent()) {
            joinRoom(open.get().getRoomId());
        } else {
            Platform.runLater(() -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("No Open Rooms");
                confirm.setHeaderText("No rooms available.");
                confirm.setContentText("Create a new room?");
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    handleCreateRoom();
                } else {
                    Platform.runLater(app::showRoomScene);
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
        Platform.runLater(app::showGameScene);
    }

    private void showWaiting(String text) {
        Platform.runLater(() -> {
            messageLabel.setText(text);
            roomListView.setDisable(true);
            roomIdField.setDisable(true);
        });
    }

    private void showError(String text) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, text).showAndWait());
    }
}
