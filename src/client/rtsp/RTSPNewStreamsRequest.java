/*
 * @(#)RTSPNewStreamsRequest.java
 * Created: 2006-11-08
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
 * Represents a request to get the new streams
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPNewStreamsRequest extends RTSPRequest {

    private static final int NEXT_NEXT_POS = 2;

    private static final int NEXT_POS = 1;

    // The RTSP Method of the request
    private static final String METHOD = "NEWSTREAMS";
    
    private Vector<Stream> streams = new Vector<Stream>();

    /**
     * Creates a new play request
     * 
     * @param url
     *            The URL of the request
     * @param sessionId
     *            The id of the session to get the new streams from
     */
    public RTSPNewStreamsRequest(String url, String sessionId) {
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

        char[] data = packet.getData();
        int startPos = 0;
        int endPos = 0;
        
        while ((startPos < data.length) && (endPos < data.length)) {
            boolean endFound = false;
            while ((startPos < data.length) 
                    && ((data[startPos] == '\r') || (data[startPos] == '\n'))) {
                startPos++;
            }
            endPos = startPos + 1;
            while ((endPos < data.length) && !endFound) {
                if (data[endPos] == '\r' || data[endPos] == '\n') {
                    if ((endPos + NEXT_POS) < data.length) {
                        if (data[endPos + NEXT_POS] == '\r') {
                            if ((endPos + NEXT_NEXT_POS) < data.length) {
                                if (data[endPos + NEXT_NEXT_POS] == '\n') {
                                    endFound = true;
                                }
                            } else {
                                endFound = true;
                            }
                        }
                    } else {
                        endFound = true;
                    }
                }
                if (!endFound) {
                    endPos++;
                }
            }
            if (endPos != (startPos + 1)) {
                SDPParser parser = new SDPParser();
                parser.parse(data, startPos, (endPos - startPos) + 1);
                String name = parser.getName();
                String desc = parser.getDescription();
                String type = parser.getType();
                String ssrc = parser.getSSRC();
                double startTime = Double.valueOf(
                        parser.getStartTime()).doubleValue();
                double endTime = Double.valueOf(
                        parser.getEndTime()).doubleValue();
                Stream stream = new Stream(name, desc, type, ssrc, 
                        endTime - startTime);
                streams.add(stream);
            }
            startPos = endPos + 1;
        }
    }
    
    /**
     * Returns the new streams
     * @return The new streams
     */
    public Vector<Stream> getStreams() {
        return streams;
    }
}