import java.util.List;

import models.Common;
import models.Peer;

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
		int bytefieldLength = (int) (commonConfig.getFileSize() / (long) commonConfig.getPieceSize());
		List<Peer> peers = cp.getPeerConfig();
		int selfPeerId = Integer.parseInt(args[0]);
		Peer selfPeer = peers.stream()
				.filter(p -> p.getPeerId() == selfPeerId)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Peer ID not found"));

		// Start server thread
		PeerNode peerNode = new PeerNode(selfPeer, bytefieldLength);
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
