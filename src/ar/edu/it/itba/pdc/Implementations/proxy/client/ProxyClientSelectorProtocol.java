package ar.edu.it.itba.pdc.Implementations.proxy.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import ar.edu.it.itba.pdc.Implementations.proxy.TCPSelector;
import ar.edu.it.itba.pdc.Implementations.proxy.utils.AttachmentImpl;
import ar.edu.it.itba.pdc.Interfaces.Attachment;
import ar.edu.it.itba.pdc.Interfaces.ProxyWorker;
import ar.edu.it.itba.pdc.Interfaces.TCPProtocol;
import ar.edu.it.itba.pdc.v2.implementations.monitor.DataStorageImpl;
import ar.edu.it.itba.pdc.v2.implementations.utils.DecoderImpl;
import ar.edu.it.itba.pdc.v2.interfaces.Decoder;

public class ProxyClientSelectorProtocol implements TCPProtocol {

	private static int bufSize = 20 * 1024;
	public static Charset charset = Charset.forName("UTF-8");

	private Map<SocketChannel, Decoder> decoders = new HashMap<SocketChannel, Decoder>();
	private ProxyWorker worker;
	private TCPSelector caller;
	private DataStorageImpl storage = DataStorageImpl.getInstance();

	public ProxyClientSelectorProtocol() {
	}

	public void handleAccept(SelectionKey key) throws IOException {
		SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
		clntChan.configureBlocking(false); // Must be nonblocking to register
		// Register the selector with new channel for read and attach byte
		// buffer
		decoders.put(clntChan, new DecoderImpl(bufSize));
		clntChan.register(key.selector(), SelectionKey.OP_READ,
				ByteBuffer.allocate(bufSize));
	}

	public void handleRead(SelectionKey key) throws IOException {
		// Client socket channel has pending data
		SocketChannel clntChan = (SocketChannel) key.channel();
		ByteBuffer buf = ByteBuffer.allocate(bufSize);

		// Get the decoder that parsed the request
		Decoder decoder = decoders.get(((Attachment) key.attachment())
				.getFrom());

		if (decoder == null) {
			decoder = new DecoderImpl(bufSize);
		}
		long bytesRead = -1;
		try {
			bytesRead = clntChan.read(buf);
		} catch (IOException e) {
			clntChan.close();
			return;
		}
		if (bytesRead == -1) { // Did the other end close?
			clntChan.close();
		} else if (bytesRead > 0) {
			decoder.decode(buf.array(), (int) bytesRead);
			// decoder.decode(buf.array(), (int) bytesRead);
			byte[] write = buf.array();
			// HTTPHeaders headers = decoder.getHeaders();
			// TODO: here we should analyze if the request is accepted by the
			// proxy
			storage.addTotalBytes(bytesRead);
			storage.addProxyServerBytes(bytesRead);

			System.out
					.println(Calendar.getInstance().getTime().toString()
							+ "-> Response from external server to proxy. Server address: "
							+ clntChan.socket().getInetAddress());

			boolean keepReading = decoder.keepReading();
			String connection = decoder.getHeader("Connection");
			// if(connection != null && connection.contains("close")) {
			// clntChan.close();
			// return;
			// }
			worker.sendData(caller, ((Attachment) key.attachment()).getFrom(),
					write, bytesRead, keepReading);
			buf.clear();
			if (keepReading) {
				key.interestOps(SelectionKey.OP_READ);
			} else {
				decoders.put(((Attachment) key.attachment()).getFrom(),
						new DecoderImpl(bufSize));
			}
		}
	}

	public void handleWrite(SelectionKey key,
			Map<SocketChannel, Queue<ByteBuffer>> map) throws IOException {
		// TODO: peek, do not remove. In case the buffer can not be completely
		// written
		ByteBuffer buf = map.get(key.channel()).peek();
		// buf.flip(); // Prepare buffer for writing
		SocketChannel clntChan = (SocketChannel) key.channel();
		if (buf == null) {
			return;
		}
		clntChan.write(buf);
		AttachmentImpl att = (AttachmentImpl) key.attachment();

		storage.addTotalBytes(buf.toString().length());
		storage.addProxyServerBytes(buf.toString().length());

		System.out
				.println(Calendar.getInstance().getTime().toString()
						+ "-> Request from proxy server to external server. Server address: "
						+ clntChan.socket().getInetAddress());
		if (!buf.hasRemaining()) { // Buffer completely written?
			map.get(key.channel()).remove();
			if (map.get(key.channel()).isEmpty() && !att.isMultipart()) {
				// Nothing left, so no longer interested in writes
				key.interestOps(SelectionKey.OP_READ);
			} else {
				key.interestOps(SelectionKey.OP_WRITE);
				buf.clear();
			}
			buf.compact(); // Make room for more data to be read in
		} else {
			buf.compact();
			buf.clear();
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	public void setWorker(ProxyWorker worker) {
		this.worker = worker;
	}

	public void setCaller(TCPSelector caller) {
		this.caller = caller;
	}
}
