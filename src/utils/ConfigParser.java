package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.util.*;
import java.util.Properties;
import models.*;

public class ConfigParser {

	public Common getCommonConfig() {
		String configFilePath = "../Common.cfg";
		Properties props = new Properties();
		try {
			FileInputStream fs = new FileInputStream(configFilePath);
			props.load(fs);
			int k = Integer.parseInt(props.getProperty("NumberOfPreferredNeighbors"));
			int m = Integer.parseInt(props.getProperty("UnchokingInterval"));
			int n = Integer.parseInt(props.getProperty("OptimisticUnchokingInterval"));
			String fileName = props.getProperty("FileName");
			long fileSize = Long.parseLong(props.getProperty("FileSize"));
			int pieceSize = Integer.parseInt(props.getProperty("PieceSize"));

			System.out.println("k is " + k);
			System.out.println("m = is " + m);
			System.out.println("n = is " + n);
			System.out.println("fileName is " + fileName);
			System.out.println("fileSize is " + fileSize);
			System.out.println("pieceSize is " + pieceSize);
			fs.close();
			return new Common(k, n, n, fileName, fileSize, pieceSize);
		} catch (Exception ex) {
			System.out.println("Error is " + ex.toString());
		}
		return new Common();
	}

	public List<Peer> getPeerConfig() {
		String configFilePath = "../PeerInfo.cfg";
		List<Peer> peers = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFilePath));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String[] lineParts = line.split("\\s+");

				int peerId = Integer.parseInt(lineParts[0]);
				String hostName = lineParts[1];
				int portNo = Integer.parseInt(lineParts[2]);
				boolean flag = Integer.parseInt(lineParts[3]) == 1;
				peers.add(new Peer(peerId, hostName, portNo, flag));
			}
			// for (Peer peer : peers) {
			// System.out.println(peer);
			// }
			br.close();
		} catch (Exception ex) {
			System.out.println("Error is " + ex.toString());
		}
		return peers;
	}

}
