package shared;

import Controller.LoginController;
import Controller.RoomView;
import Controller.RoomController;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ProgressIndicator;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.binding.Bindings;
import java.util.Random;
import javafx.animation.PauseTransition;


import java.util.Optional;


import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.IOException;

public class ConnectFourApp extends Application {
    private Stage primaryStage;
    private User currentUser;
    private Client client;  // or ClientConnection + Client wrapper
    private ClientConnection conn;

    // Font
    public static final String FONT_PATH = "/fonts/m6x11plus.ttf";
    public static final double DEFAULT_FONT_SIZE = 18;
    public static Font globalFont;
    public static String globalFontFamily = "W95FA";

    // Controllers
    private LoginController loginCtrl;
    private shared.RegisterController registerCtrl;
    private shared.ProfileController profileCtrl;

    private Scene roomScene, waitingScene, gameScene, resultScene;
    private String currentRoomId;
    private TextField roomIdField, statusField;
    private Label messageLabel;   // ← pull this out
    private TextArea chatArea;
    private RoomController roomCtrl;
    private final List<RoomView> availableRooms = new ArrayList<>();
    private TextArea roomListArea;
    public Label onlineCountField;


    private boolean gameOver = false;

    private boolean singlePlayerMode = false;
    private PauseTransition aiPause;

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
        roomScene    = null;
        waitingScene = null;
        gameScene    = null;
        resultScene  = null;
        roomCtrl     = null;


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
        Image bgImage = new Image(getClass().getResource("/backgrounds/hi.res.crt.png").toExternalForm());

        BackgroundImage bg = new BackgroundImage(
                bgImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, false)
        );

        Label title = new Label("CONNECT FOUR");
//        title.setFont(Font.font(ConnectFourApp.globalFontFamily));  // Big title
        title.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 160;" +
                "-fx-background-color: transparent;" +
                "-fx-padding: 0 0 0 0;" +
                "-fx-text-alignment: center;"
        );
        title.setAlignment(Pos.TOP_CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Button lanButton = new Button("LAN PLAY");
        lanButton.setStyle("-fx-background-color: #0087F1; -fx-text-fill: white; -fx-font-weight: bold;");
        lanButton.setPrefWidth(100);
        Button vsComputerBtn = new Button("COMPUTER");
        vsComputerBtn.setPrefWidth(100);
        vsComputerBtn.setStyle("-fx-background-color: #7D52AE; -fx-text-fill: white; -fx-font-weight: bold;");
        Button howToPlayButton = new Button("HOW TO PLAY");
        howToPlayButton.setPrefWidth(100);
        howToPlayButton.setStyle("-fx-background-color: #F98C02; -fx-text-fill: white; -fx-font-weight: bold;");
        Button profileButton = new Button("PROFILE");
        profileButton.setPrefWidth(100);
        profileButton.setStyle("-fx-background-color: #3E926F; -fx-text-fill: white; -fx-font-weight: bold;");
        Button logoutButton = new Button("LOGOUT");
        logoutButton.setPrefWidth(100);
        logoutButton.setStyle("-fx-background-color: #F24339; -fx-text-fill: white; -fx-font-weight: bold;");

        lanButton.setOnAction(e -> {
            gameOver = false;
            singlePlayerMode = false;
            showRoomScene();
        });

        vsComputerBtn.setOnAction(e -> {
            gameOver = false;
            singlePlayerMode = true;
            setMyTurn(true);         // ← give the human the first turn
            showGameScene();
        });
        howToPlayButton.setOnAction(e -> showHowToPlayScene());
        profileButton.setOnAction(e -> showProfileScene());
        logoutButton.setOnAction(e -> logout());

        VBox root = new VBox(10, lanButton, vsComputerBtn, howToPlayButton, profileButton, logoutButton);
        root.setStyle(
                "-fx-background-color: #374A4D;" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, black, 10, 0.5, 0, 4);"
        );
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(180);
        root.setMaxHeight(250);
        root.setPrefHeight(300);
        root.setMinHeight(250);

        VBox layout = new VBox(50, title, root);  // 50 px space between title and menu
        layout.setAlignment(Pos.CENTER);
        VBox.setMargin(title, new Insets(0, 0, 0, 0));

        StackPane background = new StackPane(layout); // layout = your VBox with title + buttons
        background.setBackground(new Background(bg));


        Scene scene = new Scene(background, 1600, 900);
        primaryStage.setScene(scene);
        applyGlobalStyles(scene);
    }

    public void showRoomScene() {
        if (onlineCountField == null) {
            onlineCountField = new Label("Online: 0");
            onlineCountField.setStyle("-fx-text-fill: white; -fx-font-size: 36px;");
        }
        onlineCountField.setStyle("-fx-text-fill: white; -fx-font-size: 36px;");
        singlePlayerMode = false;
        gameOver         = false;
        if (roomScene == null) {
            // 1) Create the display area
            TextField roomTitle = new TextField("AVAILABLE ROOMS");
            roomTitle.setFocusTraversable(false);
            roomTitle.setEditable(false);
            roomTitle.setStyle(
                    "-fx-text-fill: white;"+
                    "-fx-font-size: 64;" +
                    "-fx-background-color: #F24339;"+
                    "-fx-border-radius: 10;" +
                    "-fx-background-radius: 10;" +
                    " -fx-border-color: #374A4D;"
            );
            roomTitle.setMinWidth(800);
            roomTitle.setMaxWidth(200);
            roomTitle.setAlignment(Pos.CENTER);

            roomListArea = new TextArea();
            roomListArea.setEditable(false);
            roomListArea.setPrefRowCount(10);
            roomListArea.setWrapText(true);
            roomListArea.setPrefWidth(800);
            roomListArea.setMaxWidth(800);
            roomListArea.setPrefHeight(500);
            roomListArea.setMaxHeight(500);
            roomListArea.setStyle(
                    "-fx-control-inner-background: #1D2529;" +
                    " -fx-border-radius: 10;" +
                    " -fx-background-radius: 10;" +
                    " -fx-border-color: #374A4D;" +
                    "-fx-font-family: '" + ConnectFourApp.globalFontFamily + "';" +
                    "-fx-font-size: 48px;" +
                    "-fx-border-width: 2;" +
                    "-fx-focus-color: transparent;" +
                    "-fx-faint-focus-color: transparent;" +
                    "-fx-text-fill: white;" +
                    "-fx-caret-color: transparent;"
            );
            roomListArea.setEditable(false);
            roomListArea.setFocusTraversable(false);

            // 2) Reuse your existing input controls
            roomIdField = new TextField();
            roomIdField.setPromptText("Enter room ID");

            messageLabel   = new Label();

            Button refreshButton = new Button("REFRESH");
            refreshButton.setStyle("-fx-background-color: #3E926F; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 36px");
            refreshButton.setPrefWidth(190);
            refreshButton.setPrefHeight(40);
            Button quickJoinButton = new Button("QUICK JOIN");
            quickJoinButton.setStyle("-fx-background-color: #0087F1; -fx-text-fill: white; -fx-font-weight: bold;  -fx-font-size: 36px");
            quickJoinButton.setPrefWidth(190);
            quickJoinButton.setPrefHeight(40);
            Button joinByIdButton = new Button("JOIN");
            joinByIdButton.setStyle("-fx-background-color: #FF6368; -fx-text-fill: white; -fx-font-weight: bold;  -fx-font-size: 24px");
            Button createButton = new Button("CREATE");
            createButton.setStyle("-fx-background-color: #7D52AE; -fx-text-fill: white; -fx-font-weight: bold;  -fx-font-size: 36px");
            createButton.setPrefWidth(190);
            createButton.setPrefHeight(40);
            Button backButton  = new Button("BACK");
            backButton.setStyle("-fx-background-color: #F98C02; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 36px");
            backButton.setPrefWidth(190);
            backButton.setPrefHeight(40);

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
            refreshButton.setOnAction(e -> roomCtrl.fetchAvailableRooms());
            quickJoinButton.setOnAction(e -> roomCtrl.handleQuickJoin());
            joinByIdButton.setOnAction(e -> roomCtrl.handleJoinRoomById());
            createButton.setOnAction(e -> roomCtrl.handleCreateRoom());
//            spectateBtn .setOnAction(e -> roomCtrl.handleJoinAsSpectator());
            backButton.setOnAction(e -> showOptionMenuScene());

            HBox idRow = new HBox(8, roomIdField, joinByIdButton);
            idRow.setAlignment(Pos.CENTER_LEFT);
            idRow.setMaxWidth(800);
            HBox buttons = new HBox(10, refreshButton, quickJoinButton, createButton, backButton);
            buttons.setAlignment(Pos.CENTER);

            VBox root = new VBox(10,
                    roomTitle,
                    onlineCountField,
                    idRow,
                    roomListArea,
                    messageLabel,
                    buttons
            );
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #374A4D;");

            roomScene = new Scene(root, 1600, 900);
            applyGlobalStyles(roomScene);
            Platform.runLater(() -> primaryStage.getScene().getRoot().requestFocus());
        }
        else {
//            roomCtrl = new RoomController(
//                    conn,
//                    currentUser,
//                    this,
//                    availableRooms,
//                    roomListArea,
//                    roomIdField,
//                    messageLabel
//            );
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
            Label title = new Label("Waiting for an opponent to join…");
            title.setAlignment(Pos.CENTER);
            title.setStyle("-fx-font-size: 48px; -fx-text-fill: #FFF; -fx-background-color: #1D2529;");
            title.setWrapText(true);
            ProgressIndicator spinner = new ProgressIndicator();

            Button backButton = new Button("BACK");
            backButton.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #F98C02;");
            backButton.setPrefWidth(80);
            backButton.setOnAction(e -> roomCtrl.cancelCreateRoom());

            VBox card = new VBox(20, spinner, title, backButton);
            card.setAlignment(Pos.CENTER);
            card.setMaxWidth(1400);
            card.setPadding(new Insets(20));
            card.setStyle(
                    "-fx-background-color: #1D2529;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-radius: 10;" +
                    "-fx-border-color: #374A4D;" +
                    "-fx-font-size: 48px;" +
                    "-fx-border-width: 2;" +
                    "-fx-text-fill: #FFF"
            );

            VBox root = new VBox(card);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #374A4D;");

            root.setStyle("-fx-background-color: #374A4D;");
            waitingScene = new Scene(root, 1600, 900);
        }
        primaryStage.setScene(waitingScene);
        applyGlobalStyles(primaryStage.getScene());
    }




    public void showGameScene() {
        final boolean isSingle = singlePlayerMode;
        // 1) Build the static board
        GridPane board = new GridPane();
        board.setHgap(5);
        board.setVgap(5);
        board.setPadding(new Insets(10));
        Circle[][] cells = new Circle[6][7];

        statusField = new TextField();
        statusField.setEditable(false);
        statusField.setPrefWidth(120);
        statusField.setText(myTurn ? "Your turn!" : "Opponent…");

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                Circle cell = new Circle(20, Color.LIGHTGRAY);
                cell.setStroke(Color.DARKGRAY);
                cells[row][col] = cell;

                final int column = col;

                cell.setOnMouseEntered(e -> {
                    if (!myTurn) return;
                    for (int r = 0; r < 6; r++) {
                        Circle c = cells[r][column];
                        // only color the empty slots
                        if (c.getFill().equals(Color.LIGHTGRAY)) {
                            c.setFill(Color.BLACK);
                        }
                    }
                });

                cell.setOnMouseExited(e -> {
                    if (!myTurn) return;
                    for (int r = 0; r < 6; r++) {
                        Circle c = cells[r][column];
                        // restore only those we painted black
                        if (c.getFill().equals(Color.BLACK)) {
                            c.setFill(Color.LIGHTGRAY);
                        }
                    }
                });

                cell.setOnMouseClicked(e -> {
                    for (int r = 0; r < 6; r++) {
                        Circle c = cells[r][column];
                        // restore only those we painted black
                        if (c.getFill().equals(Color.BLACK)) {
                            c.setFill(Color.LIGHTGRAY);
                        }
                    }
                    if (!myTurn || gameOver) return;
                    if (isSingle) {
                        handleLocalMove(column, cells);
                    } else {
                        // multiplayer: send to server
                        try {
                            conn.sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.MOVE,
                                    String.valueOf(column),
                                    currentUser.getUsername(),
                                    null,
                                    System.currentTimeMillis()
                            ));
                            myTurn = false;
                            Platform.runLater(() -> statusField.setText("Opponent…"));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                board.add(cell, col, row);
            }
        }



        HBox inputRow = new HBox(10, statusField);
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
            if (singlePlayerMode) {
                myTurn = false;
                showResultPopUp("YOU LOSE!");
            } else {
                // LAN mode: tell the server
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

        gameScene = new Scene(root, 1600, 900);
        primaryStage.setScene(gameScene);
        primaryStage.show();

        // 5) Listener thread: paint moves, chat, end, *and* flip turns
        if (!singlePlayerMode) {
            new Thread(() -> {
                try {
                    while (true) {
                        Message msg = conn.receiveMessage();
                        MessageType t = msg.getType();
                        if (t == MessageType.ERROR) {
                            // show the alert, then put control back to the user
                            Platform.runLater(() -> {
                                new Alert(AlertType.WARNING, msg.getContent()).showAndWait();
                                // it must still be your turn, so re-enable the drop controls:
                                myTurn = true;
                                statusField.setText("Your turn!");
                            });
                        } else if (t == MessageType.MOVE) {
                            String[] parts = msg.getContent().split(",", 2);
                            int c = Integer.parseInt(parts[0]);
                            int r = Integer.parseInt(parts[1]);
                            Color fill = msg.getSender().equals(currentUser.getUsername())
                                    ? Color.BLUE
                                    : Color.RED;
                            Platform.runLater(() -> cells[r][c].setFill(fill));

                            // if it was *their* move, now it's your turn
                            if (!msg.getSender().equals(currentUser.getUsername())) {
                                myTurn = true;
                                Platform.runLater(() -> {
                                    statusField.setText("Your turn!");
                                });
                            } else {
                                statusField.setText("Opponent…");
                            }
                        } else if (t == MessageType.CHAT) {
                            Platform.runLater(() ->
                                    chatArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n")
                            );
                        } else if (t == MessageType.GAME_END) {
                            setMyTurn(false);
                            final String outcome;
                            if ("YOU_WIN".equals(msg.getContent())) {
                                outcome = "YOU WIN!";
                                currentUser.setScore(currentUser.getScore() + 10);
                                currentUser.setWinCount(currentUser.getWinCount() + 1);
                                currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                            } else if ("YOU_LOSE".equals(msg.getContent())) {
                                outcome = "YOU LOSE!";
                                currentUser.setScore(currentUser.getScore() - 10);
                                if (currentUser.getScore() < 0) {
                                    currentUser.setScore(currentUser.getScore() * 0);
                                }
                                currentUser.setLossCount(currentUser.getLossCount() + 1);
                                currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                            } else {
                                outcome = "DRAW!";
                                currentUser.setScore(currentUser.getScore() + 5);
                                currentUser.setScore(currentUser.getScore());
                                currentUser.setDrawCount(currentUser.getDrawCount() + 1);
                                currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                            }
                            Platform.runLater(() -> {
                                Platform.runLater(() -> showResultPopUp(outcome));
                            });
                            return;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, "GameListener").start();
        }
    }
    // ───── Single-player helper methods ─────

    /** Drops human piece, checks win, then AI move and checks win. */
    private void handleLocalMove(int column, Circle[][] cells) {
        if (gameOver){
            singlePlayerMode=false;
            return;
        }

        // 1) human drop
        int row = findDropRow(column, cells);
        if (row < 0) {
            new Alert(AlertType.WARNING, "That column is full!").showAndWait();
            return;
        }
        cells[row][column].setFill(Color.BLUE);
        if (checkWin(cells, row, column, Color.BLUE)) {
            gameOver = true;
            showResultPopUp("YOU WIN!");
            singlePlayerMode=false;
            return;
        }
        // **draw?**
        if (isDraw(cells)) {
            gameOver = true;
            showResultPopUp("Draw!");
            singlePlayerMode=false;
            return;
        }

        myTurn = false;
        statusField.setText("Opponent…");
        if (aiPause != null) aiPause.stop();

        // 2) AI move after delay
        aiPause = new PauseTransition(Duration.seconds(1));
        aiPause.setOnFinished(evt -> {
            int aiCol = pickRandomColumn(cells);
            int aiRow = findDropRow(aiCol, cells);
            cells[aiRow][aiCol].setFill(Color.RED);

            if (checkWin(cells, aiRow, aiCol, Color.RED)) {
                gameOver = true;
                Platform.runLater(() -> showResultPopUp("YOU LOSE!"));
                singlePlayerMode=false;
                return;
            }
            // **draw?**
            if (isDraw(cells)) {
                gameOver = true;
                Platform.runLater(() -> showResultPopUp("DRAW!"));
                singlePlayerMode=false;
                return;
            }

            myTurn = true;
            statusField.setText("Your turn!");
        });
        aiPause.play();
    }

    private int findDropRow(int col, Circle[][] cells) {
        for (int r = cells.length - 1; r >= 0; r--) {
            if (cells[r][col].getFill().equals(Color.LIGHTGRAY)) {
                return r;
            }
        }
        return -1;
    }

    private int pickRandomColumn(Circle[][] cells) {
        List<Integer> valid = new ArrayList<>();
        for (int c = 0; c < cells[0].length; c++) {
            if (findDropRow(c, cells) >= 0) valid.add(c);
        }
        return valid.get(new Random().nextInt(valid.size()));
    }

    private boolean checkWin(Circle[][] cells, int row, int col, Color color) {
        int[][] dirs = {
                { 0, 1 },
                { 1, 0 },
                { 1, 1 },
                { 1, -1 }
        };

        for (int[] d : dirs) {
            int count = 1
                    + countDirection(cells, row, col,  d[0],  d[1], color)
                    + countDirection(cells, row, col, -d[0], -d[1], color);
            if (count >= 4) return true;
        }
        return false;
    }

    private int countDirection(Circle[][] cells,
                               int r, int c,
                               int dr, int dc,
                               Color color) {
        int cnt = 0;
        for (int rr = r+dr, cc = c+dc;
             rr >= 0 && rr < cells.length && cc >= 0 && cc < cells[0].length;
             rr += dr, cc += dc) {
            if (cells[rr][cc].getFill().equals(color)) cnt++;
            else break;
        }
        return cnt;
    }
    /** true when there are no more valid drops left */
    private boolean isDraw(Circle[][] cells) {
        for (int c = 0; c < cells[0].length; c++) {
            if (findDropRow(c, cells) >= 0) return false;
        }
        return true;
    }

    // ONlY FOR 15s
    private void showResultScene(String outcomeText) {
        // Outcome message label
        Label outcome = new Label(outcomeText);
        outcome.setStyle("-fx-font-size: 48px; -fx-text-fill: #FFF; -fx-background-color: #1D2529");
        outcome.setAlignment(Pos.CENTER);
        outcome.setWrapText(true);
        outcome.setPrefWidth(1400);

        int seconds=0;
        if (singlePlayerMode) {
            seconds=5;
        }
        else if (outcomeText.equals("YOU WIN!")) {
            seconds = 14;
        }
        else if (outcomeText.equals("YOU LOSE!")) {
            seconds = 15;
        }
        else if (outcomeText.equals("DRAW!")) {
            seconds = 30;
        }
        // Create the two action buttons
        Button rematchButton = new Button("REMATCH");
        rematchButton.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #F24339;");
        rematchButton.setPrefWidth(100);
        Button menuButton  = new Button("MENU");
        menuButton.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #F98C02;");
        menuButton.setPrefWidth(100);

        // Create a label to show the countdown timer
        Label countdownLabel = new Label();
        countdownLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #0087F1;");

        // Property holding the remaining seconds with an initial value of 15
        final IntegerProperty timeSeconds = new SimpleIntegerProperty(seconds);
        // Bind the countdown label's text so it updates automatically
        countdownLabel.textProperty().bind(Bindings.concat("GO BACK MENU AFTER ", timeSeconds.asString(), " SECONDS"));

        // Layout the buttons in an HBox
        HBox buttons = new HBox(20, rematchButton, menuButton);
        buttons.setAlignment(Pos.CENTER);

        VBox innerBox = new VBox(20, outcome, countdownLabel, buttons);
        innerBox.setAlignment(Pos.CENTER);
        innerBox.setPrefWidth(1000);
        innerBox.setMaxWidth(1000);
        innerBox.setPadding(new Insets(20));
        innerBox.setStyle(
                "-fx-background-color: #1D2529;" +
                        "-fx-border-color: #374A4D;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10"
        );

        // VBox holds the outcome label, buttons, and countdown label
        VBox root = new VBox(innerBox);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #374A4D;");

        // Create the scene and show it in the primary stage
        resultScene = new Scene(root, 1600, 900);
        primaryStage.setScene(resultScene);
        applyGlobalStyles(primaryStage.getScene());


        if (singlePlayerMode){
            // 2a) live countdown
            Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                if (timeSeconds.get() > 0) timeSeconds.set(timeSeconds.get() - 1);
            }));
            countdown.setCycleCount(seconds);
            countdown.play();

            // 2b) auto-back
            PauseTransition autoBack = new PauseTransition(Duration.seconds(seconds));
            autoBack.setOnFinished(e -> showOptionMenuScene());
            autoBack.play();

            rematchButton.setOnAction(e -> {
                if (aiPause != null) aiPause.stop();
                autoBack.stop();
                countdown.stop();
                gameOver = false;
                singlePlayerMode = true;
                setMyTurn(true);
                showGameScene();
            });
            menuButton.setOnAction(e -> {
                autoBack.stop();
                countdown.stop();
                showOptionMenuScene();
            });

        }
        else{
            // 3a) live countdown
            Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                if (timeSeconds.get() > 0) timeSeconds.set(timeSeconds.get() - 1);
            }));
            countdown.setCycleCount(seconds);

            // 3b) auto-reject exactly once
            Timeline autoReject = new Timeline(new KeyFrame(Duration.seconds(seconds), e -> {
                try {
                    conn.sendMessage(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.REMATCH_REJECT,
                            currentRoomId,
                            currentUser.getUsername(),
                            null,
                            System.currentTimeMillis()
                    ));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                showOptionMenuScene();
            }));
            autoReject.setCycleCount(1);

            rematchButton.setOnAction(e -> {
                countdown.stop();
                autoReject.stop();
                try {
                    conn.sendMessage(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.REMATCH_REQUEST,
                            currentRoomId,
                            currentUser.getUsername(),
                            null,
                            System.currentTimeMillis()
                    ));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                showWaitingScene();
                new Thread(() -> {
                    try {
                        roomCtrl.waitForGameRematch();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(this::showOptionMenuScene);
                    }
                }).start();
            });

            menuButton.setOnAction(e -> {
                countdown.stop();
                autoReject.stop();
                try {
                    conn.sendMessage(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.REMATCH_REJECT,
                            currentRoomId,
                            currentUser.getUsername(),
                            null,
                            System.currentTimeMillis()
                    ));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                showOptionMenuScene();
            });

            countdown.play();
            autoReject.play();
        }
    }




    private void showResultPopUp(String outcomeText) {
        // Create an informational Alert dialog
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Result");
        dialog.setHeaderText(null);
        dialog.setContentText(outcomeText);

        // Set a custom "Next" button
        ButtonType nextBtn = new ButtonType("Next", ButtonBar.ButtonData.OK_DONE);
        dialog.getButtonTypes().setAll(nextBtn);

        // Create a Timeline to auto-dismiss the popup after 5 seconds
        Timeline autoCloseTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            System.out.println("5 seconds passed without user action. Auto-transitioning...");
            dialog.setResult(nextBtn); // Set the result as "Next"
            dialog.hide();             // Close the dialog
        }));
        autoCloseTimeline.setCycleCount(1);
        autoCloseTimeline.play();

        // Show the Alert dialog and wait for the user's response
        Optional<ButtonType> result = dialog.showAndWait();

        // Stop the timer in case the user responded before the timeout
        autoCloseTimeline.stop();

        // Process the user's response (or auto-transition)
        if (result.isPresent() && result.get() == nextBtn) {
            System.out.println("User clicked Next or timed out. Transitioning to result scene...");
            showResultScene(outcomeText);
        }
    }

    private void showHowToPlayScene() {
        TextField title = new TextField("HOW TO PLAY");
        title.setMouseTransparent(true);
        title.setFocusTraversable(false);
        title.setEditable(false);  // Make it read-only
        title.setStyle(
                "-fx-text-fill: white;"+
                "-fx-font-size: 64;" +
                "-fx-background-color: #AE65FF;"+
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                " -fx-border-color: #374A4D;"
        );
        title.setMinWidth(1000);
        title.setMaxWidth(300);
        title.setAlignment(Pos.CENTER);

        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);

        TextArea howTo = new TextArea(
                        "- Two players take turns dropping discs into columns.\n\n" +
                        "- Discs fall to the lowest available space in the selected column.\n\n" +
                        "- The goal is to connect four of your discs in a row:\n\n" +
                        "        - Horizontally\n\n" +
                        "        - Vertically\n\n" +
                        "        - Diagonally\n\n" +
                        "- The first player to connect four wins the game.\n\n" +
                        "- If the board is full and no one wins, it’s a draw."
        );
        howTo.setEditable(false);
        howTo.setMouseTransparent(true);
        howTo.setFocusTraversable(false);
        howTo.setPrefWidth(1000);
        howTo.setMaxWidth(1000);
        howTo.setPrefHeight(650);
        howTo.setMaxHeight(650);
        howTo.setStyle(
                "-fx-control-inner-background: #1D2529;" +
                "-fx-background-color: #1D2529;" +
                "-fx-border-color: #374A4D;" +
                "-fx-border-width: 2;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-caret-color: transparent;" +
                "-fx-font-size: 36px;"
        );

        VBox container = new VBox(howTo);
        container.setAlignment(Pos.CENTER);
        container.setPrefWidth(900);


        Button back = new Button("BACK");
        back.setStyle("-fx-background-color: #F98C02; -fx-text-fill: white; -fx-font-size: 36px");
        back.setPrefWidth(120);
        back.setPrefHeight(40);
        back.setOnAction(e -> showOptionMenuScene());

        HBox buttonBox = new HBox(back);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, titleRow, container, buttonBox);
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 1600, 900));
        root.setStyle("-fx-background-color: #374A4D;");
        applyGlobalStyles(primaryStage.getScene());
    }
    private void showProfileScene() {
        profileCtrl.displayProfileInfo(currentUser);
        primaryStage.setScene(profileCtrl.getScene());
    }
    public void showDeleteAccountConfirm() {
        Label confirm = new Label("ARE YOU SURE WANT TO DELETE YOUR ACCOUNT?");
        confirm.setStyle("-fx-font-size: 48px;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #1D2529"
        );
        confirm.setWrapText(true);
        confirm.setAlignment(Pos.CENTER);
        confirm.setPrefWidth(1400);

        Button yes = new Button("YES");
        yes.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #F24339;");
        yes.setPrefWidth(80);
        Button no = new Button("NO");
        no.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #F98C02;");
        no.setPrefWidth(80);

        yes.setOnAction(e -> {
            yes.setDisable(true);
            no.setDisable(true);
            confirm.setText("Deleting account...");

            new Thread(this::run, "DeleteAccount-Thread").start();
        });

        no.setOnAction(e -> showProfileScene());

        HBox buttons = new HBox(20, yes, no);
        buttons.setAlignment(Pos.CENTER);
        buttons.setStyle("-fx-background-color: #1D2529");
        VBox innerBox = new VBox(20, confirm, buttons);
        innerBox.setAlignment(Pos.CENTER);
        innerBox.setPrefWidth(1000);
        innerBox.setMaxWidth(1000);
        innerBox.setPadding(new Insets(20));
        innerBox.setStyle(
                "-fx-background-color: #1D2529;" +
                "-fx-border-color: #374A4D;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10"
        );

        VBox root = new VBox(innerBox);

        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #374A4D;");

        primaryStage.setScene(new Scene(root, 1600, 900));
        applyGlobalStyles(primaryStage.getScene());
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