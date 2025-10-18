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
		this.otherPeerBitfield.get(peerId).setInterestedInMe(true);
	}

	public void setPeerNotInterested(int peerId) {
		this.otherPeerBitfield.get(peerId).setInterestedInMe(false);
	}

	public void setOtherPeerBitfield(int peerId, byte[] otherPeerBitfield) {
		this.otherPeerBitfield.put(peerId, new Neighbour(otherPeerBitfield, peerId));
	}

	public void setBit(int peerId, int pieceIndex) {
		int byteIndex = pieceIndex / 8;
		int bitIndex = 7 - pieceIndex % 8;
		byte val = this.bitfield[byteIndex];
		val = (byte) (val | (1 << bitIndex));
		this.bitfield[byteIndex] = val;
		removeInterestedPieces(peerId, pieceIndex);
	}

	public void setOtherPeerBit(int peerId, int pieceIndex) {
		Neighbour neighbour = this.otherPeerBitfield.get(peerId);
		byte[] bitfield = neighbour.getBitfield();
		int byteIndex = pieceIndex / 8;
		int bitIndex = 7 - pieceIndex % 8;
		byte val = bitfield[byteIndex];

		byte mask = (byte) (1 << bitIndex);
		boolean alreadySet = (val & mask) != 0;
		if (alreadySet) {
			return;
		}
		val = (byte) (val | mask);
		bitfield[byteIndex] = val;
		neighbour.setBitfield(bitfield);
		addInterestedPieces(peerId, pieceIndex);
	}

	private void addInterestedPieces(int peerId, int pieceIndex) {
		Neighbour neighbour = this.otherPeerBitfield.get(peerId);
		neighbour.addInterestingPieces(pieceIndex);
	}

	private void removeInterestedPieces(int peerId, int pieceIndex) {
		Neighbour neighbour = this.otherPeerBitfield.get(peerId);
		neighbour.removeInterestingPieces(pieceIndex);
	}

	public void braodcastToServers(byte[] payload) {
		this.clientManager.broadcastToServers(payload);
	}

	public void broadcastToClients(byte[] payload) {
		this.server.broadcastingToMyClients(payload);
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

	public boolean setInterestedPieces(int peerId, byte[] payload) {
		List<Integer> interestedPieces = new ArrayList<>();
		for (int i = 0; i < bitfield.length; i++) {
			byte mine = this.bitfield[i];
			byte theirs = payload[i];
			for (int bit = 7; bit >= 0; bit--) {
				int myBit = (mine >> bit) & 1;
				int theirBit = (theirs >> bit) & 1;
				if (myBit == 0 && theirBit == 1) {
					int pieceIndex = i * 8 + (7 - bit);
					interestedPieces.add(pieceIndex);
				}
			}
		}
		this.otherPeerBitfield.get(peerId).setInterestingPieces(interestedPieces);
		return interestedPieces.isEmpty();
	}

	public List<Integer> getInterestedPieces(int peerId) {
		return this.otherPeerBitfield.get(peerId).getInterestingPieces();
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
