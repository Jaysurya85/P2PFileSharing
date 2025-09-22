
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.*;

import models.HandshakeInfo;
import models.Peer;

class ClientListener implements Runnable {
	private InputStream in;

	public ClientListener(InputStream in) {
		this.in = in;
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
				String msg = new String(buffer, 0, read, StandardCharsets.UTF_8);
				System.out.println("Received from server: " + msg);
			}
		} catch (Exception e) {
			System.out.println("Error reading from server: " + e);
		}
	}
}

/*
 * client (selfClientinfo, selfServerINfo, serverInfoToWhichIAmClient)
 */

public class ClientConnectionSetter implements Runnable {
	private ServerConnectionSetter serverConnectionSetter;
	private Peer serverPeer;
	private Peer clientPeer;
	private HandshakeInfo serverHandshakeInfo;

	public ClientConnectionSetter(ServerConnectionSetter serverConnectionSetter, Peer clientPeer, Peer serverPeer) {
		this.serverConnectionSetter = serverConnectionSetter;
		this.clientPeer = clientPeer;
		this.serverPeer = serverPeer;
		this.serverHandshakeInfo = new HandshakeInfo();
	}

	public boolean doHandshake(InputStream in, OutputStream out) throws Exception {

		Message message = new Message();
		byte[] handshakeMessage = message.buildHandshake(this.clientPeer.getPeerId());
		byte[] serverHandshakeBuffer = new byte[32];
		out.write(handshakeMessage);
		in.read(serverHandshakeBuffer);
		this.serverHandshakeInfo = message.parseHandshake(serverHandshakeBuffer);
		boolean isHandshakeDone = message.verifyHeader(this.serverHandshakeInfo.getHeader());
		return isHandshakeDone;
	}

	@Override
	public void run() {
		String serverHost = this.serverPeer.getHostName();
		int serverPort = this.serverPeer.getPortNo();
		int serverPeerId = this.serverPeer.getPeerId();

		try {

			Socket socket = new Socket(serverHost, serverPort);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			System.out.println("Connected to server " + serverPeerId + " " + serverHost + ":" + serverPort);
			boolean isHandshakeDone = doHandshake(in, out);
			if (isHandshakeDone) {

				System.out.println("Handshake done with Server " + this.serverHandshakeInfo.getHeader() + " peer id is "
						+ this.serverHandshakeInfo.getPeerId());
			} else {
				out.write("exit".getBytes());
				System.out.println("Closing Client to server because it wasn't a match: " + serverPeerId);
			}
			// 2. Start listening thread **after handshake**
			Thread listenerThread = new Thread(new ClientListener(in));
			listenerThread.start();
			Thread.sleep(10000);
			ServerConnectionSetter sc = this.serverConnectionSetter;
			System.out.println("sending message from server to client");
			sc.serverSendMessageAsTesting();

			socket.close();
		} catch (Exception ex) {
			System.out.println("Exception while creating client" + ex);
		}

	}

}
