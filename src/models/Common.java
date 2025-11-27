package models;

public class Common {

	int unChockingInterval;
	int noOfPreferredNeighbours;
	int n;
	String fileName;
	long fileSize;
	int pieceSize;

	public Common() {

	}

	public Common(int noOfPreferredNeighbours, int unChockingInterval, int n, String fileName, long fileSize,
			int pieceSize) {
		this.noOfPreferredNeighbours = noOfPreferredNeighbours;
		this.unChockingInterval = unChockingInterval;
		this.n = n;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.pieceSize = pieceSize;
	}

	public int getUnChockingInterval() {
		return this.unChockingInterval;
	}

	public int getNoOfPreferredNeighbours() {
		return this.noOfPreferredNeighbours;
	}

	public int getN() {
		return this.n;
	}

	public String getFileName() {
		return this.fileName;
	}

	public long getFileSize() {
		return this.fileSize;
	}

	public int getPieceSize() {
		return this.pieceSize;
	}
}
