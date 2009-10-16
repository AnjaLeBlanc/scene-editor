/*
 * @(#)PlaybackManager.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Manages the playback of streams.
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class PlaybackManager {

    /**
     * The attribute name for the playback manager in the web context
     */
    public static final String PLAYBACK_MANAGER_ATTRIBUTE = "playbackManager";

    // The message indicating this is the teardown method
    private static final String TEARDOWN_MESSAGE = 
        "Playback_Manager::teardown: ";

    // The message indicating that the session id is invalid
    private static final String INVALID_SESSION_ID_ERROR = 
        "Invalid Session Id ";
    
    // The first id
    private static final int INITIAL_ID = 100;
    
    // Response when a stream identifier was not found in a session
    private static final String STREAM_NOT_FOUND_RESPONSE = "Stream not found";

    // Response when nothing went wrong
    private static final String OK_RESPONSE = "OK";

//    // Response when the user can't play the session
//    private static final String FORBIDDEN_INSUFFICIENT_PRIV_RESPONSE = 
//        "Insufficient Privileges to play this session";

    // Response when there was an error with the playback manager
    private static final String PLAY_MANAGER_ERROR_RESPONSE = 
        "Could not add stream to manager";

    // Response when the transport could not be created
    private static final String PLAYBACK_TRANSPORT_NULL_RESPONSE = 
        "Cannot create playback transport";

    // Response when a play request is made on a session set up by someone else
    private static final String FORBIDDEN_RESPONSE = 
        "This session belongs to someone else";

    // Response when a session id could not be found
    private static final String NOT_FOUND_RESPONSE = "Invalid session id";

    // The log file for the database
    private static Log logger = 
        LogFactory.getLog(PlaybackManager.class.getName());

    // The ID to be given to the next session
    private int nextSessionId = INITIAL_ID;

    // The Manager of Transports
    private TransportManager transportManager = null;

    // An ID -> PlaySession map
    private HashMap<Integer,PlaySessionManager> playSessionMap = 
    	new HashMap<Integer,PlaySessionManager>();

    // A link to the Database
//    private Database arenaDB = null;

    // The location of the recorded files
    private String sFilePath = "";
    
    // The session manager
//    private SessionManager sessionManager = null;

    /**
     * Creates a new PlaybackManager
     * 
     * @param transportManager
     *            The transport manager to use
     * @param arenaDB
     *            The connection to the Arena database
     * @param sFilePath The path where recordings are stored
     */
    public PlaybackManager(TransportManager transportManager, /*Database arenaDB,*/
            String sFilePath) {
        this.transportManager = transportManager;
//        this.arenaDB = arenaDB;
        this.sFilePath = sFilePath;
    }
//    public PlaybackManager(TransportManager transportManager, Database arenaDB,
//            String sFilePath) {
//        this.transportManager = transportManager;
//        this.arenaDB = arenaDB;
//        this.sFilePath = sFilePath;
//    }

    /**
     * Handles an RTSP SETUP request.
     * 
     * @param setupRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleSetup(RTSPSetupRequest setupRequest) throws RTSPResponse {
        
        // Get the session id from the packet
        PlaySessionManager playManager = null;
        PlaybackNetworkTransport netTrans = null;
        String session = setupRequest.getRequestPacket().getSession();
        int sessionId = 0;
        logger.debug("Session <" + session + ">");

        // If there is a session ID, check it is a valid one
        if ((session != null) && (session.length() > 0)) {
            
            // Create a session ID and check it is valid
            sessionId = Integer.valueOf(session).intValue();
            playManager = (PlaySessionManager) playSessionMap.get(new Integer(
                    sessionId));

            // If the session ID is invalid, tell the client
            if (playManager == null) {
                logger.warn(INVALID_SESSION_ID_ERROR + sessionId);
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, setupRequest.getRequestPacket());
            }

            // If the session is valid, but it belongs to someone else, throw an
            // error
            if (!playManager.getOwner().equals(
                    setupRequest.getRequestPacket().getUsername())) {
                throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                        FORBIDDEN_RESPONSE, setupRequest
                                .getRequestPacket());
            }
        } else {

            // If no session ID was specified, create one now and map it up
            sessionId = nextSessionId++;
            playManager = new PlaySessionManager(/*arenaDB, sessionManager,*/
                    sessionId, setupRequest.getSessionId(), sFilePath, 
                    setupRequest.getRequestPacket().getUsername());
            playSessionMap.put(new Integer(sessionId), playManager);
        }

        // Create a transport for this stream, fail on error
        netTrans = transportManager.setupPlaybackTransport(setupRequest, 
                sessionId);
        if (netTrans == null) {
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    PLAYBACK_TRANSPORT_NULL_RESPONSE, setupRequest
                            .getRequestPacket());
        }

        if ((playManager == null)
                || (playManager.addStream(setupRequest, netTrans)
                    != PlaySessionManager.SUCCESS)) {
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    PLAY_MANAGER_ERROR_RESPONSE, setupRequest
                            .getRequestPacket());
        }
    }

    /**
     * Handles an RTSP PLAY request.
     * 
     * @param playRequest
     * @throws RTSPResponse
     */
    public void handlePlay(RTSPPlayRequest playRequest) throws RTSPResponse {
        String session = playRequest.getRequestPacket().getSession();
        int sessionId;
        PlaySessionManager playManager;

        if (session != null) {

            sessionId = Integer.valueOf(session).intValue();
            playManager = (PlaySessionManager) playSessionMap.get(new Integer(
                    sessionId));
            if (playManager == null) {
                logger.warn(INVALID_SESSION_ID_ERROR + sessionId);
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, playRequest.getRequestPacket());
            }

            // Check if the user can play the session
//            if (!arenaDB.userCanPlay(playRequest.getSessionId(), playRequest
//                    .getRequestPacket().getUsername())) {
//                throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
//                        FORBIDDEN_INSUFFICIENT_PRIV_RESPONSE,
//                        playRequest.getRequestPacket());
//            }

            // If the session is valid, but it belongs to someone else, throw an
            // error
            if (!playManager.getOwner().equals(
                    playRequest.getRequestPacket().getUsername())) {
                throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                        FORBIDDEN_RESPONSE, playRequest
                                .getRequestPacket());
            }

        } else {
            logger.warn(INVALID_SESSION_ID_ERROR + session);
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, playRequest.getRequestPacket());
        }

        logger.debug("playManager " + playManager
                + " gets play request for id " + sessionId);

        playManager.play(playRequest);
    }

    /**
     * Handles an RTSP GET_PARAMETER request
     * 
     * @param getParamRequest
     * @throws RTSPResponse
     */
    public void handleGetParam(RTSPGetParamRequest getParamRequest)
            throws RTSPResponse {

        // Get the session ID
        RTSPRequestPacket pRequest = getParamRequest.getRequestPacket();
        String session = pRequest.getSession();
        int sessionId;
        PlaySessionManager playManager;
        RTSPResponse resp = null;

        // If the session ID is available, get the session
        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();
            playManager = (PlaySessionManager) playSessionMap.get(new Integer(
                    sessionId));

            // If the session is not valid, throw an error
            if (playManager == null) {
                logger.warn(INVALID_SESSION_ID_ERROR + sessionId);
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, getParamRequest
                                .getRequestPacket());
            }

            // If the session is valid, but it belongs to someone else, throw an
            // error
            if (!playManager.getOwner().equals(
                    getParamRequest.getRequestPacket().getUsername())) {
                throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                        FORBIDDEN_RESPONSE, getParamRequest
                                .getRequestPacket());
            }

        } else {
            
            // If the session ID is not available, throw an error
            logger.warn(INVALID_SESSION_ID_ERROR + session);
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, getParamRequest.getRequestPacket());
        }

        // Send a response to the getparam request
        resp = new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE, pRequest);
        resp.setHeader(Headers.RTSP_SESSION, session);
        resp.send();
    }

    /**
     * Stops playback of a playSession
     * 
     * @param sessionId
     *            The id of the session
     * @param user The user requesting the teardown
     * @return True if the teardown was successful
     */
    public boolean teardown(int sessionId, String user) {
        PlaySessionManager playManager = (PlaySessionManager) playSessionMap
                .get(new Integer(sessionId));
        if (playManager != null) {
            if (playManager.getOwner().equals(user)) {
                if (playManager.teardown()) {
                    playSessionMap.remove(new Integer(sessionId));
                    transportManager.closeAllTransports(sessionId);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handles an RTSP TEARDOWN request
     * 
     * @param teardownRequest
     * @throws RTSPResponse
     */
    public void handleTeardown(RTSPTeardownRequest teardownRequest)
            throws RTSPResponse {

        // Get the session ID
        String session = teardownRequest.getRequestPacket().getSession();
        String streamId = teardownRequest.getStreamId();
        int sessionId;
        PlaySessionManager playManager;

        // If the session ID is available, get the session
        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();
            playManager = (PlaySessionManager) playSessionMap.get(new Integer(
                    sessionId));

            // If the session is valid, tear it down
            if (playManager != null) {

                // If the session is valid, but it belongs to someone else,
                // throw an error
                if (!playManager.getOwner().equals(
                        teardownRequest.getRequestPacket().getUsername())) {
                    throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                            FORBIDDEN_RESPONSE,
                            teardownRequest.getRequestPacket());
                }

                if (playManager.teardown(streamId)) {

                    // If there are no more players in the session, destroy the
                    // manager
                    if (playManager.getNumPlayers() == 0) {
                        logger.debug(TEARDOWN_MESSAGE
                            + "No more streams in Playback_Session_Manager "
                            + "- deleting playManager");
                        playSessionMap.remove(new Integer(sessionId));
                        transportManager.closeAllTransports(sessionId);
                    } else {
                        logger.debug(TEARDOWN_MESSAGE
                            + "More streams in Playback_Session_Manager");
                    }
                } else {
                    
                    // Send an error response to the teardown request
                    RTSPRequestPacket request = teardownRequest
                            .getRequestPacket();
                    RTSPResponse resp = 
                        new RTSPResponse(Headers.RTSP_NOT_FOUND, 
                                STREAM_NOT_FOUND_RESPONSE, request);
                    resp.setHeader(Headers.RTSP_SESSION, String
                            .valueOf(sessionId));
                    throw resp;
                }
            } else {
                
                // If the session is not valid, throw an error
                logger.warn(INVALID_SESSION_ID_ERROR + sessionId);
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, teardownRequest
                                .getRequestPacket());
            }
        } else {

            // If the session ID is not available, throw an error
            logger.warn(INVALID_SESSION_ID_ERROR + session);
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, teardownRequest.getRequestPacket());
        }
    }

    /**
     * Handles an RTSP PAUSE request
     * 
     * @param pauseRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handlePause(RTSPPauseRequest pauseRequest) throws RTSPResponse {
        
        // Get the session ID
        String session = pauseRequest.getRequestPacket().getSession();
        int sessionId;
        PlaySessionManager playManager;

        logger.debug("PlaybackManager::handlePause: Session = " + session);

        // If the session ID is available, get the session
        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();
            playManager = (PlaySessionManager) playSessionMap.get(new Integer(
                    sessionId));

            // If the session is valid, tear it down
            if (playManager != null) {

                // If the session is valid, but it belongs to someone else,
                // throw an error
                if (!playManager.getOwner().equals(
                        pauseRequest.getRequestPacket().getUsername())) {
                    throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                            FORBIDDEN_RESPONSE,
                            pauseRequest.getRequestPacket());
                }

                playManager.pause(pauseRequest);
            } else {

                // If the session is not valid, throw an error
                logger.warn(INVALID_SESSION_ID_ERROR + sessionId);
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, pauseRequest.getRequestPacket());
            }
        } else {

            // If the session ID is not available, throw an error
            logger.warn(INVALID_SESSION_ID_ERROR + session);
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, pauseRequest.getRequestPacket());
        }
    }

    /**
     * Checks if the given session ID is a play session ID
     * 
     * @param request
     *            The request containing the session ID
     * @return True if the session ID is a play session ID, False otherwise
     */
    public boolean isPlaySession(RTSPRequest request) {
        boolean isPlaySession = false;
        String session = request.getRequestPacket().getSession();

        if (session != null) {
            int sessionId = Integer.valueOf(session).intValue();
            isPlaySession = 
                (playSessionMap.get(new Integer(sessionId)) != null);
        }

        return isPlaySession;
    }

    /**
     * Returns a list of sessions started by the given user
     * 
     * @param owner
     *            The user that started the session
     * @return A vector of PlaySessionManagers
     */
    public Vector<PlaySessionManager> getSessions(String owner) {
        Vector<PlaySessionManager> sessions = new Vector<PlaySessionManager>();
        Iterator<Integer> iterator = playSessionMap.keySet().iterator();
        while (iterator.hasNext()) {
            PlaySessionManager playSession = 
                (PlaySessionManager) playSessionMap.get(iterator.next());
            if (playSession.getOwner().equals(owner)) {
                sessions.add(playSession);
            }
        }
        return sessions;
    }
    
    /**
     * Sets the session manager
     * @param sessionManager The session manager to set
     */
//    public void setSessionManager(SessionManager sessionManager) {
//        this.sessionManager = sessionManager;
//    }
}