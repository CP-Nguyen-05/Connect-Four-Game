package Controller;

import javafx.geometry.Pos;
import javafx.scene.text.Font;
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
    private ConnectFourApp myApp;
    private Scene myLoginScene;
    private TextField usernameTextBox;
    private PasswordField passwordTextBox;
    private Label errorMessageLabel;

    public LoginController(ConnectFourApp app) {
        myApp = app;
        buildLoginScene();
    }

    // Build the login UI
    private void buildLoginScene() {
        Label titleLabel = new Label("LOGIN");
        titleLabel.setFont(Font.font(ConnectFourApp.globalFontFamily, 48));
        titleLabel.setStyle("-fx-text-fill: #0087F1");

        HBox titleContainer = new HBox();
        titleContainer.getChildren().add(titleLabel);
        titleContainer.setAlignment(Pos.CENTER_LEFT);

        usernameTextBox = new TextField();
        usernameTextBox.setPromptText("Enter Username");

        passwordTextBox = new PasswordField();
        passwordTextBox.setPromptText("Enter Password");

        Button loginButton = new Button("LOGIN");
        loginButton.setMaxWidth(1000);
        loginButton.setStyle("-fx-background-color: #0087F1; -fx-text-fill: #FFF; -fx-font-weight: bold;");

        Button registerButton = new Button("Need an account? Sign up");
        registerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #0087F1;");

        errorMessageLabel = new Label("");
        HBox messageContainer = new HBox();
        messageContainer.getChildren().add(errorMessageLabel);
        errorMessageLabel.setStyle("-fx-text-fill: #F24339");
        messageContainer.setAlignment(Pos.CENTER_LEFT);

        // Button actions
        loginButton.setOnAction(event -> {
            doLogin();
        });

        registerButton.setOnAction(event -> {
            myApp.showRegisterScene();
        });

        VBox formLayout = new VBox();
        formLayout.setSpacing(10);
        formLayout.getChildren().add(titleContainer);
        formLayout.getChildren().add(messageContainer);
        formLayout.getChildren().add(usernameTextBox);
        formLayout.getChildren().add(passwordTextBox);
        formLayout.getChildren().add(loginButton);
        formLayout.getChildren().add(registerButton);
        formLayout.setAlignment(Pos.CENTER);
        formLayout.setMaxWidth(500);
        formLayout.setMaxHeight(200);
        formLayout.setPadding(new Insets(20));
        formLayout.setStyle("-fx-background-color: #1D2529; -fx-border-color: #374A4D; -fx-border-radius: 10; -fx-background-radius: 10;");

        StackPane rootLayout = new StackPane();
        rootLayout.getChildren().add(formLayout);
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.setStyle("-fx-background-color: #374A4D;");

        myLoginScene = new Scene(rootLayout, 1600, 900);
        myApp.applyGlobalStyles(myLoginScene);
    }

    public Scene getScene() {
        return myLoginScene;
    }

    // Handle login attempt
    private void doLogin() {
        String username = usernameTextBox.getText();
        String password = passwordTextBox.getText();
        passwordTextBox.setText("");
        errorMessageLabel.setText("");

        if (username.equals("") || password.equals("")) {
            errorMessageLabel.setText("Please enter username and password!");
            errorMessageLabel.setStyle("-fx-text-fill: #FF6368;");
            return;
        }

        try {
            ClientConnection connection = myApp.getOrCreateConnection();
            Message loginMessage = new Message(
                    MessageType.LOGIN,
                    password,
                    username,
                    null
            );
            connection.sendMessage(loginMessage);

            Message serverReply = connection.receiveMessage();
            if (serverReply.getType() == MessageType.LOGIN_SUCCESS) {
                errorMessageLabel.setText("Login worked!");
                errorMessageLabel.setStyle("-fx-text-fill: #0087F1;");
                myApp.finishLogin(serverReply.getContent());
            } else {
                errorMessageLabel.setText(serverReply.getContent());
                errorMessageLabel.setStyle("-fx-text-fill: #FF6368;");
            }
        } catch (Exception e) {
            errorMessageLabel.setText("Error connecting to server: " + e.getMessage());
            errorMessageLabel.setStyle("-fx-text-fill: #FF6368;");
        }
    }
}