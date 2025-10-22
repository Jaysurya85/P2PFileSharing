
# Peer-to-Peer File Sharing System

## Commands to Build and Run the Code

### Without Make

`cd src
javac -d ../build *.java cd ../build
java PeerProcess 1001` 

### With Make

`make build
make run PEER=1001` 

> **Note:**  
> There is currently no script for running multiple peers automatically.  
> Each peer must be started manually by executing the above command with the corresponding peer ID.

----------

## Configuration Files

### Sample `PeerInfo.cfg`

```
1001 localhost  6001 0 
1002 localhost  6002 1  
1003 localhost  6003 0
```
    

----------

### Sample `Common.cfg`

```
NumberOfPreferredNeighbors  3  
UnchokingInterval  5
OptimisticUnchokingInterval  10
FileName  thefile  
FileSize  2167705  
PieceSize  16384
```

----------

## Environment

All testing has been performed in a **local environment** using `localhost` as the hostname.
