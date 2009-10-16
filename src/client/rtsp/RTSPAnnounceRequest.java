/*
 * @(#)RTSPAnnounceRequest.java
 * Created: 2005-06-03
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

import common.Headers;
import common.RTSPResponseException;
import common.SDPParser;

/**
 * A request to create a new a session
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPAnnounceRequest extends RTSPRequest {

    // The number of ms in a second
    private static final int MS_PER_SEC = 1000;

    // The session id to send
    private static final int SDP_SESSION = 0;
    
    // The SDP version being sent
    private static final int SDP_VERSION = 0;
    
    // The RTSP Method of the request
    private static final String METHOD = "ANNOUNCE";
    
    // The session id created
    private String sessionId = "";

    /**
     * Creates a new announce request
     * 
     * @param url The url of the request
     * @param username The username of the owner
     * @param name The name of the session
     * @param description The description of the session
     * @param address The address of the session
     * @param startTime The start time of the session
     * @param endTime The end time of the session
     */
    public RTSPAnnounceRequest(String url, String username, String name,
            String description, String address, long startTime, long endTime) {
        super(url, METHOD);
        String data = "";
        setHeader(Headers.RTSP_CONTENT_TYPE, Headers.RTSP_CONTENT_TYPE_SDP);
        data += SDPParser.VERSION + SDP_VERSION + SDPParser.EOL;
        data += SDPParser.OWNER + username + SDPParser.SDP_SEPARATOR
                + SDP_SESSION + SDPParser.SDP_SEPARATOR
                + System.currentTimeMillis() + SDPParser.SDP_SEPARATOR
                + SDPParser.INTERNET + SDPParser.SDP_SEPARATOR
                + SDPParser.IPV4_ADDRESS + SDPParser.SDP_SEPARATOR
                + address + SDPParser.EOL;
        data += SDPParser.NAME + name + SDPParser.EOL;
        data += SDPParser.DESCRIPTION + description + SDPParser.EOL;
        data += SDPParser.TIME + (startTime / MS_PER_SEC) 
                + SDPParser.SDP_SEPARATOR 
                + (endTime / MS_PER_SEC) + SDPParser.EOL;
        setData(data.toCharArray());
    }

    /**
     * 
     * @see client.rtsp.RTSPRequest#handleResponse
     *         (client.rtsp.RTSPResponsePacket)
     */
    public void handleResponse(RTSPResponsePacket packet)
            throws RTSPResponseException {

        // If the code is not OK, fail
        if (packet.getCode() != Headers.RTSP_OK) {
            throw new RTSPResponseException(packet.getReason());
        }
        sessionId = packet.getSession();
    }

    /**
     * Returns the session id of the new session
     * @return the session identifier
     */
    public String getSessionId() {
        return sessionId;
    }
}