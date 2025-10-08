package models;

public class Common {

	int k;
	int m;
	int n;
	String fileName;
	long fileSize;
	int pieceSize;

	public Common() {

	}

	public Common(int k, int m, int n, String fileName, long fileSize, int pieceSize) {
		this.k = k;
		this.m = m;
		this.n = n;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.pieceSize = pieceSize;
	}

	public int getK() {
		return this.k;
	}

	public int getM() {
		return this.m;
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
