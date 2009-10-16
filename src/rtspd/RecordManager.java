/*
 * @(#)RecordManager.java
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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Manages the recording of streams
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RecordManager {

    /**
     * The attribute name for the record manager in the web context
     */
    public static final String RECORD_MANAGER_ATTRIBUTE = "recordManager";

    // The slash character
//    private static final String SLASH = System.getProperty("file.separator");

    // The timer record end delay if it is less than 0
    private static final int DEFAULT_END_DELAY = 1000;

    // The message for the log when there is an error
//    private static final String ERROR_MESSAGE = "Error";

    // The first Id
    private static final int INITIAL_ID = 1000;

    // The response to send when there are no problems
    private static final String OK_RESPONSE = "OK";

    // The response to send when the recording directory could not be created
    private static final String FAILED_CREATE_DIRECTORY_RESPONSE =
        "Could not create directory for recording";

    // The response to send when a stream could not be added to the recording
//    private static final String FAILED_ADD_STREAM_RESPONSE =
//        "Cannot add stream";

    // The response to send when the record manager could not be created
//    private static final String FAILED_RECORD_MANAGER_RESPONSE =
//        "Cannot create record manager";

    // The response to send when the record transport could not be created
//    private static final String FAILED_RECORD_TRANSPORT_RESPONSE =
//        "Cannot create record transport";

    // The response to send when the user is not the owner of the record session
    private static final String NOT_OWNER_RESPONSE =
        "This session belongs to someone else";

    // The response to send when the session was not found
    private static final String NOT_FOUND_RESPONSE = "Invalid session id";

    // The response to send when the user cannot record the session
//    private static final String FORBIDDEN_RESPONSE =
//        "Insufficient Privileges to record this session";

    // The message sent when a session is recording on record or play
//    private static final String CURRENTLY_RECORDING_RESPONSE =
//        "Session is currently being recorded";

    // The log file
    private static Log logger =
        LogFactory.getLog(RecordManager.class.getName());

    // The ID to be given to the next session
    private int nextSessionId = INITIAL_ID;

    // The Manager of Transports
    private TransportManager transportManager = null;

    // An ID -> RecordSession map
    private HashMap<Integer,RecordSessionManager> recordSessionMap = 
    	new HashMap<Integer,RecordSessionManager>();

    // A link to the Database
    //private Database arenaDB = null;

    // The location where files are stored
    private String sFilePath = "";

    // The session manager
    //private SessionManager sessionManager = null;

    // Map of id -> timer
    private HashMap<Integer,Timer> timerMap = new HashMap<Integer,Timer>();

    /**
     * Creates a new RecordManager
     *
     * @param transportManager
     *            The transport manager to use
     * @param arenaDB
     *            The connection to the Arena database
     * @param sFilePath The path where recordings are stored
     * @param sessionManager The session manager
     */
//    public RecordManager(TransportManager transportManager, /*Database arenaDB,*/
//            String sFilePath/*, SessionManager sessionManager*/) {
//
//        // Setup any timer recording sessions that are waiting
////        HashMap sessions = new HashMap();/*sessionManager.getTimerRecordSessions(
////                arenaDB.getUsername(), arenaDB.getPassword());*/
////        Iterator iter = sessions.keySet().iterator();
//        this.transportManager = transportManager;
//        //this.arenaDB = arenaDB;
//        this.sFilePath = sFilePath;
////        this.sessionManager = sessionManager;
////        while (iter.hasNext()) {
////            String sessionId = (String) iter.next();
////            HashMap details = (HashMap) sessions.get(sessionId);
////            String username = (String) details.get(Session.TIMER_RECORD_USER);
////            String password = (String) details.get(Session.TIMER_RECORD_PASS);
////            Vector addresses = (Vector) details.get(Session.MEETING_ADDRESS);
////            Session session = sessionManager.findArenaSession(sessionId,
////                    username, password);
////            boolean setupOK = true;
////            for (int i = 0; (i < addresses.size()) && setupOK; i++) {
////                if (addresses.get(i) != null) {
////                    int id = setupRecording(session,
////                            Headers.STREAM_PREFIX + "_" + i,
////                            username, password,
////                            (NetworkEndpoint) addresses.get(i));
////                    if (id != -1) {
////                        session.setRecordSessionId(String.valueOf(id));
////                    } else {
////                        logger.error("Error Setting up Timer Recording");
////                        setupOK = false;
////                        break;
////                    }
////                } else {
////                    logger.error("Error setting up Timer Recording:"
////                            + " null address");
////                    setupOK = false;
////                }
////            }
////            if (setupOK) {
////                try {
////                    scheduleSession(session, Integer.parseInt(
////                            session.getRecordSessionId()),
////                            username, password);
////                } catch (Exception e) {
////                    logger.error(ERROR_MESSAGE, e);
////                    setupOK = false;
////                    if (session.getRecordSessionId() != null) {
////                        teardown(Integer.parseInt(
////                                session.getRecordSessionId()));
////                    }
////                }
////            } else if (session.getRecordSessionId() != null) {
////                teardown(Integer.parseInt(session.getRecordSessionId()));
////            }
////        }
//    }
//    public RecordManager(TransportManager transportManager, /*Database arenaDB,*/
//            String sFilePath, SessionManager sessionManager) {
//
//        // Setup any timer recording sessions that are waiting
//        HashMap sessions = new HashMap();/*sessionManager.getTimerRecordSessions(
//                arenaDB.getUsername(), arenaDB.getPassword());*/
//        Iterator iter = sessions.keySet().iterator();
//        this.transportManager = transportManager;
//        //this.arenaDB = arenaDB;
//        this.sFilePath = sFilePath;
//        this.sessionManager = sessionManager;
//        while (iter.hasNext()) {
//            String sessionId = (String) iter.next();
//            HashMap details = (HashMap) sessions.get(sessionId);
//            String username = (String) details.get(Session.TIMER_RECORD_USER);
//            String password = (String) details.get(Session.TIMER_RECORD_PASS);
//            Vector addresses = (Vector) details.get(Session.MEETING_ADDRESS);
//            Session session = sessionManager.findArenaSession(sessionId,
//                    username, password);
//            boolean setupOK = true;
//            for (int i = 0; (i < addresses.size()) && setupOK; i++) {
//                if (addresses.get(i) != null) {
//                    int id = setupRecording(session,
//                            Headers.STREAM_PREFIX + "_" + i,
//                            username, password,
//                            (NetworkEndpoint) addresses.get(i));
//                    if (id != -1) {
//                        session.setRecordSessionId(String.valueOf(id));
//                    } else {
//                        logger.error("Error Setting up Timer Recording");
//                        setupOK = false;
//                        break;
//                    }
//                } else {
//                    logger.error("Error setting up Timer Recording:"
//                            + " null address");
//                    setupOK = false;
//                }
//            }
//            if (setupOK) {
//                try {
//                    scheduleSession(session, Integer.parseInt(
//                            session.getRecordSessionId()),
//                            username, password);
//                } catch (Exception e) {
//                    logger.error(ERROR_MESSAGE, e);
//                    setupOK = false;
//                    if (session.getRecordSessionId() != null) {
//                        teardown(Integer.parseInt(
//                                session.getRecordSessionId()));
//                    }
//                }
//            } else if (session.getRecordSessionId() != null) {
//                teardown(Integer.parseInt(session.getRecordSessionId()));
//            }
//        }
//    }

    /**
     * Sets up a recording
     * @param session The session to set up the recording for
     * @param streamId The id of the record stream
     * @param owner The user setting up the recording
     * @param password The password of the user setting up the recording
     * @param ePoint The end point of the recording
     * @return the id of the session
     */
    public int setupRecording(Session session, String streamId, String owner,
            String password, NetworkEndpoint ePoint) {
        RecordSessionManager recordManager = null;
        int id = 0;
        RecordNetworkTransport netTrans = null;

        if (session.getRecordSessionId() != null) {
            Integer myid = Integer.valueOf(session.getRecordSessionId());
            logger.debug("Adding to session " + myid);
            recordManager =
                (RecordSessionManager) recordSessionMap.get(myid);
            id = myid.intValue();
        } else {
            int myid = nextSessionId++;
            id = myid;
            logger.debug("Creating session " + myid);
            recordManager = new RecordSessionManager(myid, owner, password,
                    sFilePath, session);
            recordSessionMap.put(new Integer(myid), recordManager);
        }
        netTrans = transportManager.setupRecordTransport(ePoint, id);
        if (netTrans == null) {
            return -1;
        }
        recordManager.addSender(streamId, netTrans);
        return id;
    }

    /**
     * Handles an RTSP SETUP request.
     *
     * @param setupRequest
     *            The request to handle
     * @throws RTSPResponse
     */
//    public void handleSetup(RTSPSetupRequest setupRequest) throws RTSPResponse {
//
//        // Get the session id from the packet
//        RecordSessionManager recordManager = null;
//        String session = setupRequest.getRequestPacket().getSession();
//        int sessionId;
//        RecordNetworkTransport netTrans = null;
//
//        // Find the session
//        Session sess = sessionManager.findArenaSession(
//                setupRequest.getSessionId(),
//                setupRequest.getRequestPacket().getUsername(),
//                setupRequest.getRequestPacket().getPassword());
//        logger.debug("Session <" + session + ">");
//        if ((sess != null) && sess.isRecording()) {
//            throw new RTSPResponse(Headers.RTSP_INVALID_STATE,
//                    CURRENTLY_RECORDING_RESPONSE,
//                    setupRequest.getRequestPacket());
//        }
//
////        // Check if the user can record the session
////        if (!arenaDB.userCanRecord(setupRequest.getSessionId(), setupRequest
////                .getRequestPacket().getUsername())) {
////            throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
////                    FORBIDDEN_RESPONSE,
////                    setupRequest.getRequestPacket());
////        }
//
//        // If there is a session ID, check it is a valid one
//        if ((session != null) && (session.length() > 0)) {
//
//            // Create a session ID and check it is valid
//            sessionId = Integer.valueOf(session).intValue();
//            recordManager = (RecordSessionManager) recordSessionMap
//                    .get(new Integer(sessionId));
//
//            // If the session ID is invalid, tell the client
//            if (recordManager == null) {
//                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
//                        NOT_FOUND_RESPONSE, setupRequest.getRequestPacket());
//            }
//
//            // If the session ID is valid, but is for a different user, throw an
//            // error
//            if (!recordManager.getOwner().equals(
//                    setupRequest.getRequestPacket().getUsername())) {
//                throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
//                        NOT_OWNER_RESPONSE, setupRequest
//                                .getRequestPacket());
//            }
//        } else {
//
//            // If no session ID was specified, create one now and map it up
//            int myid = nextSessionId++;
//            sessionId = myid;
//            recordManager = new RecordSessionManager(myid,
//                    setupRequest, sFilePath, sess);
//            recordSessionMap.put(new Integer(myid), recordManager);
//        }
//
//        // Create a transport for this stream, fail on error
//        netTrans = transportManager.setupRecordTransport(setupRequest,
//                sessionId);
//        if (netTrans == null) {
//            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
//                    FAILED_RECORD_TRANSPORT_RESPONSE, setupRequest
//                            .getRequestPacket());
//        } else if (recordManager == null) {
//            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
//                    FAILED_RECORD_MANAGER_RESPONSE, setupRequest
//                            .getRequestPacket());
//        } else if (recordManager.addSender(setupRequest, netTrans)
//            != RecordSessionManager.SUCCESS) {
//            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
//                    FAILED_ADD_STREAM_RESPONSE,
//                    setupRequest.getRequestPacket());
//        }
//    }

    /**
     * Schedules the given session for recording and stopping
     *
     * @param session
     *            The session to schedule
     * @param recSessionId
     *            The record id of the session
     * @param username The user scheduling the session
     * @param password The password of the user
     * @throws MalformedURLException
     */
    public void scheduleSession(Session session, int recSessionId,
            String username, String password) throws MalformedURLException {
        RecordSessionManager recordManager =
            (RecordSessionManager) recordSessionMap.get(
                    new Integer(recSessionId));
        Timer timer = (Timer) timerMap.get(new Integer(recSessionId));
        long now = System.currentTimeMillis();
        long endTime = session.getEndTime();
        logger.debug("Scheduling Session " + session.getId());
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        if (!recordManager.isRecording() && (now < endTime)) {
            long startDelay = session.getStartTime() - now;
            long endDelay = endTime - now;
            if (startDelay < 0) {
                startDelay = 0;
            }
            timer.schedule(
                    new RecordTimer(this, recSessionId, true), startDelay);

            if (endDelay < 0) {
                endDelay = DEFAULT_END_DELAY;
            }
            timer.schedule(
                    new RecordTimer(this, recSessionId, false), endDelay);

            timerMap.put(new Integer(recSessionId), timer);
//            session.setLock(username);
//            session.setPassword(password);
//            session.clearSessionRecordAddress();
//            session.addSessionRecordAddress(
//                    transportManager.getRecordEndPoints(recSessionId));
//            session.getUnlock();
        }
    }

    /**
     * Starts the recording of a session
     * @param sessionId The id of the session to start recording
     * @return True if the recording was started
     */
    public boolean startRecording(int sessionId) {
        RecordSessionManager recordManager =
            (RecordSessionManager) recordSessionMap.get(
                    new Integer(sessionId));
        Session session = null;
        if (recordManager == null) {
            return false;
        }

        if (!recordManager.createDataDir()) {
            return false;
        }

        recordManager.record(null);
        session = recordManager.getSession();
        session.setRecordSessionId(String.valueOf(sessionId));
        return true;
    }

    /**
     * Handles an RTSP RECORD request.
     *
     * @param recordRequest
     * @throws RTSPResponse
     */
    public void handleRecord(RTSPRecordRequest recordRequest)
            throws RTSPResponse {
        String session = recordRequest.getRequestPacket().getSession();
        int sessionId;
        RecordSessionManager recordManager;
//        Session s = null;
        RTSPRequestPacket request = recordRequest.getRequestPacket();
        RTSPResponse resp = new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE,
                request);

        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();
            recordManager = (RecordSessionManager) recordSessionMap
                    .get(new Integer(sessionId));
            if (recordManager == null) {
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, recordRequest.getRequestPacket());
            }
        } else {
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, recordRequest.getRequestPacket());
        }

        // Create the data directory
        if (!recordManager.createDataDir()) {
            resp = new RTSPResponse(Headers.RTSP_NOT_ACCEPTABLE,
                    FAILED_CREATE_DIRECTORY_RESPONSE, request);
            throw resp;
        }

//        s = sessionManager.findArenaSession(recordRequest.getSessionId(),
//                recordRequest.getRequestPacket().getUsername(), recordRequest
//                        .getRequestPacket().getPassword());
//        logger.debug("recordManager " + recordManager
//                + " gets record request for id " + sessionId + "("
//                + s.getStartTime() + ")");
//        if ((s.getStartTime() > 0) && (recordRequest.getStreamId() == null)) {
//            try {
//                scheduleSession(s, sessionId,
//                        recordRequest.getRequestPacket().getUsername(),
//                        recordRequest.getRequestPacket().getPassword());
//            } catch (MalformedURLException e) {
//                logger.error(ERROR_MESSAGE, e);
//                throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
//                        e.getMessage(), recordRequest.getRequestPacket());
//            }
//        } else {
            recordManager.record(recordRequest.getStreamId());
//        }
        resp.setHeader(Headers.RTSP_SESSION, String.valueOf(sessionId));
        resp.send();
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
        String session = getParamRequest.getRequestPacket().getSession();
        int sessionId;
        RecordSessionManager recordManager;
        RTSPRequestPacket pRequest = getParamRequest.getRequestPacket();
        RTSPResponse resp =
            new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE, pRequest);

        // If the session ID is available, get the session
        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();
            recordManager = (RecordSessionManager) recordSessionMap
                    .get(new Integer(sessionId));

            // If the session is not valid, throw an error
            if (recordManager == null) {
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, getParamRequest
                                .getRequestPacket());
            }

        } else {

            // If the session ID is not available, throw an error
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, getParamRequest.getRequestPacket());
        }

        // Send a response to the getparam request
        resp.setHeader(Headers.RTSP_SESSION, session);
        resp.send();
    }

    /**
     * Stops recording of the session
     *
     * @param sessionId
     *            The id of the session
     * @return true if the session was stopped
     */
    public boolean teardown(int sessionId) {
        RecordSessionManager recordManager =
            (RecordSessionManager) recordSessionMap.get(
                    new Integer(sessionId));
        if (recordManager != null) {
//            Session session = recordManager.getSession();
//            try {
////                session.setLock(arenaDB.getUsername());
////                session.setPassword(arenaDB.getPassword());
////                session.clearSessionRecordAddress();
//                session.getUnlock();
//            } catch (MalformedURLException e) {
//                logger.error(ERROR_MESSAGE, e);
//                return false;
//            }
            recordManager.teardown(transportManager);
            recordSessionMap.remove(new Integer(sessionId));
            return true;
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
        int sessionId;
        RecordSessionManager recordManager;

        // If the session ID is available, get the session
        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();

            // Stop any timers on the session
            Timer timer = (Timer) timerMap.get(new Integer(sessionId));
            if (timer != null) {
                timer.cancel();
                timerMap.remove(new Integer(sessionId));
            }

            recordManager = (RecordSessionManager) recordSessionMap
                    .get(new Integer(sessionId));

            // If the session is valid, tear it down
            if (recordManager != null) {

                // If the session ID is valid, but is for a different user,
                // throw an error
                if (!recordManager.getOwner().equals(
                        teardownRequest.getRequestPacket().getUsername())) {
                    throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                            NOT_OWNER_RESPONSE,
                            teardownRequest.getRequestPacket());
                }

                recordManager.teardown(teardownRequest, transportManager);

                // If there are no more players in the session, destroy the
                // manager
                if (recordManager.getNumListeners() == 0) {
                    logger.debug("Record_Manager::teardown: No more streams "
                            + "in Record_Session_Manager");
                    recordSessionMap.remove(new Integer(sessionId));
//                    Session sess = recordManager.getSession();
//                    try {
////                        sess.setLock(arenaDB.getUsername());
////                        sess.setPassword(arenaDB.getPassword());
////                        sess.clearSessionRecordAddress();
//                        sess.getUnlock();
//                    } catch (MalformedURLException e) {
//                        logger.error(ERROR_MESSAGE, e);
//                        throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
//                                e.getMessage(),
//                                teardownRequest.getRequestPacket());
//                    }
                } else {
                    logger.debug("Record_Manager::teardown: "
                            + "More streams in Record_Session_Manager");
                }
            } else {

                // If the session is not valid, throw an error
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, teardownRequest
                                .getRequestPacket());
            }
        } else {

            // If the session ID is not available, throw an error
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, teardownRequest.getRequestPacket());
        }
    }

    /**
     * Handles RTSP REPAIR Requests
     *
     * @param repairRequest The request to handle
     * @throws RTSPResponse
     */
    public void handleRepair(RTSPRepairRequest repairRequest)
            throws RTSPResponse {
//        String username = repairRequest.getRequestPacket().getUsername();
//        String password = repairRequest.getRequestPacket().getPassword();
//        Session sess = sessionManager.findArenaSession(
//                repairRequest.getSessionId(), username, password);
//        if (sess == null) {
//            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
//                    NOT_FOUND_RESPONSE, repairRequest.getRequestPacket());
//        }
//        String directory = sFilePath + SLASH + sess.getId();
//        RecordArchiveManager archiveManager = new RecordArchiveManager(
//                directory, sess, username, password);
//        Vector streams = sess.getStreams();
//        for (int i = 0; i < streams.size(); i++) {
//            Stream stream = (Stream) streams.get(i);
//            try {
//                new StreamArchive(archiveManager,
//                        directory, Long.parseLong(stream.getSsrc()), sess,
//                        username, password);
//            } catch (IOException e) {
//                throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
//                        ERROR_MESSAGE + ": Error reading existing stream",
//                        repairRequest.getRequestPacket());
//            }
//        }
//        archiveManager.terminate();
        RTSPResponse response = new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE,
                repairRequest.getRequestPacket());
        response.send();
    }

    /**
     * Handles RTSP LISTEN Requests
     *
     * @param listenRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void handleListen(RTSPListenRequest listenRequest)
            throws RTSPResponse {

        // Get the session ID
        String session = listenRequest.getRequestPacket().getSession();
        int sessionId;
        RecordSessionManager recordManager;

        // If the session ID is available, get the session
        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();
            recordManager = (RecordSessionManager) recordSessionMap
                    .get(new Integer(sessionId));

            // If the session is valid, tear it down
            if (recordManager != null) {

                // If the session ID is valid, but is for a different user,
                // throw an error
                if (!recordManager.getOwner().equals(
                        listenRequest.getRequestPacket().getUsername())) {
                    throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                            NOT_OWNER_RESPONSE,
                            listenRequest.getRequestPacket());
                }

                recordManager.addListener(listenRequest);
            } else {

                // If the session is not valid, throw an error
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE, listenRequest.getRequestPacket());
            }
        } else {

            // If the session ID is not available, throw an error
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, listenRequest.getRequestPacket());
        }
    }

    /**
     * Checks if the given session ID is a play session ID
     *
     * @param request
     *            The request containing the session ID
     * @return True if the session ID is a play session ID, False otherwise
     */
    public boolean isRecordSession(RTSPRequest request) {
        boolean isRecordSession = false;
        String session = request.getRequestPacket().getSession();

        if (session != null) {
            int sessionId = Integer.valueOf(session).intValue();
            isRecordSession =
                (recordSessionMap.get(new Integer(sessionId)) != null);
        }

        return isRecordSession;
    }

    /**
     * Returns a list of sessions started by the given user
     *
     * @param owner
     *            The user that started the session
     * @return A vector of RecordSessionManagers
     */
    public Vector<RecordSessionManager> getSessions(String owner) {
        Vector<RecordSessionManager> sessions = new Vector<RecordSessionManager>();
        Iterator<Integer> iterator = recordSessionMap.keySet().iterator();
        while (iterator.hasNext()) {
            RecordSessionManager recordSession =
                (RecordSessionManager) recordSessionMap.get(iterator.next());
            if (recordSession.getOwner().equals(owner)) {
                sessions.add(recordSession);
            }
        }
        return sessions;
    }

    /**
     * Sets the venue of a record session to leave when the recording stops
     * @param id The id of the session
     * @param venue The venue
     * @param connectionId The connection id to the venue
     */
    public void setVenue(int id, String venue, String connectionId) {
        RecordSessionManager manager = (RecordSessionManager)
            recordSessionMap.get(new Integer(id));
        if (manager != null) {
            manager.setAG3Venue(venue, connectionId);
        }
    }

    /**
     * Handles an RTSP NEWSTREAMS request
     * @param newStreamsRequest The request
     * @throws RTSPResponse
     */
    public void handleNewStreams(RTSPNewStreamsRequest newStreamsRequest)
            throws RTSPResponse {
        // Get the session ID
        String session = newStreamsRequest.getRequestPacket().getSession();
        int sessionId;
        RecordSessionManager recordManager;

        // If the session ID is available, get the session
        if (session != null) {
            sessionId = Integer.valueOf(session).intValue();
            recordManager = (RecordSessionManager) recordSessionMap
                    .get(new Integer(sessionId));

            // If the session is valid, tear it down
            if (recordManager != null) {

                // If the session ID is valid, but is for a different user,
                // throw an error
                if (!recordManager.getOwner().equals(
                        newStreamsRequest.getRequestPacket().getUsername())) {
                    throw new RTSPResponse(Headers.RTSP_FORBIDDEN,
                            NOT_OWNER_RESPONSE,
                            newStreamsRequest.getRequestPacket());
                }

                recordManager.handleNewStreams(newStreamsRequest);
            } else {

                // If the session is not valid, throw an error
                throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                        NOT_FOUND_RESPONSE,
                        newStreamsRequest.getRequestPacket());
            }
        } else {

            // If the session ID is not available, throw an error
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND,
                    NOT_FOUND_RESPONSE, newStreamsRequest.getRequestPacket());
        }
    }
}
