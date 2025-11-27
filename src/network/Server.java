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
import utils.MessageUtils;

/*
 * :
 */

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
			case 0:
				ChokeMessageHandler clientChokeHandler = new ChokeMessageHandler();
				System.out.println("Client is choking  us " + clientChokeHandler);
				break;

			case 1:
				UnChokeMessageHandler clientUnchokeHandler = new UnChokeMessageHandler();
				System.out.println("Client is un choking us " + clientUnchokeHandler);
				int interestedPiece = peerNode.getInterestedPiece(this.clientHandshakeInfo.getPeerId());
				if (interestedPiece > 0) {
					MessageUtils.sendRequest(interestedPiece, out);
				} else {
					System.out.println("No peice to request");
				}
				break;

			case 2:
				InterestedMessageHandler clientInterestedMessage = new InterestedMessageHandler();
				System.out.println("Client is sending interested as " + clientInterestedMessage);
				this.peerNode.setPeerInterested(this.clientHandshakeInfo.getPeerId());
				// MessageUtils.sendUnChoke(this.out);
				break;

			case 3:
				NotInterestedMessageHandler clientNotInterestedMessage = new NotInterestedMessageHandler();
				System.out.println("Client is sending not interested as " + clientNotInterestedMessage);
				this.peerNode.setPeerNotInterested(this.clientHandshakeInfo.getPeerId());
				break;

			case 4:
				HaveMessageHandler clientHaveMessage = HaveMessageHandler.fromByteArray(payload);
				System.out.println("Client is sending have message as " + clientHaveMessage);
				this.peerNode.setOtherPeerBit(this.clientHandshakeInfo.getPeerId(), clientHaveMessage.getPieceIndex());
				break;

			case 5:
				BitfieldMessageHandler clientBitfieldMessage = BitfieldMessageHandler.fromByteArray(payload);
				System.out.println("Client is sending bitfield as " + clientBitfieldMessage);
				this.peerNode.setOtherPeerBitfield(this.clientHandshakeInfo.getPeerId(),
						clientBitfieldMessage.getPayload());
				BitfieldMessageHandler bitfieldMessage = new BitfieldMessageHandler(this.peerNode.getBitfield());
				System.out.println("Server is sending bitfield as " + bitfieldMessage);
				this.out.write(bitfieldMessage.toByteArray());
				boolean isInterested = peerNode.setInterestedPieces(this.clientHandshakeInfo.getPeerId(), payload);
				MessageUtils.sendInterestedOrNot(isInterested, this.out);
				break;

			case 6:
				RequestMessageHandler clientRequestMessage = RequestMessageHandler.fromByteArray(payload);
				System.out.println("Client is sending request as " + clientRequestMessage);
				int pieceIndex = clientRequestMessage.getPieceIndex();
				FileManager fm = this.peerNode.getFileManager();
				byte[] piece = fm.getPiece(pieceIndex);
				MessageUtils.sendPiece(pieceIndex, piece, this.out);
				break;

			case 7:
				PieceMessageHandler clientPieceMessage = PieceMessageHandler.fromByteArray(payload);
				System.out.println("Client is sending piece as " + clientPieceMessage);

				// Save the piece in file manager
				pieceIndex = clientPieceMessage.getPieceIndex();
				byte[] pieceData = clientPieceMessage.getPieceData();
				fm = this.peerNode.getFileManager();
				fm.setPeice(pieceIndex, pieceData);

				// set the bitfield remove the interested pieces
				this.peerNode.setBit(this.clientHandshakeInfo.getPeerId(), pieceIndex);
				HaveMessageHandler haveMessage = new HaveMessageHandler(pieceIndex);
				byte[] havePayload = haveMessage.toByteArray();

				// send the have messages to all the connected peers
				this.peerNode.broadcastToClients(havePayload);
				this.peerNode.braodcastToServers(havePayload);

				// Request the next piece
				interestedPiece = peerNode.getInterestedPiece(this.clientHandshakeInfo.getPeerId());
				if (interestedPiece > 0) {
					MessageUtils.sendRequest(interestedPiece, out);
				} else {
					System.out.println("No peice to request");
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
		System.out.println(
				"Client connected from: " + this.socket.getInetAddress().getHostAddress());
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
			System.out.println("Error in client handler is " + ex);
		} finally {
			try {
				System.out.println(" Client disconnected.");
				this.socket.close(); // Close the client socket

			} catch (Exception ex) {
				System.out.println("Error closing connection with client " + this.clientPeerId);
			}
		}
	}

	public Optional<Integer> doHandshake() throws Exception {
		byte[] clientHandshakeBuffer = new byte[32];
		HandshakeMessage message = new HandshakeMessage();
		in.read(clientHandshakeBuffer);
		this.clientHandshakeInfo = message.parseHandshake(clientHandshakeBuffer);
		System.out
				.println("Received from client: " + clientHandshakeInfo.getHeader() + "client peer id is :"
						+ clientHandshakeInfo.getPeerId());
		boolean isHandshakeDone = message.verifyHeader(this.clientHandshakeInfo.getHeader());
		if (isHandshakeDone) {
			byte[] handshakeMessage = message.buildHandshake(this.serverPeerId);
			System.out.println("Handshake done with client: " + clientHandshakeInfo.getPeerId());
			this.out.write(handshakeMessage);
			return Optional.of(clientHandshakeInfo.getPeerId());
		} else {
			System.out.println("Not connecting with client with peerid: " + clientHandshakeInfo.getPeerId()
					+ " wrong header");
			return Optional.empty();
		}

	}

	public void sendHaveMessage(byte[] byteArray) {
		try {
			System.out.println("Sending to client peer " + this.clientHandshakeInfo.getPeerId());
			this.out.write(byteArray);
		} catch (Exception ex) {
			System.out.println("Error occured while testing to send message from server to client");
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
		System.out.println("Server peer sending to client message is " + this.peer.getPeerId());
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
				System.out.println("[SERVER] Sent CHOKE to client peer " + peerId);
			} catch (Exception ex) {
				System.out.println("[SERVER] Error sending CHOKE to client " + peerId + ": " + ex);
			}
		} else {
			System.out.println("[SERVER] Cannot send CHOKE to " + peerId + " - not connected as client");
		}
	}

	public void sendUnchokeToClient(int peerId) {
		ClientHandler handler = clientHandlers.get(peerId);
		if (handler != null) {
			try {
				MessageUtils.sendUnChoke(handler.out);
				System.out.println("[SERVER] Sent UNCHOKE to client peer " + peerId);
			} catch (Exception ex) {
				System.out.println("[SERVER] Error sending UNCHOKE to client " + peerId + ": " + ex);
			}
		} else {
			System.out.println("[SERVER] Cannot send UNCHOKE to " + peerId + " - not connected as client");
		}
	}

	@Override
	public void run() {

		try {
			ServerSocket serverSocket = new ServerSocket(peer.getPortNo(), 50,
					InetAddress.getByName(peer.getHostName()));
			System.out.println(this.peer.getPeerId() + " Server is active " + serverSocket.toString());
			while (true) {
				// Wait for a client to connect
				Socket clientSocket = serverSocket.accept();
				System.out.println("Cilent handler created");
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
			System.out.println("Exception while creating server " + this.peer.getPeerId() + ex);
		}
	}

}
