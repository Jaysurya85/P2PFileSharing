import models.HandshakeInfo;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HandshakeMessage {
	private String header;

	public HandshakeMessage() {
		this.header = "P2PFILESHARINGPROJ";
	}

	public boolean verifyHeader(String header) {
		return header.equals(this.header);
	}

	public byte[] buildHandshake(int peerId) {
		byte[] handshake = new byte[32];

		// 1. Handshake header (18 bytes)
		byte[] header = this.header.getBytes(StandardCharsets.UTF_8);
		System.arraycopy(header, 0, handshake, 0, header.length);

		// 2. Zero bits (10 bytes)
		for (int i = 18; i < 28; i++) {
			handshake[i] = 0;
		}

		// 3. Peer ID (4 bytes, big-endian)
		byte[] peerIdBytes = ByteBuffer.allocate(4).putInt(peerId).array();
		System.arraycopy(peerIdBytes, 0, handshake, 28, 4);

		return handshake;
	}

	public HandshakeInfo parseHandshake(byte[] handshake) {
		String header = new String(handshake, 0, 18, StandardCharsets.UTF_8);

		int peerId = ByteBuffer.wrap(handshake, 28, 4).getInt();

		return new HandshakeInfo(header, peerId);
	}

	public void printOutput(byte[] data) {
		System.out.println("Data is");
		for (int i = 0; i < data.length; i++) {
			System.out.print(" " + (char) data[i]);
		}
		System.out.println();

	}

}
