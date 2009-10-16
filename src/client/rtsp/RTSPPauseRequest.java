/*
 * @(#)RTSPPauseRequest.java
 * Created: 2005-04-21
 * Version: 2-0-alpha
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 */

package client.rtsp;

import common.Headers;
import common.RTSPResponseException;

/**
 * Represents a request to pause a session
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPPauseRequest extends RTSPRequest {

    // The RTSP Method of the request
    private static final String METHOD = "PAUSE";

    /**
     * Creates a new play request
     * 
     * @param url
     *            The URL of the request
     * @param sessionId
     *            The id of the session to play
     */
    public RTSPPauseRequest(String url, String sessionId) {
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
    }
}