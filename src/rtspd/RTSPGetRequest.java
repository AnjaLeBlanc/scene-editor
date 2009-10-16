/*
 * @(#)RTSPGetRequest.java
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Represents an RTSP Get Request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPGetRequest extends RTSPRequest {

    // Response to send when the content type is not found
    private static final String BAD_CONTENT_TYPE_RESPONSE = 
        "Unrecognised content-type";

    // The log file
    private static Log logger = LogFactory.getLog(RTSPEngine.class.getName());

    // Variables parsed from the request
    private HashMap<String,String> variables = new HashMap<String,String>();

    /**
     * @see rtspd.RTSPRequest#dispatch(rtspd.Dispatcher)
     */
    public void dispatch(Dispatcher d) throws RTSPResponse {
        d.handleGetRequest(this);
    }

    /**
     * @see rtspd.RTSPRequest#setRequestPacket(rtspd.RTSPRequestPacket)
     */
    protected boolean setRequestPacket(RTSPRequestPacket packet)
            throws RTSPResponse {
        try {
            String encoding = Headers.RTSP_CONTENT_TYPE_URLENC;
            String data = packet.getQueryString();
            setPacket(packet);
            if (data != null) {
                parseData(encoding, data);
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Exception ", e);
            return false;
        }
        return true;
    }

    // Parses the form data
    protected void parseData(String encoding, String data) throws RTSPResponse,
            UnsupportedEncodingException {
        if (encoding.startsWith(Headers.RTSP_CONTENT_TYPE_URLENC)) {

            // Split the string
            StringTokenizer tokens = 
                new StringTokenizer(data, Headers.QUERY_VAR_SEPARATOR);
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                StringTokenizer parts = 
                    new StringTokenizer(token, Headers.QUERY_VAL_SEPARATOR);
                String var = URLDecoder.decode(parts.nextToken(), 
                        Headers.URL_ENCODING);
                String val = "";
                if (parts.hasMoreTokens()) {
                    val = URLDecoder.decode(parts.nextToken(), 
                            Headers.URL_ENCODING);
                }
                variables.put(var, val);
            }

        } else {
            throw new RTSPResponse(Headers.RTSP_BAD_MEDIA,
                    BAD_CONTENT_TYPE_RESPONSE, getRequestPacket());
        }
    }

    /**
     * Returns the value of a form variable or null if not set
     * 
     * @param variable
     *            The variable to get the value of
     * @return The value of the variable
     */
    public String getValue(String variable) {
        return (String) variables.get(variable);
    }
}