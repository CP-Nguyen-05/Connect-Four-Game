package shared;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private final MessageType type;
    private final String      content;
    private final String      sender;
    private final String      recipient;

    public Message(MessageType type,
                   String content,
                   String sender,
                   String recipient) {
        this.type      = type;
        this.content   = content;
        this.sender    = sender;
        this.recipient = recipient;
    }

    public MessageType getType()      { return type; }
    public String      getContent()   { return content; }
    public String      getSender()    { return sender; }
    public String      getRecipient() { return recipient; }
}