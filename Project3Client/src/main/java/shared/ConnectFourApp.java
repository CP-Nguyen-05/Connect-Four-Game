package shared;

import Controller.LoginController;
import Controller.RoomView;
import Controller.RoomController;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.net.SocketTimeoutException;
import javafx.scene.control.ProgressIndicator;



import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;

import java.io.IOException;

public class ConnectFourApp extends Application {
    private Stage primaryStage;
    private User currentUser;
    private Client client;  // or ClientConnection + Client wrapper
    private ClientConnection conn;

    // Font
    public static final String FONT_PATH = "/fonts/W95FA.otf";
    public static final double DEFAULT_FONT_SIZE = 18;
    public static Font globalFont;
    public static String globalFontFamily = "W95FA";

    // Controllers
    private LoginController loginCtrl;
    private shared.RegisterController registerCtrl;
    private shared.ProfileController profileCtrl;

    private Scene roomScene, waitingScene, gameScene;
    private String currentRoomId;
    private TextField roomIdField;
    private Label messageLabel;   // ← pull this out
    private TextArea chatArea;
    private RoomController roomCtrl;
    private List<RoomView> availableRooms = new ArrayList<>();
    private TextArea roomListArea;

    public void setCurrentRoomId(String id) {
        this.currentRoomId = id;
    }

    private void loadCustomFont() {
        globalFont = Font.loadFont(getClass().getResourceAsStream(FONT_PATH), DEFAULT_FONT_SIZE);
        if (globalFont != null) {
            globalFontFamily = globalFont.getFamily();
            System.out.println("Loaded font: " + globalFont.getName());
        } else {
            System.out.println("Failed to load custom font");
        }
    }



    @Override
    public void start(Stage stage) {
        loadCustomFont();
        this.primaryStage = stage;
        stage.setTitle("Connect Four");

        loginCtrl   = new LoginController(this);
        registerCtrl = new shared.RegisterController(this);
        profileCtrl = new shared.ProfileController(this);

        showLoginScene();
        stage.show();
    }
    public ClientConnection getOrCreateConnection() throws IOException {
        if (conn == null) {
            conn = new ClientConnection();
            conn.connect("localhost", 12345);
        }
        return conn;
    }
    public void finishLogin(String csvPayload) {
        // parse your CSV into currentUser
        String[] parts = csvPayload.split(",", -1);
        currentUser = new User(
                parts[0], parts[1], parts[2],
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5]),
                Integer.parseInt(parts[6]),
                Integer.parseInt(parts[7])
        );
        showOptionMenuScene();
    }
    public void logout() {
        // 1) Close the socket, if open
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn = null;

        // 2) Clear any user state
        currentUser = null;
        currentRoomId = null;

        // 3) Go back to the log in screen
        showLoginScene();
    }


    public void showLoginScene() {
        primaryStage.setScene(loginCtrl.getScene());
    }

    public void applyGlobalStyles(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/styles/global.css").toExternalForm());
    }

    public void showRegisterScene() {
        primaryStage.setScene(registerCtrl.getScene());
    }

    public void showOptionMenuScene() {
        Button lanBtn      = new Button("LAN Play");
        Button compBtn     = new Button("Computer");
        Button howToPlayBtn= new Button("How To Play");
        Button profileBtn  = new Button("Profile");
        Button logoutBtn   = new Button("Logout");

        lanBtn.setOnAction(e -> showRoomScene());
        // compBtn.setOnAction(e -> startSinglePlayer());
        howToPlayBtn.setOnAction(e -> showHowToPlayScene());
        profileBtn.setOnAction(e -> showProfileScene());
        logoutBtn.setOnAction(e -> logout());

        VBox root = new VBox(10, lanBtn, compBtn, howToPlayBtn, profileBtn, logoutBtn);
        root.setStyle(
                "-fx-background-color: #C0C0C0;" +         // VBox background
                        "-fx-border-color: #FFFFFF;" +            // Border color
                        "-fx-border-width: 3;" +                  // Border thickness
                        "-fx-border-radius: 20;" +                // Rounded border corners
                        "-fx-background-radius: 20;"              // Rounded background to match
        );
        // VBox background
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(250);
        root.setMaxHeight(250);

        StackPane background = new StackPane(root);
        background.setStyle("-fx-background-color: #008081;");  // Scene background

        Scene scene = new Scene(background, 1600, 900);
        primaryStage.setScene(scene);
        applyGlobalStyles(scene);
    }

    public void showRoomScene() {
        if (roomScene == null) {
            // 1) Create the display area
            roomListArea = new TextArea();
            roomListArea.setEditable(false);
            roomListArea.setPrefRowCount(10);
            roomListArea.setWrapText(true);

            // 2) Reuse your existing input controls
            roomIdField = new TextField();
            roomIdField.setPromptText("Enter room ID");

            messageLabel   = new Label();

            Button refreshBtn    = new Button("Refresh");
            Button quickJoinBtn  = new Button("Quick Join");
            Button joinByIdBtn   = new Button("Join By ID");
            Button createBtn     = new Button("Create Room");
            Button spectateBtn   = new Button("Spectate");
            Button backBtn       = new Button("Back");

            // 3) Instantiate controller with the plain list + text area
            roomCtrl = new RoomController(
                    conn,
                    currentUser,
                    this,
                    availableRooms,
                    roomListArea,
                    roomIdField,
                    messageLabel
            );

            // 4) Wire buttons
            refreshBtn  .setOnAction(e -> roomCtrl.fetchAvailableRooms());
            quickJoinBtn.setOnAction(e -> roomCtrl.handleQuickJoin());
            joinByIdBtn .setOnAction(e -> roomCtrl.handleJoinRoomById());
            createBtn   .setOnAction(e -> roomCtrl.handleCreateRoom());
            spectateBtn .setOnAction(e -> roomCtrl.handleJoinAsSpectator());
            backBtn     .setOnAction(e -> showOptionMenuScene());

            HBox idRow   = new HBox(8, roomIdField, joinByIdBtn, spectateBtn);
            idRow.setAlignment(Pos.CENTER);
            HBox buttons = new HBox(10, refreshBtn, quickJoinBtn, createBtn, backBtn);
            buttons.setAlignment(Pos.CENTER);

            VBox root = new VBox(15,
                    new Label("Available Rooms"),
                    roomListArea,
                    idRow,
                    messageLabel,
                    buttons
            );
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);

            roomScene = new Scene(root, 1600, 900);
        }
        else {
            // ** re‑enable and clear them any time you come back **
            roomListArea.setDisable(false);
            roomIdField.setDisable(false);
            roomIdField.clear();
            messageLabel.setText("");
        }

        primaryStage.setScene(roomScene);
        roomCtrl.fetchAvailableRooms();
    }

    public void showWaitingScene() {
        if (waitingScene == null) {
            Label lbl = new Label("Waiting for an opponent to join…");
            lbl.setWrapText(true);
            ProgressIndicator spinner = new ProgressIndicator();

            Button cancel = new Button("Cancel");
            cancel.setOnAction(e -> roomCtrl.cancelCreateRoom());

            VBox root = new VBox(20, spinner, lbl, cancel);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            waitingScene = new Scene(root, 1600, 900);
        }
        primaryStage.setScene(waitingScene);
    }




    public void showGameScene() {
        // ——— Build the Connect‐4 grid (7 cols × 6 rows) ———
        GridPane board = new GridPane();
        board.setHgap(5);
        board.setVgap(5);
        board.setPadding(new Insets(10));
        Circle[][] cells = new Circle[6][7];
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                Circle cell = new Circle(20, Color.LIGHTGRAY);
                cell.setStroke(Color.DARKGRAY);
                final int c = col;
                cell.setOnMouseClicked(e -> {
                    try {
                        conn.sendMessage(new Message(
                                UUID.randomUUID().toString(),
                                MessageType.MOVE,
                                Integer.toString(c),
                                currentUser.getUsername(),
                                null,
                                System.currentTimeMillis()
                        ));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                cells[row][col] = cell;
                board.add(cell, col, row);
            }
        }

        // ——— Build the chat panel ———
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefWidth(250);
        chatArea.setPrefHeight(300);

        TextField chatInput = new TextField();
        chatInput.setPromptText("Type message...");
        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty()) {
                try {
                    conn.sendMessage(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.CHAT,
                            text,
                            currentUser.getUsername(),
                            null,
                            System.currentTimeMillis()
                    ));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                chatInput.clear();
            }
        });

        HBox chatForm = new HBox(5, chatInput, sendBtn);
        chatForm.setAlignment(Pos.CENTER);

        // ——— Surrender button ———
        Button surrenderBtn = new Button("Surrender");
        surrenderBtn.setOnAction(e -> {
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.SURRENDER,
                        currentRoomId,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        VBox chatPane = new VBox(10,
                new Label("Chat"),
                chatArea,
                chatForm,
                surrenderBtn
        );
        chatPane.setPadding(new Insets(10));
        chatPane.setAlignment(Pos.CENTER);

        // ——— Combine board + chat in one scene ———
        HBox root = new HBox(20, board, chatPane);
        root.setPadding(new Insets(10));
        gameScene = new Scene(root, 1600, 900);

        primaryStage.setScene(gameScene);

        // ——— Start a background listener for incoming messages ———
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = conn.receiveMessage();
                    switch (msg.getType()) {
                        case CHAT:
                            Platform.runLater(() ->
                                    chatArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n")
                            );
                            break;

                        case MOVE:
                            // payload is the column index, server must also tell row.
                            // here we assume server encodes "col,row" in content:
                            String[] parts = msg.getContent().split(",", -1);
                            int col = Integer.parseInt(parts[0]);
                            int row = Integer.parseInt(parts[1]);
                            Color color = msg.getSender().equals(currentUser.getUsername())
                                    ? Color.RED : Color.YELLOW;
                            Platform.runLater(() ->
                                    cells[row][col].setFill(color)
                            );
                            break;

                        case GAME_END:
                            Platform.runLater(() -> {
                                int checkResult;
                                AlertType type = AlertType.INFORMATION;
                                String result="";
                                if (msg.getContent().equals("YOU_WIN")){
                                    checkResult =0;
                                }
                                else if (msg.getContent().equals("YOU_LOSE")){
                                    checkResult =1;
                                }
                                else{
                                    checkResult =2;
                                }
                                if (checkResult == 0 || checkResult == 2){
                                    type = AlertType.INFORMATION;
                                }
                                if (checkResult == 0){
                                    result = "You won!";
                                }
                                else if (checkResult == 1){
                                    result = "You lost!";
                                }
                                else{
                                    result = "Draw!";
                                }
                                new Alert(type, result).showAndWait();
                                if (checkResult == 0) {
                                    this.currentUser.setWinCount(this.currentUser.getWinCount()+1);
                                }
                                else if (checkResult == 1) {
                                    this.currentUser.setDrawCount(this.currentUser.getDrawCount()+1);
                                }
                                else{
                                    this.currentUser.setLossCount(this.currentUser.getLossCount()+1);
                                }

                                this.currentUser.setGamesPlayed(this.currentUser.getGamesPlayed()+1);

                                showOptionMenuScene();
                            });
                            return;  // stop listening

                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "GameListener-Thread").start();
    }

    private void showHowToPlayScene() {
        TextField title = new TextField("How to play");
        title.setEditable(false);  // Make it read-only
        title.setFont(Font.font(ConnectFourApp.globalFontFamily, 64));
        title.setMinWidth(1000);
        title.setMaxWidth(300);
        title.setAlignment(Pos.CENTER); // Center text horizontally

        title.setStyle("-fx-background-color: #C0C0C0;");

        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);

        TextArea howTo = new TextArea(
                        "– Two players take turns dropping discs into columns.\n\n" +
                        "– Discs fall to the lowest available space in the selected column.\n\n" +
                        "– The goal is to connect four of your discs in a row:\n\n" +
                        "        – Horizontally\n\n" +
                        "        – Vertically\n\n" +
                        "        – Diagonally\n\n" +
                        "– The first player to connect four wins the game.\n\n" +
                        "– If the board is full and no one wins, it’s a draw."
        );
        howTo.setFont(Font.font(globalFontFamily, 36));
        howTo.setPrefWidth(1000);
        howTo.setMaxWidth(1000);
        howTo.setPrefHeight(650);
        howTo.setMaxHeight(650);
        howTo.setEditable(false);
        howTo.setStyle("-fx-control-inner-background: #C0C0C0;");

        VBox container = new VBox(howTo);
        container.setAlignment(Pos.CENTER);
        container.setPrefWidth(900);


        Button back = new Button("Back");
        back.setPrefWidth(200);
        back.setPrefHeight(50);
        back.setOnAction(e -> showOptionMenuScene());

        HBox buttonBox = new HBox(10, back);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, titleRow, container, buttonBox);
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 1600, 900));
        root.setStyle("-fx-background-color: #008081;");
        applyGlobalStyles(primaryStage.getScene());
    }
    private void showProfileScene() {
        profileCtrl.displayProfileInfo(currentUser);
        primaryStage.setScene(profileCtrl.getScene());
    }
    public void showDeleteAccountConfirm() {
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

        primaryStage.setScene(new Scene(root, 1600, 900));
    }

    public static void main(String[] args) {
        launch(args);
    }
}