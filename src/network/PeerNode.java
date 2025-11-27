package network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import models.Neighbour;
import models.Peer;
import utils.FileManager;

public class PeerNode {
	private Peer peer;
	private Server server;
	private ClientManager clientManager;
	private Set<Integer> requestedPeices;
	private byte[] bitfield;
	private FileManager fileManager;

	private int unChokingInterval;
	private int numberOfPreferredNeighbors;

	private Set<Integer> currentlyUnchoked;
	private Set<Integer> connectedToAsClient;
	private Set<Integer> connectedToAsServer;

	private ConcurrentHashMap<Integer, Integer> bytesDownloaded;
	private ScheduledExecutorService scheduler;

	private ConcurrentHashMap<Integer, Neighbour> otherPeerBitfield;

	private Object pieceLock;
	private Object bitfieldLock;

	public PeerNode(Peer peer, FileManager fileManager, int noOfPieces, int unChokingInterval,
			int numberOfPreferredNeighbors) {
		int bytefieldLength = Math.ceilDiv(noOfPieces, 8);
		this.peer = peer;
		this.fileManager = fileManager;
		this.server = new Server(this);
		this.clientManager = new ClientManager(this);
		this.requestedPeices = ConcurrentHashMap.newKeySet();
		this.pieceLock = new Object();
		this.bitfieldLock = new Object();
		this.bitfield = new byte[bytefieldLength];
		this.otherPeerBitfield = new ConcurrentHashMap<>();

		this.currentlyUnchoked = Collections.synchronizedSet(new HashSet<>());
		this.connectedToAsClient = Collections.synchronizedSet(new HashSet<>());
		this.connectedToAsServer = Collections.synchronizedSet(new HashSet<>());

		this.bytesDownloaded = new ConcurrentHashMap<>();
		this.scheduler = Executors.newScheduledThreadPool(1);

		this.unChokingInterval = unChokingInterval;
		this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;

		if (peer.getisFilePresent()) {
			fillBitfield(noOfPieces);
			this.fileManager.breakFileIntoPeices();
			this.fileManager.setNoOfMissingPeices(0);
		} else {
			Arrays.fill(this.bitfield, (byte) 0);
			this.fileManager.setNoOfMissingPeices(noOfPieces);
		}
	}

	public void setPeerInterested(int peerId) {
		this.otherPeerBitfield.get(peerId).setInterestedInMe(true);
	}

	public void setPeerNotInterested(int peerId) {
		this.otherPeerBitfield.get(peerId).setInterestedInMe(false);
	}

	public void setOtherPeerBitfield(int peerId, byte[] otherPeerBitfield) {
		synchronized (bitfieldLock) {
			this.otherPeerBitfield.put(peerId, new Neighbour(otherPeerBitfield, peerId));
		}
	}

	public void setBit(int peerId, int pieceIndex) {
		synchronized (bitfieldLock) {
			int byteIndex = pieceIndex / 8;
			int bitIndex = 7 - pieceIndex % 8;
			byte val = this.bitfield[byteIndex];
			val = (byte) (val | (1 << bitIndex));
			this.bitfield[byteIndex] = val;
		}
		removeInterestedPieces(peerId, pieceIndex);
		this.requestedPeices.remove(pieceIndex);
	}

	public void setOtherPeerBit(int peerId, int pieceIndex) {
		Neighbour neighbour = this.otherPeerBitfield.getOrDefault(peerId, null);
		if (neighbour == null)
			return;
		synchronized (bitfieldLock) {
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
		}
		addInterestedPieces(peerId, pieceIndex);
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
		this.connectedToAsClient.add(serverPeerId);
	}

	public void addClient(int peerId) {
		this.connectedToAsServer.add(peerId);
	}

	public boolean setInterestedPieces(int peerId, byte[] payload) {
		List<Integer> interestedPieces = new ArrayList<>();
		synchronized (bitfieldLock) {
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
		}
		synchronized (pieceLock) {
			this.otherPeerBitfield.get(peerId).setInterestingPieces(interestedPieces);
		}
		return !interestedPieces.isEmpty();
	}

	public void recordBytesDownloaded(int peerId, int bytes) {
		bytesDownloaded.merge(peerId, bytes, Integer::sum);
	}

	public int getInterestedPiece(int peerId) {
		synchronized (pieceLock) {
			List<Integer> pieces = this.otherPeerBitfield.get(peerId).getInterestingPieces();
			int ind = pieces.size() - 1;
			while (ind >= 0) {
				if (this.requestedPeices.contains(pieces.get(ind))) {
					ind--;
				} else {
					this.requestedPeices.add(pieces.get(ind));
					return pieces.get(ind);
				}
			}
			return -1;
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

	public byte[] getOtherBitfield(int peerId) {
		return this.otherPeerBitfield.get(peerId).getBitfield();
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public void setFileManager(FileManager fileManager) {
		this.fileManager = fileManager;
	}

	public Set<Integer> getRequestedPeices() {
		return requestedPeices;
	}

	public void setRequestedPeices(Set<Integer> requestedPeices) {
		this.requestedPeices = requestedPeices;
	}

	// PUBLIC METHOD that returns Runnable for scheduler
	public Runnable selectPreferedNeighbours() {
		return () -> {
			selectPreferredNeighbors();
		};
	}

	private void addInterestedPieces(int peerId, int pieceIndex) {
		synchronized (pieceLock) {
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour != null) {
				neighbour.addInterestingPieces(pieceIndex);
			}
		}
	}

	private void removeInterestedPieces(int peerId, int pieceIndex) {
		synchronized (pieceLock) {
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour != null) {
				neighbour.removeInterestingPieces(pieceIndex);
			}
		}
	}

	private void fillBitfield(int noOfPieces) {
		synchronized (bitfieldLock) {
			int fullBytes = noOfPieces / 8;
			Arrays.fill(this.bitfield, 0, fullBytes, (byte) 0xFF);
			int remainder = noOfPieces % 8;
			if (remainder != 0) {
				byte lastByte = 0;
				for (int i = 0; i < remainder; i++) {
					lastByte |= (1 << (7 - i));
				}
				bitfield[fullBytes] = lastByte;
			}
		}
	}

	private boolean hasCompleteFile() {
		return this.fileManager.getNoOfMissingPeices() == 0;
	}

	private void selectPreferredNeighbors() {
		System.out.println("[SCHEDULER] Running preferred neighbor selection...");

		Set<Integer> allConnectedPeers = new HashSet<>();
		for (Integer it : connectedToAsServer) {
			System.out.println("connected peers are " + it);
		}
		for (Integer it : connectedToAsClient) {
			System.out.println("connected peers are " + it);
		}
		allConnectedPeers.addAll(this.connectedToAsClient);
		allConnectedPeers.addAll(this.connectedToAsServer);

		List<Integer> interestedPeers = new ArrayList<>();
		for (Integer peerId : allConnectedPeers) {
			Neighbour neighbour = otherPeerBitfield.get(peerId);
			if (neighbour != null && neighbour.isInterestedInMe()) {
				interestedPeers.add(peerId);
			}
		}

		System.out.println("[SCHEDULER] Interested peers: " + interestedPeers);

		Set<Integer> newPreferredNeighbors = new HashSet<>();

		if (hasCompleteFile()) {
			System.out.println("[SCHEDULER] Complete file - selecting randomly");
			Collections.shuffle(interestedPeers);
			for (int i = 0; i < Math.min(this.numberOfPreferredNeighbors, interestedPeers.size()); i++) {
				newPreferredNeighbors.add(interestedPeers.get(i));
			}
		} else {
			// Select based on download rate
			System.out.println("[SCHEDULER] Incomplete file - selecting by download rate");
			Map<Integer, Integer> downloadRates = new HashMap<>();
			for (Integer peerId : interestedPeers) {
				int downloaded = bytesDownloaded.getOrDefault(peerId, 0);
				downloadRates.put(peerId, downloaded);
				System.out.println("[SCHEDULER] Peer " + peerId + " downloaded: " + downloaded + " bytes");
			}

			// Sort by download rate (descending)
			List<Integer> sortedPeers = downloadRates.entrySet().stream()
					.sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
					.map(Map.Entry::getKey)
					.collect(Collectors.toList());

			// Pick top K
			for (int i = 0; i < Math.min(numberOfPreferredNeighbors, sortedPeers.size()); i++) {
				newPreferredNeighbors.add(sortedPeers.get(i));
			}
		}

		// Reset download rates for next interval
		bytesDownloaded.clear();

		System.out.println("[SCHEDULER] New preferred neighbors: " + newPreferredNeighbors);

		// Send UNCHOKE to newly preferred neighbors
		synchronized (currentlyUnchoked) {
			for (Integer peerId : newPreferredNeighbors) {
				if (!currentlyUnchoked.contains(peerId)) {
					System.out.println("[SCHEDULER] Unchoking peer " + peerId);
					sendUnchokeMessage(peerId);
				}
			}

			// Send CHOKE to previously unchoked peers no longer preferred
			for (Integer peerId : currentlyUnchoked) {
				if (!newPreferredNeighbors.contains(peerId)) {
					System.out.println("[SCHEDULER] Choking peer " + peerId);
					sendChokeMessage(peerId);
				}
			}

			// Update current set
			currentlyUnchoked.clear();
			currentlyUnchoked.addAll(newPreferredNeighbors);
		}

	}

	private void sendUnchokeMessage(int peerId) {
		// Send to peer regardless of connection type
		if (connectedToAsClient.contains(peerId)) {
			clientManager.sendUnchoke(peerId);
		}
		if (connectedToAsServer.contains(peerId)) {
			server.sendUnchokeToClient(peerId);
		}
	}

	private void sendChokeMessage(int peerId) {
		// Send to peer regardless of connection type
		if (connectedToAsClient.contains(peerId)) {
			clientManager.sendChoke(peerId);
		}
		if (connectedToAsServer.contains(peerId)) {
			server.sendChokeToClient(peerId);
		}
	}
}
