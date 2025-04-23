package shared;
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
        usernameField = new TextField();
        passwordField = new PasswordField();
        messageLabel  = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        Button loginBtn = new Button("Login");
        Button regBtn   = new Button("Register");

        loginBtn.setOnAction(e -> doLogin());
        regBtn  .setOnAction(e -> app.showRegisterScene());

        HBox buttons = new HBox(10, loginBtn, regBtn);
        buttons.setPadding(new Insets(10));
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        VBox root = new VBox(10,
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                buttons,
                messageLabel
        );
        root.setPadding(new Insets(20));
        root.setAlignment(javafx.geometry.Pos.CENTER);

        scene = new Scene(root, 320, 240);
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