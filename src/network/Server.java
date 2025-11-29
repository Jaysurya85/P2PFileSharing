package network;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import Messages.*;

import java.io.*;
import java.net.*;

import models.HandshakeInfo;
import models.Peer;
import utils.FileManager;
import utils.Logger;
import utils.MessageUtils;

class ClientHandler implements Runnable {

	Socket socket;
	int serverPeerId;
	int clientPeerId;
	HandshakeInfo clientHandshakeInfo;
	PeerNode peerNode;
	InputStream in;
	OutputStream out;

	public ClientHandler(Socket socket, int serverPeerId, PeerNode peerNode) throws Exception {
		this.socket = socket;
		this.serverPeerId = serverPeerId;
		this.peerNode = peerNode;
		this.clientHandshakeInfo = new HandshakeInfo();
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();
	}

	public void serverMessageHandler(byte type, byte[] payload) throws Exception {
		switch (type) {
			case 0: // Choke
				// ChokeMessageHandler clientChokeHandler = new ChokeMessageHandler();
				Logger.logChoked(this.serverPeerId, this.clientHandshakeInfo.getPeerId());
				break;

			case 1: // Unchoke
				// UnChokeMessageHandler clientUnchokeHandler = new UnChokeMessageHandler();
				Logger.logUnchoked(this.serverPeerId, this.clientHandshakeInfo.getPeerId());

				int interestedPiece = peerNode.getInterestedPiece(this.clientHandshakeInfo.getPeerId());
				if (interestedPiece >= 0) {
					MessageUtils.sendRequest(interestedPiece, out);
				}
				break;

			case 2: // Interested
				// InterestedMessageHandler clientInterestedMessage = new
				// InterestedMessageHandler();
				Logger.logReceivingInterested(this.serverPeerId, this.clientHandshakeInfo.getPeerId());
				this.peerNode.setPeerInterested(this.clientHandshakeInfo.getPeerId());
				MessageUtils.sendUnChoke(this.out);
				break;

			case 3: // Not Interested
				// NotInterestedMessageHandler clientNotInterestedMessage = new
				// NotInterestedMessageHandler();
				Logger.logReceivingNotInterested(this.serverPeerId, this.clientHandshakeInfo.getPeerId());
				this.peerNode.setPeerNotInterested(this.clientHandshakeInfo.getPeerId());
				break;

			case 4: // Have
				HaveMessageHandler clientHaveMessage = HaveMessageHandler.fromByteArray(payload);
				// System.out.println("Client is sending have message as " + clientHaveMessage);
				Logger.logReceivingHave(this.serverPeerId, this.clientHandshakeInfo.getPeerId(),
						clientHaveMessage.getPieceIndex());
				this.peerNode.setOtherPeerBit(this.clientHandshakeInfo.getPeerId(), clientHaveMessage.getPieceIndex());
				break;

			case 5: // Bitfield
				BitfieldMessageHandler clientBitfieldMessage = BitfieldMessageHandler.fromByteArray(payload);
				this.peerNode.setOtherPeerBitfield(this.clientHandshakeInfo.getPeerId(),
						clientBitfieldMessage.getPayload());
				BitfieldMessageHandler bitfieldMessage = new BitfieldMessageHandler(this.peerNode.getBitfield());
				this.out.write(bitfieldMessage.toByteArray());
				boolean isInterested = peerNode.setInterestedPieces(this.clientHandshakeInfo.getPeerId(), payload);
				MessageUtils.sendInterestedOrNot(isInterested, this.out);
				break;

			case 6: // Request
				RequestMessageHandler clientRequestMessage = RequestMessageHandler.fromByteArray(payload);
				int pieceIndex = clientRequestMessage.getPieceIndex();
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

				this.peerNode.recordBytesDownloaded(this.clientHandshakeInfo.getPeerId(), pieceData.length);

				int currentPieceCount = fm.getTotalPieces() - fm.getNoOfMissingPeices();
				Logger.logDownloadingPiece(this.serverPeerId, this.clientHandshakeInfo.getPeerId(),
						pieceIndex, currentPieceCount);

				if (fm.getNoOfMissingPeices() == 0) {
					Logger.logDownloadComplete(this.serverPeerId);
				}

				this.peerNode.setBit(this.clientHandshakeInfo.getPeerId(), pieceIndex);
				HaveMessageHandler haveMessage = new HaveMessageHandler(pieceIndex);
				byte[] havePayload = haveMessage.toByteArray();

				this.peerNode.broadcastToClients(havePayload);
				this.peerNode.braodcastToServers(havePayload);

				interestedPiece = peerNode.getInterestedPiece(this.clientHandshakeInfo.getPeerId());
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

				serverMessageHandler(typeBytes[0], payload);
			}
		} catch (Exception ex) {
			System.out.println("Error in client handler: " + ex);
		} finally {
			try {
				this.socket.close();
			} catch (Exception ex) {
				System.out.println("Error closing connection with client " +
						this.clientPeerId);
			}
		}
	}

	public Optional<Integer> doHandshake() throws Exception {
		byte[] clientHandshakeBuffer = new byte[32];
		HandshakeMessage message = new HandshakeMessage();
		in.read(clientHandshakeBuffer);
		this.clientHandshakeInfo = message.parseHandshake(clientHandshakeBuffer);

		boolean isHandshakeDone = message.verifyHeader(this.clientHandshakeInfo.getHeader());
		if (isHandshakeDone) {
			byte[] handshakeMessage = message.buildHandshake(this.serverPeerId);
			Logger.logTCPConnectionFrom(this.serverPeerId, this.clientHandshakeInfo.getPeerId());
			this.out.write(handshakeMessage);
			return Optional.of(clientHandshakeInfo.getPeerId());
		} else {
			return Optional.empty();
		}
	}

	public void sendHaveMessage(byte[] byteArray) {
		try {
			this.out.write(byteArray);
		} catch (Exception ex) {
			System.out.println("Error occurred while trying to send message from server to client");
		}
	}
}

public class Server implements Runnable {
	PeerNode peerNode;
	Peer peer;
	Map<Integer, ClientHandler> clientHandlers;

	public Server(PeerNode peerNode) {
		this.peerNode = peerNode;
		this.peer = peerNode.getPeer();
		this.clientHandlers = new HashMap<>();
	}

	public void broadcastingToMyClients(byte[] byteArray) {
		if (!this.clientHandlers.isEmpty()) {
			for (Map.Entry<Integer, ClientHandler> entry : clientHandlers.entrySet()) {
				ClientHandler clientHandler = entry.getValue();
				clientHandler.sendHaveMessage(byteArray);
			}
		}
	}

	public void sendChokeToClient(int peerId) {
		ClientHandler handler = clientHandlers.get(peerId);
		if (handler != null) {
			try {
				MessageUtils.sendChoke(handler.out);
			} catch (Exception ex) {
				System.out.println("[SERVER] Error sending CHOKE to client " + peerId + ": "
						+ ex);
			}
		}
	}

	public void sendUnchokeToClient(int peerId) {
		ClientHandler handler = clientHandlers.get(peerId);
		if (handler != null) {
			try {
				MessageUtils.sendUnChoke(handler.out);
			} catch (Exception ex) {
				System.out.println("[SERVER] Error sending UNCHOKE to client " + peerId + ": " + ex);
			}
		}
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(peer.getPortNo(), 50,
					InetAddress.getByName(peer.getHostName()));
			while (true) {
				Socket clientSocket = serverSocket.accept();
				ClientHandler clientHandler = new ClientHandler(clientSocket, this.peer.getPeerId(), this.peerNode);
				Optional<Integer> clientPeerId = clientHandler.doHandshake();
				if (clientPeerId.isPresent()) {
					this.clientHandlers.put(clientPeerId.get(), clientHandler);
					this.peerNode.addClient(clientPeerId.get());
					Thread clientThread = new Thread(clientHandler);
					clientThread.start();
				}
			}
		} catch (Exception ex) {
			System.out.println("Exception while creating server " + this.peer.getPeerId()
					+ ": " + ex);
		}
	}
}
