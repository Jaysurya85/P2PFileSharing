
package Messages;

import java.nio.ByteBuffer;

public class ChokeMessageHandler {
	int length;
	byte type;

	public ChokeMessageHandler() {
		this.length = 1;
		this.type = (byte) 0;
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

	public static ChokeMessageHandler fromByteArray() {

		return new ChokeMessageHandler();
	}

	@Override
	public String toString() {
		return "Message{" +
				"length=" + this.length +
				", type=" + this.type +
				"}";
	}
}
