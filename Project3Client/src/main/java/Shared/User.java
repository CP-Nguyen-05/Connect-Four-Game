public class User {
    private String displayName;
    private String username;
    private String password;
    private int score;
    private int gamesPlayed;
    private int winCount;
    private int lossCount;
    private int drawCount;

    public User(String displayName, String username, String password,
                int score, int gamesPlayed, int winCount, int lossCount, int drawCount) {
        this.displayName = displayName;
        this.username = username;
        this.password = password;
        this.score = score;
        this.gamesPlayed = gamesPlayed;
        this.winCount = winCount;
        this.lossCount = lossCount;
        this.drawCount = drawCount;
    }

    public String getDisplayName() { return displayName; }
    public String getUsername()    { return username; }
    public String getPassword()    { return password; }
    public int    getScore()       { return score; }
    public void   setScore(int s)  { this.score = s; }
    public int    getGamesPlayed() { return gamesPlayed; }
    public void   setGamesPlayed(int g) { this.gamesPlayed = g; }
    public int    getWinCount()    { return winCount; }
    public void   setWinCount(int w) { this.winCount = w; }
    public int    getLossCount()   { return lossCount; }
    public void   setLossCount(int l){ this.lossCount = l; }
    public int    getDrawCount()   { return drawCount; }
    public void   setDrawCount(int d){ this.drawCount = d; }
}