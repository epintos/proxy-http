package ar.edu.it.itba.pdc.v2.implementations.proxy;import java.io.IOException;import java.net.InetAddress;import java.net.ServerSocket;import java.net.Socket;import java.util.concurrent.ExecutorService;import java.util.concurrent.Executors;import org.apache.log4j.Level;import org.apache.log4j.Logger;import ar.edu.it.itba.pdc.v2.implementations.configurator.ConfiguratorImpl;import ar.edu.it.itba.pdc.v2.implementations.monitor.Monitor;import ar.edu.it.itba.pdc.v2.interfaces.Configurator;import ar.edu.it.itba.pdc.v2.interfaces.ConnectionHandler;import ar.edu.it.itba.pdc.v2.interfaces.ConnectionManager;public class ThreadedSocketServer implements Runnable {	private ServerSocket serverSocket;	private ConnectionHandler handler;	private ConnectionManager connectionManager;	private Configurator configurator;	public ThreadedSocketServer(final int port, final InetAddress interfaz,			final ConnectionHandler handler, Configurator configurator)			throws IOException {		init(new ServerSocket(port, 50, interfaz), handler, configurator);	}	private void init(final ServerSocket s, final ConnectionHandler handler,			final Configurator configurator) {		if (s == null || handler == null || configurator == null) {			throw new IllegalArgumentException();		}		this.serverSocket = s;		this.handler = handler;		this.connectionManager = new ConnectionManagerImpl();		this.configurator = configurator;	}	public void run() {		Logger server = Logger.getLogger("proxy.sever");		server.setLevel(Level.INFO);		server.info("Proxy listenting on port 9090");		ExecutorService es = Executors.newFixedThreadPool(30);		while (true) {			Socket socket;			try {				socket = this.serverSocket.accept();				server.info("Connection accepted from " + socket.getInetAddress());				es.execute(new Attend(socket, handler, connectionManager,						new AnalyzerImp(connectionManager, configurator),						configurator));			} catch (IOException e) {				// TODO Auto-generated catch block				e.printStackTrace();			}		}	}}
