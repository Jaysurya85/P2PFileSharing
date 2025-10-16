
package Messages;

import java.nio.ByteBuffer;

public class UnChokeMessageHandler {
	int length;
	byte type;

	public UnChokeMessageHandler() {
		this.length = 1;
		this.type = (byte) 1;
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

	public static UnChokeMessageHandler fromByteArray() {

		return new UnChokeMessageHandler();
	}

	@Override
	public String toString() {
		return "Message{" +
				"length=" + this.length +
				", type=" + this.type +
				"}";
	}
}
