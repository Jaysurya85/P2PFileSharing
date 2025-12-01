package network;

import java.sql.Time;
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
import utils.Logger;

public class PeerNode {
	private Peer peer;
	private Server server;
	private ClientManager clientManager;
	private Set<Integer> requestedPeices;
	private byte[] bitfield;
	private FileManager fileManager;

	private int numberOfPreferredNeighbors;

	private Set<Integer> currentlyUnchoked;
	private Set<Integer> connectedToAsClient;
	private Set<Integer> connectedToAsServer;
	private Integer optimisticallyUnchokedPeer;

	private ConcurrentHashMap<Integer, Integer> bytesDownloaded;
	private ScheduledExecutorService preferredNeighborScheduler;
	private ScheduledExecutorService optimisticScheduler;

	private ConcurrentHashMap<Integer, Neighbour> otherPeerBitfield;

	private Object pieceLock;
	private Object bitfieldLock;
	private Object optimisticLock;

	private volatile boolean isTerminating = false;
	private volatile boolean hasNotifiedCompletion = false;
	private boolean startedWithCompleteFile; // NEW: Track if we started with file

	public PeerNode(Peer peer, FileManager fileManager, int noOfPieces, int numberOfPreferredNeighbors) {
		int bytefieldLength = Math.ceilDiv(noOfPieces, 8);
		this.peer = peer;
		this.fileManager = fileManager;
		this.server = new Server(this);
		this.clientManager = new ClientManager(this);
		this.requestedPeices = ConcurrentHashMap.newKeySet();
		this.pieceLock = new Object();
		this.bitfieldLock = new Object();
		this.optimisticLock = new Object();
		this.bitfield = new byte[bytefieldLength];
		this.otherPeerBitfield = new ConcurrentHashMap<>();

		this.currentlyUnchoked = Collections.synchronizedSet(new HashSet<>());
		this.connectedToAsClient = Collections.synchronizedSet(new HashSet<>());
		this.connectedToAsServer = Collections.synchronizedSet(new HashSet<>());

		this.bytesDownloaded = new ConcurrentHashMap<>();
		this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
		this.optimisticallyUnchokedPeer = null;

		this.preferredNeighborScheduler = Executors.newScheduledThreadPool(1);
		this.optimisticScheduler = Executors.newScheduledThreadPool(1);

		if (peer.getisFilePresent()) {
			fillBitfield(noOfPieces);
			this.fileManager.breakFileIntoPeices();
			this.fileManager.setNoOfMissingPeices(0);
			this.startedWithCompleteFile = true;
		} else {
			Arrays.fill(this.bitfield, (byte) 0);
			this.fileManager.setNoOfMissingPeices(noOfPieces);
			this.startedWithCompleteFile = false; // NEW: We need to download
		}
	}

	public void setPeerInterested(int peerId) {
		Neighbour neighbour = this.otherPeerBitfield.get(peerId);
		if (neighbour != null) {
			neighbour.setInterestedInMe(true);
		}
	}

	public void setPeerNotInterested(int peerId) {
		Neighbour neighbour = this.otherPeerBitfield.get(peerId);
		if (neighbour != null) {
			neighbour.setInterestedInMe(false);
		}
	}

	public void setPeerCompleted(int peerId) {
		Neighbour neighbour = this.otherPeerBitfield.get(peerId);
		if (neighbour != null) {
			neighbour.setHasCompletedFile(true);
			checkAndTerminate();
		}
	}

	public void setOtherPeerBitfield(int peerId, byte[] otherPeerBitfield) {
		synchronized (bitfieldLock) {
			this.otherPeerBitfield.put(peerId, new Neighbour(otherPeerBitfield, peerId));
		}

		if (checkPeerHasCompleteFile(otherPeerBitfield)) {
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour != null) {
				neighbour.setHasCompletedFile(true);
			}
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

		removeInterestedPiecesFromAll(pieceIndex);
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
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour != null) {
				neighbour.setInterestingPieces(interestedPieces);
			}
		}
		return !interestedPieces.isEmpty();
	}

	public void recordBytesDownloaded(int peerId, int bytes) {
		bytesDownloaded.merge(peerId, bytes, Integer::sum);
	}

	// FIXED: Use atomic add() instead of check-then-act pattern
	public int getInterestedPiece(int peerId) {
		synchronized (pieceLock) {
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour == null)
				return -1;
			List<Integer> pieces = neighbour.getInterestingPieces();
			int ind = pieces.size() - 1;
			while (ind >= 0) {
				int piece = pieces.get(ind);
				// CRITICAL FIX: Use add() which returns false if already present
				// This is atomic - if add() returns true, we got it, else skip
				if (this.requestedPeices.add(piece)) {
					// Successfully added - this piece is now ours
					return piece;
				}
				// Piece was already requested by another thread, try next
				ind--;
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
		Neighbour neighbour = this.otherPeerBitfield.get(peerId);
		return neighbour != null ? neighbour.getBitfield() : null;
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

	public Runnable selectPreferedNeighbours() {
		return () -> {
			if (!isTerminating) {
				selectPreferredNeighbors();
			}
		};
	}

	public Runnable selectOptimisticNeighbor() {
		return () -> {
			if (!isTerminating) {
				selectOptimisticUnchokedNeighbor();
			}
		};
	}

	// NEW: Check if a bitfield represents a complete file
	private boolean checkPeerHasCompleteFile(byte[] peerBitfield) {
		int totalPieces = this.fileManager.getTotalPieces();

		for (int i = 0; i < totalPieces; i++) {
			int byteIndex = i / 8;
			int bitIndex = 7 - i % 8;

			if (byteIndex >= peerBitfield.length) {
				return false; // Invalid bitfield
			}

			byte val = peerBitfield[byteIndex];
			byte mask = (byte) (1 << bitIndex);
			if ((val & mask) == 0) {
				return false; // Missing this piece
			}
		}

		return true; // Has all pieces
	}

	public void notifyCompletionToAll() {
		if (hasNotifiedCompletion) {
			return;
		}
		hasNotifiedCompletion = true;

		try {
			byte[] completedMessage = new Messages.CompletedMessageHandler().toByteArray();
			broadcastToClients(completedMessage);
			braodcastToServers(completedMessage);
		} catch (Exception e) {
			System.err.println("Error broadcasting COMPLETED message: " + e);
		}
	}

	public void onDownloadComplete() {
		notifyCompletionToAll();
		checkAndTerminate();
	}

	private void addInterestedPieces(int peerId, int pieceIndex) {
		synchronized (pieceLock) {
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour != null) {
				neighbour.addInterestingPieces(pieceIndex);
			}
		}
	}

	// NEW: Remove piece from ALL peers' interesting pieces lists
	private void removeInterestedPiecesFromAll(int pieceIndex) {
		synchronized (pieceLock) {
			// Remove from all neighbors' interesting pieces lists
			for (Neighbour neighbour : otherPeerBitfield.values()) {
				neighbour.removeInterestingPieces(pieceIndex);
			}
			// CRITICAL: Also remove from requested set in SAME critical section
			this.requestedPeices.remove(pieceIndex);
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

	public boolean hasCompleteFile() {
		return this.fileManager.getNoOfMissingPeices() == 0;
	}

	private synchronized void selectPreferredNeighbors() {
		Set<Integer> allConnectedPeers = new HashSet<>();
		allConnectedPeers.addAll(this.connectedToAsClient);
		allConnectedPeers.addAll(this.connectedToAsServer);

		List<Integer> interestedPeers = new ArrayList<>();
		for (Integer peerId : allConnectedPeers) {
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour != null && neighbour.isInterestedInMe()) {
				interestedPeers.add(peerId);
			}
		}

		Set<Integer> newPreferredNeighbors = new HashSet<>();

		if (hasCompleteFile()) {
			Collections.shuffle(interestedPeers);
			for (int i = 0; i < Math.min(this.numberOfPreferredNeighbors, interestedPeers.size()); i++) {
				newPreferredNeighbors.add(interestedPeers.get(i));
			}
		} else {
			Map<Integer, Integer> downloadRates = new HashMap<>();
			for (Integer peerId : interestedPeers) {
				int downloaded = this.bytesDownloaded.getOrDefault(peerId, 0);
				downloadRates.put(peerId, downloaded);
			}

			List<Integer> sortedPeers = downloadRates.entrySet().stream()
					.sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
					.map(Map.Entry::getKey)
					.collect(Collectors.toList());

			for (int i = 0; i < Math.min(this.numberOfPreferredNeighbors, sortedPeers.size()); i++) {
				newPreferredNeighbors.add(sortedPeers.get(i));
			}
		}

		bytesDownloaded.clear();

		synchronized (currentlyUnchoked) {
			for (Integer peerId : newPreferredNeighbors) {
				if (!currentlyUnchoked.contains(peerId)) {
					sendUnchokeMessage(peerId);
				}
			}

			for (Integer peerId : currentlyUnchoked) {
				if (!newPreferredNeighbors.contains(peerId)) {
					synchronized (optimisticLock) {
						if (optimisticallyUnchokedPeer == null || !optimisticallyUnchokedPeer.equals(peerId)) {
							sendChokeMessage(peerId);
						}
					}
				}
			}

			currentlyUnchoked.clear();
			currentlyUnchoked.addAll(newPreferredNeighbors);
		}

		Logger.logPreferredNeighbors(peer.getPeerId(), new ArrayList<>(newPreferredNeighbors));
	}

	private void selectOptimisticUnchokedNeighbor() {
		synchronized (optimisticLock) {
			Set<Integer> allConnectedPeers = new HashSet<>();
			allConnectedPeers.addAll(this.connectedToAsClient);
			allConnectedPeers.addAll(this.connectedToAsServer);

			List<Integer> candidates = new ArrayList<>();
			synchronized (currentlyUnchoked) {
				for (Integer peerId : allConnectedPeers) {
					Neighbour neighbour = this.otherPeerBitfield.get(peerId);
					if (neighbour != null && neighbour.isInterestedInMe() && !currentlyUnchoked.contains(peerId)) {
						candidates.add(peerId);
					}
				}
			}

			if (candidates.isEmpty()) {
				optimisticallyUnchokedPeer = null;
				return;
			}

			Collections.shuffle(candidates);
			Integer newOptimisticPeer = candidates.get(0);

			if (optimisticallyUnchokedPeer != null && !optimisticallyUnchokedPeer.equals(newOptimisticPeer)) {
				synchronized (currentlyUnchoked) {
					if (!currentlyUnchoked.contains(optimisticallyUnchokedPeer)) {
						sendChokeMessage(optimisticallyUnchokedPeer);
					}
				}
			}

			sendUnchokeMessage(newOptimisticPeer);
			optimisticallyUnchokedPeer = newOptimisticPeer;

			Logger.logOptimisticallyUnchokedNeighbor(peer.getPeerId(), newOptimisticPeer);
		}
	}

	private void sendUnchokeMessage(int peerId) {
		if (connectedToAsClient.contains(peerId)) {
			clientManager.sendUnchoke(peerId);
		}
		if (connectedToAsServer.contains(peerId)) {
			server.sendUnchokeToClient(peerId);
		}
	}

	private void sendChokeMessage(int peerId) {
		if (connectedToAsClient.contains(peerId)) {
			clientManager.sendChoke(peerId);
		}
		if (connectedToAsServer.contains(peerId)) {
			server.sendChokeToClient(peerId);
		}
	}

	private boolean checkAllPeersComplete() {
		if (!hasCompleteFile()) {
			return false;
		}

		Set<Integer> allConnectedPeers = new HashSet<>();
		allConnectedPeers.addAll(this.connectedToAsClient);
		allConnectedPeers.addAll(this.connectedToAsServer);

		for (Integer peerId : allConnectedPeers) {
			Neighbour neighbour = this.otherPeerBitfield.get(peerId);
			if (neighbour == null || !neighbour.hasCompletedFile()) {
				return false;
			}
		}

		return true;
	}

	public void checkAndTerminate() {
		if (checkAllPeersComplete()) {
			terminateGracefully();
		}
	}

	private void terminateGracefully() {
		if (isTerminating) {
			return;
		}
		isTerminating = true;

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// Ignore
		}

		if (preferredNeighborScheduler != null && !preferredNeighborScheduler.isShutdown()) {
			preferredNeighborScheduler.shutdownNow();
		}
		if (optimisticScheduler != null && !optimisticScheduler.isShutdown()) {
			optimisticScheduler.shutdownNow();
		}

		clientManager.shutdown();
		server.shutdown();
	}

	public void startSchedulers(int preferredInterval, int optimisticInterval) {
		preferredNeighborScheduler.scheduleAtFixedRate(
				selectPreferedNeighbours(),
				0,
				preferredInterval,
				java.util.concurrent.TimeUnit.SECONDS);

		optimisticScheduler.scheduleAtFixedRate(
				selectOptimisticNeighbor(),
				0,
				optimisticInterval,
				java.util.concurrent.TimeUnit.SECONDS);
	}
}
