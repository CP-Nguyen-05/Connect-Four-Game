// Message.java
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String      messageId;
    private final MessageType type;
    private final String      content;
    private final String      sender;
    private final String      recipient;
    private final long        timestamp;

    public Message(String messageId,
                   MessageType type,
                   String content,
                   String sender,
                   String recipient,
                   long timestamp) {
        this.messageId = messageId;
        this.type      = type;
        this.content   = content;
        this.sender    = sender;
        this.recipient = recipient;
        this.timestamp = timestamp;
    }

    public String      getMessageId() { return messageId; }
    public MessageType getType()      { return type; }
    public String      getContent()   { return content; }
    public String      getSender()    { return sender; }
    public String      getRecipient() { return recipient; }
    public long        getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        if (recipient != null && !recipient.isEmpty()) {
            return "[PRIVATE] " + sender + " → " + recipient + ": " + content;
        }
        return sender + ": " + content;
    }
}