package Controller;

public class RoomView {
    private final String roomId;
    private final int currentPlayerCount;
    private final int maxPlayerCapacity;
    private final boolean isOpen;

    public RoomView(String roomId,
                    int currentPlayerCount,
                    int maxPlayerCapacity,
                    boolean isOpen) {
        this.roomId             = roomId;
        this.currentPlayerCount = currentPlayerCount;
        this.maxPlayerCapacity  = maxPlayerCapacity;
        this.isOpen             = isOpen;
    }

    public String  getRoomId()             { return roomId; }
    public int     getCurrentPlayerCount() { return currentPlayerCount; }
    public int     getMaxPlayerCapacity()  { return maxPlayerCapacity; }
    public boolean isOpen()                { return isOpen; }
}