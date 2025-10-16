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

	public static void sendRequest(Integer peice, OutputStream out) throws Exception {
	}
}
