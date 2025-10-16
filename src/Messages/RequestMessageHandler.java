
package Messages;

import java.nio.ByteBuffer;

public class RequestMessageHandler {
	int length;
	byte type;
	int pieceIndex;
	byte[] payload;

	public int getPieceIndex() {
		return pieceIndex;
	}

	public void setPieceIndex(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	public RequestMessageHandler(int pieceIndex) {
		this.length = 5;
		this.pieceIndex = pieceIndex;
		this.type = (byte) 6;
		this.payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
	}

	public int getMessageLength() {
		return 4 + this.length;
	}

	public byte[] toByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(getMessageLength());
		buffer.putInt(this.length);
		buffer.put(this.type);
		if (this.payload != null) {
			buffer.put(this.payload);
		}
		return buffer.array();
	}

	public static RequestMessageHandler fromByteArray(byte[] payload) {
		int pieceIndex = ByteBuffer.wrap(payload).getInt();
		return new RequestMessageHandler(pieceIndex);
	}

	@Override
	public String toString() {
		String payloadStr = "";
		for (byte b : payload) {
			payloadStr += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
		}
		return "Message{" +
				"length=" + this.length +
				", type=" + this.type +
				", peiceInd=" + this.pieceIndex +
				", payload=" + payloadStr +
				"}";
	}
}
