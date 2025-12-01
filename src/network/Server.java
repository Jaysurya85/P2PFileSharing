package network;

import java.nio.ByteBuffer;
import java.util.HashMap;
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
				Logger.logChoked(this.serverPeerId, this.clientHandshakeInfo.getPeerId());
				break;

			case 1: // Unchoke
				Logger.logUnchoked(this.serverPeerId, this.clientHandshakeInfo.getPeerId());

				int interestedPiece = peerNode.getInterestedPiece(this.clientHandshakeInfo.getPeerId());
				if (interestedPiece >= 0) {
					MessageUtils.sendRequest(interestedPiece, out);
				}
				break;

			case 2: // Interested
				Logger.logReceivingInterested(this.serverPeerId, this.clientHandshakeInfo.getPeerId());
				this.peerNode.setPeerInterested(this.clientHandshakeInfo.getPeerId());
				MessageUtils.sendUnChoke(this.out);
				break;

			case 3: // Not Interested
				Logger.logReceivingNotInterested(this.serverPeerId, this.clientHandshakeInfo.getPeerId());
				this.peerNode.setPeerNotInterested(this.clientHandshakeInfo.getPeerId());
				break;

			case 4: // Have
				HaveMessageHandler clientHaveMessage = HaveMessageHandler.fromByteArray(payload);
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
				if (this.peerNode.hasCompleteFile()) {
					MessageUtils.sendCompleted(this.out);
				}
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

				this.peerNode.setBit(this.clientHandshakeInfo.getPeerId(), pieceIndex);

				fm.setPeice(pieceIndex, pieceData);

				this.peerNode.recordBytesDownloaded(this.clientHandshakeInfo.getPeerId(), pieceData.length);

				int currentPieceCount = fm.getTotalPieces() - fm.getNoOfMissingPeices();
				Logger.logDownloadingPiece(this.serverPeerId, this.clientHandshakeInfo.getPeerId(),
						pieceIndex, currentPieceCount);

				if (fm.getNoOfMissingPeices() == 0) {
					Logger.logDownloadComplete(this.serverPeerId);
					this.peerNode.onDownloadComplete();
				}

				HaveMessageHandler haveMessage = new HaveMessageHandler(pieceIndex);
				byte[] havePayload = haveMessage.toByteArray();

				this.peerNode.broadcastToClients(havePayload);
				this.peerNode.braodcastToServers(havePayload);

				interestedPiece = peerNode.getInterestedPiece(this.clientHandshakeInfo.getPeerId());
				if (interestedPiece >= 0) {
					MessageUtils.sendRequest(interestedPiece, out);
				}
				break;

			case 8:
				this.peerNode.setPeerCompleted(this.clientHandshakeInfo.getPeerId());
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
			// Connection closed or error - normal during shutdown
		} finally {
			try {
				this.socket.close();
			} catch (Exception ex) {
				// Ignore
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
			this.out.flush(); // NEW: Force flush
		} catch (Exception ex) {
			// Connection may be closed
		}
	}

	public void close() {
		try {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
			// Ignore
		}
	}
}

public class Server implements Runnable {
	PeerNode peerNode;
	Peer peer;
	Map<Integer, ClientHandler> clientHandlers;
	private ServerSocket serverSocket;
	private volatile boolean isRunning = true;

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
				// Connection may be closed
			}
		}
	}

	public void sendUnchokeToClient(int peerId) {
		ClientHandler handler = clientHandlers.get(peerId);
		if (handler != null) {
			try {
				MessageUtils.sendUnChoke(handler.out);
			} catch (Exception ex) {
				// Connection may be closed
			}
		}
	}

	public void shutdown() {
		isRunning = false;

		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close(); // This unblocks the accept() call
			}
		} catch (IOException e) {
			// Ignore
		}

		for (ClientHandler handler : clientHandlers.values()) {
			handler.close();
		}
		clientHandlers.clear();
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(peer.getPortNo(), 50,
					InetAddress.getByName(peer.getHostName()));

			while (isRunning) {
				try {
					Socket clientSocket = serverSocket.accept();
					if (!isRunning) {
						clientSocket.close();
						break;
					}
					ClientHandler clientHandler = new ClientHandler(clientSocket, this.peer.getPeerId(), this.peerNode);
					Optional<Integer> clientPeerId = clientHandler.doHandshake();
					if (clientPeerId.isPresent()) {
						this.clientHandlers.put(clientPeerId.get(), clientHandler);
						this.peerNode.addClient(clientPeerId.get());
						Thread clientThread = new Thread(clientHandler);
						clientThread.setDaemon(true); // NEW: Make daemon so JVM can exit
						clientThread.start();
					}
				} catch (SocketException e) {
					if (!isRunning) {
						break;
					}
					throw e;
				}
			}
		} catch (Exception ex) {
			if (isRunning) {
				System.err.println("Server exception: " + ex);
			}
		}
	}
}
