package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Logger {
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static String getLogFileName(int peerId) {
		return "log_peer_" + peerId + ".log";
	}

	private static String getCurrentTime() {
		return dateFormat.format(new Date());
	}

	private static void writeLog(int peerId, String message) {
		try (PrintWriter out = new PrintWriter(new FileWriter(getLogFileName(peerId), true))) {
			out.println(message);
			out.flush();
		} catch (IOException e) {
			System.err.println("Error writing to log file: " + e.getMessage());
		}
	}

	// TCP connection - when peer makes connection to another
	public static void logTCPConnectionTo(int peerId1, int peerId2) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1 + "] makes a connection to Peer [" + peerId2
				+ "].";
		writeLog(peerId1, message);
	}

	// TCP connection - when peer is connected from another
	public static void logTCPConnectionFrom(int peerId1, int peerId2) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1 + "] is connected from Peer [" + peerId2 + "].";
		writeLog(peerId1, message);
	}

	// Change of preferred neighbors
	public static void logPreferredNeighbors(int peerId, List<Integer> preferredNeighborIds) {
		String neighborList = preferredNeighborIds.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(", "));
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId + "] has the preferred neighbors ["
				+ neighborList + "].";
		writeLog(peerId, message);
	}

	// Change of optimistically unchoked neighbor
	public static void logOptimisticallyUnchokedNeighbor(int peerId, int optimisticallyUnchokedId) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId + "] has the optimistically unchoked neighbor ["
				+ optimisticallyUnchokedId + "].";
		writeLog(peerId, message);
	}

	// Unchoking - when this peer is unchoked by another
	public static void logUnchoked(int peerId1, int peerId2) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1 + "] is unchoked by [" + peerId2 + "].";
		writeLog(peerId1, message);
	}

	// Choking - when this peer is choked by another
	public static void logChoked(int peerId1, int peerId2) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1 + "] is choked by [" + peerId2 + "].";
		writeLog(peerId1, message);
	}

	// Receiving 'have' message
	public static void logReceivingHave(int peerId1, int peerId2, int pieceIndex) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1 + "] received the 'have' message from ["
				+ peerId2 + "] for the piece [" + pieceIndex + "].";
		writeLog(peerId1, message);
	}

	// Receiving 'interested' message
	public static void logReceivingInterested(int peerId1, int peerId2) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1 + "] received the 'interested' message from ["
				+ peerId2 + "].";
		writeLog(peerId1, message);
	}

	// Receiving 'not interested' message
	public static void logReceivingNotInterested(int peerId1, int peerId2) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1
				+ "] received the 'not interested' message from [" + peerId2 + "].";
		writeLog(peerId1, message);
	}

	// Downloading a piece
	public static void logDownloadingPiece(int peerId1, int peerId2, int pieceIndex, int numberOfPieces) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId1 + "] has downloaded the piece [" + pieceIndex
				+ "] from [" + peerId2 + "]. Now the number of pieces it has is [" + numberOfPieces + "].";
		writeLog(peerId1, message);
	}

	// Completion of download
	public static void logDownloadComplete(int peerId) {
		String message = "[" + getCurrentTime() + "]: Peer [" + peerId + "] has downloaded the complete file.";
		writeLog(peerId, message);
	}
}
