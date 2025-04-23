package Controller;
import javafx.geometry.Pos;
import shared.ClientConnection;
import shared.ConnectFourApp;
import shared.Message;
import shared.MessageType;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.UUID;

public class LoginController {
    private final ConnectFourApp app;
    private Scene scene;

    private TextField    usernameField;
    private PasswordField passwordField;
    private Label         messageLabel;

    public LoginController(ConnectFourApp app) {
        this.app = app;
        buildScene();
    }

    private void buildScene() {
        // Title label styled like original
        Label title = new Label("Login");
        title.setStyle("-fx-font-family: W95FA;");
        title.setStyle("-fx-font-size: 36");
        HBox titleBox = new HBox(title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // Input fields
        usernameField = new TextField();
        usernameField.setPromptText("Username");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        // Buttons
        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Button regBtn = new Button("Need an account? Sign up");
        regBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #26268C; -fx-underline: true; -fx-cursor: hand;");

        // Message label
        messageLabel = new Label();
        HBox messageBox = new HBox(messageLabel);
        messageBox.setAlignment(Pos.CENTER_LEFT);

        // Button actions
        loginBtn.setOnAction(e -> doLogin());
        regBtn.setOnAction(e -> app.showRegisterScene());

        // Form layout
        VBox form = new VBox(10,
                titleBox,
                messageBox,
                usernameField,
                passwordField,
                loginBtn,
                regBtn
        );
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(300);
        form.setMaxHeight(100);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #C0C0C0; -fx-border-color: #FFF; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Root layout
        StackPane root = new StackPane(form);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #008081;");

        // Set scene
        scene = new Scene(root, 1600, 900);
    }

    public Scene getScene() {
        return scene;
    }

    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        usernameField.clear();
        passwordField.clear();
        if (u.isEmpty() || p.isEmpty()) {
            messageLabel.setText("Enter both username & password");
            return;
        }

        try {
            // get or open the connection
            ClientConnection conn = app.getOrCreateConnection();

            // send LOGIN
            Message m = new Message(
                    UUID.randomUUID().toString(),
                    MessageType.LOGIN,
                    p, u, null,
                    System.currentTimeMillis()
            );
            conn.sendMessage(m);

            // await reply
            Message reply = conn.receiveMessage();
            if (reply.getType() == MessageType.LOGIN_SUCCESS) {
                app.finishLogin(reply.getContent());
            } else {
                messageLabel.setText(reply.getContent());
            }
        } catch (Exception ex) {
            messageLabel.setText("Server error: " + ex.getMessage());
        }
    }
}