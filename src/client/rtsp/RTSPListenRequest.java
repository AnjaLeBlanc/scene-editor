/*
 * @(#)RTSPListenRequest.java
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

import common.Headers;
import common.RTSPResponseException;

/**
 * Represents a request to listen to recording RTP streams
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPListenRequest extends RTSPRequest {

    // The RTSP Method of the request
    private static final String METHOD = "LISTEN";

    // The address to listen on
    private String address = "";

    // The port to listen on
    private int port = 0;

    /**
     * Creates a new play request
     * 
     * @param url
     *            The URL of the request
     * @param sessionId
     *            The id of the session to play
     */
    public RTSPListenRequest(String url, String sessionId) {
        super(url, METHOD);
        setHeader(Headers.RTSP_SESSION, sessionId);
    }

    /**
     * Handles a response to a play request
     * 
     * @see client.rtsp.RTSPRequest#handleResponse(
     *     client.rtsp.RTSPResponsePacket)
     */
    public void handleResponse(RTSPResponsePacket packet)
            throws RTSPResponseException {

        // If the request was not successful, return an error
        if (packet.getCode() != Headers.RTSP_OK) {
            throw new RTSPResponseException(packet.getReason());
        }

        address = packet.getRTPAddress();
        port = packet.getRTPPort();
    }

    /**
     * Returns the address to listen on
     * @return the address to listen on
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the port to listen on
     * @return the port to listen on
     */
    public int getPort() {
        return port;
    }
}