/*
 * @(#)RecordSessionManager.java
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

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ag3.ClientUpdateThread;
import ag3.interfaces.Venue;

import common.Headers;

/**
 * Manages the setup of a recording session
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RecordSessionManager {

    /**
     * Return status failed
     */
    public static final int FAILURE = 0;

    /**
     * Return status succeeded
     */
    public static final int SUCCESS = 1;

    // The slash character
    private static final String SLASH = System.getProperty("file.separator");

    // The response sent when everything is OK
    private static final String OK_RESPONSE = "OK";

    // The error message for the log
    private static final String ERROR_MESSAGE = "Error";

    // The log file
    private static Log logger =
        LogFactory.getLog(RecordSessionManager.class.getName());

    // The id of the session
    private int sessionId = 0;

    // A map of listeners to ssrcs
    private HashMap<String,RecordListener> listenMap = 
    	new HashMap<String,RecordListener>();

    // The object that saves streams to disk
    private RecordArchiveManager archiveMgr = null;

    // The owner of the session
    private String owner = "";

    // The password of the owner
    private String password = "";

    // True if the directory has not been made
    private boolean qNeedToMkdir = true;

    // The directory containing the files
    private String directory = "";

    // The session being recorded
    private Session session = null;

    // The AG3 venue address if using a venue server
    private String ag3Venue = null;

    // The AG3 connection id
    private String connectionId = null;

    // The AG3 client update thread
    private ClientUpdateThread ag3Updater = null;

    /**
     * Creates a new RecordSessionManager
     *
     * @param id
     *            The id of the session
     * @param request
     *            The request to set up the session
     * @param sFilePath The location of recorded streams
     * @param session The session
     */
    public RecordSessionManager(int id, RTSPSetupRequest request,
            String sFilePath, Session session) {
        this.session = session;
        this.sessionId = id;
        this.owner = request.getRequestPacket().getUsername();
        this.password = request.getRequestPacket().getPassword();

        // Get the path where files are to be stored
        directory = sFilePath + SLASH + session.getId();
        logger.debug("Record_Session_Manager::Record_Session_Manager: got "
                + "path " + directory);

        // Setup the archive manager
        archiveMgr = new RecordArchiveManager(directory, session, owner,
                request.getRequestPacket().getPassword());
    }

    /**
     * Creates a new RecordSessionManager
     *
     * @param id
     *            The id of the session
     * @param owner The owner of the session
     * @param password The password of the owner
     * @param sFilePath The path where recordings are stored
     * @param session The session
     */
    public RecordSessionManager(int id,
            String owner, String password, String sFilePath,
            Session session) {
        String slash = SLASH;
        this.session = session;
        this.sessionId = id;
        this.owner = owner;

        // Get the path where files are to be stored
        directory = sFilePath + slash + session.getId();
        logger.debug("Record_Session_Manager::Record_Session_Manager: "
                        + "got path " + directory);

        // Setup the archive manager
        archiveMgr = new RecordArchiveManager(directory,
                session, owner, password);
    }

    /**
     * Sets the AG3 venue in use
     * @param ag3Venue The AG3 venue url
     * @param connectionId The connection id of the connection
     */
    public void setAG3Venue(String ag3Venue, String connectionId) {
        this.ag3Venue = ag3Venue;
        this.connectionId = connectionId;
        try {
            this.ag3Updater = new ClientUpdateThread(new Venue(ag3Venue),
                    connectionId);
        } catch (MalformedURLException e) {
            logger.error(ERROR_MESSAGE, e);
        }
    }

    /**
     * Adds a listener to the recording
     *
     * @param listenRequest
     *            The request to listen
     * @throws RTSPResponse
     */
    public void addListener(RTSPListenRequest listenRequest)
            throws RTSPResponse {
        archiveMgr.addListener(listenRequest);
    }


    /**
     * Add a stream to the recording
     *
     * @param streamId The id of the stream to add
     * @param netTrans The transport to use for the stream
     */
    public void addSender(String streamId, RecordNetworkTransport netTrans) {
        RecordListener listener = new RecordListener(netTrans, archiveMgr);
        logger.debug("Record_Session_Manager: Adding listener for setup");
        logger.debug("Record_Session_Manager::addSender streamId " + streamId);
        listenMap.put(streamId, listener);
    }

    /**
     * Add a stream to the recording. Note that stream in this sense is a
     * logical stream, matching a particular network host/port pair. Each
     * logical stream may generate many actual RTP streams, one for each sender
     * on that channel (in the multicast case). We create a Record_Streamer to
     * sink each of the logical streams.
     *
     * The streamer is responsible for opening up the destination file on the
     * media filesystem (it makes db lookups to find the path it should use).
     *
     * @param setupRequest
     *            The request to setup the recording
     * @param netTrans
     *            The transport used to recieve messages
     * @return The status of the request
     */
    public int addSender(RTSPSetupRequest setupRequest,
            RecordNetworkTransport netTrans) {
        int errCode = FAILURE;
        RTSPRequestPacket request = setupRequest.getRequestPacket();
        RTSPResponse resp =
            new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE, request);

        String streamId = setupRequest.getStreamId();
        addSender(streamId, netTrans);

        resp.setHeader(Headers.RTSP_SESSION, String.valueOf(sessionId));
        resp.send();

        errCode = SUCCESS;
        return errCode;
    }

    /**
     * Creates the data directory
     *
     * @return true if the data directory exists
     */
    public boolean createDataDir() {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (qNeedToMkdir && !dir.exists()) {
            logger.error("Record_Archive_Manager::createDataDir:"
                            + "mkdir failed: " + directory);
        } else {
            qNeedToMkdir = false;
        }
        return (!qNeedToMkdir);
    }

    /**
     * Returns true if recording is taking place
     * @return True if the manager is recording
     */
    public boolean isRecording() {
        return archiveMgr.isRecording();
    }

    // Leaves the current AG3 venue
    private void leaveVenue() {
        if ((ag3Venue != null) && (connectionId != null)) {
            try {
                Venue venue = new Venue(ag3Venue);
                venue.exit(connectionId);
            } catch (Exception e) {
                logger.error(ERROR_MESSAGE, e);
            }
            ag3Updater.close();
            ag3Updater = null;
        }
    }

    /**
     * Starts the recording
     *
     * @param streamId
     *            The id of the stream to start, or null for all
     */
    public void record(String streamId) {

        Iterator<RecordListener> iter = listenMap.values().iterator();

        // Start the archive manager
        archiveMgr.record(streamId);

        // Start each of the listeners
        while (iter.hasNext()) {
            RecordListener listener = iter.next();
            if (!listener.isRecording()) {
                listener.record();
            }
        }
    }

    /**
     * Stops recording all streams
     *
     * @param transMan
     *            The transport used to receive the streams
     */
    public void teardown(TransportManager transMan) {
        Iterator<String> iterator = listenMap.keySet().iterator();
        while (iterator.hasNext()) {
            RecordListener listener =
                (RecordListener) listenMap.get(iterator.next());
            listener.teardown(transMan);
        }
        listenMap.clear();
        archiveMgr.terminate();
        leaveVenue();
    }

    /**
     * Stops the recording
     *
     * @param teardownRequest
     *            The request to stop the recording
     * @param transMan
     *            The transport used to recieve the stream
     */
    public void teardown(RTSPTeardownRequest teardownRequest,
            TransportManager transMan) {
        RecordListener listener;
        String streamId = teardownRequest.getStreamId();
        RTSPResponse response = new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE,
                teardownRequest.getRequestPacket());

        // We've been passed the teardown message
        logger.debug("Record_Session_Manager::teardown: tearing down stream "
                + streamId);

        // If the streamId is null, teardown all the streams
        if (streamId == null) {
            teardown(transMan);
        } else {

            listener = (RecordListener) listenMap.get(streamId);

            if (listener != null) {

                logger.debug("Record_Session_Manager::teardown: "
                                + "GOOD TEARDOWN for Listener!\n");

                // Pass the teardown request to the guy listening on the socket
                listener.teardown(transMan);

                // Clean up time
                listenMap.remove(streamId);

                // If the last listener has been removed, remove the archiver
                if (listenMap.size() == 0) {

                    // Then shut down the file writers
                    archiveMgr.terminate();
                    leaveVenue();
                }
            } else {
                archiveMgr.teardown(streamId);
            }
        }

        response.send();
    }

    /**
     * Returns the record id of the session
     *
     * @return The id of the session
     */
    public int getSessionId() {
        return sessionId;
    }

    /**
     * Returns the database session
     * @return The session being recorded
     */
    public Session getSession() {
        return session;
    }

    /**
     * Returns the number of streams currently being recorded
     * @return The number of streams being recorded
     */
    public int getNumListeners() {
        return listenMap.size();
    }

    /**
     * Returns the owner of this session
     * @return The username of the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns the password of the owner of the session
     * @return The password of the owner
     */
    public String getPassword() {
        return password;
    }

    /**
     * Handles the RTSP NEWSTREAMS request
     * @param newStreamsRequest The request
     */
    public void handleNewStreams(RTSPNewStreamsRequest newStreamsRequest) {
        archiveMgr.handleNewStreamsRequest(newStreamsRequest);
    }
}