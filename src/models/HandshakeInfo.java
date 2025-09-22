
package models;

public class HandshakeInfo {
	private int peerId;
	private String header;

	public HandshakeInfo(String header, int peerId) {
		this.header = header;
		this.peerId = peerId;
	}

	public HandshakeInfo() {

	}

	public int getPeerId() {
		return this.peerId;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public String getHeader() {
		return this.header;
	}

	@Override
	public String toString() {
		return this.peerId + " " + this.header;
	}
}
