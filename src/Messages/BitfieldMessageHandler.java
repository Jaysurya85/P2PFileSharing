package Messages;

import java.nio.ByteBuffer;

public class BitfieldMessageHandler {
	int length;
	byte type;
	byte[] payload;

	public BitfieldMessageHandler(byte[] payload) {
		this.length = payload.length + 1;
		this.payload = payload;
		this.type = (byte) 5;
	}

	public byte[] getPayload() {
		return this.payload;
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

	public static BitfieldMessageHandler fromByteArray(byte[] payload) {
		return new BitfieldMessageHandler(payload);
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
				", payload=" + payloadStr +
				"}";
	}
}
