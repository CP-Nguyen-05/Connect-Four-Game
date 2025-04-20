import shared.User;
import javafx.scene.control.*;

public class ProfileController {
    public Label displayNameLabel;
    public Label usernameLabel;
    public Label scoreLabel;
    public Label gamesPlayedLabel;
    public Label winCountLabel;
    public Label lossCountLabel;
    public Label drawCountLabel;

    public void displayProfileInfo(User user) {
        displayNameLabel.setText(user.getDisplayName());
        usernameLabel.setText(user.getUsername());
        scoreLabel.setText(String.valueOf(user.getScore()));
        gamesPlayedLabel.setText(String.valueOf(user.getGamesPlayed()));
        winCountLabel.setText(String.valueOf(user.getWinCount()));
        lossCountLabel.setText(String.valueOf(user.getLossCount()));
        drawCountLabel.setText(String.valueOf(user.getDrawCount()));
    }
}