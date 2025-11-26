package utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileManager {
	int peerId;
	String fileName;
	String filePath;
	long fileSize;
	int pieceSize;
	int noOfPieces;
	byte[][] pieces;
	int noOfMissingPeices;

	public FileManager(int peerId, String fileName, long fileSize, int peiceSize, int noOfPieces) {
		this.peerId = peerId;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.pieceSize = peiceSize;
		this.noOfPieces = noOfPieces;
		this.filePath = "../project_config_file_small/" + String.valueOf(peerId) + "/" + fileName;
		this.pieces = new byte[noOfPieces][peiceSize];
	}

	public void breakFileIntoPeices() {
		try {
			System.out.println("filepath is " + this.filePath);
			byte[] bytes = Files.readAllBytes(Paths.get(this.filePath));
			int start = 0;
			int ind = 0;
			while (start < bytes.length) {
				int end = start + Math.min(this.pieceSize, bytes.length);
				pieces[ind] = Arrays.copyOfRange(bytes, start, end);
				ind++;
				start = end;
			}
			// for (int i = 0; i < this.pieces.length; i++) {
			// System.out.println("Piece as text: " + new String(this.pieces[i],
			// StandardCharsets.UTF_8));
			// }

		} catch (IOException io) {
			System.err.println("File not found");
		} catch (IndexOutOfBoundsException indEx) {
			System.err.println("Error no of peices are wrong");
		} catch (Exception ex) {
			System.err.println("Error while break file into peices:" + ex.toString());
		}
	}

	public byte[] getPiece(int pieceIndex) {
		return this.pieces[pieceIndex];
	}

	public void setPeice(int pieceIndex, byte[] piece) {
		this.pieces[pieceIndex] = piece;
		this.noOfMissingPeices = this.noOfMissingPeices - 1;
		System.out.println("No of remaining pieces are " + this.noOfMissingPeices);
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

	private void savePiecesToFile() {
		try (FileOutputStream fos = new FileOutputStream(this.filePath)) {
			for (int i = 0; i < this.noOfPieces - 1; i++) {
				fos.write(pieces[i]);
			}
			int lastByteTrim = (int) (this.fileSize % (long) this.pieceSize);
			fos.write(pieces[this.noOfPieces - 1], 0, lastByteTrim);
			fos.flush();
			System.out.println("File successfully reassembled at " + this.filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
