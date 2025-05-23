package shared;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpDiscoveryClient {
    /** Broadcasts a ping, waits up to timeoutMs for a reply,
     returns the server’s InetSocketAddress. */
    public static InetSocketAddress discover(int timeoutMs) throws IOException {
        try (DatagramSocket sock = new DatagramSocket()) {
            sock.setSoTimeout(timeoutMs);
            sock.setBroadcast(true);

            byte[] sendBuf = Utils.DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(
                    sendBuf, sendBuf.length,
                    InetAddress.getByName("255.255.255.255"),
                    Utils.DISCOVERY_PORT);

            System.out.println("[DiscoverCli] Broadcasting to 255.255.255.255:"
                    + Utils.DISCOVERY_PORT);
            sock.send(sendPacket);

            System.out.println("[DiscoverCli] Waiting up to " + timeoutMs + " ms for reply...");
            byte[] recvBuf = new byte[256];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
            sock.receive(recvPacket);

            String resp = new String(
                    recvPacket.getData(), 0, recvPacket.getLength(), StandardCharsets.UTF_8);
            System.out.println("[DiscoverCli] ← Got reply “" + resp
                    + "” from " + recvPacket.getAddress());

            if (resp.startsWith(Utils.DISCOVERY_RESPONSE)) {
                int tcpPort = Integer.parseInt(resp.split(":")[1]);
                return new InetSocketAddress(recvPacket.getAddress(), tcpPort);
            }
            throw new IOException("Bad discovery response: " + resp);
        }
    }
}