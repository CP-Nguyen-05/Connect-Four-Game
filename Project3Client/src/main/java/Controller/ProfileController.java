// File: ProfileController.java
package shared;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class ProfileController {
    private final ConnectFourApp app;
    private final Scene scene;

    // these labels will show the user's data
    private final Label usernameOnlyLabel    = new Label();
    private final Label scoreLabel       = new Label();
    private final Label gamesPlayedLabel = new Label();
    private final Label winCountLabel    = new Label();
    private final Label lossCountLabel   = new Label();
    private final Label drawCountLabel   = new Label();

    public ProfileController(ConnectFourApp app) {
        this.app = app;

        // build the UI
        Button homeButton = new Button("Home");
        homeButton.setOnAction(e -> app.showOptionMenuScene());
        homeButton.setStyle("-fx-background-color: #1D90FF; -fx-text-fill: white; -fx-border-color: white");

        HBox homeArea = new HBox(homeButton);
        homeArea.setAlignment(Pos.TOP_LEFT);
        homeArea.setPadding(new Insets(10));

        usernameOnlyLabel.setStyle(
                "-fx-background-color: #F5B759;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 36;" +
                        "-fx-border-color: white;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-padding: 10;" +
                        "-fx-alignment: center;"
        );
        usernameOnlyLabel.setAlignment(Pos.CENTER);
        usernameOnlyLabel.setMaxWidth(300);
        usernameOnlyLabel.setWrapText(true);

        VBox usernameBox = new VBox(usernameOnlyLabel);
        usernameBox.setAlignment(Pos.CENTER);
        usernameBox.setPadding(new Insets(10));

        // Delete button box (bottom part)
        Button deleteButton = new Button("Delete Account");
        deleteButton.setStyle(
                "-fx-background-color: #e74c3c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 24px;" +              // ⬅️ Larger font
                        "-fx-padding: 15 30 15 30;" +
                        "-fx-border-color: #c0392b;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-cursor: hand;"
        );
        deleteButton.setOnAction(e -> app.showDeleteAccountConfirm());

        VBox deleteBox = new VBox(deleteButton);
        deleteBox.setAlignment(Pos.CENTER);
        deleteBox.setPadding(new Insets(20));
        deleteBox.setStyle("-fx-background-color: #03BF9A; -fx-background-radius: 30;");

        VBox statsArea = new VBox(10,
                createStatRow("Score:", scoreLabel),
                createStatRow("Games Played:", gamesPlayedLabel),
                createStatRow("Wins:", winCountLabel),
                createStatRow("Losses:", lossCountLabel),
                createStatRow("Draws:", drawCountLabel)
        );
        statsArea.setStyle("-fx-font-size: 36px;" +
                "-fx-alignment: center;" +
                "-fx-background-color: white;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;"
        );
        statsArea.setAlignment(Pos.CENTER);
        statsArea.setMaxWidth(400);
        statsArea.setMaxHeight(600);
        statsArea.setPadding(new Insets(20));
        // Combine into one vertical layout with spacing
        VBox centerBox = new VBox(40, usernameBox, statsArea, deleteBox);  // ⬅️ 40px gap
        centerBox.setAlignment(Pos.CENTER);

        // Combine with layout
        BorderPane root = new BorderPane();
        root.setTop(homeArea);      // Home at top-left
        root.setCenter(centerBox);  // Stats + delete in center
        root.setStyle("-fx-background-color: #03BF9A;");

        scene = new Scene(root, 1600, 900);
        app.applyGlobalStyles(scene);
    }

    /** Call this right before showing the profile scene. */
    public void displayProfileInfo(User user) {
        usernameOnlyLabel.setText(user.getUsername());
        scoreLabel.setText(String.valueOf(user.getScore()));
        gamesPlayedLabel.setText(String.valueOf(user.getGamesPlayed()));
        winCountLabel.setText(String.valueOf(user.getWinCount()));
        lossCountLabel.setText(String.valueOf(user.getLossCount()));
        drawCountLabel.setText(String.valueOf(user.getDrawCount()));
    }

    private HBox createStatRow(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setMinWidth(150);  // consistent width for titles
        label.setAlignment(Pos.CENTER_LEFT);  // left-align title

        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);  // right-align value

        HBox row = new HBox(10, label, valueLabel);
        row.setAlignment(Pos.CENTER);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);  // allow value label to grow
        return row;
    }

    public Scene getScene() {
        return scene;
    }
}
