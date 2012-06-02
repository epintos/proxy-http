package ar.edu.it.itba.pdc.v2.implementations.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ar.edu.it.itba.pdc.v2.implementations.utils.ConnectionStatus;
import ar.edu.it.itba.pdc.v2.interfaces.ConnectionManager;

public class ConnectionManagerImpl implements ConnectionManager {

	private Map<InetAddress, List<ConnectionStatus>> connections;
	private Logger connectionLog = Logger
			.getLogger(ConnectionManagerImpl.class);

	public ConnectionManagerImpl() {
		connections = new HashMap<InetAddress, List<ConnectionStatus>>();
		connectionLog.setLevel(Level.INFO);
	}
	
	public synchronized Socket getConnection(String host) throws IOException {
		URL url = new URL("http://" + host);
		InetAddress addr = InetAddress.getByName(url.getHost());
		connectionLog.info("Requested connection for " + url.toString());
		List<ConnectionStatus> connectionList = connections.get(addr);
		if (connectionList == null) {
			connectionList = new LinkedList<ConnectionStatus>();
			connections.put(addr, connectionList);
		}
		for (ConnectionStatus connection : connectionList) {
			Socket s = connection.getSocket();
			if (!connection.isInUse() && !s.isClosed() && s.isConnected()) {
				connectionLog.info("Reused connection to " + url.toString());
				connection.takeConnection();
				return s;
			}
		}
		connectionLog.info("Created new connection to " + url.toString());
		int port = (url.getPort() == -1) ? 80 : url.getPort();
		Socket s = new Socket(addr, port);
		connections.get(addr).add(new ConnectionStatus(s, true));
		return s;

	}

	public synchronized void releaseConnection(Socket socket, boolean keepAlive) {
		List<ConnectionStatus> connectionList = connections.get(socket
				.getInetAddress());
		synchronized (connections) {	
			for (ConnectionStatus connection : connectionList) {
				Socket s = connection.getSocket();
				if (equals(socket, s) && keepAlive) {
					connection.releaseConnection();
					return;
				} else if (socket.equals(s)) {
					try {
						s.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	private boolean equals(Socket s1, Socket s2) {
		return s1.getLocalPort() == s2.getLocalPort()
				&& s1.getLocalSocketAddress().equals(s2.getLocalSocketAddress())
				&& s1.getPort() == s2.getPort()
				&& s1.getRemoteSocketAddress().equals(s2.getRemoteSocketAddress());
	}

	public void run() {
		while(!Thread.interrupted()) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}
