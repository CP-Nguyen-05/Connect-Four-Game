import shared.User;

import shared.MessageType;
import shared.Message;
import Controller.RoomView;

import java.util.UUID;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.net.SocketTimeoutException;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class ConnectFourApp extends Application {
    private Stage primaryStage;
    private User currentUser;
    private Client client;  // or ClientConnection + Client wrapper
    private ClientConnection conn;

    private Scene roomScene;
    private RoomController roomCtrl;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Connect Four");
        showLoginScene();
        stage.show();
    }

    private void showLoginScene() {
        // Build controls
        Label userLbl = new Label("Username:");
        TextField userFld = new TextField();
        Label passLbl = new Label("Password:");
        PasswordField passFld = new PasswordField();
        Label msgLbl  = new Label();      // <-- will show register/login feedback

        Button loginBtn = new Button("Login");
        Button regBtn   = new Button("Register");

        // REGISTER handler
        regBtn.setOnAction(e -> {
            String u = userFld.getText().trim();
            String p = passFld.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                msgLbl.setText("Enter both username & password");
                return;
            }
            try {
                // 1) Connect once
                if (conn == null) {
                    conn = new ClientConnection();
                    conn.connect("localhost", 12345);
                }
                // 2) Send REGISTER
                Message reg = new Message(
                        UUID.randomUUID().toString(),
                        MessageType.REGISTER,
                        p,    // password in content
                        u,    // sender=username
                        null,
                        System.currentTimeMillis()
                );
                conn.sendMessage(reg);

                // 3) BLOCKING read of server’s reply
                Message reply = conn.receiveMessage();
                if (reply.getType() == MessageType.CHAT) {
                    msgLbl.setText(reply.getContent());
                } else {
                    msgLbl.setText("Error: " + reply.getContent());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                msgLbl.setText("Server error: " + ex.getMessage());
            }
        });

        // LOGIN handler
        loginBtn.setOnAction(e -> {
            String u = userFld.getText().trim();
            String p = passFld.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                msgLbl.setText("Enter both username & password");
                return;
            }
            try {
                // 1) If we haven’t connected yet, do so now
                if (conn == null) {
                    conn = new ClientConnection();
                    conn.connect("localhost", 12345);
                }

                // 2) Send LOGIN (no need to REGISTER first)
                Message login = new Message(
                        UUID.randomUUID().toString(),
                        MessageType.LOGIN,
                        p,    // password in content
                        u,    // sender=username
                        null,
                        System.currentTimeMillis()
                );
                conn.sendMessage(login);

                // 3) Wait for server reply
                Message reply = conn.receiveMessage();
                if (reply.getType() == MessageType.ERROR) {
                    msgLbl.setText(reply.getContent());
                }
                else if (reply.getType() == MessageType.LOGIN_SUCCESS) {
                    // Split the CSV back into fields:
                    String[] parts = reply.getContent().split(",", -1);
                    // parts[0]=displayName,1=username,2=password,3=score,4=gamesPlayed,5=winCount,6=lossCount,7=drawCount
                    currentUser = new User(
                            parts[0],
                            parts[1],
                            parts[2],
                            Integer.parseInt(parts[3]),
                            Integer.parseInt(parts[4]),
                            Integer.parseInt(parts[5]),
                            Integer.parseInt(parts[6]),
                            Integer.parseInt(parts[7])
                    );
                    showOptionMenuScene();
                }
                else {
                    msgLbl.setText("Unexpected response: " + reply.getType());
                }


            } catch (Exception ex) {
                ex.printStackTrace();
                msgLbl.setText("Server error: " + ex.getMessage());
            }
        });

        // Layout
        HBox buttons = new HBox(10, loginBtn, regBtn);
        VBox root = new VBox(10,
                userLbl, userFld,
                passLbl, passFld,
                buttons,
                msgLbl
        );
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 320, 240));
    }

    private void showOptionMenuScene() {
        Button lanBtn      = new Button("LAN Play");
        Button compBtn     = new Button("Computer");
        Button howToPlayBtn= new Button("How To Play");
        Button profileBtn  = new Button("Profile");

        lanBtn.setOnAction(e -> showRoomScene());
        // compBtn.setOnAction(e -> startSinglePlayer());
        howToPlayBtn.setOnAction(e -> showHowToPlayScene());
        profileBtn.setOnAction(e -> showProfileScene());

        VBox root = new VBox(15, lanBtn, compBtn, howToPlayBtn, profileBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        primaryStage.setScene(new Scene(root, 400, 300));
    }

    private void showRoomScene() {
        // Build UI once
        if (roomScene == null) {
            // 1) Create controls
            ListView<RoomView> listView = new ListView<>(availableRooms);
            TextField roomIdField       = new TextField();
            roomIdField.setPromptText("Enter room ID");
            Label messageLabel          = new Label();

            Button refreshBtn    = new Button("Refresh");
            Button quickJoinBtn  = new Button("Quick Join");
            Button joinByIdBtn   = new Button("Join By ID");
            Button createBtn     = new Button("Create Room");
            Button spectateBtn   = new Button("Spectate");
            Button backBtn       = new Button("Back");

            // 2) Instantiate the RoomController
            roomCtrl = new RoomController(
                    conn,
                    currentUser,
                    this,
                    availableRooms,
                    listView,
                    roomIdField,
                    messageLabel
            );

            // 3) Wire buttons
            refreshBtn   .setOnAction(e -> roomCtrl.fetchAvailableRooms());
            quickJoinBtn .setOnAction(e -> roomCtrl.handleQuickJoin());
            joinByIdBtn  .setOnAction(e -> roomCtrl.handleJoinRoomById());
            createBtn    .setOnAction(e -> roomCtrl.handleCreateRoom());
            spectateBtn  .setOnAction(e -> roomCtrl.handleJoinAsSpectator());
            backBtn      .setOnAction(e -> showOptionMenuScene());

            // 4) Layout
            HBox idRow   = new HBox(8, roomIdField, joinByIdBtn, spectateBtn);
            idRow.setAlignment(Pos.CENTER);
            HBox buttons = new HBox(10, refreshBtn, quickJoinBtn, createBtn, backBtn);
            buttons.setAlignment(Pos.CENTER);

            VBox root = new VBox(15,
                    new Label("Available Rooms"),
                    listView,
                    idRow,
                    messageLabel,
                    buttons
            );
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);

            roomScene = new Scene(root, 500, 400);
        }

        // Show the scene and load data
        primaryStage.setScene(roomScene);
        roomCtrl.fetchAvailableRooms();
    }





    private void showGameScene() {
        // build your 7×6 GridPane of Circles here,
        // add click‑handlers that call client.send(MOVE) and
        // update the board locally.
    }


    private void showHowToPlayScene() {
        TextArea howTo = new TextArea("Rules:\n1. … \n2. …");
        howTo.setEditable(false);
        Button back = new Button("Back");
        back.setOnAction(e -> showOptionMenuScene());
        VBox root = new VBox(10, howTo, back);
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 400, 400));
    }
    private void showProfileScene() {
        // Build labels from currentUser
        Label nameLbl  = new Label("Display Name:   " + currentUser.getDisplayName());
        Label userLbl  = new Label("Username:       " + currentUser.getUsername());
        Label scoreLbl = new Label("Score:          " + currentUser.getScore());
        Label playedLbl= new Label("Games Played:   " + currentUser.getGamesPlayed());
        Label winLbl   = new Label("Wins:           " + currentUser.getWinCount());
        Label lossLbl  = new Label("Losses:         " + currentUser.getLossCount());
        Label drawLbl  = new Label("Draws:          " + currentUser.getDrawCount());

        // Home button
        Button homeBtn = new Button("Home");
        homeBtn.setOnAction(e -> showOptionMenuScene());

        // Delete account button
        Button delBtn = new Button("Delete Account");
        delBtn.setOnAction(e -> showDeleteAccountConfirm());

        VBox root = new VBox(10,
                nameLbl, userLbl,
                scoreLbl, playedLbl,
                winLbl, lossLbl, drawLbl,
                new HBox(10, homeBtn, delBtn)
        );
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 350, 300));
    }
    private void showDeleteAccountConfirm() {
        Label confirm = new Label("Are you sure you want to delete\nyour account?");
        confirm.setWrapText(true);

        Button yes = new Button("Yes");
        Button no = new Button("No");

        yes.setOnAction(e -> {
            yes.setDisable(true);
            no.setDisable(true);
            confirm.setText("Deleting account...");

            new Thread(() -> {
                try {
                    Message del = new Message(
                            UUID.randomUUID().toString(),
                            MessageType.DELETE_ACCOUNT,
                            currentUser.getPassword(),
                            currentUser.getUsername(),
                            null,
                            System.currentTimeMillis()
                    );
                    System.out.println("Sending DELETE_ACCOUNT message for user: " + currentUser.getUsername());
                    conn.sendMessage(del);

                    Message reply = conn.receiveMessage();
                    System.out.println("Received response: " + reply.getType() + " - " + reply.getContent());

                    Platform.runLater(() -> {
                        if (reply.getType() == MessageType.ERROR) {
                            new Alert(AlertType.ERROR, reply.getContent()).showAndWait();
                            showProfileScene();
                        } else if (reply.getType() == MessageType.DELETE_ACCOUNT_SUCCESS ) {
                            try {
                                conn.close();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            conn = null;
                            currentUser = null;
                            showLoginScene();
                        } else {
                            new Alert(AlertType.ERROR, "Unexpected server response.").showAndWait();
                            showProfileScene();
                        }
                    });
                } catch (SocketTimeoutException ex) {
                } catch (Exception ex) {
                }
            }, "DeleteAccount-Thread").start();
        });

        no.setOnAction(e -> showProfileScene());

        HBox buttons = new HBox(10, yes, no);
        VBox root = new VBox(10, confirm, buttons);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        primaryStage.setScene(new Scene(root, 300, 150));
    }

    public static void main(String[] args) {
        launch(args);
    }
}