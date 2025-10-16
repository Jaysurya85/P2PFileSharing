
package Messages;

import java.nio.ByteBuffer;

public class NotInterestedMessageHandler {
	int length;
	byte type;

	public NotInterestedMessageHandler() {
		this.length = 1;
		this.type = (byte) 3;
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

	public static NotInterestedMessageHandler fromByteArray() {

		return new NotInterestedMessageHandler();
	}

	@Override
	public String toString() {
		return "Message{" +
				"length=" + this.length +
				", type=" + this.type +
				"}";
	}
}
