/*
 * @(#)RTSPEngine.java
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Handles the client RTSP requests
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPEngine extends Thread {

    // The log file
    private static Log logger = LogFactory.getLog(RTSPEngine.class.getName());

    // The message sent when there is a server error
    private static final String SERVER_ERROR_MESSAGE = "Internal Server Error";

    // The packet of the request
    private RTSPRequestPacket request = null;

    // The servlet request
    private HttpServletRequest servletRequest = null;

    // The servlet response
    private HttpServletResponse servletResponse = null;

    // The dispatcher to dispatch the request to
    private Dispatcher dispatcher = null;

    /**
     * Creates a new RTSPEngine
     * 
     * @param dispatcher
     *            The dispatcher to dispatch requests to
     */
    public RTSPEngine(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Services the given request
     * 
     * @param req
     *            The request
     * @param res
     *            The response
     */
    public void service(HttpServletRequest req, HttpServletResponse res) {
        this.servletRequest = req;
        this.servletResponse = res;
        request = new RTSPRequestPacket(this);
        try {
            boolean parseStatus = request.parseRequest(req);
            if (parseStatus) {
                logger.debug("Request parsed");
                handleRequest(request);
            } else {
                logger.debug("Request not parsed");
            }
        } catch (RTSPResponse resp) {
            logger.debug("Threw an exception: " + resp.getCode() + " "
                    + resp.getReason());
            resp.send();
        } catch (Exception e) {
            RTSPResponse resp = new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    SERVER_ERROR_MESSAGE, request);
            logger.error("Exception ", e);
            resp.send();
        }
    }

    /**
     * Handles the request
     * 
     * @param request
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleRequest(RTSPRequestPacket request) throws RTSPResponse {
        dispatcher.handleRequestPacket(request);
    }

    /**
     * Returns the response to the request
     * 
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return servletResponse;
    }

    /**
     * Returns the address of the client
     * 
     * @return The address of the current client
     */
    public String getPeerAddress() {
        return (servletRequest.getRemoteHost());
    }
}