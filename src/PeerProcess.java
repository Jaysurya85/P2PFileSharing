import java.util.List;

import models.Common;
import models.Peer;
import network.PeerNode;
import utils.ConfigParser;
import utils.FileManager;

public class PeerProcess {
	private Peer peer;

	public PeerProcess(Peer peer) {
		this.peer = peer;
	}

	public Peer getPeer() {
		return this.peer;
	}

	public static void main(String[] args) {
		ConfigParser cp = new ConfigParser();
		Common commonConfig = cp.getCommonConfig();
		int noOfPieces = (int) Math.ceilDiv(commonConfig.getFileSize(), (long) commonConfig.getPieceSize());
		List<Peer> peers = cp.getPeerConfig();
		int selfPeerId = Integer.parseInt(args[0]);
		Peer selfPeer = peers.stream()
				.filter(p -> p.getPeerId() == selfPeerId)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Peer ID not found"));

		FileManager fileManager = new FileManager(selfPeerId, commonConfig.getFileName(), commonConfig.getFileSize(),
				commonConfig.getPieceSize(), noOfPieces);

		// Start server thread
		PeerNode peerNode = new PeerNode(selfPeer, fileManager, noOfPieces);
		peerNode.startServer();
		System.out.println("Peer " + selfPeerId + " starts as a server");

		// Connect as client to all peers with smaller IDs
		for (Peer otherPeer : peers) {
			if (otherPeer.getPeerId() < selfPeerId) {
				System.out.println("Peer " + selfPeerId + " connects as a client to Peer " + otherPeer.getPeerId());
				peerNode.connectClient(otherPeer.getPortNo(), otherPeer.getPeerId(), otherPeer.getHostName());
			}
		}
	}

}
