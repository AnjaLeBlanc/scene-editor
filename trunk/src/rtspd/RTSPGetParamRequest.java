/*
 * @(#)RTSPGetParamRequest.java
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

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Represents an RTSP Get Parameter Request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPGetParamRequest extends RTSPRequest {

    // The message sent when there is an error reading parameters
    private static final String PARAMETER_ERROR_MESSAGE = 
        "Error reading parameters";

    // The log file
    private static Log logger = 
        LogFactory.getLog(RTSPGetParamRequest.class.getName());

    private Vector<String> parameters = new Vector<String>();

    /**
     * @see rtspd.RTSPRequest#dispatch(rtspd.Dispatcher)
     */
    public void dispatch(Dispatcher d) throws RTSPResponse {
        d.handleGetParamRequest(this);
    }

    /**
     * @throws RTSPResponse
     * @see rtspd.RTSPRequest#setRequestPacket(rtspd.RTSPRequestPacket)
     */
    protected boolean setRequestPacket(RTSPRequestPacket packet)
            throws RTSPResponse {
        BufferedReader reader = 
            new BufferedReader(new CharArrayReader(packet.getPacketData()));
        String line = "";
        setPacket(packet);
        if (packet.getContentType() != Headers.RTSP_CONTENT_TYPE_PARAMTERS) {
            throw new RTSPResponse(Headers.RTSP_NOT_ACCEPTABLE,
                    "Content type not recognised", packet);
        }
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.equals("")) {
                    parameters.add(line);
                }
            }
        } catch (IOException e) {
            logger.error("Error", e);
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    PARAMETER_ERROR_MESSAGE, packet);
        }
        return true;
    }

    /**
     * Returns the paramters requested
     * 
     * @return a vector of strings
     */
    public Vector<String> getParameters() {
        return parameters;
    }
}