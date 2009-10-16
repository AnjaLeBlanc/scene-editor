/*
 * @(#)PlaySessionManager.java
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
 * Handles a play session
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class PlaySessionManager {

    /**
     * Result was a failure
     */
    public static final int FAILURE = 0;

    /**
     * Result was a success
     */
    public static final int SUCCESS = 1;
    
    // The time to wait for streams to setup
    private static final int WAIT_TIME = 1000;

    // The response when all is good
    private static final String OK_RESPONSE = "OK";

    // The log file for the database
    private static Log logger = 
        LogFactory.getLog(PlaySessionManager.class.getName());

    // The id of the session
    private int sessionId = 0;

    // The database id of the session
    private String dbSessionId = "";

    // The database storing sessions
//    private Database databaseRef = null;

    // The map of streams in the session
    private HashMap<String,PlayStreamManager> streamMap = 
    	new HashMap<String,PlayStreamManager>();

    // Used to make sure the right parts get sent out at the right time
    private PlayStreamSynchronizer synch = null;

    // The location of the recorded files
    private String sFilePath = "";

    // The owner of the session
    private String owner = "";
    
    // The session manager
//    private SessionManager sessionManager = null;

    /**
     * Creates a new PlaySessionManager
     * 
     * @param db
     *            The database to store values in
     * @param sessionManager The session Manager
     * @param id
     *            The id of the manager
     * @param dbId
     *            The id of the session in the database
     * @param sFilePath The path where recordings are stored
     * @param owner The owner of the session
     */
    public PlaySessionManager(/*Database db, SessionManager sessionManager, */
            int id, String dbId, String sFilePath, String owner) {
        dbSessionId = dbId;
//        databaseRef = db;
        sessionId = id;
        this.sFilePath = sFilePath;
        this.owner = owner;
//        this.sessionManager = sessionManager;

        logger.debug("Play_Session_Manager::Play_Session_Manager: creating!");
        synch = new PlayStreamSynchronizer(sessionId);
    }
//    public PlaySessionManager(Database db, SessionManager sessionManager, 
//            int id, String dbId, String sFilePath, String owner) {
//        dbSessionId = dbId;
//        databaseRef = db;
//        sessionId = id;
//        this.sFilePath = sFilePath;
//        this.owner = owner;
//        this.sessionManager = sessionManager;
//
//        logger.debug("Play_Session_Manager::Play_Session_Manager: creating!");
//        synch = new PlayStreamSynchronizer(sessionId);
//    }

    /**
     * Adds a stream to the session
     * 
     * @param setupRequest
     *            The request to add the stream
     * @param netTrans
     *            The transport to use to send the data
     * @return The result of setting up the stream
     * @throws RTSPResponse 
     */
    public int addStream(RTSPSetupRequest setupRequest,
            PlaybackNetworkTransport netTrans) throws RTSPResponse {
        int errCode = FAILURE;
        RTSPRequestPacket request = setupRequest.getRequestPacket();
        RTSPResponse resp = 
            new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE, request);
        int port = netTrans.getEndpoint().getPort();
        
        // Get a manger for the stream
        final Session session = new Session("");
//        final Session session = sessionManager.findArenaSession(
//                setupRequest.getSessionId(), 
//                setupRequest.getRequestPacket().getUsername(),
//                setupRequest.getRequestPacket().getPassword());
        if (setupRequest.getStreamId() != null) {
            PlayStreamManager playStream = new PlayStreamManager(/*databaseRef,*/
                    setupRequest, synch, netTrans, sFilePath, session,
                    setupRequest.getStreamId());
            
            // Get the ID of the stream and map it to the manager
            String streamId = setupRequest.getStreamId();
            streamMap.put(streamId, playStream);
            
            logger.debug("Play_Session_Manager::addStream: assigned stream "
                    + streamId + " to Play_Stream_Manager,"
                            + " mapsize=" + streamMap.size());
            logger.debug("Play_Session_Manager::addStream: sending response "
                    + "to setup, id=" + sessionId);
            
            // Start prefetch process
            playStream.prefetch();
        } else if (setupRequest.getRequestPacket().getStream() != null) {
            final String[] streams = 
                setupRequest.getRequestPacket().getStream().split(",");
            final Vector<Integer[]> streamsSetup = new Vector<Integer[]>();
            final Vector<RTSPResponse> errors = new Vector<RTSPResponse>();
            for (int i = 0; i < streams.length; i++) {
                final int no = i;
                final PlaybackNetworkTransport trans = netTrans;
                final RTSPSetupRequest req = setupRequest;
                Thread playThread = new Thread() {
                    public void run() {
                        PlayStreamManager playStream;
                        try {
                            playStream = new PlayStreamManager(
                                    /*databaseRef,*/
                                    req, synch, trans, sFilePath, session,
                                    streams[no]);
                            
                            // Get the ID of the stream and map it to manager
                            streamMap.put(streams[no], playStream);
                            
                            // Start prefetch process
                            playStream.prefetch();
                            synchronized (streamsSetup) {
                                streamsSetup.add(new Integer[no]);
                                streamsSetup.notify();
                                logger.debug("Play_Session_Manager::"
                                        + "addStream: "
                                        + "assigned stream "
                                        + streams[no] 
                                        + " to Play_Stream_Manager, mapsize="
                                        + streamMap.size());
                                logger.debug("Play_Session_Manager::addStream: "
                                        + "sending response to setup, id=" 
                                        + sessionId);
                            }
                        } catch (RTSPResponse e) {
                            synchronized (streamsSetup) {
                                errors.add(e);
                                streamsSetup.notify();
                            }
                        }
                    }
                };
                playThread.start();
            }
            synchronized (streamsSetup) {
                while ((streamsSetup.size() + errors.size())
                        < streams.length) {
                    try {
                        streamsSetup.wait(WAIT_TIME);
                    } catch (InterruptedException e) {
                        
                        // Do Nothing
                    }
                }
            }
            if (errors.size() > 0) {
                RTSPResponse e = (RTSPResponse) errors.get(0);
                throw e;
            }
        } else {
            return errCode;
        }

        // Send the OK response
        resp.setHeader(Headers.RTSP_SESSION, String.valueOf(sessionId));
        resp.setHeader(Headers.RTSP_TRANSPORT, request.getTransport()
                + Headers.RTSP_TRANSPORT_SEPARATOR
                + Headers.RTSP_TRANSPORT_SERVER_PORT
                + Headers.RTSP_TRANSPORT_VAR_VAL_SEP
                + port + Headers.RTSP_TRANSPORT_PORT_SEP
                + (port + 1));
        resp.send();

        errCode = SUCCESS;
        return errCode;
    }

    /**
     * Starts the playback of the recorded streams
     * 
     * @param request
     *            The request made to play the streams
     * @return The result of setting up the playback
     */
    public int play(RTSPPlayRequest request) {
        int errCode = SUCCESS;
        synch.play(request);
        return errCode;
    }

    /**
     * Pauses the session
     * @param request The request to pause 
     */
    public void pause(RTSPPauseRequest request) {
        synch.pause(request);
    }

    /**
     * Stops the playback
     * @param streamId The id of the stream to stop
     * @return True if the teardown was successful
     */
    public boolean teardown(String streamId) {
        
        if (streamId == null) {
            return teardown();
        }

        // Get the stream to stop
        PlayStreamManager playStreamManager = 
            (PlayStreamManager) streamMap.get(streamId);
        logger.debug("Play_Session_Manager::teardown: tearing down stream "
                + streamId);

        // If the stream exists, stop it
        if (playStreamManager != null) {
            logger.debug("Play_Session_Manager::teardown: GOOD TEARDOWN!");

            // Pass the teardown request on to its Play_Stream_Manager
            playStreamManager.teardown();

            // Clean up time
            streamMap.remove(streamId);

            // Return that the removal was successful
            return true;
        }

        // If the stream does not exist send an error
        logger.warn("Play_Session_Manager::teardown: BAD TEARDOWN!");
        return false;
    }

    /**
     * Stops all playback
     * 
     * @return true if the teardown is successful
     */
    public boolean teardown() {
        Vector<String> streams = new Vector<String>();
        Iterator<String> iterator = streamMap.keySet().iterator();
        while (iterator.hasNext()) {
            streams.add(iterator.next());
        }

        for (int i = 0; i < streams.size(); i++) {
            String streamId = (String) streams.get(i);
            if (!teardown(streamId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the play session id
     * @return The ID of the session
     */
    public int getSessionId() {
        return sessionId;
    }

    /**
     * Returns the number of streams
     * @return The number of streams being played
     */
    public int getNumPlayers() {
        return streamMap.size();
    }

    /**
     * Returns the owner of the session
     * @return the owner of the session
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns the database session ID
     * @return the session id being played
     */
    public String getDBSessionId() {
        return dbSessionId;
    }
}