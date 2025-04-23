// File: ProfileController.java
package shared;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfileController {
    private final ConnectFourApp app;
    private final Scene scene;

    // these labels will show the user's data
    private final Label usernameLabel    = new Label();
    private final Label scoreLabel       = new Label();
    private final Label gamesPlayedLabel = new Label();
    private final Label winCountLabel    = new Label();
    private final Label lossCountLabel   = new Label();
    private final Label drawCountLabel   = new Label();

    public ProfileController(ConnectFourApp app) {
        this.app = app;

        // build the UI
        Label header = new Label("Your Profile");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button homeBtn = new Button("Home");
        homeBtn.setOnAction(e -> app.showOptionMenuScene());
        Button deleteBtn = new Button("Delete Account");
        deleteBtn.setOnAction(e -> app.showDeleteAccountConfirm());

        HBox buttons = new HBox(10, homeBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(10,
                header,
                new HBox(10, new Label("Username:       "), usernameLabel),
                new HBox(10, new Label("Score:          "), scoreLabel),
                new HBox(10, new Label("Games Played:   "), gamesPlayedLabel),
                new HBox(10, new Label("Wins:           "), winCountLabel),
                new HBox(10, new Label("Losses:         "), lossCountLabel),
                new HBox(10, new Label("Draws:          "), drawCountLabel),
                buttons
        );
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);

        scene = new Scene(root, 1600, 900);
        app.applyGlobalStyles(scene);
    }

    /** Call this right before showing the profile scene. */
    public void displayProfileInfo(User user) {
        usernameLabel   .setText(user.getUsername());
        scoreLabel      .setText(String.valueOf(user.getScore()));
        gamesPlayedLabel.setText(String.valueOf(user.getGamesPlayed()));
        winCountLabel   .setText(String.valueOf(user.getWinCount()));
        lossCountLabel  .setText(String.valueOf(user.getLossCount()));
        drawCountLabel  .setText(String.valueOf(user.getDrawCount()));
    }

    public Scene getScene() {
        return scene;
    }
}
