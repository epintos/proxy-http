package ar.edu.it.itba.pdc.v2.implementations.configurator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.ws.rs.core.MediaType;

import ar.edu.it.itba.pdc.v2.interfaces.ConfiguratorConnectionDecoderInt;

public class ConfiguratorConnectionDecoder implements ConfiguratorConnectionDecoderInt {

	private boolean logged = false;
	private boolean closeConnection = false;
	private boolean applyTransformations = false;
	private boolean applyRotations = false;
	private Map<String, String> reply;
	private Set<InetAddress> blockedAddresses;
	private Set<MediaType> blockedMediaType;
	private Set<Pattern> blockedURIs;
	private int maxSize = -1;

	public ConfiguratorConnectionDecoder() {
		reply = new HashMap<String, String>();
		fillReply();
		blockedAddresses = new TreeSet<InetAddress>();
		blockedMediaType = new TreeSet<MediaType>();
		blockedURIs = new TreeSet<Pattern>();
	}

	public boolean closeConnection() {
		return closeConnection;
	}

	public String decode(String s) {
		if (!logged) {
			String[] credentials = s.split(":");
			if (credentials.length == 2 && credentials[0].equals("admin")
					&& credentials[1].equals("pdc2012")) {
				logged = true;
				return reply.get("LOG_IN_OK");
			} else {
				closeConnection = true;
				return reply.get("LOGIN_ERROR");
			}
		} else {
			String[] args = s.split(" ");
			if (args[0].equals("BLOCK")) {
				return analyzeBlockCommand(args);
			} else if (args[0].equals("UNBLOCK")) {
				return analyzeUnblockCommand(args);
			} else if (args[0].equals("TRANSFORM")) {
				if (args[1].equals("ON")) {
					applyTransformations = true;
					return reply.get("TRANSF_ON");
				} else if (args[1].equals("OFF")) {
					applyTransformations = false;
					return reply.get("TRANSF_OFF");
				} else {
					return reply.get("WRONG_PARAMETERS");
				}
			} else if (args[0].equals("ROTATIONS")) {
				if (args[1].equals("ON")) {
					applyRotations = true;
					return reply.get("ROT_ON");
				} else if (args[1].equals("OFF")) {
					applyRotations = false;
					return reply.get("ROT_OFF");
				} else {
					return reply.get("WRONG_PARAMETERS");
				}
			} else if (args[0].equals("GET") && args[1].equals("CONF")) {
				if (args[2].equals("ROTATIONS")) {
					if (applyRotations) {
						return reply.get("ROT_ON");
					} else {
						return reply.get("ROT_OFF");
					}
				} else if (args[2].equals("TRANSFORMATIONS")) {
					if (applyTransformations) {
						return reply.get("TRANSF_ON");
					} else {
						return reply.get("TRANSF_OFF");
					}
				} else if(args[2].equals("BLOCK")) {
					StringBuffer sb = new StringBuffer();
					sb.append("200 - Blocked list:\n");
					sb.append("Media Types:\n");
					for(MediaType mt: blockedMediaType) {
						sb.append("\t" + mt.toString()+"\n");
					}
					sb.append("URIs:\n");
					for(Pattern p: blockedURIs) {
						sb.append("\t" + p.toString() + "\n");
					}
					sb.append("IP addresses:\n");
					for(InetAddress addr: blockedAddresses) {
						sb.append("\t" + addr.toString() + "\n");
					}
					return sb.toString();
				} else {
					return reply.get("WRONG_PARAMETERS");
				}
			} else {
				return reply.get("WRONG_COMMAND");
			}
		}
	}

	private String analyzeBlockCommand(String[] line) {
		String type = line[1];
		String arg = line[2];
		if (type.equals("IP")) {
			InetAddress addr;
			try {
				addr = InetAddress.getByAddress(arg.getBytes());
				blockedAddresses.add(addr);
				return "200 - " + arg + " blocked\n";
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (type.equals("MTYPE")) {
			MediaType mt = analyzeMediaType(arg);
			if (mt == null) {
				return "400 - Invalid media type\n";
			}
			blockedMediaType.add(mt);
		} else if (type.equals("SIZE")) {
			try {
				Integer max = Integer.parseInt(arg);
				maxSize = max;
				return "200 - Sizes bigger than " + maxSize
						+ " are now blocked\n";
			} catch (NumberFormatException e) {
				return "400 - Invalid size\n";
			}
		} else if (type.equals("URI")) {
			try {
				Pattern p = Pattern.compile(arg);
				blockedURIs.add(p);
			} catch (PatternSyntaxException e) {
				return "400 - Invalid pattern\n";
			}
		} else {
			return reply.get("WRONG_PARAMETERS");
		}
		return null;
	}

	private String analyzeUnblockCommand(String[] line) {
		String type = line[1];
		String arg = line[2];
		if (type.equals("IP")) {
			InetAddress addr;
			try {
				addr = InetAddress.getByAddress(arg.getBytes());
				blockedAddresses.remove(addr);
				return "200 - " + arg + " blocked\n";
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (type.equals("MTYPE")) {
			MediaType mt = analyzeMediaType(arg);
			if (mt == null) {
				return "400 - Invalid media type\n";
			}
			blockedMediaType.remove(mt);
		} else if (type.equals("SIZE")) {
			try {
				Integer max = Integer.parseInt(arg);
				maxSize = max;
				if(max == -1) {
					return "200 - All sizes are permited\n";
				}
				return "200 - Sizes bigger than " + maxSize
						+ " are now blocked\n";
			} catch (NumberFormatException e) {
				return "400 - Invalid size\n";
			}
		} else if (type.equals("URI")) {
			try {
				Pattern p = Pattern.compile(arg);
				blockedURIs.remove(p);
			} catch (PatternSyntaxException e) {
				return "400 - Invalid pattern\n";
			}
		} else {
			return reply.get("WRONG_PARAMETERS");
		}
		return null;
	}

	private MediaType analyzeMediaType(String mtype) {
		MediaType media;
		try {
			media = MediaType.valueOf(mtype);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return media;
	}

	private void fillReply() {
		reply.put("LOG_IN_OK", "200 - Welcome\n");
		reply.put("LOGIN_ERROR", "400 - Wrong username and/or password\n");
		reply.put("WRONG_COMMAND", "401 - Wrong command\n");
		reply.put("WRONG_PARAMETERS", "401 - Wrong parameters\n");
		reply.put("TRANSF_ON", "200 - Transformations are on\n");
		reply.put("TRANSF_OFF", "200 - Transformations are off\n");
		reply.put("ROT_ON", "200 - Rotations are on\n");
		reply.put("ROT_OFF", "200 - Rotations are off\n");
	}
	
	public Set<InetAddress> getBlockedAddresses() {
		return blockedAddresses;
	}
	
	public Set<MediaType> getBlockedMediaType() {
		return blockedMediaType;
	}
	
	public Set<Pattern> getBlockedURIs() {
		return blockedURIs;
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	public boolean applyRotations() {
		return applyRotations;
	}
	
	public boolean applyTransformations() {
		return applyTransformations;
	}
	

}
