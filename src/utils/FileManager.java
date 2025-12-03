package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileManager {
	int peerId;
	String fileName;
	String filePath;
	String outFilePath;
	long fileSize;
	int pieceSize;
	int noOfPieces;
	byte[][] pieces;
	int noOfMissingPeices;

	public FileManager(int peerId, String fileName, long fileSize, int pieceSize, int noOfPieces) {
		this.peerId = peerId;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.pieceSize = pieceSize;
		this.noOfPieces = noOfPieces;
		this.filePath = "../project_config_file_large/" + String.valueOf(peerId) + "/" + fileName;
		this.outFilePath = String.valueOf(peerId) + "/" + fileName;
		this.pieces = new byte[noOfPieces][];
	}

	public void breakFileIntoPeices() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(this.filePath));
			int start = 0;
			int ind = 0;
			while (start < bytes.length) {
				int end = Math.min(start + this.pieceSize, bytes.length);
				pieces[ind] = Arrays.copyOfRange(bytes, start, end);
				ind++;
				start = end;
			}
		} catch (IOException io) {
			System.err.println("File not found: " + this.filePath);
		} catch (IndexOutOfBoundsException indEx) {
			System.err.println("Error: number of pieces are wrong");
		} catch (Exception ex) {
			System.err.println("Error while breaking file into pieces: " + ex.toString());
		}
	}

	public byte[] getPiece(int pieceIndex) {
		return this.pieces[pieceIndex];
	}

	public void setPeice(int pieceIndex, byte[] piece) {
		if (this.pieces[pieceIndex] != null && this.pieces[pieceIndex].length > 0) {
			return;
		}

		this.pieces[pieceIndex] = piece;
		this.noOfMissingPeices = this.noOfMissingPeices - 1;
		if (this.noOfMissingPeices == 0) {
			savePiecesToFile();
		}
	}

	public int getNoOfMissingPeices() {
		return noOfMissingPeices;
	}

	public void setNoOfMissingPeices(int noOfMissingPeices) {
		this.noOfMissingPeices = noOfMissingPeices;
	}

	public int getTotalPieces() {
		return noOfPieces;
	}

	private void savePiecesToFile() {
		try {
			File outFile = new File(this.outFilePath);

			File parentDir = outFile.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}

			try (FileOutputStream fos = new FileOutputStream(outFile)) {
				for (int i = 0; i < this.noOfPieces - 1; i++) {
					fos.write(pieces[i]);
				}
				int lastByteTrim = (int) (this.fileSize % (long) this.pieceSize);
				if (lastByteTrim == 0) {
					fos.write(pieces[this.noOfPieces - 1]);
				} else {
					fos.write(pieces[this.noOfPieces - 1], 0, lastByteTrim);
				}
				fos.flush();
			}

		} catch (IOException e) {
			System.err.println("Error saving file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
