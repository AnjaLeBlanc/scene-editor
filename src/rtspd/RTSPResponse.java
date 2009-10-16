/*
 * @(#)RTSPResponse.java
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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Represents a response to be sent to an RTSP Request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPResponse extends IOException {


	private static final long serialVersionUID = 1L;

	// The log file
    private static Log logger = LogFactory.getLog(RTSPResponse.class.getName());

    // The version of RTSP of the response
    private String version = Headers.RTSP_VERSION;

    // The response
    private HttpServletResponse response = null;

    // The request that this is a reponse to
    private RTSPRequestPacket request = null;

    // The main part of the response
    private String body = "";

    // The size of the body
    private int bodyCount = 0;

    // True if the Response has been constructed
    private boolean done = false;

    // The code of the response
    private int code = 0;

    /**
     * Creates a new RTSP Response
     * 
     * @param code
     *            The code of the response
     * @param reason
     *            The reason for the response
     * @param request The request packet
     */
    public RTSPResponse(int code, String reason, RTSPRequestPacket request) {
        super(reason);
        this.response = request.getEngine().getResponse();
        this.code = code;
        this.request = request;
    }

    /**
     * Sets a header value in the response
     * 
     * @param hdr
     *            The header to set
     * @param value
     *            The value to set to the header
     */
    public void setHeader(String hdr, String value) {
        logger.debug("setHeader " + hdr + " " + value);
        response.setHeader(hdr, value);
    }

    /**
     * Initialises the body to be a certain size
     * 
     * @param size
     *            The size of the body
     */
    public void bodyInit(int size) {
        
        // Do Nothing
    }

    /**
     * Appends the string to the body
     * 
     * @param s
     *            The string to append to the body
     */
    public void bodyAppend(String s) {
        logger.debug("Append '" + s + "'");
        body += s;
        bodyCount += s.length();
    }

    /**
     * Tells the response that the body has been created.
     * @param response The response packet
     */
    public void setBodyDone(HttpServletResponse response) {
        response.setContentLength(bodyCount);
        int seq = getSequence();
        if (seq != -1) {
            response.setHeader(Headers.RTSP_CSEQ, String.valueOf(seq));
        }
        done = true;
    }

    /**
     * Sends the RTSP Response using the engine
     */
    public void send() {
        RTSPEngine engine = request.getEngine();
        HttpServletResponse response = engine.getResponse();
//        response.setStatus(getCode(), getReason());
        response.setStatus(getCode());
        if (!done) {
            setBodyDone(response);
        }

        try {
            PrintWriter out = response.getWriter();
            if (body != null) {
                out.print(body);
            }
            out.flush();
        } catch (IOException e) {
            logger.error("Exception ", e);
        }
    }

    /**
     * Returns the response code
     * 
     * @return The response code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the reason string
     * 
     * @return The reason associated with the response code
     */
    public String getReason() {
        return super.getMessage();
    }

    /**
     * Returns the RTSP version string
     * 
     * @return The version of RTSP in use
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the packet sequence number
     * 
     * @return The sequence number
     */
    public int getSequence() {
        if (request.getSequence() == null) {
            return -1;
        }
        return Integer.valueOf(request.getSequence()).intValue();
    }
}