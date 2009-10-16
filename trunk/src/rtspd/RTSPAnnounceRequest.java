/*
 * @(#)RTSPAnnouceRequest.java
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
import common.RTSPResponseException;
import common.SDPParser;

/**
 * Represents an RTSP Announce Request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPAnnounceRequest extends RTSPRequest {

    // The number of ms in a second
    private static final int MS_PER_SEC = 1000;

    // The message sent when the address is missing
    private static final String ADDRESS_MISSING_MESSAGE = 
        "Address not present in request";

    // The message sent when the owner is missing
    private static final String OWNER_MISSING_MESSAGE = 
        "Owner not present in request";

    // The message sent when the name is missing
    private static final String NAME_MISSING_MESSAGE = 
        "Name not present in request";

    // The log
    private static Log logger = LogFactory.getLog(RTSPAnnounceRequest.class
            .getName());

    // An SDP Parser
    private SDPParser parser = new SDPParser();

    /**
     * @see rtspd.RTSPRequest#dispatch(rtspd.Dispatcher)
     */
    public void dispatch(Dispatcher d) throws RTSPResponse {
        d.handleAnnounceRequest(this);
    }

    /**
     * @throws RTSPResponse
     * @see rtspd.RTSPRequest#setRequestPacket(rtspd.RTSPRequestPacket)
     */
    protected boolean setRequestPacket(RTSPRequestPacket packet)
            throws RTSPResponse {
        setPacket(packet);
        try {
            parser.parse(packet.getPacketData());
            if (parser.getName().equals("")) {
                throw new RTSPResponse(Headers.RTSP_NOT_ACCEPTABLE,
                        NAME_MISSING_MESSAGE, packet);
            } else if (parser.getOwner().equals("")) {
                throw new RTSPResponse(Headers.RTSP_NOT_ACCEPTABLE,
                        OWNER_MISSING_MESSAGE, packet);
            } else if (parser.getAddress().equals("")) {
                throw new RTSPResponse(Headers.RTSP_NOT_ACCEPTABLE,
                        ADDRESS_MISSING_MESSAGE, packet);
            }
        } catch (RTSPResponseException e) {
            logger.error("Error", e);
            return false;
        }
        return true;
    }

    /**
     * Returns the name of the session
     * 
     * @return the name
     */
    public String getName() {
        return parser.getName();
    }

    /**
     * Returns the description of the session
     * 
     * @return the description
     */
    public String getDescription() {
        return parser.getDescription();
    }

    /**
     * Returns the owner of the session
     * 
     * @return the username of the owner
     */
    public String getOwner() {
        return parser.getOwner();
    }

    /**
     * Returns the address of the request
     * 
     * @return the address
     */
    public String getAddress() {
        return parser.getAddress();
    }

    /**
     * Returns the start time of the session in ms
     * 
     * @return the start time in ms since the epoch
     */
    public long getStartTime() {
        return Double.valueOf(parser.getStartTime()).longValue() * MS_PER_SEC;
    }

    /**
     * Returns the end time of the session in ms
     * 
     * @return the end time in ms since the epoch
     */
    public long getEndTime() {
        return Double.valueOf(parser.getEndTime()).longValue() * MS_PER_SEC;
    }
}