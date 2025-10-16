package Messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PieceMessageHandler {
	private int length;
	private byte type;
	private byte[] payload; // pieceIndex + pieceData

	public PieceMessageHandler(int pieceIndex, byte[] pieceData) {
		this.type = 7; // message type = 7 (PIECE)

		// Combine pieceIndex + pieceData into payload
		ByteBuffer payloadBuffer = ByteBuffer.allocate(4 + pieceData.length);
		payloadBuffer.putInt(pieceIndex);
		payloadBuffer.put(pieceData);

		this.payload = payloadBuffer.array();
		this.length = 1 + this.payload.length; // type + payload
	}

	public byte[] toByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(4 + this.length);
		buffer.putInt(this.length);
		buffer.put(this.type);
		buffer.put(this.payload);
		return buffer.array();
	}

	public static PieceMessageHandler fromByteArray(byte[] messageBytes) {
		ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 5, messageBytes.length - 5);
		int pieceIndex = buffer.getInt();
		byte[] pieceData = Arrays.copyOfRange(messageBytes, 9, messageBytes.length);
		return new PieceMessageHandler(pieceIndex, pieceData);
	}

	public int getPieceIndex() {
		return ByteBuffer.wrap(payload, 0, 4).getInt();
	}

	public byte[] getPieceData() {
		return Arrays.copyOfRange(payload, 4, payload.length);
	}

	@Override
	public String toString() {
		return "PieceMessage{length=" + length +
				", type=" + type +
				", pieceIndex=" + getPieceIndex() +
				", dataSize=" + getPieceData().length + "}";
	}
}
