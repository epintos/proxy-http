package ar.edu.it.itba.pdc.Implementations.proxy.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import ar.edu.it.itba.pdc.Interfaces.HTTPHeaders;

/**
 * This class parses an array of bytes and builds up the headers according to de
 * HTTP rfc
 * */
public class HTTPPacket implements HTTPHeaders {

	private Map<String, String> headers;
	private int bodyBytes = 0;
	private boolean completeHeaders = false;
	private boolean bodyHeaders = false;
	private boolean contentExpected = true;

	public HTTPPacket() {

		headers = new HashMap<String, String>();

	}

	public void parse(byte[] data, int count) {
		String[] args = null;
		try {

			if (completeHeaders) {
				bodyBytes += count;
				return;
			}

			String s = new String(data).substring(0, count);

			if (count == -1) {
				System.out.println("-1");
			}

			String[] lines = s.split("\r\n");

			if (lines.length == 0) {
				System.out.println("No deberia pasar");
			}
			String startLine = lines[0];
			args = startLine.split(" ");
			if (args[0].equals("GET") || args[0].equals("POST")
					|| args[0].equals("HEAD")) {
				parseRequest(lines);
			} else if (args[0].contains("HTTP")) {
				parseResponse(lines);
			} else {
				// TODO: not supported
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			// System.out.println(args.length);
		}

	}

	private void parseRequest(String[] message) {

		String[] lines = message;

		String firstLine = lines[0];
		String[] args = firstLine.split(" ");
		String method = args[0];
		headers.put("Method", method);
		String requestURI = args[1];
		headers.put("RequestedURI", requestURI);
		String httpVersion = args[2];
		headers.put("HTTPVersion", httpVersion);

		parseHeaders(lines);

	}

	private void parseResponse(String[] message) {

		// HTTP-Version SP Status-Code SP Reason-Phrase CRLF
		String[] lines = message;

		String firstLine = lines[0];
		String[] args = firstLine.split(" ");
		String statusCode = args[1];
		headers.put("StatusCode", statusCode);
		String reason = args[2];
		headers.put("Reason", reason);
		String httpVersion = args[0];
		headers.put("HTTPVersion", httpVersion);

		statusCode = statusCode.replaceAll(" ", "");

		if (statusCode.matches("1..") || statusCode.equals("204")
				|| statusCode.equals("304")) {
			contentExpected = false;
		}

		parseHeaders(lines);
	}

	private void parseHeaders(String[] lines) {
		// will read until an empty line appears
		int length = lines.length;
		int i = 1;
		for (i = 1; i < length && !completeHeaders; i++) {
			if (lines[i].isEmpty()) {
				completeHeaders = true;
			} else {
				String[] headerValue = lines[i].split(":");
				headerValue[1] = headerValue[1].replaceAll(" ", "");
				if (headerValue.length < 2) {
					return;
				} else {
					for (int j = 2; j < headerValue.length; j++) {
						headerValue[1] += headerValue[j];
					}
				}
				headers.put(headerValue[0], headerValue[1]);
			}
		}

		// Body Part
		// add "\r\n" bytes deleted when splitting
		bodyBytes += (length - i) * 2;
		String buf = "";
		for (; i < length; i++) {
			buf += lines[i];
			bodyBytes += lines[i].length();
		}

	}

	@Override
	public String getHeader(String header) {
		return this.headers.get(header);
	}

	@Override
	public int getReadBytes() {
		return bodyBytes;
	}

	@Override
	public boolean contentExpected() {
		return contentExpected;
	}

	@Override
	public void dumpHeaders() {
		for (String h : headers.keySet()) {
			System.out.print(h + ": ");
			System.out.println(headers.get(h));
		}

	}

	@Override
	public String getBody(byte[] data, int count) {

		String s = new String(data).substring(0, count);
		String[] lines = s.split("\r\n");

		int length = lines.length;
		int i;
		for (i = 0; i < length && !bodyHeaders && length != 1; i++) {
			if (lines[i].isEmpty()) {
				bodyHeaders = true;
			} else {
			}
		}
		// Body Part
		String buf = "";
		boolean firstTime = true;
		for (; i < length; i++) {
			if(!firstTime) {
				buf += "\r\n";
			}
			firstTime = false;
			buf += lines[i];
		}
		

		return buf;

	}

}
