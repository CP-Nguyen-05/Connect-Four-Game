package shared;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpDiscoveryServer implements Runnable {
    private final int tcpPort;
    public UdpDiscoveryServer(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    @Override
    public void run() {
        try (DatagramSocket sock = new DatagramSocket(
                Utils.DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"))) {
            sock.setBroadcast(true);
            System.out.println("[DiscoverSrv] Listening on UDP port " + Utils.DISCOVERY_PORT);

            byte[] buf = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                sock.receive(packet);
                System.out.println("[DiscoverSrv] GOT ping from " + packet.getAddress());

                String msg = new String(
                        packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                if (Utils.DISCOVERY_REQUEST.equals(msg)) {
                    byte[] resp = (Utils.DISCOVERY_RESPONSE + tcpPort)
                            .getBytes(StandardCharsets.UTF_8);
                    DatagramPacket reply = new DatagramPacket(
                            resp, resp.length,
                            packet.getAddress(), packet.getPort());
                    sock.send(reply);
                    System.out.println("[DiscoverSrv] Replied to "
                            + packet.getAddress() + ":" + packet.getPort()
                            + " → TCP port " + tcpPort);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}