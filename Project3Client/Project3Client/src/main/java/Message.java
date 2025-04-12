import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public String sender;      // Username of the sender
    public String recipientUser;  // Username of the receiver
    public String message;

    public Message(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public Message(String sender, String recipientUser, String message) {
        this.sender = sender;
        this.recipientUser = recipientUser;
        this.message = message;
    }

    public String toString() {
        if (recipientUser != null && !recipientUser.isEmpty()) {
            return "[Private] " + sender + " --→ " + recipientUser + ": " + message;
        } else {
            return sender + ": " + message;
        }
    }
}