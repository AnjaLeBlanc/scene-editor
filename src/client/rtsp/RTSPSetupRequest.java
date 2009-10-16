/*
 * @(#)RTSPSetupRequest.java
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

package client.rtsp;

import java.net.InetAddress;
import java.util.Vector;

import common.Headers;
import common.RTSPResponseException;

/**
 * Represents an RTSP Setup Request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPSetupRequest extends RTSPRequest {

    // The RTSP Method of the request
    private static final String METHOD = "SETUP";
    
    // The id of the request
    private String sessionId = null;

    /**
     * Creates a new setup request
     * 
     * @param url
     *            The url of the request
     * @param mode
     *            The mode (play or record) of the request
     * @param destination
     *            The destination address from / to which to record / playback
     *            the stream
     * @param port
     *            The port from / to which to record / playback the stream
     * @param ttl
     *            The ttl to set for sending the stream
     * @param encType
     *            The encryption type or null for none
     * @param encKey
     *            The encryption key or null for none
     * @param sessionId
     *            The id of the session to setup (or -1 if not yet set)
     * @param streams The streams to play back, or empty for none (or record)
     */
    public RTSPSetupRequest(String url, String mode, InetAddress destination,
            int port, int ttl, String encType, String encKey, 
            String sessionId, Vector<String> streams) {
        super(url, METHOD);

        // Setup the transport header
        String transport = "";
        transport += Headers.RTSP_TRANSPORT_PROTOCOL_RTP
                + Headers.RTSP_TRANSPORT_PROFILE_SEPARATOR
                + Headers.RTSP_TRANSPORT_PROFILE_AVP
                + Headers.RTSP_TRANSPORT_SEPARATOR;
        if (destination.isMulticastAddress()) {
            transport += Headers.RTSP_TRANSPORT_MULTICAST
                    + Headers.RTSP_TRANSPORT_SEPARATOR;
        } else {
            transport += Headers.RTSP_TRANSPORT_UNICAST
                    + Headers.RTSP_TRANSPORT_SEPARATOR;
        }
        transport += Headers.RTSP_TRANSPORT_DESTINATION
                + Headers.RTSP_TRANSPORT_VAR_VAL_SEP
                + destination.getHostAddress()
                + Headers.RTSP_TRANSPORT_SEPARATOR;
        transport += Headers.RTSP_TRANSPORT_PORT 
                + Headers.RTSP_TRANSPORT_VAR_VAL_SEP + port
                + Headers.RTSP_TRANSPORT_SEPARATOR;
        transport += Headers.RTSP_TRANSPORT_MODE
                + Headers.RTSP_TRANSPORT_VAR_VAL_SEP + mode
                + Headers.RTSP_TRANSPORT_SEPARATOR;
        transport += Headers.RTSP_TRANSPORT_TTL
                + Headers.RTSP_TRANSPORT_VAR_VAL_SEP + ttl;
        if ((encType != null) && (encKey != null)) {
            transport += Headers.RTSP_TRANSPORT_SEPARATOR;
            transport += Headers.RTSP_TRANSPORT_ENC_TYPE
                    + Headers.RTSP_TRANSPORT_VAR_VAL_SEP + encType;
            transport += Headers.RTSP_TRANSPORT_SEPARATOR;
            transport += Headers.RTSP_TRANSPORT_ENC_KEY
                    + Headers.RTSP_TRANSPORT_VAR_VAL_SEP + encKey;
        }

        // Add a sessionId if one has been assigned
        if (sessionId != null) {
            this.sessionId = sessionId;
            setHeader(Headers.RTSP_SESSION, String.valueOf(sessionId));
        }
        setHeader(Headers.RTSP_TRANSPORT, transport);
        
        // Add the streams header
        if ((streams != null) && (streams.size() > 0)) {
            String streamHeader = "";
            for (int i = 0; i < streams.size(); i++) {
                String ssrc = (String) streams.get(i);
                streamHeader += ssrc;
                if ((i + 1) < streams.size()) {
                    streamHeader += ",";
                }
            }
            setHeader(Headers.RTSP_STREAM, streamHeader);
        }
    }

    /**
     * Handles the response to a setup request
     * 
     * @see client.rtsp.RTSPRequest#handleResponse(
     *     client.rtsp.RTSPResponsePacket)
     */
    public void handleResponse(RTSPResponsePacket packet)
            throws RTSPResponseException {
        
        // Get the session id, even if there is an error
        if (sessionId == null) {
            sessionId = packet.getSession();
        }

        // If the response is not OK, fail
        if (packet.getCode() != Headers.RTSP_OK) {
            throw new RTSPResponseException(packet.getReason());
        }
    }

    /**
     * Retrieves the session id
     * 
     * @return The session Id
     */
    public String getSessionId() {
        return sessionId;
    }
}