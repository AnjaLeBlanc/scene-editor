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

package rtspd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Represents an RTSP Setup Request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPSetupRequest extends RTSPRequest {
    
    // The default id number
    private static final int INITIAL_ID = 1000;

    // The message sent when the session is not valid
    private static final String INVALID_SESSION_MESSAGE = 
        "Session ID 0 not valid";

    // The message sent when the transport failed to parse
    private static final String TRANSPORT_FAILED_MESSAGE = 
        "Transport would not parse";

    // The log file
    private static Log logger = 
        LogFactory.getLog(RTSPSetupRequest.class.getName());

    // The server session id
    private int serverSessionId = 0;

    // The transport information
    private RTSPTransportInfo transportInfo = null;

    /**
     * @see rtspd.RTSPRequest#dispatch(rtspd.Dispatcher)
     */
    public void dispatch(Dispatcher d) throws RTSPResponse {
        if (isModePlay()) {
            d.handlePlaySetupRequest(this);
        } else if (isModeRecord()) {
            d.handleRecordSetupRequest(this);
        }
    }

    /**
     * @throws RTSPResponse
     * @see rtspd.RTSPRequest#setRequestPacket(rtspd.RTSPRequestPacket)
     */
    protected boolean setRequestPacket(RTSPRequestPacket packet)
            throws RTSPResponse {
        if (!parseTransport(packet)) {
            return false;
        }
        setPacket(packet);
        return true;
    }

    /**
     * Returns the transport information
     * @return Returns the transport information
     */
    public RTSPTransportInfo getTransportInfo() {
        return transportInfo;
    }

    /**
     * Returns the play or record session id
     * 
     * @return the id of the session
     */
    public int getServerSessionId() {
        return serverSessionId;
    }

    /**
     * Parses the transport header
     * 
     * @param packet
     *            The packet of the request
     * @return true if the header parsed
     * @throws RTSPResponse
     */
    public boolean parseTransport(RTSPRequestPacket packet) 
            throws RTSPResponse {
        transportInfo = new RTSPTransportInfo();

        if (!transportInfo.parse(packet)) {
            throw new RTSPResponse(
                    Headers.RTSP_INVALID_HEADER_FIELD,
                    TRANSPORT_FAILED_MESSAGE,  packet);
        }

        return true;
    }

    /**
     * Works out the session of the request
     * 
     * @param packet
     *            The packet to get the session from
     * @throws RTSPResponse
     */
    public void parseSession(RTSPRequestPacket packet) throws RTSPResponse {
        String session = packet.getSession();
        logger.debug("RTSP_Setup_Request::parseSession: <" + session + ">");

        if (session.length() == 0) {
            serverSessionId = INITIAL_ID;
        } else {
            int id = Integer.valueOf(session).intValue();

            if (id == 0) {
                throw new RTSPResponse(Headers.RTSP_INVALID_HEADER_FIELD,
                        INVALID_SESSION_MESSAGE, packet);
            }
            serverSessionId = id;
        }
    }

    /**
     * Returns true if the mode of the transport is PLAY
     * 
     * @return true if the mode is play
     */
    public boolean isModePlay() {
        return transportInfo.isPlay();
    }

    /**
     * Returns true if the mode of the transport is RECORD
     * 
     * @return true if the mode is record
     */
    public boolean isModeRecord() {
        return transportInfo.isRecord();
    }
}