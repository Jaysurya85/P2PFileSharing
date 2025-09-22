package models;

public class Peer {
	private int peerId;
	private String hostName;
	private int portNo;
	private boolean isFilePresent;

	public Peer(int peerId, String hostName, int portNo, boolean isFilePresent) {
		this.peerId = peerId;
		this.hostName = hostName;
		this.portNo = portNo;
		this.isFilePresent = isFilePresent;
	}

	public int getPortNo() {
		return this.portNo;
	}

	public void setPortNo(int portNo) {
		this.portNo = portNo;
	}

	public int getPeerId() {
		return this.peerId;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public String getHostName() {
		return this.hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public boolean getisFilePresent() {
		return this.isFilePresent;
	}

	public void setisFilePresent(boolean isFilePresent) {
		this.isFilePresent = isFilePresent;
	}

	@Override
	public String toString() {
		return this.peerId + " " + this.hostName + " " + this.portNo + " " + this.isFilePresent;
	}
}
