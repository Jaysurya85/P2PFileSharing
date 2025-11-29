import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

		PeerNode peerNode = new PeerNode(selfPeer, fileManager, noOfPieces, commonConfig.getNoOfPreferredNeighbours());
		peerNode.startServer();

		for (Peer otherPeer : peers) {
			if (otherPeer.getPeerId() < selfPeerId) {
				peerNode.connectClient(otherPeer.getPortNo(), otherPeer.getPeerId(), otherPeer.getHostName());
			}
		}

		ScheduledExecutorService chokingScheduler = Executors.newScheduledThreadPool(1);
		chokingScheduler.scheduleAtFixedRate(
				peerNode.selectPreferedNeighbours(),
				0, // initial delay
				commonConfig.getUnChockingInterval(),
				TimeUnit.SECONDS);

	}
}
