package Messages;

import java.nio.ByteBuffer;

public class HaveMessageHandler {
	private int length;
	private byte type;
	private byte[] payload;

	public HaveMessageHandler(int pieceIndex) {
		this.type = 4;
		this.payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
		this.length = 1 + payload.length;
	}

	public byte[] toByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(4 + length);
		buffer.putInt(length);
		buffer.put(type);
		buffer.put(payload);
		return buffer.array();
	}

	public static HaveMessageHandler fromByteArray(byte[] messageBytes) {
		ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 5, 4);
		int pieceIndex = buffer.getInt();
		return new HaveMessageHandler(pieceIndex);
	}

	public int getPieceIndex() {
		return ByteBuffer.wrap(payload).getInt();
	}

	@Override
	public String toString() {
		return "HaveMessage{type=" + type + ", pieceIndex=" + getPieceIndex() + "}";
	}
}
