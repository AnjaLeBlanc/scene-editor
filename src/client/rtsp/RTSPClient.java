/*
 * @(#)Client.java
 * Created: 03-Jun-2005
 * Version: 2-0-alpha
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package client.rtsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import common.Headers;
import common.RTSPResponseException;

/**
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPClient {

	// The text to display if the authorization is invalid
	private static final String INVALID_AUTH_ERROR = "Incorrect username or password";

	// The maximum number of authorizations to try before giving up
	private static final int MAX_AUTH_RETRIES = 3;

	// The number of parts in a media type
	private static final int PARTS_PER_TYPE = 2;

	// The status to display when recording is starting
	private static final String RECORDING_STARTING_STATUS = "Starting Recording...";

	// The error to display when the session id is missing
	private static final String ID_MISSING_ERROR = "Did not get a session Id";

	// The status to display when the session details are being set
	private static final String SETTING_SESSION_DETAILS_STATUS = "Setting Session Details...";

	// The status to set when cancelled
	private static final String CANCEL_STATUS = "Cancelled";

	// The "All" type of stream
	private static final String ALL_TYPE = "All";

	// The recording TTL
	private static final int RECORD_TTL = 127;

	// The string to put around the record mode
	private static final String MODE_SURROUND = "\"";

	// The percent complete when half done
	private static final int HALF_DONE = 50;

	// The percent complete when starting
	private static final int STARTING = 0;

	// The percent complete when complete
	private static final int COMPLETE = 100;

	// The status text when complete
	private static final String DONE_STATUS_TEXT = "Done";

	// The url path separator
	private static final String PATH_SEPARATOR = "/";

	// The url protocol
	private static final String PROTOCOL = "rtsp://";

	// The socks proxy port system property
	private static final String SOCKS_PROXY_PORT_PROPERTY = "socksProxyPort";

	// The socks proxy host system property
	private static final String SOCKS_PROXY_HOST_PROPERTY = "socksProxyHost";

	// The string to put between the server and port
	private static final String PORT_SEPARATOR = ":";

	// The connection protocol
	private static final String CONNECT_PROTOCOL = "http://";

	// The server path
	private static final String PATH = "rtsp/";

	// The proxy to use
	private static Proxy proxy = null;

	// The client socket
	private Socket socket = null;

	// The server name
	private String server = "";

	// The server port
	private int port = 0;

	// True if the connection has been cancelled
	private boolean cancelled = false;

	// A reader for reading responses
	private BufferedReader reader = null;

	// A writer for sending requests
	private PrintWriter writer = null;

	// The authenticator to get the username and password from
	private Authenticator authenticator = null;

	// The status of the request
	private StatusInformation status = null;

	// True if a http proxy is in use
	private boolean useProxy = false;

	/**
	 * Creates a new RTSP Client
	 *
	 * @param server
	 *            The server name to connect to
	 * @param port
	 *            The port to connect to on the server
	 * @param authenticator
	 *            The object to use to get authentication
	 */
	public RTSPClient(String server, int port, Authenticator authenticator) {
		this.server = server;
		this.port = port;
		this.authenticator = authenticator;
		status = new NullStatus();
	}

	// Makes the connection to the server
	private void connect() throws IOException {
		if (proxy == null) {
			String url = CONNECT_PROTOCOL + server + PORT_SEPARATOR + port;
			List<Proxy> proxies;
			try {
				proxies = ProxySelector.getDefault().select(new URI(url));
				for (int i = 0; (i < proxies.size()) && (proxy == null); i++) {
					proxy = (Proxy) proxies.get(i);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		if ((proxy == null) || (proxy == Proxy.NO_PROXY)
				|| (proxy.type() == Proxy.Type.DIRECT)) {
			socket = new Socket(server, port);
			reader = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream());
		} else if (proxy.type() == Proxy.Type.SOCKS) {
			String proxyHost = ((InetSocketAddress) proxy.address())
					.getHostName();
			int proxyPort = ((InetSocketAddress) proxy.address()).getPort();
			System.err.println("Using a SOCKS Proxy");
			System.setProperty(SOCKS_PROXY_HOST_PROPERTY, proxyHost);
			System.setProperty(SOCKS_PROXY_PORT_PROPERTY, String
					.valueOf(proxyPort));
			socket = new Socket(server, port);
			reader = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream());
		} else if (proxy.type() == Proxy.Type.HTTP) {
			String proxyHost = ((InetSocketAddress) proxy.address())
					.getHostName();
			int proxyPort = ((InetSocketAddress) proxy.address()).getPort();
			System.err.println("Using an HTTP Proxy");
			socket = new Socket(proxyHost, proxyPort);
			reader = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream());
			useProxy = true;
		}
	}

	/**
	 * Sets the current object representing the status
	 *
	 * @param status
	 *            The status information object
	 */
	public void setStatusObject(StatusInformation status) {
		if (status != null) {
			this.status = status;
		} else {
			this.status = new NullStatus();
		}
	}

	/**
	 * Closes the connection to the server
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
		}
		if (reader != null) {
			reader.close();
		}
		if (socket != null) {
			socket.close();
		}
	}

	/**
	 * Gets a list of sessions available
	 *
	 * @return A vector of Session objects
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Vector<Session> getSessionList() throws IOException {
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH;
		RTSPGetRequest request = new RTSPGetRequest(url);
		Vector<Session> sessions = new Vector<Session>();
		HashMap<String, String> sessionList = null;
		Iterator<String> iterator = null;
		int noDone = 1;

		status.setStatusText("Requesting a list of sessions...");
		status.setPercentComplete(STARTING);
		sendRequest(request);

		sessionList = request.getSessionList();
		iterator = sessionList.keySet().iterator();
		status.setStatusText("Parsing Sessions...");
		status.setPercentComplete(COMPLETE / (sessions.size() + 1));
		while (iterator.hasNext()) {
			String id = ((String) iterator.next());
			Session session = new Session(id);
			session.setName((String) sessionList.get(id));
			sessions.add(session);
			noDone++;
			status.setPercentComplete((COMPLETE * noDone)
					/ (sessions.size() + 1));
		}
		Collections.sort(sessions, new SessionComparator());
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return sessions;
	}

	/**
	 * Renames a session
	 *
	 * @param session
	 *            The session to rename
	 * @param name
	 *            The new name
	 * @param description
	 *            The new description
	 * @param owner
	 *            The new owner
	 * @param startTime
	 *            The start time of the session
	 * @param endTime
	 *            The end time of the session
	 * @return The session after it has been renamed
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Session renameSession(Session session, String name,
			String description, String owner, long startTime, long endTime)
			throws IOException {

		// Ask the server to rename the session
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId() + PATH_SEPARATOR;
		RTSPAnnounceRequest request = new RTSPAnnounceRequest(url, owner, name,
				description, InetAddress.getLocalHost().getHostAddress(),
				startTime, endTime);

		status.setStatusText("Renaming Session...");
		status.setPercentComplete(STARTING);
		sendRequest(request);

		status.setStatusText(SETTING_SESSION_DETAILS_STATUS);
		status.setPercentComplete(HALF_DONE);
		session.setName(name);
		session.setDescription(description);
		if (!session.isRecorded()) {
			session.setStartTime(startTime);
			session.setEndTime(endTime);
			if (startTime != 0) {
				session.setTimerRecording(true);
			} else {
				session.setTimerRecording(false);
			}
		}
		session.setOwner(owner);

		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	/**
	 * Creates a new session
	 *
	 * @param name
	 *            The session name
	 * @param description
	 *            The session description
	 * @param owner
	 *            The owner of the session
	 * @param startTime
	 *            The time at which the session starts
	 * @param endTime
	 *            The time at which the session ends
	 *
	 * @return The new session
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Session createSession(String name, String description, String owner,
			long startTime, long endTime) throws IOException {

		// Ask the server to create the session
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH;
		RTSPAnnounceRequest request = new RTSPAnnounceRequest(url, owner, name,
				description, InetAddress.getLocalHost().getHostAddress(),
				startTime, endTime);
		String sessionId = null;
		Session session = null;

		status.setStatusText("Creating Session...");
		status.setPercentComplete(STARTING);
		sendRequest(request);

		status.setStatusText(SETTING_SESSION_DETAILS_STATUS);
		status.setPercentComplete(HALF_DONE);
		sessionId = request.getSessionId();
		if (!sessionId.equals("")) {
			session = new Session(sessionId);
			session.setName(name);
			session.setDescription(description);
			session.setStartTime(startTime);
			session.setEndTime(endTime);
			if (startTime != 0) {
				session.setTimerRecording(true);
			} else {
				session.setTimerRecording(false);
			}
			session.setOwner(owner);
		}

		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	/**
	 * Deletes the given session
	 *
	 * @param session
	 *            The session to delete
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public void deleteSession(Session session) throws IOException {

		// Ask the server to delete the session
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		RTSPDeleteRequest request = new RTSPDeleteRequest(url);
		status.setStatusText("Deleting Session...");
		status.setPercentComplete(STARTING);
		sendRequest(request);
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
	}

	/**
	 * Deletes the given stream
	 *
	 * @param session
	 *            The session to delete from
	 * @param stream
	 *            The stream to delete
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public void deleteStream(Session session, Stream stream)
			throws IOException {

		// Ask the server to delete the stream
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId() + PATH_SEPARATOR + stream.getSSRC();
		status.setStatusText("Deleting Stream...");
		status.setPercentComplete(STARTING);
		RTSPDeleteRequest request = new RTSPDeleteRequest(url);
		sendRequest(request);
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
	}

	/**
	 * Retrieves the details of a given session.
	 *
	 * @param session
	 *            The session to be described
	 * @return The session after it has been described
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Session describeSession(Session session) throws IOException {

		// Ask the server to describe the current session
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		RTSPDescribeRequest request = new RTSPDescribeRequest(url);

		status.setStatusText("Describing Session...");
		status.setPercentComplete(STARTING);
		sendRequest(request);

		// Set the session details
		session.setName(request.getName());
		session.setDescription(request.getDescription());
		session.setOwner(request.getOwner());
		session.setDurationInSecs(request.getDurationInSecs());
		session.setStartTime(request.getStartTime());
		session.setEndTime(request.getEndTime());
		session.setTimerRecording(request.getStartTime() > 0);
		session.setServerSessionId(request.getServerSessionId());

		// If the session has not already got any streams or has not been
		// recorded
		if ((session.getStreams() == null) || !session.isRecorded()) {

			// Get the list of streams in the session
			Vector<String> streamList = request.getStreams();
			Vector<Stream> newStreamList = new Vector<Stream>();
			status.setStatusText("Describing Streams...");
			status.setPercentComplete(COMPLETE / (streamList.size() + 1));

			// Go through the streams and ask for a description of each of
			// them
			for (int i = 0; i < streamList.size(); i++) {
				request = new RTSPDescribeRequest(streamList.get(i));
				sendRequest(request);
				newStreamList.add(request.getStream());
				status.setPercentComplete((COMPLETE * ((i + 1) + 1))
						/ (streamList.size() + 1));
			}

			// Set the stream list of the session to this list
			// streamList = newStreamList;
			if (streamList.size() > 0) {
				// session.setStreams(streamList);
				session.setStreams(newStreamList);
			} else {
				session.setStreams(null);
			}

			// Set the recorded status of the session
			// session.setRecorded(streamList.size() > 0);
			session.setRecorded(newStreamList.size() > 0);
		}

		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	/**
	 * Starts the recording of the session
	 *
	 * @param session
	 *            The session to start the recording of
	 * @param addresses
	 *            A vector of addresses to record from
	 * @param ports
	 *            A vector of ports to record from (should be of equal length to
	 *            addresses)
	 * @param encTypes
	 *            The encryption type used for each address (null where none)
	 * @param encKeys
	 *            The encryption key used for each address (null where none)
	 * @return The session
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Session recordSession(Session session, Vector<InetAddress> addresses,
			Vector<Integer> ports, Vector<String> encTypes, Vector<String> encKeys) throws IOException {

		// Calculate the url
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		int lastSessionSetup = -1;
		RTSPRecordRequest request = null;

		status.setStatusText("Setting Up Recording...");
		status.setPercentComplete(STARTING);
		String serverSessionId = null;

		try {

			// Go through the addresses, asking that each be recorded
			for (int i = 0; i < addresses.size(); i++) {
				InetAddress address = (InetAddress) addresses.get(i);
				int recPort = ((Integer) ports.get(i)).intValue();
				String encType = (String) encTypes.get(i);
				String encKey = (String) encKeys.get(i);
				RTSPSetupRequest setupRequest = new RTSPSetupRequest(url
						+ PATH_SEPARATOR + Headers.STREAM_PREFIX + i,
						MODE_SURROUND + Headers.RTSP_TRANSPORT_MODE_RECORD
								+ MODE_SURROUND, address, recPort, RECORD_TTL,
						encType, encKey, serverSessionId, null);
				sendRequest(setupRequest);

				// Get the request session ID, fail on error
				serverSessionId = setupRequest.getSessionId();
				if (serverSessionId == null) {
					throw new RTSPResponseException(ID_MISSING_ERROR);
				}

				// Remember where we got to in case we get an error
				lastSessionSetup = i;
				status.setPercentComplete((COMPLETE * (i + 1))
						/ (addresses.size() + 1));
			}

			// Start the recording of the streams
			status.setStatusText(RECORDING_STARTING_STATUS);
			request = new RTSPRecordRequest(url, serverSessionId);
			sendRequest(request);

			// Store the server session id
			session.setServerSessionId(serverSessionId);
		} catch (RTSPResponseException e) {

			// Catch RTSP Errors

			// If the request was cancelled, reconnect to the server
			if (cancelled) {
				connect();
			}

			// If this happens, we need to remove all our set-up streams
			for (int i = 0; i <= lastSessionSetup; i++) {
				try {
					RTSPTeardownRequest tearRequest = new RTSPTeardownRequest(
							url + PATH_SEPARATOR + Headers.STREAM_PREFIX + i,
							serverSessionId);
					sendRequest(tearRequest);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			if (cancelled) {
				close();
			}

			// Re-throw the exception
			throw e;
		}

		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	/**
	 * Restarts the recording of the given stream
	 *
	 * @param session
	 *            The session containing the stream
	 * @param stream
	 *            The stream to record
	 * @throws RTSPResponseException
	 * @throws IOException
	 */
	public void recordStream(Session session, Stream stream)
			throws IOException {

		// Work out the URL
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId() + PATH_SEPARATOR + stream.getSSRC();
		String serverSessionId = session.getServerSessionId();
		status.setStatusText(RECORDING_STARTING_STATUS);
		status.setPercentComplete(STARTING);

		// Only record the stream if the session is already recording
		if (serverSessionId != null) {
			RTSPRecordRequest recordRequest = new RTSPRecordRequest(url,
					serverSessionId);
			sendRequest(recordRequest);
		}
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
	}

	// Tries to match a stream type with an address/port
	private int getAddressForStream(String type, Vector<String> addressForType) {

		// Try to get an address specifically for this type
		int index = addressForType.indexOf(type);

		// If this fails...
		if (index == -1) {

			// ... try to get an address for the super-type (audio, video or
			// data)
			String[] parts = type.split(PATH_SEPARATOR);
			if (parts.length < PARTS_PER_TYPE) {
				return -1;
			}
			index = addressForType.indexOf(parts[0]);

			// If this fails, get the address for all streams (or -1 if none
			// exists)
			if (index == -1) {
				return addressForType.indexOf(ALL_TYPE);
			}
		}
		return index;
	}

	/**
	 * Starts the playback of a session
	 *
	 * The vector "addressForType" determines which types should be played to
	 * which address. Each stream has a type of the form "type/encoding" e.g.
	 * video/h.261. The "addressForType" vector is the same size as the address
	 * vector. Each entry is a mapping of the corresponding address number and a
	 * type specifier. In order to determine which stream will be sent to which
	 * address, the following algorithm is applied: 1) If the "type/encoding"
	 * appears in the addressForType vector, playback the stream to the address
	 * with the same index 2) If the "type" appears in the addressForType
	 * vector, playback the stream to the address with the same index 3) If
	 * "All" appears in the addressForType vector, playback the stream to the
	 * address with the same index 4) If 1-4 fail, don't playback the stream
	 *
	 * @param session
	 *            The session to start the playback of
	 * @param addresses
	 *            The addresses to playback to
	 * @param ports
	 *            The ports to playback to
	 * @param ttls
	 *            The ttls to playback to
	 * @param encTypes
	 *            The encryption types for each address (null where none)
	 * @param encKeys
	 *            The encryption keys for each address (null where none)
	 * @param addressForType
	 *            A mapping of address index to type of stream
	 * @param offset
	 *            The offset at which to start playback (0 = start)
	 * @param scale
	 *            The speed with which to start playback (1.0 = normal)
	 * @return The session
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Session playSession(Session session, Vector<InetAddress> addresses, Vector<Integer> ports,
			Vector<Integer> ttls, Vector<String> encTypes, Vector<String> encKeys,
			Vector<String> addressForType, long offset, double scale)
			throws IOException {

		// Set the URL of the request
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		String serverSessionId = null;
		RTSPPlayRequest playRequest = null;
		status.setStatusText("Setting Up Playback...");
		status.setPercentComplete(STARTING);
		try {
			Vector<Stream> streamList = null;
			HashMap<Integer, Vector<String>> streamAddressMap =
				new HashMap<Integer, Vector<String>>();
			Iterator<Integer> iterator = null;
			int setupCount = 0;
			int noSetup = 0;

			// If the streams have not been loaded, load them now
			if (session.getStreams() == null) {
				session = describeSession(session);
			}

			// Go through each stream and determine where it should be played
			streamList = session.getStreams();
			for (int i = 0; i < streamList.size(); i++) {
				Stream stream = (Stream) streamList.get(i);

				if (stream.isSelected()) {
					String type = stream.getType();

					// Work out which address this stream is to be sent to
					int index = getAddressForStream(type, addressForType);
					if (index != -1) {
						setupCount++;
						Vector<String> streams = streamAddressMap
								.get(new Integer(index));
						if (streams == null) {
							streams = new Vector<String>();
						}
						streams.add(stream.getSSRC());
						streamAddressMap.put(new Integer(index), streams);
					} else {
						stream.setSelected(false);
					}
				}
			}

			// If there are no streams, don't try to start playback (it will
			// fail)
			if (setupCount == 0) {
				throw new RTSPResponseException(
						"Sorry, there are no streams to play!");
			}

			// Go through each address and start the appropriate streams
			iterator = streamAddressMap.keySet().iterator();
			while (iterator.hasNext()) {
				Integer index = (Integer) iterator.next();
				int i = index.intValue();
				Vector<String> streams = (Vector<String>) streamAddressMap.get(index);
				InetAddress address = (InetAddress) addresses.get(i);
				int playPort = ((Integer) ports.get(i)).intValue();
				int ttl = ((Integer) ttls.get(i)).intValue();
				String encType = (String) encTypes.get(i);
				String encKey = (String) encKeys.get(i);
				RTSPSetupRequest request = new RTSPSetupRequest(url,
						MODE_SURROUND + Headers.RTSP_TRANSPORT_MODE_PLAY
								+ MODE_SURROUND, address, playPort, ttl,
						encType, encKey, serverSessionId, streams);
				sendRequest(request);

				serverSessionId = request.getSessionId();
				if (serverSessionId == null) {
					throw new RTSPResponseException(ID_MISSING_ERROR);
				}
				noSetup += 1;
				status
						.setPercentComplete((COMPLETE * (streamAddressMap
								.size() + 1))
								/ noSetup);
			}

			// Send a play request
			status.setStatusText("Playing Session...");
			playRequest = new RTSPPlayRequest(url, serverSessionId, scale,
					offset);
			sendRequest(playRequest);
			session.setServerSessionId(serverSessionId);
			status.setPercentComplete(COMPLETE);
		} catch (IOException e) {

			// Catch RTSP errors
			if (serverSessionId != null) {
				session.setServerSessionId(serverSessionId);
				stop(session);

				if (cancelled) {
					close();
				}
			}
			e.printStackTrace();
			throw e;
		}

		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	// Setup a stream for playback
	private String setupStream(Session session, InetAddress address, int port,
			int ttl, String encType, String encKey, Stream stream)
			throws  IOException {
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId() + PATH_SEPARATOR + stream.getSSRC();

		RTSPSetupRequest request = new RTSPSetupRequest(url, MODE_SURROUND
				+ Headers.RTSP_TRANSPORT_MODE_PLAY + MODE_SURROUND, address,
				port, ttl, encType, encKey, session.getServerSessionId(), null);
		sendRequest(request);
		return request.getSessionId();
	}

	/**
	 * Sets up an individual stream for playback.
	 *
	 * @param session
	 *            The session containing the stream
	 * @param addresses
	 *            The addresses to playback to
	 * @param ports
	 *            The ports to playback to
	 * @param ttls
	 *            The ttls to playback to
	 * @param encTypes
	 *            The encryption types for each address
	 * @param encKeys
	 *            The encryption keys for each address
	 * @param addressForType
	 *            A mapping of address index to type of stream
	 * @param stream
	 *            The stream to setup for playback
	 * @return The session
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Session setupStream(Session session, Vector<InetAddress> addresses, Vector<Integer> ports,
			Vector<Integer> ttls, Vector<String> encTypes, Vector<String> encKeys,
			Vector<String> addressForType, Stream stream) throws IOException {

		// Only setup the stream for a session that exists
		String serverSessionId = session.getServerSessionId();
		if (serverSessionId != null) {

			// Get the play address for the stream
			String type = stream.getType();
			int index = getAddressForStream(type, addressForType);

			// If an address was given, then set up the stream
			if (index != -1) {
				InetAddress address = (InetAddress) addresses.get(index);
				int playPort = ((Integer) ports.get(index)).intValue();
				int ttl = ((Integer) ttls.get(index)).intValue();
				String encType = (String) encTypes.get(index);
				String encKey = (String) encKeys.get(index);

				setupStream(session, address, playPort, ttl, encType, encKey,
						stream);
			} else {
				stream.setSelected(false);
			}
		}

		return session;
	}

	/**
	 * Stops the given session (playback or recording)
	 *
	 * @param session
	 *            The session to stop
	 * @return The session after it has been stopped
	 * @throws RTSPResponseException
	 * @throws IOException
	 */
	private Session stop(Session session) throws IOException {

		// Work out the URL
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		String serverSessionId = session.getServerSessionId();

		// Only stop the session if it is not already stopped
		if (serverSessionId != null) {
			RTSPTeardownRequest tearRequest = new RTSPTeardownRequest(url,
					serverSessionId);
			sendRequest(tearRequest);

			// Make sure that we can't retry this
			session.setServerSessionId(null);
		} else {
			RTSPRepairRequest repairRequest = new RTSPRepairRequest(url,
					session.getId());
			sendRequest(repairRequest);
		}
		return session;
	}

	/**
	 * Stops the recording of a session
	 *
	 * @param session
	 *            The session to stop the recording of
	 *
	 * @return The session once recording has been stopped
	 * @throws RTSPResponseException
	 * @throws IOException
	 */
	public Session stopRecording(Session session) throws IOException {
		status.setStatusText("Stopping...");
		status.setPercentComplete(STARTING);
		session = stop(session);
		session.setStreams(null);
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	/**
	 * Resumes playback of a session after a pause
	 *
	 * @param session
	 *            The session to resume the playback of
	 * @param offset
	 *            The offset at which to resume playback in ms
	 * @param scale
	 *            The speed at which to resume playback (1.0 = normal)
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public void unpausePlayback(Session session, long offset, double scale)
			throws IOException {
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		RTSPPlayRequest request = null;
		status.setStatusText("Resuming Playback...");
		status.setPercentComplete(STARTING);
		String serverSessionId = session.getServerSessionId();
		request = new RTSPPlayRequest(url, serverSessionId, scale, offset);
		sendRequest(request);
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
	}

	/**
	 * Pauses the playback of a session
	 *
	 * @param session
	 *            The session to pause
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public void pausePlayback(Session session) throws IOException {
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		RTSPPauseRequest request = null;
		status.setStatusText("Pausing Playback...");
		status.setPercentComplete(STARTING);
		String serverSessionId = session.getServerSessionId();
		request = new RTSPPauseRequest(url, serverSessionId);
		sendRequest(request);
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
	}

	/**
	 * Stops the playback of a session
	 *
	 * @param session
	 *            The session to stop
	 *
	 * @return The session after playback has stopped
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public Session stopPlayback(Session session) throws IOException {

		// Work out the URL
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		String serverSessionId = session.getServerSessionId();
		status.setStatusText("Stopping Playback...");
		status.setPercentComplete(STARTING);
		String errors = "";

		// Only stop the session if it is not already stopped
		if (serverSessionId != null) {
			Vector<Stream> streamList = null;

			// If the streams have not been loaded, load them now
			if (session.getStreams() == null) {
				session = describeSession(session);
			}

			streamList = session.getStreams();
			for (int i = 0; i < streamList.size(); i++) {
				try {
					Stream stream = (Stream) streamList.get(i);
					if (stream.isSelected()) {
						RTSPTeardownRequest tearRequest = new RTSPTeardownRequest(
								url + PATH_SEPARATOR + stream.getSSRC(),
								serverSessionId);
						sendRequest(tearRequest);
						status.setPercentComplete((COMPLETE * (i + 1))
								/ streamList.size());
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (errors.indexOf(e.getMessage()) == -1) {
						if (!errors.equals("")) {
							errors += ", ";
						}
						errors += e.getMessage();
					}
				}
			}

			// Make sure that we can't retry this
			session.setServerSessionId(null);

			// Report any errors
			if (!errors.equals("")) {
				throw new RTSPResponseException(errors);
			}
		}
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	/**
	 * Stops the playback or recording of an individual stream
	 *
	 * @param session
	 *            The session containing the stream
	 * @param stream
	 *            The stream to stop playback of
	 * @return the session
	 * @throws RTSPResponseException
	 * @throws IOException
	 */
	public Session stopStream(Session session, Stream stream)
			throws IOException {

		// Work out the URL
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId() + PATH_SEPARATOR + stream.getSSRC();
		String serverSessionId = session.getServerSessionId();
		status.setStatusText("Stopping Stream...");
		status.setPercentComplete(STARTING);

		// Only stop the stream if the session is not already stopped
		if (serverSessionId != null) {
			RTSPTeardownRequest tearRequest = new RTSPTeardownRequest(url,
					serverSessionId);
			sendRequest(tearRequest);
		}
		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return session;
	}

	/**
	 * Requests recorded streams to be forwarded to this client
	 *
	 * @param session
	 *            The session to monitor
	 *
	 * @return The address and port to listen on
	 *
	 * @throws IOException
	 * @throws RTSPResponseException
	 */
	public InetSocketAddress listenToRecording(Session session)
			throws IOException {
		InetSocketAddress address = null;

		// Work out the URL
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		String serverSessionId = session.getServerSessionId();
		status.setStatusText("Requesting Record Listening...");
		status.setPercentComplete(STARTING);

		// Only listen to an active session
		if (serverSessionId != null) {
			RTSPListenRequest listenRequest = new RTSPListenRequest(url,
					session.getServerSessionId());
			sendRequest(listenRequest);
			address = new InetSocketAddress(listenRequest.getAddress(),
					listenRequest.getPort());
		}

		status.setStatusText(DONE_STATUS_TEXT);
		status.setPercentComplete(COMPLETE);
		return address;
	}

	/**
	 * Gets the new streams since the last call; blocks until one available or
	 * the session is no longer recording
	 *
	 * @param session
	 *            The session
	 * @return A vector of streams, or null on error
	 * @throws RTSPResponseException
	 * @throws IOException
	 */
	public Vector<Stream> getNewStreams(Session session) throws IOException {
		Vector<Stream> streams = null;

		// Work out the URL
		String url = PROTOCOL + server + PORT_SEPARATOR + port + PATH_SEPARATOR
				+ PATH + session.getId();
		String serverSessionId = session.getServerSessionId();

		if (serverSessionId != null) {
			RTSPNewStreamsRequest newStreamsRequest = new RTSPNewStreamsRequest(
					url, serverSessionId);
			sendRequest(newStreamsRequest);
			streams = newStreamsRequest.getStreams();
		}
		return streams;
	}

	// Sends a request using authentication
	private void sendRequest(RTSPRequest request) throws IOException {
		String username = "";
		String password = "";
		boolean sent = false;
		int retries = 0;
		if (!authenticator.requestAuthParameters(true)) {
			throw new RTSPResponseException(
					"Did not get authentication information");
		}

		// Reconnect the socket
		try {
			close();
		} catch (IOException e) {

			// Do Nothing
		}
		connect();

		username = authenticator.getUsername();
		password = authenticator.getPassword();
		request.setAuthentication(username, password);

		while (!sent && (retries < MAX_AUTH_RETRIES)) {
			try {
				request.send(writer, useProxy, server, port);
				request.recv(reader);
				sent = true;
			} catch (RTSPAuthenticationException e) {
				if (!authenticator.requestAuthParameters(false)) {
					throw new RTSPResponseException(INVALID_AUTH_ERROR);
				}
				username = authenticator.getUsername();
				password = authenticator.getPassword();
				request.setAuthentication(username, password);

				connect();
				retries++;
			}
		}

		if (!sent) {
			throw new RTSPResponseException(INVALID_AUTH_ERROR);
		}
	}

	/**
	 * Cancels the current request
	 */
	public void cancel() {
		try {
			cancelled = true;
			status.setStatusText(CANCEL_STATUS);
			status.setPercentComplete(COMPLETE);
			close();
		} catch (IOException e) {

			// Do Nothing
		}
	}

	/**
	 * Returns true if the connection was cancelled
	 *
	 * @return true if the request was cancelled
	 */
	public boolean wasCancelled() {
		return cancelled;
	}
}
