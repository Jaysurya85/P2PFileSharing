package Messages;

import java.nio.ByteBuffer;

public class CompletedMessageHandler {
	int length;
	byte type;

	public CompletedMessageHandler() {
		this.length = 1;
		this.type = (byte) 8;
	}

	public int getMessageLength() {
		return 4 + this.length;
	}

	public byte[] toByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(getMessageLength());
		buffer.putInt(this.length);
		buffer.put(this.type);
		return buffer.array();
	}

	public static CompletedMessageHandler fromByteArray() {
		return new CompletedMessageHandler();
	}

	@Override
	public String toString() {
		return "CompletedMessage{" +
				"length=" + this.length +
				", type=" + this.type +
				"}";
	}
}
