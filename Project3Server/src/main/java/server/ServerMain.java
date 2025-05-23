package server;
import shared.UdpDiscoveryServer;
public class ServerMain {
    public static void main(String[] args) throws Exception {
        int tcpPort = 12345;
        new Thread(new UdpDiscoveryServer(tcpPort)).start();
        Server.main(args);
    }
}