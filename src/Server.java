import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import Messages.BitfieldMessageHandler;

import java.io.*;
import java.net.*;

import models.HandshakeInfo;
import models.Peer;

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

	@Override
	public void run() {
		System.out.println(
				"Client connected from: " + this.socket.getInetAddress().getHostAddress());
		try {

			byte[] clientBuffer = new byte[1024];
			int ind;
			while ((ind = this.in.read(clientBuffer)) != -1) {
				String inputString = new String(clientBuffer, 0, ind, StandardCharsets.UTF_8).trim();
				System.out.println("Recieved from client after handshake :" + inputString);
				BitfieldMessageHandler clientBitfieldMessage = BitfieldMessageHandler.fromByteArray(clientBuffer);
				System.out.println("Client is sending bitfield as " + clientBitfieldMessage);
				this.peerNode.setOtherPeerBit(this.clientHandshakeInfo.getPeerId(), clientBitfieldMessage.getPayload());
				BitfieldMessageHandler bitfieldMessage = new BitfieldMessageHandler(this.peerNode.getBitfield());
				System.out.println("Server is sending bitfield as " + bitfieldMessage);
				this.out.write(bitfieldMessage.toByteArray());
				if (inputString.equals("exit")) {
					break;
				}
				// if (inputString.equals("sendAsClient")) {
				// System.out.println("recieved sendAsClient");
				// this.peerNode.sendMessageFromClient();
				// }
				// if (inputString.equals("Hello from client")) {
				// this.out.write("sendAsServer".getBytes());
				// }
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

	public void sendMessageTesting() {
		try {
			System.out.println("Sending as server to client peer " + this.clientHandshakeInfo.getPeerId());
			this.out.write("Hello from server".getBytes());
		} catch (Exception ex) {
			System.out.println("Error occured while testing to send message from server to client");
		}
	}

}

/*
 * server (selfClientinfo, selfServerINfo, clientsOfMine)
 */

public class Server implements Runnable {
	PeerNode peerNode;
	Peer peer;
	Map<Integer, ClientHandler> clientHandlers;

	public Server(PeerNode peerNode) {
		this.peerNode = peerNode;
		this.peer = peerNode.getPeer();
		this.clientHandlers = new HashMap<>();
	}

	public void serverSendMessageAsTesting() {
		System.out.println("peer in server client message is " + this.peer.getPeerId());
		if (!this.clientHandlers.isEmpty()) {
			for (Map.Entry<Integer, ClientHandler> entry : clientHandlers.entrySet()) {
				ClientHandler clientHandler = entry.getValue();
				clientHandler.sendMessageTesting();
			}
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
					Thread clientThread = new Thread(clientHandler);
					clientThread.start();
				}

			}
		} catch (Exception ex) {
			System.out.println("Exception while creating server " + this.peer.getPeerId() + ex);
		}
	}

}
