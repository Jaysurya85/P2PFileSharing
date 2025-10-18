package Messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PieceMessageHandler {
	private int length;
	private byte type;
	private int pieceIndex;
	private byte[] payload;

	public PieceMessageHandler(int pieceIndex, byte[] pieceData) {
		this.type = 7;
		this.pieceIndex = pieceIndex;
		ByteBuffer payloadBuffer = ByteBuffer.allocate(4 + pieceData.length);
		payloadBuffer.putInt(pieceIndex);
		payloadBuffer.put(pieceData);

		this.payload = payloadBuffer.array();
		this.length = 1 + this.payload.length;
	}

	public int getMessageLength() {
		return 4 + this.length;
	}

	public byte[] toByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(getMessageLength());
		buffer.putInt(this.length);
		buffer.put(this.type);
		buffer.put(this.payload);
		return buffer.array();
	}

	public static PieceMessageHandler fromByteArray(byte[] messageBytes) {
		ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
		int pieceIndex = buffer.getInt();
		byte[] pieceData = new byte[messageBytes.length - 4];
		buffer.get(pieceData);
		return new PieceMessageHandler(pieceIndex, pieceData);
	}

	public int getPieceIndex() {
		return this.pieceIndex;
	}

	public byte[] getPieceData() {
		return Arrays.copyOfRange(payload, 4, payload.length);
	}

	@Override
	public String toString() {
		return "PieceMessage{length=" + length +
				", type=" + type +
				", pieceIndex=" + getPieceIndex() +
				"payload is= " + getPieceData() +
				", dataSize=" + getPieceData().length + "}";
	}
}
