package network;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;

import Messages.*;

import java.io.*;

import models.HandshakeInfo;
import models.Peer;
import utils.FileManager;
import utils.Logger;
import utils.MessageUtils;

class ClientListener implements Runnable {
	private InputStream in;
	private OutputStream out;
	private PeerNode peerNode;
	private int serverPeerId;
	private int clientPeerId;

	public ClientListener(InputStream in, OutputStream out, PeerNode peerNode, int serverPeerId, int clientPeerId) {
		this.in = in;
		this.out = out;
		this.peerNode = peerNode;
		this.serverPeerId = serverPeerId;
		this.clientPeerId = clientPeerId;
	}

	public void clientMessageHandler(int type, byte[] payload) throws Exception {
		switch (type) {
			case 0: // Choke
				// ChokeMessageHandler serverChokeHandler = new ChokeMessageHandler();
				Logger.logChoked(this.clientPeerId, this.serverPeerId);
				break;

			case 1: // Unchoke
				// UnChokeMessageHandler serverUnchokeHandler = new UnChokeMessageHandler();
				Logger.logUnchoked(this.clientPeerId, this.serverPeerId);

				int interestedPiece = peerNode.getInterestedPiece(this.serverPeerId);
				if (interestedPiece >= 0) {
					MessageUtils.sendRequest(interestedPiece, out);
				}
				break;

			case 2: // Interested
				// InterestedMessageHandler serverInterestedMessage = new
				// InterestedMessageHandler();
				Logger.logReceivingInterested(this.clientPeerId, this.serverPeerId);
				this.peerNode.setPeerInterested(this.serverPeerId);
				MessageUtils.sendUnChoke(this.out);
				break;

			case 3: // Not Interested
				// NotInterestedMessageHandler serverNotInterestedMessage = new
				// NotInterestedMessageHandler();
				Logger.logReceivingNotInterested(this.clientPeerId, this.serverPeerId);
				this.peerNode.setPeerNotInterested(this.serverPeerId);
				break;

			case 4: // Have
				HaveMessageHandler serverHaveMessage = HaveMessageHandler.fromByteArray(payload);
				Logger.logReceivingHave(this.clientPeerId, this.serverPeerId, serverHaveMessage.getPieceIndex());
				this.peerNode.setOtherPeerBit(this.serverPeerId, serverHaveMessage.getPieceIndex());
				break;

			case 5: // Bitfield
				BitfieldMessageHandler serverBitfieldMessage = BitfieldMessageHandler.fromByteArray(payload);
				this.peerNode.setOtherPeerBitfield(serverPeerId, serverBitfieldMessage.getPayload());
				boolean isInterested = this.peerNode.setInterestedPieces(serverPeerId, payload);
				MessageUtils.sendInterestedOrNot(isInterested, this.out);
				break;

			case 6: // Request
				RequestMessageHandler serverRequestMessage = RequestMessageHandler.fromByteArray(payload);
				int pieceIndex = serverRequestMessage.getPieceIndex();
				FileManager fm = this.peerNode.getFileManager();
				byte[] piece = fm.getPiece(pieceIndex);
				MessageUtils.sendPiece(pieceIndex, piece, this.out);
				break;

			case 7: // Piece
				PieceMessageHandler clientPieceMessage = PieceMessageHandler.fromByteArray(payload);
				pieceIndex = clientPieceMessage.getPieceIndex();
				byte[] pieceData = clientPieceMessage.getPieceData();

				fm = this.peerNode.getFileManager();
				fm.setPeice(pieceIndex, pieceData);

				this.peerNode.recordBytesDownloaded(this.serverPeerId, pieceData.length);

				int currentPieceCount = fm.getTotalPieces() - fm.getNoOfMissingPeices();
				Logger.logDownloadingPiece(this.clientPeerId, this.serverPeerId, pieceIndex, currentPieceCount);

				if (fm.getNoOfMissingPeices() == 0) {
					Logger.logDownloadComplete(this.clientPeerId);
				}

				this.peerNode.setBit(serverPeerId, pieceIndex);
				HaveMessageHandler haveMessage = new HaveMessageHandler(pieceIndex);
				byte[] havePayload = haveMessage.toByteArray();

				this.peerNode.broadcastToClients(havePayload);
				this.peerNode.braodcastToServers(havePayload);

				interestedPiece = peerNode.getInterestedPiece(serverPeerId);
				if (interestedPiece >= 0) {
					MessageUtils.sendRequest(interestedPiece, out);
				}
				break;

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

	public boolean doHandshake(InputStream in, OutputStream out) throws Exception {
		HandshakeMessage message = new HandshakeMessage();
		byte[] handshakeMessage = message.buildHandshake(this.peer.getPeerId());
		byte[] serverHandshakeBuffer = new byte[32];
		out.write(handshakeMessage);
		in.read(serverHandshakeBuffer);
		this.serverHandshakeInfo = message.parseHandshake(serverHandshakeBuffer);
		boolean isHandshakeDone = message.verifyHeader(this.serverHandshakeInfo.getHeader());
		return isHandshakeDone;
	}

	public void broadcastToServers(byte[] havePayload) {
		for (HashMap.Entry<Integer, Socket> ep : connections.entrySet()) {
			Socket socket = ep.getValue();
			try {
				OutputStream out = socket.getOutputStream();
				out.write(havePayload);
			} catch (Exception ex) {
			}
		}
	}

	public void sendChoke(int peerId) {
		Socket socket = connections.get(peerId);
		if (socket != null && !socket.isClosed()) {
			try {
				OutputStream out = socket.getOutputStream();
				MessageUtils.sendChoke(out);
			} catch (Exception ex) {
				System.out.println("[CLIENT] Error sending CHOKE to server " + peerId + ": " + ex);
			}
		}
	}

	public void sendUnchoke(int peerId) {
		Socket socket = connections.get(peerId);
		if (socket != null && !socket.isClosed()) {
			try {
				OutputStream out = socket.getOutputStream();
				MessageUtils.sendUnChoke(out);
			} catch (Exception ex) {
				System.out.println("[CLIENT] Error sending UNCHOKE to server " + peerId + ": " + ex);
			}
		}
	}

	private void sendBitfield(OutputStream out) throws Exception {
		BitfieldMessageHandler bitfieldMessage = new BitfieldMessageHandler(this.peerNode.getBitfield());
		out.write(bitfieldMessage.toByteArray());
	}

	public Thread connect(int serverPort, int serverPeerId, String serverHost) {
		return new Thread(() -> {
			try {
				Socket socket = new Socket(serverHost, serverPort);
				connections.put(serverPeerId, socket);
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();

				boolean isHandshakeDone = doHandshake(in, out);
				if (!isHandshakeDone) {
					out.write("exit".getBytes());
				}
				Logger.logTCPConnectionTo(this.peer.getPeerId(), serverPeerId);

				Thread listenerThread = new Thread(
						new ClientListener(in, out, peerNode, serverPeerId, this.peer.getPeerId()));
				listenerThread.start();
				sendBitfield(out);
			} catch (Exception ex) {
				System.out.println("Exception while creating client: " + ex);
			}
		});
	}
}
