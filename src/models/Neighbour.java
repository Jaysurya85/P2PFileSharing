package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Neighbour {
	private byte[] bitfield;
	private boolean isInterestedInMe;
	private int peerId;
	private List<Integer> interestingPieces;
	private boolean hasCompletedFile;

	public Neighbour(byte[] bitfield, int peerId) {
		this.bitfield = bitfield;
		this.isInterestedInMe = false;
		this.peerId = peerId;
		this.interestingPieces = Collections.synchronizedList(new ArrayList<>());
		this.hasCompletedFile = false;
	}

	public byte[] getBitfield() {
		return bitfield;
	}

	public boolean isInterestedInMe() {
		return isInterestedInMe;
	}

	public int getPeerId() {
		return peerId;
	}

	public void setInterestedInMe(boolean isInterestedInMe) {
		this.isInterestedInMe = isInterestedInMe;
	}

	public void setBitfield(byte[] bitfield) {
		this.bitfield = bitfield;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public List<Integer> getInterestingPieces() {
		return interestingPieces;
	}

	public void addInterestingPieces(int pieceIndex) {
		if (!interestingPieces.contains(pieceIndex)) {
			interestingPieces.add(pieceIndex);
		}
	}

	public void removeInterestingPieces(int pieceIndex) {
		interestingPieces.remove(Integer.valueOf(pieceIndex));
	}

	public void setInterestingPieces(List<Integer> interestingPieces) {
		this.interestingPieces = Collections.synchronizedList(new ArrayList<>(interestingPieces));
	}

	public boolean hasCompletedFile() {
		return hasCompletedFile;
	}

	public void setHasCompletedFile(boolean hasCompletedFile) {
		this.hasCompletedFile = hasCompletedFile;
	}
}
