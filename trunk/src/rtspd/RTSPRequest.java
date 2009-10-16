/*
 * @(#)RTSPRequest.java
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
 * Represents a General RTSP Request
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public abstract class RTSPRequest {

    // The response sent when a method is not found
    private static final String METHOD_NOT_FOUND_RESPONSE =
        "Request method not recognised";

    // The log file
    private static Log logger = LogFactory.getLog(RTSPRequest.class.getName());

    // The packet from which this request is made.
    private RTSPRequestPacket requestPacket = null;

    // The session ID of the request
    private String sessionId = null;

    // The stream ID of the request
    private String streamId = "";

    /**
     * Creates a new RTSPRequest
     */
    public RTSPRequest() {

        // Does Nothing
    }

    /**
     * Creates a new RTSPRequest with type dependant on the method of the
     * request
     *
     * @param packet
     *            The request packet
     * @return A new RTSPRequest implementation
     * @throws RTSPResponse
     */
    public static RTSPRequest createRequest(RTSPRequestPacket packet)
            throws RTSPResponse {

        // Choose which type of request to create based on the packet type
        switch (packet.getType()) {
        case RTSPRequestPacket.DESCRIBE: {
            RTSPDescribeRequest describeRequest = new RTSPDescribeRequest();
            if (!describeRequest.setRequest(packet)) {
                logger.warn("Describe request failed!\n");
                return null;
            }
            logger.debug("Describe request succeeded\n");
            return describeRequest;
        }

        case RTSPRequestPacket.SETUP: {
            RTSPSetupRequest setupRequest = new RTSPSetupRequest();
            if (!setupRequest.setRequest(packet)) {
                logger.warn("Setup request failed!\n");
                return null;
            }
            logger.debug("Setup request succeeded\n");
            return setupRequest;
        }
        case RTSPRequestPacket.PLAY: {
            RTSPPlayRequest playRequest = new RTSPPlayRequest();
            if (!playRequest.setRequest(packet)) {
                logger.warn("Play request failed!\n");
                return null;
            }
            logger.debug("Play request succeeded\n");
            return playRequest;
        }
        case RTSPRequestPacket.GET_PARAMETER: {
            RTSPGetParamRequest getParamRequest = new RTSPGetParamRequest();
            if (!getParamRequest.setRequest(packet)) {
                logger.warn("Get Parameter request failed!\n");
                return null;
            }
            logger.debug("Get Parameter request succeeded\n");
            return getParamRequest;
        }
        case RTSPRequestPacket.TEARDOWN: {
            RTSPTeardownRequest teardownRequest = new RTSPTeardownRequest();
            if (!teardownRequest.setRequest(packet)) {
                logger.warn("Teardown request failed!\n");
                return null;
            }
            logger.debug("Teardown request succeeded\n");
            return teardownRequest;
        }
        case RTSPRequestPacket.RECORD: {
            RTSPRecordRequest recordRequest = new RTSPRecordRequest();
            if (!recordRequest.setRequest(packet)) {
                logger.warn("Record request failed!\n");
                return null;
            }
            logger.debug("Record request succeeded\n");
            return recordRequest;
        }
        case RTSPRequestPacket.OPTIONS: {
            RTSPOptionsRequest optionsRequest = new RTSPOptionsRequest();
            if (!optionsRequest.setRequest(packet)) {
                logger.warn("Options request failed!\n");
                return null;
            }
            logger.debug("Options request succeeded\n");
            return optionsRequest;
        }
        case RTSPRequestPacket.ANNOUNCE: {
            RTSPAnnounceRequest announceRequest = new RTSPAnnounceRequest();
            if (!announceRequest.setRequest(packet)) {
                logger.warn("Announce request failed!");
                return null;
            }
            logger.debug("Announce request succeeded\n");
            return announceRequest;
        }
        case RTSPRequestPacket.GET: {
            RTSPGetRequest getRequest = new RTSPGetRequest();
            if (!getRequest.setRequest(packet)) {
                logger.warn("Get request failed!");
                return null;
            }
            logger.debug("Get request succeeded\n");
            return getRequest;
        }
        case RTSPRequestPacket.DELETE: {
            RTSPDeleteRequest deleteRequest = new RTSPDeleteRequest();
            if (!deleteRequest.setRequest(packet)) {
                logger.warn("Delete request failed!");
                return null;
            }
            logger.debug("Delete request succeeded\n");
            return deleteRequest;
        }
        case RTSPRequestPacket.PAUSE: {
            RTSPPauseRequest pauseRequest = new RTSPPauseRequest();
            if (!pauseRequest.setRequest(packet)) {
                logger.warn("Pause request failed!");
                return null;
            }
            logger.debug("Pause request succeeded\n");
            return pauseRequest;
        }
        case RTSPRequestPacket.LISTEN: {
            RTSPListenRequest listenRequest = new RTSPListenRequest();
            if (!listenRequest.setRequest(packet)) {
                logger.warn("Listen request failed!");
                return null;
            }
            logger.debug("Listen request succeeded\n");
            return listenRequest;
        }
        case RTSPRequestPacket.NEWSTREAMS: {
            RTSPNewStreamsRequest newStreamsRequest =
                new RTSPNewStreamsRequest();
            if (!newStreamsRequest.setRequest(packet)) {
                logger.warn("New Streams request failed!");
                return null;
            }
            logger.debug("New Streams request succeeded\n");
            return newStreamsRequest;
        }
        case RTSPRequestPacket.REPAIR: {
            RTSPRepairRequest repairRequest =
                new RTSPRepairRequest();
            if (!repairRequest.setRequest(packet)) {
                logger.warn("Repair request failed!");
                return null;
            }
            return repairRequest;
        }
        default:
            logger.warn("Request method not recognised: " + packet.getType());
            throw new RTSPResponse(Headers.RTSP_NOT_IMPLEMENTED,
                    METHOD_NOT_FOUND_RESPONSE, packet);
        }
    }

    /**
     * Sets the request packet of the request
     *
     * @param packet
     *            The request packet to set
     * @return True if the packet was set successfully
     * @throws RTSPResponse
     */
    public boolean setRequest(RTSPRequestPacket packet) throws RTSPResponse {
        String uri = packet.getPath();
        int slashIdx = uri.indexOf(Headers.URI_PATH_SEPARATOR);
        int sessEndIdx = slashIdx;
        logger.debug("Parsing URI " + uri);
        if (slashIdx == -1) {
            sessEndIdx = uri.length();
        }

        if (sessEndIdx != 0) {
            sessionId = uri.substring(0, sessEndIdx);
        }

        if (slashIdx == -1) {
            streamId = null;
        } else {
            streamId = uri.substring(slashIdx + 1);
        }

        return setRequestPacket(packet);
    }

    /**
     * Returns the request packet
     * @return The request packet of the request
     */
    public RTSPRequestPacket getRequestPacket() {
        return requestPacket;
    }

    /**
     * Dispatches the request to the appropriate manager
     *
     * @param dispatcher
     *            The dispatcher to dispatch to
     * @throws RTSPResponse
     */
    public abstract void dispatch(Dispatcher dispatcher) throws RTSPResponse;

    /**
     * Returns the id of the session
     * @return The ID of the session of the request
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the id of the stream
     * @return The ID of the stream of the request
     */
    public String getStreamId() {
        return streamId;
    }

    protected void setPacket(RTSPRequestPacket packet) {
        this.requestPacket = packet;
    }

    /**
     * Sets the request packet of the request
     *
     * @param packet
     *            The request packet
     * @return True if the packet was successfully set
     * @throws RTSPResponse
     */
    protected abstract boolean setRequestPacket(RTSPRequestPacket packet)
            throws RTSPResponse;
}
