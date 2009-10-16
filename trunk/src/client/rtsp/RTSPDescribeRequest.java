/*
 * @(#)RTSPDescribeRequest.java
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

import java.util.Vector;

import common.Headers;
import common.RTSPResponseException;
import common.SDPParser;

/**
 * A request to describe a session or stream
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPDescribeRequest extends RTSPRequest {

    // Milliseconds per second
    private static final int MS_PER_SEC = 1000;

    // The RTSP Method of the request
    private static final String METHOD = "DESCRIBE";

    // The parser of the request
    private SDPParser parser = new SDPParser();

    // The id of the session
    private String serverSessionId = null;

    /**
     * Create a new describe request
     * 
     * @param url
     *            The url of the request
     */
    public RTSPDescribeRequest(String url) {
        super(url, METHOD);
    }

    /**
     * Handles the response to a describe request
     * 
     * @see client.rtsp.RTSPRequest#handleResponse(
     *     client.rtsp.RTSPResponsePacket)
     */
    public void handleResponse(RTSPResponsePacket packet)
            throws RTSPResponseException {

        // If the code is not OK, fail
        if (packet.getCode() != Headers.RTSP_OK) {
            throw new RTSPResponseException(packet.getReason());
        }
        parser.parse(packet.getData());
        serverSessionId = packet.getSession();
    }

    /**
     * Retrieves the streams if this is a session
     * 
     * @return A vector of String URLs
     */
    public Vector<String> getStreams() {
        return parser.getStreams();
    }

    /**
     * Retrieves a Stream item that represents this description
     * 
     * @return The described Stream
     */
    public Stream getStream() {
        Stream stream = new Stream(getName(), getDescription(), getType(),
                getSSRC(), getDurationInSecs());
        return stream;
    }

    /**
     * Retrieves the name of this session
     * 
     * @return The name of the session
     */
    public String getName() {
        return parser.getName();
    }

    /**
     * Retrieves the description of the session
     * 
     * @return The session description
     */
    public String getDescription() {
        return parser.getDescription();
    }

    /**
     * Retrieves the MIME Type of the session
     * 
     * @return The MIME Type of the session
     */
    public String getType() {
        return parser.getType();
    }

    /**
     * Retrieves the SSRC of the stream
     * 
     * @return The SSRC of the stream
     */
    public String getSSRC() {
        return parser.getSSRC();
    }

    /**
     * Returns the owner of the session
     * @return the owner username
     */
    public String getOwner() {
        return parser.getOwner();
    }

    /**
     * Returns the duration of the session or stream
     * @return the duration of the session or stream in seconds
     */
    public double getDurationInSecs() {
        double startTime = Double.valueOf(parser.getStartTime()).doubleValue();
        double endTime = Double.valueOf(parser.getEndTime()).doubleValue();
        return endTime - startTime;
    }

    /**
     * Returns the start time in ms
     * @return the start time in ms since the epoch
     */
    public long getStartTime() {
        return Double.valueOf(parser.getStartTime()).longValue() * MS_PER_SEC;
    }

    /**
     * Returns the end time in ms
     * @return the end time in ms since the epoch
     */
    public long getEndTime() {
        return Double.valueOf(parser.getEndTime()).longValue() * MS_PER_SEC;
    }

    /**
     * Returns the server session id
     * @return the session id of the session on the server
     */
    public String getServerSessionId() {
        return serverSessionId;
    }
}