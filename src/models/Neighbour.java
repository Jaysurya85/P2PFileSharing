package models;

import java.util.List;

public class Neighbour {
	private byte[] bitfield;
	private boolean isInterested;
	private int peerId;
	private List<Integer> interestedPeices;

	public Neighbour(byte[] bitfield, int peerId) {
		this.bitfield = bitfield;
		this.isInterested = false;
		this.peerId = peerId;

	}

	public byte[] getBitfield() {
		return bitfield;
	}

	public boolean isInterested() {
		return isInterested;
	}

	public int getPeerId() {
		return peerId;
	}

	public void setInterested(boolean isInterested) {
		this.isInterested = isInterested;
	}

	public void setBitfield(byte[] bitfield) {
		this.bitfield = bitfield;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public List<Integer> getInterestedPeices() {
		return interestedPeices;
	}

	public void setInterestedPeices(List<Integer> interestedPeices) {
		this.interestedPeices = interestedPeices;
	}

}
