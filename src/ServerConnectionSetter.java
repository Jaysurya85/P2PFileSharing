
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
	boolean isHandshakeDone;
	PeerProcess peerProcess;

	public ClientHandler(PeerProcess peerProcess, Socket socket, int serverPeerId) {
		this.socket = socket;
		this.serverPeerId = serverPeerId;
		this.clientHandshakeInfo = new HandshakeInfo();
		this.isHandshakeDone = false;
		this.peerProcess = peerProcess;
	}

	public void doHandshake(InputStream in, OutputStream out, byte[] clientHandshakeBuffer) throws Exception {
		if (this.isHandshakeDone) {
			return;
		}
		Message message = new Message();
		this.clientHandshakeInfo = message.parseHandshake(clientHandshakeBuffer);
		System.out
				.println("Received from client: " + clientHandshakeInfo.getHeader() + "client peer id is :"
						+ clientHandshakeInfo.getPeerId());
		this.isHandshakeDone = message.verifyHeader(this.clientHandshakeInfo.getHeader());
		if (isHandshakeDone) {
			byte[] handshakeMessage = message.buildHandshake(this.serverPeerId);
			System.out.println("Handshake done with client: " + clientHandshakeInfo.getPeerId());
			out.write(handshakeMessage);
		} else {
			System.out.println("Not connecting with client with peerid: " + clientHandshakeInfo.getPeerId()
					+ " wrong header");
		}
	}

	public void sendMessageTesting() {
		try {
			OutputStream out = this.socket.getOutputStream();
			System.out.println("Sending as server to client peer " + this.clientHandshakeInfo.getPeerId());
			out.write("How you doing".getBytes());
		} catch (Exception ex) {
			System.out.println("Error occured while testing to send message from server to client");
		}
	}

	@Override
	public void run() {
		System.out.println(
				"Client connected from: " + this.socket.getInetAddress().getHostAddress());
		try {

			InputStream in = this.socket.getInputStream();
			OutputStream out = this.socket.getOutputStream();
			byte[] clientBuffer = new byte[1024];
			int ind;
			while ((ind = in.read(clientBuffer)) != -1) {
				if (this.isHandshakeDone) {
					if (new String(clientBuffer, StandardCharsets.UTF_8).equals("exit")) {
						break;
					}
					if (new String(clientBuffer, StandardCharsets.UTF_8).equals("sendAsClient")) {

					}
				} else {
					doHandshake(in, out, clientBuffer);
				}
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

}

/*
 * server (selfClientinfo, selfServerINfo, clientsOfMine)
 */

public class ServerConnectionSetter implements Runnable {
	ClientConnectionSetter clientConnectionSetter;
	ClientHandler clientHandler;
	Peer peer;

	public ServerConnectionSetter(ClientConnectionSetter clientConnectionSetter, Peer peer) {
		this.clientConnectionSetter = clientConnectionSetter;
		this.peer = peer;
	}

	public void serverSendMessageAsTesting() throws Exception {
		System.out.println("peer in server client message is " + this.peer.getPeerId());
		if (this.clientHandler != null) {
			this.clientHandler.sendMessageTesting();
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
				this.clientHandler = new ClientHandler(clientSocket, this.peer.getPeerId());
				Thread clientThread = new Thread(this.clientHandler);
				clientThread.start();
			}
		} catch (Exception ex) {
			System.out.println("Exception while creating server " + this.peer.getPeerId() + ex);
		}
	}

}
