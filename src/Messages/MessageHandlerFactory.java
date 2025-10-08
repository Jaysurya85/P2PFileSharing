package Messages;

import java.nio.ByteBuffer;

public class MessageHandlerFactory {

	private MessageHandler handler;

	public MessageHandlerFactory(byte[] byteArray) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
		int length = byteBuffer.getInt();
		byte type = byteBuffer.get();
		switch (type) {
			// case 1:
			// handler = new BitfieldMessage();
			// break;
			// case 2:
			// handler = new ImageMessageHandler();
			// break;
			// case 3:
			// handler = new VideoMessageHandler();
			// break;
			// case 4:
			// handler = new AudioMessageHandler();
			// break;
			case 5:
				byte[] payload = new byte[length];
				byteBuffer.get(payload, 0, length);
				handler = new BitfieldMessageHandler(payload);
				break;
			// case 6:
			// handler = new XmlMessageHandler();
			// break;
			// case 7:
			// handler = new BinaryMessageHandler();
			// break;
			// case 8:
			// handler = new ControlMessageHandler();
			// break;
			default:
				throw new IllegalArgumentException("Unknown message type: " + type);
		}
	}

	public MessageHandler getHandler() {
		return this.handler;
	}
}
