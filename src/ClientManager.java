
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import Messages.BitfieldMessageHandler;

import java.io.*;

import models.HandshakeInfo;
import models.Peer;

class ClientListener implements Runnable {
	private InputStream in;
	private PeerNode peerNode;
	private int serverPeerId;

	public ClientListener(InputStream in, PeerNode peerNode, int serverPeerId) {
		this.in = in;
		this.peerNode = peerNode;
		this.serverPeerId = serverPeerId;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[1024];
		try {
			while (true) {
				int read = in.read(buffer);
				if (read == -1) {
					// Server closed connection
					System.out.println("Server disconnected");
					break;
				}
				BitfieldMessageHandler serverBitfieldMessage = BitfieldMessageHandler.fromByteArray(buffer);
				System.out.println("Server is sending bitfield as " + serverBitfieldMessage);
				this.peerNode.setOtherPeerBit(serverPeerId, serverBitfieldMessage.getPayload());
				// String msg = new String(buffer, 0, read, StandardCharsets.UTF_8);
				// System.out.println("Received from server: " + msg);
				// if (msg.equals("sendAsServer")) {
				// System.out.println("Send message to clients as server");
				// this.peerNode.sendMessageFromServer();
				// }
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
				Thread listenerThread = new Thread(new ClientListener(in, peerNode, serverPeerId));
				listenerThread.start();
				sendBitfield(out);
				// socket.close();
			} catch (Exception ex) {
				System.out.println("Exception while creating client" + ex);
			}
		});
	}

}
