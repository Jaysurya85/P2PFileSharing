package network;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

import Messages.*;

import java.io.*;

import models.HandshakeInfo;
import models.Peer;
import utils.MessageUtils;

class ClientListener implements Runnable {
	private InputStream in;
	private OutputStream out;
	private PeerNode peerNode;
	private int serverPeerId;

	public ClientListener(InputStream in, OutputStream out, PeerNode peerNode, int serverPeerId) {
		this.in = in;
		this.out = out;
		this.peerNode = peerNode;
		this.serverPeerId = serverPeerId;
	}

	public void clientMessageHandler(int type, byte[] payload) throws Exception {
		switch (type) {
			case 0:
				ChokeMessageHandler serverChokeHandler = new ChokeMessageHandler();
				System.out.println("Server is choking  us " + serverChokeHandler);
				break;

			case 1:
				UnChokeMessageHandler serverUnchokeHandler = new UnChokeMessageHandler();
				System.out.println("Server is un choking us " + serverUnchokeHandler);
				List<Integer> interestedPeices = peerNode.getInterestedPeices(serverPeerId);
				if (!interestedPeices.isEmpty()) {
					MessageUtils.sendRequest(interestedPeices.get(0), out);
				}
				break;

			case 2:
				InterestedMessageHandler serverInterestedMessage = new InterestedMessageHandler();
				System.out.println("Server is sending interested as " + serverInterestedMessage);
				this.peerNode.setPeerInterested(serverPeerId);
				MessageUtils.sendUnChoke(this.out);
				break;

			case 3:
				NotInterestedMessageHandler serverNotInterestedMessage = new NotInterestedMessageHandler();
				System.out.println("Server is sending not interested as " + serverNotInterestedMessage);
				this.peerNode.setPeerNotInterested(serverPeerId);
				break;

			// handler = new VideoMessageHandler();
			// break;
			// case 4:
			// handler = new AudioMessageHandler();
			// break;
			case 5:
				BitfieldMessageHandler serverBitfieldMessage = BitfieldMessageHandler.fromByteArray(payload);
				System.out.println("Server is sending bitfield as " + serverBitfieldMessage);
				this.peerNode.setOtherPeerBit(serverPeerId, serverBitfieldMessage.getPayload());
				boolean isInterested = peerNode.setInterestedPeices(serverPeerId, payload);
				MessageUtils.sendInterestedOrNot(isInterested, this.out);
				break;

			// case 6:
			// handler = new XmlMessageHandler();
			// break;
			// case 7:
			// handler = new BinaryMessageHandler();
			// break;
			// case 8:
			// handler = new ControlMessageHandler();
			// break;
			default:
				throw new IllegalArgumentException("Unknown message type: " + type);
		}
	}

	private void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
		int read = 0;
		while (read < length) {
			int r = in.read(buffer, offset + read, length - read);
			if (r == -1)
				throw new EOFException("Stream closed prematurely");
			read += r;
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				byte[] lengthBytes = new byte[4];
				readFully(in, lengthBytes, 0, 4);
				int length = ByteBuffer.wrap(lengthBytes).getInt();

				byte[] typeBytes = new byte[1];
				readFully(in, typeBytes, 0, 1);

				byte[] payload = new byte[length - 1];
				readFully(in, payload, 0, length - 1);

				byte[] fullMessage = new byte[4 + length];
				System.arraycopy(lengthBytes, 0, fullMessage, 0, 4);
				fullMessage[4] = typeBytes[0];
				System.arraycopy(payload, 0, fullMessage, 5, length - 1);

				clientMessageHandler(typeBytes[0], payload);
			}
		} catch (Exception e) {
			System.out.println("Error reading from server: " + e);
		}
	}
}

/*
 * client (selfClientinfo, selfServerINfo, serverInfoToWhichIAmClient)
 */
public class ClientManager {
	private PeerNode peerNode;
	private Peer peer;
	private HandshakeInfo serverHandshakeInfo;
	private HashMap<Integer, Socket> connections;

	public ClientManager(PeerNode peerNode) {
		this.peerNode = peerNode;
		this.peer = peerNode.getPeer();
		this.serverHandshakeInfo = new HandshakeInfo();
		this.connections = new HashMap<>();

	}

	// public void clientSendMessageAsTesting() {
	// for (Map.Entry<Integer, Socket> entry : connections.entrySet()) {
	// Socket socket = entry.getValue();
	// int serverPeerId = entry.getKey();
	// System.out.println("Sending message to server " + serverPeerId);
	// try {
	//
	// OutputStream out = socket.getOutputStream();
	// out.write("Hello from client".getBytes());
	// } catch (Exception ex) {
	// System.out.println("Error while sending message to servers i am connected ex:
	// " + ex.toString());
	// }
	// }
	// }

	public boolean doHandshake(InputStream in, OutputStream out) throws Exception {

		HandshakeMessage message = new HandshakeMessage();
		byte[] handshakeMessage = message.buildHandshake(this.peer.getPeerId());
		byte[] serverHandshakeBuffer = new byte[32];
		message.printOutput(handshakeMessage);
		out.write(handshakeMessage);
		in.read(serverHandshakeBuffer);
		this.serverHandshakeInfo = message.parseHandshake(serverHandshakeBuffer);
		boolean isHandshakeDone = message.verifyHeader(this.serverHandshakeInfo.getHeader());
		return isHandshakeDone;
	}

	private void sendBitfield(OutputStream out) throws Exception {
		BitfieldMessageHandler bitfieldMessage = new BitfieldMessageHandler(this.peerNode.getBitfield());
		System.out.println("Client is sending bitfield as " + bitfieldMessage);
		out.write(bitfieldMessage.toByteArray());
	}

	public Thread connect(int serverPort, int serverPeerId, String serverHost) {
		return new Thread(() -> {
			try {
				Socket socket = new Socket(serverHost, serverPort);
				connections.put(serverPeerId, socket);
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();
				System.out.println("Connecting to server " + serverPeerId + " " + serverHost + ":" + serverPort);
				boolean isHandshakeDone = doHandshake(in, out);
				if (!isHandshakeDone) {
					out.write("exit".getBytes());
					System.out.println("Closing Client to server because it wasn't a match: " + serverPeerId);
				}
				System.out.println(
						"Handshake done with Server " + this.serverHandshakeInfo.getHeader() + " peer id is "
								+ this.serverHandshakeInfo.getPeerId());
				// 2. Start listening thread **after handshake**
				Thread listenerThread = new Thread(new ClientListener(in, out, peerNode, serverPeerId));
				listenerThread.start();
				sendBitfield(out);
				// socket.close();
			} catch (Exception ex) {
				System.out.println("Exception while creating client" + ex);
			}
		});
	}

}
