/*
 * @(#)Dispatcher.java
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
 * This class is used to dispatch the received packets to the appropriate
 * manager.
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Dispatcher {

    // The log file for the database
    private static Log logger = LogFactory.getLog(Dispatcher.class.getName());

    // The error message when a session is not found
    private static final String NOT_FOUND_ERROR = "Session Not Found";

    // The error message when a method is not implemented
    private static final String NOT_IMPLEMENTED_ERROR = "Not Implemented";

    // The default index file
    private static final String INDEX_FILE = "index.html";

    // The SessionManager to dispatch to
    //private SessionManager sessionManager = null;

    // The PlaybackManager to dispatch to
    private PlaybackManager playbackManager = null;

    // The RecordManager to dispatch to
    private RecordManager recordManager = null;

    /**
     * Creates a new Dispatcher.
     *
     * @param sessionManager
     *            The SessionManager to dispatch to
     * @param playbackManager
     *            The PlaybackManager to dispatch to
     * @param recordManager
     *            The RecordManager to dispatch to
     */
//    public Dispatcher(SessionManager sessionManager,
//            PlaybackManager playbackManager, RecordManager recordManager) {
//        this.sessionManager = sessionManager;
//        this.playbackManager = playbackManager;
//        this.recordManager = recordManager;
//    }
    public Dispatcher(PlaybackManager playbackManager, RecordManager recordManager) {
        //this.sessionManager = sessionManager;
        this.playbackManager = playbackManager;
        this.recordManager = recordManager;
    }
    /**
     * Dispatches the request to the appropriate handler.
     *
     * @param requestPacket
     *            The packet to handle
     * @throws RTSPResponse
     */
    public void handleRequestPacket(RTSPRequestPacket requestPacket)
            throws RTSPResponse {

        // Create a request from the packet
        RTSPRequest request = RTSPRequest.createRequest(requestPacket);
        logger.debug("Handling request method=" + requestPacket.getMethod()
                + " uri=" + requestPacket.getUri() + " version="
                + requestPacket.getVersion());

        // If the request is valid, dispatch it
        if (request != null) {
            request.dispatch(this);
        } else {
            logger.error("Failed to create request");
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    "Internal Server Error", requestPacket);
        }
    }

    /**
     * Handles an RTSP describe request
     *
     * @param describeRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleDescribeRequest(RTSPDescribeRequest describeRequest)
            throws RTSPResponse {
        //sessionManager.handleDescribe(describeRequest);
    }

    /**
     * Handles an RTSP announce request
     *
     * @param announceRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleAnnounceRequest(RTSPAnnounceRequest announceRequest)
            throws RTSPResponse {
        //sessionManager.handleAnnounce(announceRequest);
    }

    /**
     * Handles an RTSP GET request
     *
     * @param getRequest
     *            getRequest The request to handle
     * @throws RTSPResponse
     */
    public void handleGetRequest(RTSPGetRequest getRequest)
            throws RTSPResponse {
        String path = getRequest.getRequestPacket().getPath();
        if (path.equals("") || path.equals(INDEX_FILE)) {
            //sessionManager.handleGet(getRequest);
        } else {
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND, "File not found",
                    getRequest.getRequestPacket());
        }
    }

    /**
     * Handles an RTSP DELETE request
     *
     * @param deleteRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleDeleteRequest(RTSPDeleteRequest deleteRequest)
            throws RTSPResponse {
        try {
            //sessionManager.handleDelete(deleteRequest);
        } catch (Exception e) {
            logger.error("Error", e);
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR, e.getMessage(),
                    deleteRequest.getRequestPacket());
        }
    }

    /**
     * Handles an RTSP GET_PARAMETER request
     *
     * @param getParamRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleGetParamRequest(RTSPGetParamRequest getParamRequest)
            throws RTSPResponse {
        if (getParamRequest.getRequestPacket().getSession() == null) {
            //sessionManager.handleGetParam(getParamRequest);
        } else if (playbackManager.isPlaySession(getParamRequest)) {
            playbackManager.handleGetParam(getParamRequest);
        } else if (recordManager.isRecordSession(getParamRequest)) {
            recordManager.handleGetParam(getParamRequest);
        } else {
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    "Session was not found",
                    getParamRequest.getRequestPacket());
        }
    }

    /**
     * Handles an RTSP OPTIONS request
     *
     * @param optionsRequest
     *            The request to handle
     */
    public void handleOptionsRequest(RTSPOptionsRequest optionsRequest) {
        //sessionManager.handleOptions(optionsRequest);
    }

    /**
     * Handles an RTSP PAUSE request
     *
     * This throws an RTSP 501 response.
     *
     * @param pauseRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handlePauseRequest(RTSPPauseRequest pauseRequest)
            throws RTSPResponse {
        playbackManager.handlePause(pauseRequest);
    }

    /**
     * Handles an RTSP PLAY request
     *
     * @param playRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handlePlayRequest(RTSPPlayRequest playRequest)
            throws RTSPResponse {
        playbackManager.handlePlay(playRequest);
    }

    /**
     * Handles an RTSP RECORD request
     *
     * @param recordRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleRecordRequest(RTSPRecordRequest recordRequest)
            throws RTSPResponse {
        //sessionManager.handleRecord(recordRequest);
        recordManager.handleRecord(recordRequest);
    }

    /**
     * Handles an RTSP REDIRECT request
     *
     * This throws an RTSP 501 response.
     *
     * @param redirectRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleRedirectRequest(RTSPRedirectRequest redirectRequest)
            throws RTSPResponse {
        throw new RTSPResponse(Headers.RTSP_NOT_IMPLEMENTED,
                NOT_IMPLEMENTED_ERROR,
                redirectRequest.getRequestPacket());
    }

    /**
     * Handles an RTSP SETUP request
     *
     * @param setupRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handlePlaySetupRequest(RTSPSetupRequest setupRequest)
            throws RTSPResponse {
        //sessionManager.handlePlaySetup(setupRequest);
        playbackManager.handleSetup(setupRequest);
    }

    /**
     * Handles an RTSP SETUP request
     *
     * @param setupRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleRecordSetupRequest(RTSPSetupRequest setupRequest)
            throws RTSPResponse {
       // recordManager.handleSetup(setupRequest);
    }

    /**
     * Handles an RTSP TEARDOWN request
     *
     * @param teardownRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleTeardownRequest(RTSPTeardownRequest teardownRequest)
            throws RTSPResponse {
        if (playbackManager.isPlaySession(teardownRequest)) {
            playbackManager.handleTeardown(teardownRequest);
        } else if (recordManager.isRecordSession(teardownRequest)) {
            recordManager.handleTeardown(teardownRequest);
        } else {
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND, NOT_FOUND_ERROR,
                    teardownRequest.getRequestPacket());
        }
        //sessionManager.handleTeardown(teardownRequest);
    }

    /**
     * Handlers an RTSP REPAIR request
     *
     * @param repairRequest The request to handle
     * @throws RTSPResponse
     */
    public void handleRepairRequest(RTSPRepairRequest repairRequest)
            throws RTSPResponse {
        recordManager.handleRepair(repairRequest);
    }

    /**
     * Handles an RTSP LISTEN request
     *
     * @param listenRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleListen(RTSPListenRequest listenRequest)
            throws RTSPResponse {
        recordManager.handleListen(listenRequest);
    }

    /**
     * Handles an RTSP NEWSTREAMS request
     * @param request The request to handle
     * @throws RTSPResponse
     */
    public void handleNewStreams(RTSPNewStreamsRequest request)
            throws RTSPResponse {
        recordManager.handleNewStreams(request);
    }
}
