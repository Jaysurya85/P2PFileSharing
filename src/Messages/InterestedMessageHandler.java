
package Messages;

import java.nio.ByteBuffer;

public class InterestedMessageHandler {
	int length;
	byte type;

	public InterestedMessageHandler() {
		this.length = 1;
		this.type = (byte) 2;
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

	public static InterestedMessageHandler fromByteArray() {

		return new InterestedMessageHandler();
	}

	@Override
	public String toString() {
		return "Message{" +
				"length=" + this.length +
				", type=" + this.type +
				"}";
	}
}
