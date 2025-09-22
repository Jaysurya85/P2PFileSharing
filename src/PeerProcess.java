import java.util.List;
import models.Peer;

public class PeerProcess {
	private ClientConnectionSetter clientConnectionSetter;
	private ServerConnectionSetter serverConnectionSetter;
	private Peer peer;

	public PeerProcess(Peer peer) {
		this.peer = peer;
		this.serverConnectionSetter = new ServerConnectionSetter(this.clientConnectionSetter);
	}

	public Peer getPeer() {
		return this.peer;
	}

	public ServerConnectionSetter gServerConnectionSetter() {
		return this.serverConnectionSetter;
	}

	public static void main(String[] args) {
		ConfigParser cp = new ConfigParser();
		List<Peer> peers = cp.getPeerConfig();
		int selfPeerId = Integer.parseInt(args[0]);
		Peer selfPeer = peers.stream()
				.filter(p -> p.getPeerId() == selfPeerId)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Peer ID not found"));

		// Create a peerProcess instance
		PeerProcess selfPeerProcess = new PeerProcess(selfPeer);
		// System.out.println("Peer is " + selfPeerProcess.getPeer().getPeerId());
		// Start server thread
		Thread serverThread = new Thread(selfPeerProcess.serverConnectionSetter);
		serverThread.start();
		System.out.println("Peer " + selfPeerId + " starts as a server");

		// Connect as client to all peers with smaller IDs
		for (Peer otherPeer : peers) {
			if (otherPeer.getPeerId() < selfPeerId) {
				System.out
						.println("Peer " + selfPeerId + " connects as a client to Peer " +
								otherPeer.getPeerId());
				selfPeerProcess.clientConnectionSetter = new ClientConnectionSetter(selfPeer, otherPeer,
						selfPeerProcess.serverConnectionSetter);
				Thread clientThread = new Thread(selfPeerProcess.clientConnectionSetter);
				clientThread.start();
			}
		}
	}

}
