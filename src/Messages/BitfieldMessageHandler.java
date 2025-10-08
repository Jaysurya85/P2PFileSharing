package Messages;

import java.nio.ByteBuffer;

public class BitfieldMessageHandler implements MessageHandler {
	int length;
	byte type;
	byte[] payload;

	public BitfieldMessageHandler(byte[] payload) {
		this.length = payload.length;
		this.payload = payload;
		this.type = (byte) 5;
	}

	public byte[] getPayload() {
		return this.payload;
	}

	public int getMessageLength() {
		return 5 + this.length;
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

	public static BitfieldMessageHandler fromByteArray(byte[] data) {
		ByteBuffer buffer = ByteBuffer.wrap(data);

		int length = buffer.getInt();
		buffer.get();
		byte[] payload = new byte[length];
		buffer.get(payload, 0, length);

		return new BitfieldMessageHandler(payload);
	}

	@Override
	public String toString() {
		String payloadStr = "";
		for (byte b : payload) {
			payloadStr += b;
		}
		return "Message{" +
				"length=" + this.length +
				", type=" + this.type +
				", payload=" + payloadStr +
				"}";
	}
}
