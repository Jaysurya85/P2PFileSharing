# Peer-to-Peer File Sharing System

A simplified **BitTorrent-like peer-to-peer file sharing system** implemented in **Java**.

Peers connect to each other using **TCP sockets** and collaboratively download pieces of a file until every peer obtains the complete file.

## Demo

Peers exchanging pieces of a file across the network.

![P2P Demo](demo.gif)

The system implements core BitTorrent mechanisms such as:
-   Piece-based file distribution
-   Bitfield exchange
-   Interested / Not Interested messages
-   Request / Piece transfer
-   Choking / Unchoking mechanism
-   Preferred neighbor selection
-   Optimistic unchoking
    
----------

# System Overview

Each peer acts as **both a client and a server**:
-   **Server role**: Accept incoming connections from other peers.
-   **Client role**: Initiate connections to peers that started earlier.
    
Once connected, peers exchange information about the pieces they possess and request missing pieces from neighbors. The file is split into **fixed-size pieces**, which are downloaded and assembled locally by each peer.

----------

# Project Structure

project_root/  
├── src/                 # Java source files  
├── build/               # Compiled .class files (generated after build)  
├── Makefile             # Build and run configuration  
├── Common.cfg           # Global configuration parameters  
└── PeerInfo.cfg         # Peer information configuration

----------

# Building and Running the Project

### Build Without Make
```cd src  
javac -d ../build *.java  
  
cd ../build  
java PeerProcess 1001
```

###  Build Using Makefile
```make build  
make run PEER=1001
```
----------

## Running Multiple Peers
Currently, peers must be started **manually**.

Example:
```
make run PEER=1001  
make run PEER=1002  
make run PEER=1003
```
Each peer reads configuration files and connects to other peers based on the order defined in `PeerInfo.cfg`.

----------

# Configuration Files
## PeerInfo.cfg

Defines peers participating in the network.
1001 localhost 6001 0  
1002 localhost 6002 1  
1003 localhost 6003 0

----------

## Common.cfg

Defines global parameters.

NumberOfPreferredNeighbors 3  
UnchokingInterval 5  
OptimisticUnchokingInterval 10  
FileName thefile  
FileSize 2167705  
PieceSize 16384

----------

# Protocol Workflow

The system follows a message-based protocol similar to BitTorrent.

- **Handshake**
Peers establish TCP connections and exchange a handshake message containing peer identifiers.
- **Bitfield Exchange**
Peers share a **bitfield** indicating which pieces they possess.
- **Interest Determination**
Peers determine whether they are interested in pieces from another peer.
Possible messages:
			- **interested**
			- **not interested**
- **Choking / Unchoking**
Peers regulate bandwidth by controlling which neighbors may request pieces.
Two mechanisms are used:
	-   **Preferred neighbors**
	-   **Optimistic unchoking**
 - **Piece Requesting**
		Interested peers send request messages for missing pieces.
 -  **Piece Transfer**
	Peers send the requested data using piece messages.
-  **File Reconstruction**
	Once all pieces are received, the peer reconstructs the complete file.

----------

# Choking / Unchoking Mechanism

Every **UnchokingInterval** seconds:

1.  The peer calculates download rates from neighbors    
2.  The top **K peers** become **preferred neighbors**
3.  Preferred neighbors are **unchoked** 
4.  Others are **choked**. 

Every **OptimisticUnchokingInterval** seconds:
-   One randomly selected peer is **optimistically unchoked**.

This ensures fairness and prevents peers from being permanently choked.

----------

# File Handling

The file is split into pieces of size `PieceSize`.

Each peer:

1.  Downloads pieces independently
2.  Stores pieces locally
3.  Updates its bitfield
4.  Broadcasts `have` messages
5.  Reconstructs the file after receiving all pieces

----------

# Environment

Testing was performed in a **local environment** using:

-   `localhost` for peer communication
-   Multiple peers running on different ports
-   Java TCP socket communication
The system can also run across **multiple machines** if hostnames and ports are configured accordingly.
