package shared;

import Controller.LoginController;
import Controller.RoomView;
import Controller.RoomController;

import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

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
    private final List<RoomView> availableRooms = new ArrayList<>();
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

    private boolean myTurn;
    public void setMyTurn(boolean isMyTurn) {
        this.myTurn = isMyTurn;
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

        // 3) Go back to the login screen
        showLoginScene();
    }


    public void showLoginScene() {
        primaryStage.setScene(loginCtrl.getScene());
    }

    public void applyGlobalStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/global.css")).toExternalForm());
    }

    public void showRegisterScene() {
        primaryStage.setScene(registerCtrl.getScene());
    }

    public void showOptionMenuScene() {
        Label title = new Label("Connect Four");
        title.setFont(Font.font(ConnectFourApp.globalFontFamily, 80));  // Big title
        title.setStyle(
                "-fx-text-fill: white;" +
                "-fx-background-color: transparent;" +
                "-fx-padding: 0 0 0 0;" +
                "-fx-text-alignment: center;"
        );
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Button lanButton = new Button("LAN Play");
        Button compButton = new Button("Computer");
        Button howToPlayButton = new Button("How To Play");
        Button profileButton = new Button("Profile");
        Button logoutButton = new Button("Logout");

        lanButton.setOnAction(e -> showRoomScene());
        // compBtn.setOnAction(e -> startSinglePlayer());
        howToPlayButton.setOnAction(e -> showHowToPlayScene());
        profileButton.setOnAction(e -> showProfileScene());
        logoutButton.setOnAction(e -> logout());

        VBox root = new VBox(30, lanButton, compButton, howToPlayButton, profileButton, logoutButton);
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
        root.setMaxHeight(300);
        root.setPrefHeight(300);
        root.setMinHeight(300);

        VBox layout = new VBox(50, title, root);  // 50 px space between title and menu
        layout.setAlignment(Pos.CENTER);
        VBox.setMargin(title, new Insets(0, 0, 0, 0));

        StackPane background = new StackPane(layout);
        background.setStyle("-fx-background-color: #008081;");

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

            // 3) Instantiate controller with the plain list and text area
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
        // 1) Build the static board
        GridPane board = new GridPane();
        board.setHgap(5);
        board.setVgap(5);
        board.setPadding(new Insets(10));
        Circle[][] cells = new Circle[6][7];
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                Circle cell = new Circle(20, Color.LIGHTGRAY);
                cell.setStroke(Color.DARKGRAY);
                cells[row][col] = cell;
                board.add(cell, col, row);
            }
        }

        // 2) Turn input UI + status field
        TextField colField = new TextField();
        colField.setPromptText("0–6");
        colField.setPrefWidth(50);

        TextField statusField = new TextField();
        statusField.setEditable(false);
        statusField.setPrefWidth(120);
        statusField.setText(myTurn ? "Your turn!" : "Opponent…");

        Button dropBtn = new Button("Drop");
        dropBtn.setDisable(!myTurn);  // only clickable when it's your turn

        // only enable when a valid 0–6 is entered
        colField.textProperty().addListener((obs, o, n) -> {
            try {
                int c = Integer.parseInt(n.trim());
                dropBtn.setDisable(! (myTurn && c >= 0 && c <= 6));
            } catch (Exception ex) {
                dropBtn.setDisable(true);
            }
        });

        dropBtn.setOnAction(e -> {
            String txt = colField.getText().trim();
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.MOVE,
                        txt,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // immediately switch to opponent:
            myTurn = false;
            statusField.setText("Opponent…");
            dropBtn.setDisable(true);
            colField.clear();
        });

        HBox inputRow = new HBox(10,
                new Label("Column:"), colField, dropBtn, statusField
        );
        inputRow.setAlignment(Pos.CENTER);

        // 3) Chat panel (unchanged)
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefSize(250, 300);

        TextField chatInput = new TextField();
        chatInput.setPromptText("Type message…");
        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(ev -> {
            String txt = chatInput.getText().trim();
            if (!txt.isEmpty()) {
                try {
                    conn.sendMessage(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.CHAT,
                            txt,
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

        Button surrenderBtn = new Button("Surrender");
        surrenderBtn.setOnAction(ev -> {
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
                new Label("Chat"), chatArea, chatForm, surrenderBtn
        );
        chatPane.setPadding(new Insets(10));
        chatPane.setAlignment(Pos.CENTER);

        // 4) Layout everything
        VBox leftPane = new VBox(15, inputRow, board);
        leftPane.setAlignment(Pos.CENTER);
        HBox root = new HBox(20, leftPane, chatPane);
        root.setPadding(new Insets(10));

        gameScene = new Scene(root, 750, 450);
        primaryStage.setScene(gameScene);
        primaryStage.show();

        // 5) Listener thread: paint moves, chat, end, *and* flip turns
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = conn.receiveMessage();
                    MessageType t = msg.getType();

                    if (t == MessageType.MOVE) {
                        // “col,row”
                        String[] parts = msg.getContent().split(",",2);
                        int c = Integer.parseInt(parts[0]);
                        int r = Integer.parseInt(parts[1]);
                        Color fill = msg.getSender().equals(currentUser.getUsername())
                                ? Color.RED
                                : Color.YELLOW;
                        Platform.runLater(() -> cells[r][c].setFill(fill));

                        // if it was *their* move, now it's your turn
                        if (!msg.getSender().equals(currentUser.getUsername())) {
                            myTurn = true;
                            Platform.runLater(() -> {
                                statusField.setText("Your turn!");
                                dropBtn.setDisable(false);
                            });
                        }
                    }
                    else if (t == MessageType.CHAT) {
                        Platform.runLater(() ->
                                chatArea.appendText(msg.getSender()+": "+msg.getContent()+"\n")
                        );
                    }
                    else if (t == MessageType.GAME_END) {
                        final String outcome;
                        if ("YOU_WIN".equals(msg.getContent())) {
                            outcome = "You won!";
                            currentUser.setScore(currentUser.getScore()+10);
                            currentUser.setWinCount(currentUser.getWinCount() + 1);
                            currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                        }
                        else if ("YOU_LOSE".equals(msg.getContent())) {
                            outcome = "You lost!";
                            currentUser.setScore(currentUser.getScore()-10);
                            currentUser.setLossCount(currentUser.getLossCount() + 1);
                            currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                        }
                        else                                           outcome = "Draw!";
                        Platform.runLater(() -> {
                            new Alert(AlertType.INFORMATION, outcome).showAndWait();
                            showOptionMenuScene();
                        });
                        setMyTurn(false);
                        return;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "GameListener").start();
    }

    private void showHowToPlayScene() {
        TextField title = new TextField("How to play");
        title.setMouseTransparent(true);
        title.setFocusTraversable(false);
        title.setEditable(false);  // Make it read-only
        title.setFont(Font.font(ConnectFourApp.globalFontFamily, 64));
        title.setStyle("-fx-text-fill: white; -fx-background-color: #03BF9A;");
        title.setMinWidth(1000);
        title.setMaxWidth(300);
        title.setAlignment(Pos.CENTER); // Center text horizontally

        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);

        TextArea howTo = new TextArea(
                        "- Two players take turns dropping discs into columns.\n\n" +
                        "- Discs fall to the lowest available space in the selected column.\n\n" +
                        "- The goal is to connect four of your discs in a row:\n\n" +
                        "        - Horizontally\n\n" +
                        "        – Vertically\n\n" +
                        "        – Diagonally\n\n" +
                        "– The first player to connect four wins the game.\n\n" +
                        "– If the board is full and no one wins, it’s a draw."
        );
        howTo.setEditable(false);
        howTo.setMouseTransparent(true);
        howTo.setFocusTraversable(false);
        howTo.setFont(Font.font(globalFontFamily, 36));
        howTo.setPrefWidth(1000);
        howTo.setMaxWidth(1000);
        howTo.setPrefHeight(650);
        howTo.setMaxHeight(650);
        howTo.setEditable(false);
        howTo.setStyle("-fx-control-inner-background: #03BF9A; -fx-text-fill: #FFF; ");

        VBox container = new VBox(howTo);
        container.setAlignment(Pos.CENTER);
        container.setPrefWidth(900);


        Button back = new Button("Back");
        back.setStyle("-fx-background-color: #1D90FF; -fx-text-fill: white; -fx-border-color: white; -fx-font-size: 24px");
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
        confirm.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
        confirm.setWrapText(true);

        Button yes = new Button("Yes");
        yes.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        Button no = new Button("No");
        no.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        yes.setOnAction(e -> {
            yes.setDisable(true);
            no.setDisable(true);
            confirm.setText("Deleting account...");

            new Thread(this::run, "DeleteAccount-Thread").start();
        });

        no.setOnAction(e -> showProfileScene());

        HBox buttons = new HBox(50, yes, no);
        buttons.setAlignment(Pos.CENTER);
        VBox root = new VBox(20, confirm, buttons);

        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #008081;");

        primaryStage.setScene(new Scene(root, 1600, 900));
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void run() {
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
                } else if (reply.getType() == MessageType.DELETE_ACCOUNT_SUCCESS) {
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
        } catch (Exception ex) {

        }
    }
}
