
import java.util.Arrays;
import java.util.HashMap;

import models.Peer;

public class PeerNode {
	private Peer peer;
	private Server server;
	private ClientManager clientManager;
	private byte[] bitfield;

	private HashMap<Integer, byte[]> otherPeerBitfield;

	public PeerNode(Peer peer, int bytefieldLength) {
		this.peer = peer;
		this.server = new Server(this);
		this.clientManager = new ClientManager(this);
		this.bitfield = new byte[bytefieldLength];
		this.otherPeerBitfield = new HashMap<>();
		if (peer.getisFilePresent()) {
			Arrays.fill(this.bitfield, (byte) 1);
		} else {
			Arrays.fill(this.bitfield, (byte) 0);
		}
	}

	public Peer getPeer() {
		return this.peer;
	}

	public Server getServer() {
		return this.server;
	}

	public byte[] getBitfield() {
		return this.bitfield;
	}

	public void setOtherPeerBit(int serverPeerId, byte[] byteArray) {
		this.otherPeerBitfield.put(serverPeerId, byteArray);
	}

	public void setBit(int serverId, byte setByte) {
		int ind = setByte / 8;
		byte val = otherPeerBitfield.get(serverId)[ind];
		val = (byte) (val | (1 << val));
		otherPeerBitfield.get(serverId)[ind] = val;
	}

	public void setClientManager(ClientManager clientManager) {
		this.clientManager = clientManager;
	}

	public void startServer() {
		new Thread(this.server).start();
	}

	public void connectClient(int serverPort, int serverPeerId, String serverHost) {
		System.out.println("inside peernode");
		clientManager.connect(serverPort, serverPeerId, serverHost).start();
	}

	// public void sendMessageFromClient() {
	// this.clientManager.clientSendMessageAsTesting();
	// }

	public void sendMessageFromServer() {
		this.server.serverSendMessageAsTesting();
	}

}
