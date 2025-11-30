package utils;

import java.io.OutputStream;

import Messages.*;

public class MessageUtils {

	public static void sendInterestedOrNot(boolean isInterested, OutputStream out)
			throws Exception {
		if (isInterested) {
			InterestedMessageHandler interested = new InterestedMessageHandler();
			out.write(interested.toByteArray());
		} else {
			NotInterestedMessageHandler notInterested = new NotInterestedMessageHandler();
			out.write(notInterested.toByteArray());
		}
	}

	public static void sendChoke(OutputStream out) throws Exception {
		ChokeMessageHandler choke = new ChokeMessageHandler();
		out.write(choke.toByteArray());
	}

	public static void sendUnChoke(OutputStream out) throws Exception {
		UnChokeMessageHandler unChoke = new UnChokeMessageHandler();
		out.write(unChoke.toByteArray());
	}

	public static void sendRequest(Integer pieceIndex, OutputStream out) throws Exception {
		RequestMessageHandler request = new RequestMessageHandler(pieceIndex);
		out.write(request.toByteArray());
	}

	public static void sendPiece(int pieceIndex, byte[] pieceData, OutputStream out) throws Exception {
		PieceMessageHandler pieceMsg = new PieceMessageHandler(pieceIndex, pieceData);
		out.write(pieceMsg.toByteArray());
		out.flush();
	}

	public static void sendCompleted(OutputStream out) throws Exception {
		CompletedMessageHandler completed = new CompletedMessageHandler();
		out.write(completed.toByteArray());
		out.flush();
	}
}
