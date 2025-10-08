public class Message {
	int length;
	byte type;
	byte[] payload;

	public Message(int length, byte type, byte[] payload) throws Exception {
		this.length = length;
		this.type = type;
		this.payload = new byte[length];
		if (type < 4 && length > 0) {
			throw new Exception("Payload cant be greater than zero for type Choke Unchoke Interested Non Interested");
		}
	}

}
