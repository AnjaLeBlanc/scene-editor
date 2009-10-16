/*
 * @(#)RTSPPlayRequest.java
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
 * Represents a request to play RTP streams
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPPlayRequest extends RTSPRequest {

    // Milliseconds per second
    private static final double MS_PER_SEC = 1000.0;
    
    // The RTSP Method of the request
    private static final String METHOD = "PLAY";
    
    // The part of the string that is seconds
    private static final int SECOND_PART = 2;
    
    // The part of the string that is minutes
    private static final int MINUTE_PART = 1;
    
    // The part of the string that is hours
    private static final int HOUR_PART = 0;
    
    // The number of minutes in an hour
    private static final int MINUTES_PER_HOUR = 60;
    
    // The number of parts in a time string
    private static final int PARTS_PER_TIME = 3;
    
    /**
     * Creates a new play request
     * 
     * @param url
     *            The URL of the request
     * @param sessionId
     *            The id of the session to play
     * @param scale
     *            The speed at which to play the session (1.0 is normal)
     * @param offset
     *            The start position in milliseconds (0 is start)
     */
    public RTSPPlayRequest(String url, String sessionId, double scale,
            long offset) {
        super(url, METHOD);
        setHeader(Headers.RTSP_SESSION, sessionId);
        setHeader(Headers.RTSP_SCALE, String.valueOf(scale));
        setHeader(Headers.RTSP_RANGE, Headers.RTSP_NTP_TIME
                + String.valueOf(offset / MS_PER_SEC));
    }

    /**
     * Creates a new play request
     * 
     * @param url
     *            The URL of the request
     * @param sessionId
     *            The id of the session to play
     * @param scale
     *            The speed at which to play the session (1.0 = normal)
     */
    public RTSPPlayRequest(String url, String sessionId, double scale) {
        super(url, METHOD);
        setHeader(Headers.RTSP_SESSION, sessionId);
        setHeader(Headers.RTSP_SCALE, String.valueOf(scale));
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
    }
    
    /**
     * Retrieves the starting offset sent in the packet if there is one
     * 
     * @return The offset from the start that playback should commence from
     */
    public long getStartingOffset() {
        long offset = 0;
        String header =  this.getHeaderString(Headers.RTSP_RANGE);

        if (header != null) {
            String headerParts[] = header.split(Headers.RTSP_RANGE_SEPARATOR);
            String range = headerParts[0];

            if (range.startsWith(Headers.RTSP_NTP_TIME)) {
                String time = range.substring(Headers.RTSP_NTP_TIME.length());
                
                // If there are two times separated by a dash, 
                // just take the first
                if (time.matches(".*-.*")) {
                    time = time.substring(0, time.indexOf('-'));
                }
                if (time.equals(Headers.RTSP_TIME_NOW)) {
                    offset = 0;
                } else if (time.matches("\\d*(.\\d*)")) {

                    // Time is a floating point number
                    offset = (long) (Double.valueOf(time).doubleValue()
                            * MS_PER_SEC);
                } else if (time.matches("\\d*:\\d{1,2}:\\d{1,2}")) {
                    
                    // Time is a hh:mm:ss string
                    String parts[] = time.split(":", PARTS_PER_TIME);
                    int hour = Integer.valueOf(parts[HOUR_PART]).intValue();
                    int minute = Integer.valueOf(parts[MINUTE_PART]).intValue();
                    int second = Integer.valueOf(parts[SECOND_PART]).intValue();
                    offset = (((hour * MINUTES_PER_HOUR) + minute)
                            * MINUTES_PER_HOUR) + second;
                }
            }
        }

        return offset;
    }
}