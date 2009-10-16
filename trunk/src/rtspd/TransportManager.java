/*
 * @(#)TransportManager.java
 * Created: 2005-04-21
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

package rtspd;

import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import memetic.crypto.AESCrypt;
import memetic.crypto.DESCrypt;
import memetic.crypto.RTPCrypt;

import common.Headers;

/**
 * Keeps track of which hosts are connected to which ports.
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
class TransportManager {

    // The number of parts in a range
    private static final int PARTS_PER_RANGE = 2;

    // The recording port error
    private static final String RECORDING_PORT_ERROR =
        " is currently in use for recording";

    // The port error prefix
    private static final String PORT_ERROR = "Port ";

    // The maximum port in the linux 0 - 1024 admin-only port range
    private static final int LINUX_ADMIN_MAX_PORT = 1024;

    // The default ttl if none was specified
    private static final int DEFAULT_TTL = 16;

    // The maximum RTP port number
    private static final int MAX_RTP_PORT = 65534;

    // The log file
    private static Log logger = LogFactory.getLog(TransportManager.class
            .getName());

    // A port -> host map. Each item is a HashMap of host ->
    // RecordNetworkTransport
    private HashMap<Integer, HashMap<String, RecordNetworkTransport>> recordPortHostMap =
    	new HashMap<Integer, HashMap<String, RecordNetworkTransport>>();

    // A port -> host map. Each item is a Hashmap of host ->
    // PlayNetworkTransport
    private HashMap<Integer, HashMap<String, PlaybackNetworkTransport>> playPortHostMap =
    	new HashMap<Integer, HashMap<String, PlaybackNetworkTransport>>();

    // A RecordSessionID -> Vector of NetworkEndpoints map.
    private HashMap<Integer, Vector<NetworkEndpoint>> recordSessionMap =
    	new HashMap<Integer, Vector<NetworkEndpoint>>();

    // A PlaySessionID -> Vector of NetworkEndpoints map.
    private HashMap<Integer, Vector<NetworkEndpoint>> playSessionMap =
    	new HashMap<Integer, Vector<NetworkEndpoint>>();

    // The playback manager
    private PlaybackManager playManager = null;

    /**
     * Creates a new TransportManager.
     */
    public TransportManager() {
        // Does Nothing
    }

    /**
     * Allocates ports for recording
     * @param ePoint The endpoint to record from
     * @param id The id of the record session
     * @return Details of the setup ports, or null on failure
     */
    public synchronized RecordNetworkTransport setupRecordTransport(
            NetworkEndpoint ePoint, int id) {

        RecordNetworkTransport netTrans = null;

        // If the endpoint port is not currently recording something else ...
        if ((!ePoint.isMulticastAddress()
                && !isCurrentRecPort(ePoint.getPort()))
                || ePoint.isMulticastAddress()) {

            // Create the return value
            RTPCrypt crypt = computeEncryption(ePoint);
            netTrans = new RecordNetworkTransport(ePoint, crypt);
            logger.debug("Transport_Manager::setupRecordTransport: creating "
                    + "new transport\n");

            // If the transport was successfully made, store it
            if ((netTrans != null) && (!netTrans.isErrorSignalled())) {
                storeRecHostPort(ePoint, netTrans);
                storeEndpointForRecordSession(ePoint, id);
            } else {

                // Otherwise destroy it
                logger.debug("Transport_Manager::setupRecordTransport: "
                                + "No Record_Network_Transport!!!\n");
                netTrans = null;
            }
        }

        // Return the created transport (or null if failed)
        return netTrans;
    }

    /**
     * Locates and allocates ports for recording.
     *
     * @param setupRequest
     *            The request to set up ports for
     * @param id The id of the record session
     *
     * @return Details of the set up ports or null on failure
     * @throws RTSPResponse
     */
    public synchronized RecordNetworkTransport setupRecordTransport(
            RTSPSetupRequest setupRequest, int id) throws RTSPResponse {

        // Work out where to record from
        NetworkEndpoint ePoint = computeEndpoint(setupRequest);
        RecordNetworkTransport netTrans = null;
        if (ePoint == null) {
            return null;
        }
        netTrans = setupRecordTransport(ePoint, id);
        if (netTrans == null) {
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR, PORT_ERROR
                    + ePoint.getPort() + RECORDING_PORT_ERROR,
                    setupRequest.getRequestPacket());
        }
        return netTrans;
    }

    private PlaybackNetworkTransport createPlaybackTransport(
            NetworkEndpoint ePoint, RTPCrypt crypt, int id) {

        // Try to create a new transport
        PlaybackNetworkTransport netTrans =
            new PlaybackNetworkTransport(ePoint, crypt, id);
        logger.debug("Transport_Manager::setupPlaybackTransport: creating new"
                + " transport.\n");

        // If this succeeds, store the transport
        if ((netTrans != null) && (!netTrans.isErrorSignalled())) {
            storePlayHostPort(ePoint, netTrans);
            storeEndpointForPlaySession(ePoint, id);
        } else {

            // Otherwise destroy it
            netTrans = null;
        }

        return netTrans;
    }

    /**
     * Locates and allocates ports for playing
     *
     * @param setupRequest
     *            The request to set up ports for
     * @param id The id of the play session
     *
     * @return Details of the set up ports or null on failure
     * @throws RTSPResponse
     */
    public synchronized PlaybackNetworkTransport setupPlaybackTransport(
            RTSPSetupRequest setupRequest, int id) throws RTSPResponse {

        // Work out where to play to
        PlaybackNetworkTransport netTrans = null;
        NetworkEndpoint ePoint = computeEndpoint(setupRequest);
        RTPCrypt crypt = computeEncryption(ePoint);

        // If the port is not currently being used for recording ...
        logger.debug("Checking for use in record\n");
        if ((!ePoint.isMulticastAddress()
                && !isCurrentRecPort(ePoint.getPort()))
                || ePoint.isMulticastAddress()) {
            logger.debug("Not used in record. Checking for use in play.\n");

            // If the host and port is currently being used for playing
            if (isCurrentPlayHostPort(ePoint)) {

                // If the host and port is being used for this session id,
                // reuse the transport
                if (doesEndpointBelongToSession(ePoint, id)) {
                    netTrans = getPlayTransport(ePoint);
                    logger.debug("Transport_Manager::setupPlaybackTransport: "
                                    + "reusing transport in play map "
                                    + netTrans);

                } else {

                    // Try to stop the other playback
                    netTrans = getCurrentPlayHostPortTransport(ePoint);
                    if (!playManager.teardown(netTrans.getPlaySessionId(),
                            setupRequest.getRequestPacket().getUsername())) {
                        RTSPResponse response = new RTSPResponse(
                                Headers.RTSP_SERVER_ERROR, PORT_ERROR
                                        + ePoint.getPort()
                                        + " is currently in use for playback",
                                setupRequest.getRequestPacket());
                        response.setHeader(Headers.RTSP_SESSION,
                                        String.valueOf(
                                                netTrans.getPlaySessionId()));
                        throw response;
                    }

                    // If we stopped it, create a new one
                    netTrans = createPlaybackTransport(ePoint, crypt, id);
                }
            } else {

                // If the port is not currently being used for playing create a
                // playback transport
                netTrans = createPlaybackTransport(ePoint, crypt, id);
            }
        } else {
            RTSPResponse response = new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    PORT_ERROR + ePoint.getPort()
                            + RECORDING_PORT_ERROR,
                    setupRequest.getRequestPacket());
            throw response;
        }

        // Return the set up transport (or null if failed)
        return netTrans;
    }

    /**
     * Finishes with a Recording transport.
     *
     * @param netTrans
     *            The transport to finish with
     */
    public synchronized void closeTransport(RecordNetworkTransport netTrans) {
        removeRecHostPort(netTrans.getEndpoint());
    }

    /**
     * Finishes with a Playback transport.
     * @param id The play session id
     */
    public synchronized void closeAllTransports(int id) {
        Vector<NetworkEndpoint> ePointList = playSessionMap.get(new Integer(id));
        logger.debug("Removing Play Transport for session " + id);

        if (ePointList != null) {
            for (int i = 0; i < ePointList.size(); i++) {
                NetworkEndpoint ePoint = (NetworkEndpoint) ePointList.get(i);
                if (ePoint != null) {
                    logger.debug("    Removing Play Transport");
                    removePlayHostPort(ePoint);
                    playSessionMap.remove(new Integer(id));
                }
            }
        }
    }

    // Calculates the endpoint of the request
    private NetworkEndpoint computeEndpoint(RTSPSetupRequest setupRequest) {

        // Get the client address and hostname
        String clientHostname = setupRequest.getRequestPacket().getEngine()
                .getPeerAddress();

        // Get the port
        String portParam = setupRequest.getTransportInfo().getParameter(
                Headers.RTSP_TRANSPORT_PORT);
        int port = 0;
        String host = null;

        // Get the TTL
        String ttlParam = setupRequest.getTransportInfo().getParameter(
                Headers.RTSP_TRANSPORT_TTL);
        int ttl = 0;

        // Get the encryption parameters
        String encType = setupRequest.getTransportInfo().getParameter(
                Headers.RTSP_TRANSPORT_ENC_TYPE);
        String encKey = setupRequest.getTransportInfo().getParameter(
                Headers.RTSP_TRANSPORT_ENC_KEY);

        NetworkEndpoint ePoint = null;

        // If the port was not specified, find an unused port and use this
        if (portParam == null) {
            boolean bPortInUse = true;
            logger.debug("assigning random port\n");
            while (bPortInUse) {
                port = (int) (Math.random() * MAX_RTP_PORT) + 1;
                if (port > LINUX_ADMIN_MAX_PORT) {
                    if (!isCurrentRecPort(port) && !isCurrentPlayPort(port)) {
                        bPortInUse = false;
                    }
                }
            }
        } else {

            // If the port was specified, use that port
            if (portParam.matches("\\d{1,5}-\\d{1,5}")) {
                String[] ports = portParam.split("-", PARTS_PER_RANGE);
                port = Integer.valueOf(ports[0]).intValue();
            } else if (portParam.matches("\\d{1,5}")) {
                port = Integer.valueOf(portParam).intValue();
            } else {
                return null;
            }
        }

        // Get the destination
        host = setupRequest.getTransportInfo().getParameter(
                Headers.RTSP_TRANSPORT_DESTINATION);

        // If the destiniation was not specified and this is a play request,
        // use the client host name
        if (host == null) {
            if (setupRequest.isModePlay()) {
                logger.debug("Assigning host: " + clientHostname + " port: "
                        + port);
                host = clientHostname;
            }
        }


        // If the TTL was not specified, use default
        if (ttlParam == null) {
            ttl = DEFAULT_TTL;
        } else {

            // Otherwise, use the specified value
            ttl = Integer.valueOf(ttlParam).intValue();
        }

        // Return a new Endpoint
        if (host == null) {
            ePoint = new NetworkEndpoint(port, ttl);
        } else {
            ePoint = new NetworkEndpoint(host, port, ttl);
        }
        ePoint.setEncryption(encType, encKey);
        return ePoint;
    }

    // Calculates the encryption of the stream
    private RTPCrypt computeEncryption(NetworkEndpoint ePoint) {
        RTPCrypt crypt = null;
        String encType = ePoint.getEncryptionType();
        String encKey = ePoint.getEncryptionKey();
        if ((encType != null) && (encKey != null)) {
            if (!encType.equals("")) {
                if (encKey.equals("")) {
                    return null;
                }
                logger.debug("Encryption found with type=" + encType + ", key="
                     + encKey);
                if (encType.equals(AESCrypt.TYPE)) {
                    crypt = new RTPCrypt(new AESCrypt(encKey));
                } else if (encType.equals(DESCrypt.TYPE)) {
                    crypt = new RTPCrypt(new DESCrypt(encKey));
                } else {
                    return null;
                }
            }
        }
        return crypt;
    }

    // Returns true if the given port is in use for recording
    private boolean isCurrentRecPort(int port) {
        boolean isCurrent = false;
        HashMap<String, RecordNetworkTransport> hostMap = recordPortHostMap.get(new Integer(port));

        if (hostMap != null) {
            if (hostMap.size() > 0) {
                isCurrent = true;
            }
        }

        return isCurrent;
    }

    // Returns true if the given endpoint contains a host and port that are in
    // use for playback
    private boolean isCurrentPlayHostPort(NetworkEndpoint ePoint) {
        boolean isCurrent = false;
        HashMap<String, PlaybackNetworkTransport> hostMap =
            playPortHostMap.get(new Integer(ePoint.getPort()));

        if (hostMap != null) {
            if (hostMap.get(ePoint.getHost()) != null) {
                isCurrent = true;
            }
        }
        return isCurrent;
    }

    // Returns the transport currently using the ePoint
    private PlaybackNetworkTransport getCurrentPlayHostPortTransport(
            NetworkEndpoint ePoint) {
        PlaybackNetworkTransport netTrans = null;
        HashMap<String, PlaybackNetworkTransport> hostMap =
            playPortHostMap.get(new Integer(ePoint.getPort()));

        if (hostMap != null) {
            netTrans =
                (PlaybackNetworkTransport) hostMap.get(ePoint.getHost());
        }

        return netTrans;
    }

    // Returns true if the given end point is an endpoint of the given session
    private boolean doesEndpointBelongToSession(NetworkEndpoint ePoint,
            int session) {
        Vector<NetworkEndpoint> ePointList = playSessionMap.get(new Integer(session));
        if (ePointList != null) {
            if (ePointList.contains(ePoint)) {
                return true;
            }
        }

        return false;
    }

    // Returns true if the given port is in use for playback
    private boolean isCurrentPlayPort(int port) {
        boolean isCurrent = false;
        HashMap<String, PlaybackNetworkTransport> hostMap = playPortHostMap.get(new Integer(port));

        if (hostMap != null) {
            isCurrent = true;
        }

        logger.debug("Transport_Manager::currentPlayPort: port " + port
                + " found=" + isCurrent);
        return isCurrent;
    }


    // Stores a transport for a given endpoint for recording
    private void storeRecHostPort(NetworkEndpoint ePoint,
            RecordNetworkTransport netTrans) {
        HashMap<String, RecordNetworkTransport> hostMap =
             recordPortHostMap.get(new Integer(ePoint.getPort()));

        if (hostMap == null) {
            hostMap = new HashMap<String, RecordNetworkTransport>();
        }
        hostMap.put(ePoint.getHost(), netTrans);
        recordPortHostMap.put(new Integer(ePoint.getPort()), hostMap);
    }

    // Removes the transport for the given endpoint from recording
    private void removeRecHostPort(NetworkEndpoint ePoint) {
        HashMap<String, RecordNetworkTransport> hostMap =
             recordPortHostMap.get(new Integer(ePoint.getPort()));
        if (hostMap != null) {
            RecordNetworkTransport trans = (RecordNetworkTransport) hostMap
                    .get(ePoint.getHost());
            if (trans != null) {
                trans.close();
                hostMap.remove(ePoint.getHost());
                recordPortHostMap.put(new Integer(ePoint.getPort()), hostMap);
            }
        }
    }

    // Removes the transport for the given endpoint from playback
    private void removePlayHostPort(NetworkEndpoint ePoint) {
        HashMap<String, PlaybackNetworkTransport> hostMap =
            playPortHostMap.get(new Integer(ePoint.getPort()));
        if (hostMap != null) {
            PlaybackNetworkTransport trans =
                (PlaybackNetworkTransport) hostMap.get(ePoint.getHost());
            trans.close();
            hostMap.remove(ePoint.getHost());
            playPortHostMap.put(new Integer(ePoint.getPort()), hostMap);
        }
    }

    // Stores the endpoint as belonging to the given session
    private void storeEndpointForPlaySession(NetworkEndpoint ePoint,
            int session) {
        Vector<NetworkEndpoint> ePointList = playSessionMap.get(new Integer(session));
        if (ePointList == null) {
            ePointList = new Vector<NetworkEndpoint>();
        }
        if (!ePointList.contains(ePoint)) {
            ePointList.add(ePoint);
            playSessionMap.put(new Integer(session), ePointList);
        }
    }

    //  Stores the endpoint as belonging to the given session
    private void storeEndpointForRecordSession(NetworkEndpoint ePoint,
            int session) {
        Vector<NetworkEndpoint> ePointList = recordSessionMap.get(new Integer(session));
        if (ePointList == null) {
            ePointList = new Vector<NetworkEndpoint>();
        }
        if (!ePointList.contains(ePoint)) {
            ePointList.add(ePoint);
            recordSessionMap.put(new Integer(session), ePointList);
        }
    }

    // Stores the transport for a given endpoint for playback
    private void storePlayHostPort(NetworkEndpoint ePoint,
            PlaybackNetworkTransport netTrans) {
        HashMap<String, PlaybackNetworkTransport> hostMap =
            playPortHostMap.get(new Integer(ePoint.getPort()));

        if (hostMap == null) {
            hostMap = new HashMap<String, PlaybackNetworkTransport>();
        }
        hostMap.put(ePoint.getHost(), netTrans);
        playPortHostMap.put(new Integer(ePoint.getPort()), hostMap);

        logger.debug("Transport_Manager::storePlayHostPort: storing port "
                + ePoint.getPort() + ", host " + ePoint.getHost());
    }

    // Gets a playback transport for the given endpoint if one has been created
    private PlaybackNetworkTransport getPlayTransport(NetworkEndpoint ePoint) {
        HashMap<String, PlaybackNetworkTransport> hostMap =
            playPortHostMap.get(new Integer(ePoint.getPort()));
        if (hostMap != null) {
            return ((PlaybackNetworkTransport) hostMap.get(ePoint.getHost()));
        }
        return null;
    }

    /**
     * Returns the endpoints for a record session
     * @param id The id of the record session
     * @return The endpoints of the record session
     */
    public Vector<NetworkEndpoint> getRecordEndPoints(int id) {
        return recordSessionMap.get(new Integer(id));
    }

    /**
     * Sets the PlaybackManager of the Transport Manager
     * @param playManager The PlaybackManager to set
     */
    public void setPlaybackManager(PlaybackManager playManager) {
        this.playManager = playManager;
    }
}