package network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import models.Neighbour;
import models.Peer;

public class PeerNode {
	private Peer peer;
	private Server server;
	private ClientManager clientManager;
	private byte[] bitfield;

	private HashMap<Integer, Neighbour> otherPeerBitfield;

	public PeerNode(Peer peer, int bytefieldLength) {
		this.peer = peer;
		this.server = new Server(this);
		this.clientManager = new ClientManager(this);
		this.bitfield = new byte[bytefieldLength];
		this.otherPeerBitfield = new HashMap<>();
		if (peer.getisFilePresent()) {
			Arrays.fill(this.bitfield, (byte) 0xFF);
		} else {
			Arrays.fill(this.bitfield, (byte) 0);
		}
	}

	public void setPeerInterested(int peerId) {
		this.otherPeerBitfield.get(peerId).setInterested(true);
	}

	public void setPeerNotInterested(int peerId) {
		this.otherPeerBitfield.get(peerId).setInterested(false);
	}

	public void setInterestedPeices(int peerId, List<Integer> interestedPeices) {
	}

	public void setOtherPeerBit(int peerId, byte[] byteArray) {
		this.otherPeerBitfield.put(peerId, new Neighbour(byteArray, peerId));
	}

	public void setBit(int serverId, byte setByte) {
		int ind = setByte / 8;
		byte val = otherPeerBitfield.get(serverId).getBitfield()[ind];
		val = (byte) (val | (1 << val));
		otherPeerBitfield.get(serverId).getBitfield()[ind] = val;
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

	public boolean setInterestedPeices(int peerId, byte[] payload) {
		List<Integer> interestedPeices = new ArrayList<>();
		for (int i = 0; i < bitfield.length; i++) {
			byte mine = bitfield[i];
			byte theirs = this.bitfield[i];
			for (int bit = 7; bit >= 0; bit--) {
				int myBit = (mine >> bit) & 1;
				int theirBit = (theirs >> bit) & 1;
				if (myBit == 0 && theirBit == 1) {
					int pieceIndex = i * 8 + (7 - bit);
					interestedPeices.add(pieceIndex);
				}
			}
		}
		this.otherPeerBitfield.get(peerId).setInterestedPeices(interestedPeices);
		return interestedPeices.isEmpty();
	}

	public List<Integer> getInterestedPeices(int peerId) {
		return this.otherPeerBitfield.get(peerId).getInterestedPeices();
	}

	// public void sendMessageFromClient() {
	// this.clientManager.clientSendMessageAsTesting();
	// }

	public void sendMessageFromServer() {
		this.server.serverSendMessageAsTesting();
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
}
