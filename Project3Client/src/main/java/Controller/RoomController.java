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

public class RoomController {
    private ClientConnection myConnection;
    private User myCurrentUser;
    private ConnectFourApp myApp;
    private Thread roomCreateThread;
    private String lastRoomIdCreated;
    private boolean isHostRoom;

    private List<RoomView> roomList;
    private TextArea roomDisplayArea;
    private TextField roomIdInput;
    private Label messageDisplay;

    public RoomController(ClientConnection conn, User currentUser, ConnectFourApp app,
                          List<RoomView> rooms, TextArea roomListArea, TextField roomIdField, Label messageLabel) {
        myConnection = conn;
        myCurrentUser = currentUser;
        myApp = app;
        roomList = rooms;
        roomDisplayArea = roomListArea;
        roomIdInput = roomIdField;
        messageDisplay = messageLabel;
        isHostRoom = false;
    }

    // Get list of available rooms
    public void fetchAvailableRooms() {
        messageDisplay.setText("Loading rooms...");
        new Thread(() -> {
            try {
                myConnection.sendMessage(new Message(
                        MessageType.LIST_ROOMS,
                        "",
                        myCurrentUser.getUsername(),
                        null
                ));

                Message reply;
                do {
                    reply = myConnection.receiveMessage();
                } while (reply.getType() != MessageType.ROOM_LIST);

                String[] parts = reply.getContent().split(";", -1);
                int onlineCount = Integer.parseInt(parts[0]);

                List<RoomView> parsedRooms = Arrays.stream(parts)
                        .skip(1)
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
                    myApp.onlineCountField.setText("Online: " + onlineCount);
                    roomList.clear();
                    roomList.addAll(parsedRooms);
                    StringBuilder displayText = new StringBuilder();
                    for (RoomView room : roomList) {
                        String status = room.isOpen() ? "(" + room.getCurrentPlayerCount() + "/" + room.getMaxPlayerCapacity() + ")" : "[FULL]";
                        displayText.append(room.getRoomId()).append(" ").append(status).append("\n");
                    }
                    roomDisplayArea.setText(displayText.toString());
                    messageDisplay.setText("");
                });

            } catch (Exception ex) {
                Platform.runLater(() -> messageDisplay.setText("Error: " + ex.getMessage()));
            }
        }, "FetchRoomsThread").start();
    }

    // Create a new game room
    public void handleCreateRoom() {
        messageDisplay.setText("Waiting for opponent...");
        roomDisplayArea.setDisable(true);
        roomIdInput.setDisable(true);
        isHostRoom = true;
        myApp.setMyTurn(isHostRoom);
        roomCreateThread = new Thread(() -> {
            try {
                myConnection.sendMessage(new Message(
                        MessageType.CREATE_ROOM,
                        "",
                        myCurrentUser.getUsername(),
                        null
                ));

                Message created = myConnection.receiveMessage();
                if (created.getType() == MessageType.ROOM_CREATED) {
                    lastRoomIdCreated = created.getContent();
                }

                Platform.runLater(() -> myApp.showWaitingScene());
                waitForGameStart();

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Room creation failed: " + ex.getMessage());
                    myApp.showRoomScene();
                });
            }
        }, "CreateRoomThread");
        roomCreateThread.start();
    }

    public void cancelCreateRoom() {
        if (roomCreateThread != null) {
            roomCreateThread.interrupt();
            roomCreateThread = null;
        }

        if (lastRoomIdCreated != null) {
            try {
                myConnection.sendMessage(new Message(
                        MessageType.CANCEL_ROOM,
                        lastRoomIdCreated,
                        myCurrentUser.getUsername(),
                        null
                ));
            } catch (Exception ignored) {}
        }
        lastRoomIdCreated = null;
        isHostRoom = false;
        myApp.setMyTurn(isHostRoom);
        myApp.showRoomScene();
    }

    // Join an available room or create one
    public void handleQuickJoin() {
        isHostRoom = true;
        myApp.setMyTurn(isHostRoom);
        Optional<RoomView> openRoom = roomList.stream().filter(RoomView::isOpen).findFirst();

        if (openRoom.isPresent()) {
            joinRoom(openRoom.get().getRoomId());
        } else {
            Platform.runLater(() -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("No Rooms");
                confirm.setHeaderText("No rooms available");
                confirm.setContentText("Create a new room?");

                DialogPane pane = confirm.getDialogPane();
                pane.setStyle("-fx-background-color: #1D2529; -fx-font-family: '" + ConnectFourApp.globalFontFamily + "'; -fx-font-size: 18px;");
                Label header = (Label) pane.lookup(".header-panel .label");
                if (header != null) {
                    header.setStyle("-fx-text-fill: white;");
                }
                Label content = (Label) pane.lookup(".content.label");
                if (content != null) {
                    content.setStyle("-fx-text-fill: white;");
                }
                pane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: #F24339; -fx-text-fill: white; -fx-font-weight: bold;");
                pane.lookupButton(ButtonType.CANCEL).setStyle("-fx-background-color: #F98C02; -fx-text-fill: white; -fx-font-weight: bold;");

                confirm.showAndWait().filter(ButtonType.OK::equals).ifPresent(__ -> handleCreateRoom());
            });
        }
    }

    public void handleJoinRoomById() {
        String id = roomIdInput.getText();
        if (id.equals("")) {
            messageDisplay.setText("Enter a room ID!");
        } else {
            joinRoom("ROOM " + id);
        }
    }

    // Join a specific room
    private void joinRoom(String roomId) {
        messageDisplay.setText("Joining room " + roomId + "...");
        roomDisplayArea.setDisable(true);
        roomIdInput.setDisable(true);
        isHostRoom = false;
        myApp.setMyTurn(isHostRoom);

        new Thread(() -> {
            try {
                myConnection.sendMessage(new Message(
                        MessageType.JOIN_ROOM,
                        roomId,
                        myCurrentUser.getUsername(),
                        null
                ));

                Message reply = myConnection.receiveMessage();
                if (reply.getType() == MessageType.ERROR) {
                    Platform.runLater(() -> {
                        showError(reply.getContent());
                        myApp.showRoomScene();
                    });
                    return;
                } else if (reply.getType() == MessageType.GAME_START) {
                    String[] parts = reply.getContent().split("\\|", 2);
                    String[] names = parts[1].split(",", 2);
                    Platform.runLater(() -> {
                        myApp.setPlayerNames(names[1], names[0]);
                        myApp.setCurrentRoomId(roomId);
                        myApp.showGameScene();
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Join failed: " + ex.getMessage());
                    myApp.showRoomScene();
                });
            }
        }, "JoinRoomThread").start();
    }

    public void waitForGameRematch() throws Exception {
        if (isHostRoom) {
            isHostRoom = false;
            myApp.setMyTurn(false);
        } else {
            isHostRoom = true;
            myApp.setMyTurn(true);
        }
        Message message;
        do {
            message = myConnection.receiveMessage();
            if (message.getType() == MessageType.ROOM_CANCELLED) {
                Platform.runLater(() -> {
                    new Alert(AlertType.INFORMATION, "Opponent declined rematch").showAndWait();
                    myApp.showOptionMenuScene();
                });
                return;
            }
        } while (message.getType() != MessageType.GAME_START);

        String roomId = message.getContent();
        Platform.runLater(() -> {
            myApp.setCurrentRoomId(roomId);
            myApp.showGameScene();
        });
    }

    // Wait for game to start
    public void waitForGameStart() throws Exception {
        Message message;
        do {
            message = myConnection.receiveMessage();
            if (message.getType() == MessageType.ROOM_CANCELLED) {
                Platform.runLater(() -> myApp.showRoomScene());
                return;
            }
        } while (message.getType() != MessageType.GAME_START);

        final String payload = message.getContent();
        Platform.runLater(() -> {
            String[] parts = payload.split("\\|", 2);
            String roomId = parts[0];
            String[] names = parts[1].split(",", 2);
            myApp.setPlayerNames(names[0], names[1]);
            myApp.setCurrentRoomId(roomId);
            myApp.showGameScene();
        });
    }

    private void showError(String text) {
        new Alert(Alert.AlertType.ERROR, text).showAndWait();
    }
}