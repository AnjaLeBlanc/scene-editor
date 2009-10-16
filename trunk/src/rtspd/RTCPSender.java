package rtspd;

public interface RTCPSender {
	void sendRtcpPacket(/*DatagramPacket datagram*/byte[] data, int offset, int length);
}
