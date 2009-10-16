/*
 * @(#)Session.java
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;
import common.SDPParser;

/**
 * Represents a session in Arena
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Session {
	
	/**
     * The dublin-core ontology
     */
    public static final String DC = "http://purl.org/dc/elements/1.1/";
	/**
     * The CoAKTing portal ontology
     */
    public static final String PORTAL = 
        "http://www.aktors.org/ontology/portal#";
    /**
     * The CoAKTing meeting onology
     */
    public static final String MEETING = 
        "http://www.aktors.org/coakting/ontology/meeting-20040304-1#";
    /**
     * The CoAKTing support ontology
     */
    public static final String SUPPORT = 
        "http://www.aktors.org/ontology/support#";

    /**
     * The session type field
     */
    public static final String TYPE =
        MEETING + "Distributed-Gathering";

    /**
     * The session identifier field
     */
    public static final String ID = NetworkEndpoint.MEMETIC + "has-session-id";

    /**
     * The old session name
     */
    public static final String OLD_NAME =
        SUPPORT + "has-pretty-name";

    /**
     * The session name field
     */
    public static final String NAME = DC + "title";

    /**
     * The session description field
     */
    public static final String DESCRIPTION = DC + "description";

    /**
     * The session owner field
     */
    public static final String OWNER = NetworkEndpoint.MEMETIC + "has-owner";

    /**
     * The session duration field
     */
    public static final String END_TIME =
    	NetworkEndpoint.MEMETIC + "has-media-end-time";

    /**
     * The metadata start time of the session field
     */
    public static final String METADATA_END_TIME =
    	NetworkEndpoint.MEMETIC + "has-abs-end-time";

    /**
     * The session stream field
     */
    public static final String STREAM = NetworkEndpoint.MEMETIC + "has-stream";

    /**
     * The regular expression to recognise a permission
     */
    public static final String PERMISSION_REGEX = "/.*can-be-.*-by/i";

    /**
     * The session play permission field
     */
    public static final String PLAY_PERMISSION =
    	NetworkEndpoint.MEMETIC + "can-be-played-by";

    /**
     * The session record permission field
     */
    public static final String RECORD_PERMISSION =
    	NetworkEndpoint.MEMETIC + "can-be-recorded-by";

    /**
     * The session alter permission field
     */
    public static final String ALTER_PERMISSION =
    	NetworkEndpoint.MEMETIC + "can-be-altered-by";

    /**
     * The session delete permission field
     */
    public static final String DELETE_PERMISSION =
    	NetworkEndpoint.MEMETIC + "can-be-deleted-by";

    /**
     * The start time of the session field
     */
    public static final String START_TIME =
    	NetworkEndpoint.MEMETIC + "has-media-start-time";

    /**
     * The metadata start time of the session field
     */
    public static final String METADATA_START_TIME =
    	NetworkEndpoint.MEMETIC + "has-abs-start-time";

    /**
     * An address that is being used for timer recording
     */
    public static final String MEETING_ADDRESS =
    	NetworkEndpoint.MEMETIC + "has-venue-address";

    /**
     * The venue where the meeting is to be held
     */
    public static final String MEETING_VENUE =
    	NetworkEndpoint.MEMETIC + "has-venue";

    /**
     * The venue server where the venue was chosen from
     */
    public static final String MEETING_VENUE_SERVER =
    	NetworkEndpoint.MEMETIC + "has-venue-server";

    /**
     * The username of the user that set up the timer recording
     */
    public static final String TIMER_RECORD_USER =
    	NetworkEndpoint.MEMETIC + "has-timer-record-user";

    /**
     * The password of the user that set up the timer recording
     */
    public static final String TIMER_RECORD_PASS =
    	NetworkEndpoint.MEMETIC + "has-timer-record-password";

    /**
     * The URL of the server on which the session is held
     */
    public static final String URL = NetworkEndpoint.MEMETIC + "has-server-url";

    /**
     * Represents a file stored for the session (e.g. a compendium upload)
     */
    public static final String HAS_FILE = NetworkEndpoint.MEMETIC + "has-file";

    /**
     * Represents an AG meeting room booked for this meeting
     */
    public static final String LOCAL_EVENT =
        MEETING + "has-local-event";

    /**
     * Represents a sub-event
     */
    public static final String SUB_EVENT = PORTAL
        + "has-sub-event";

    /**
     * Represents a resource associated with the session
     */
    public static final String RESOURCE =
    	NetworkEndpoint.MEMETIC + "has-relevant-resource";

    /**
     * Represents if a meeting has a compendium transcription
     */
    public static final String TRANSCRIPTION =
        MEETING + "has-transcription";

    /**
     * Represents a compendium event
     */
    public static final String COMPENDIUM_EVENT = NetworkEndpoint.MEMETIC
        + "Compendium-Event";
    
    /** 
     * The namespace separator
     */
    public static final String NAMESPACE_SEP = "_";

//    // The separator between the names of the sender
//    private static final String SENDER_NAME_SEP = ":";

    // The number of ms in a second
    private static final int MS_PER_SEC = 1000;

    // A Space
    private static final String SPACE = " ";

//    // The text to put in place of an unknown site
//    private static final String UNKNOWN_SITE = "Unknown Site";

    // The message to put in the log for an error
    private static final String ERROR_MESSAGE = "Error";

    // Response when the stream is not found
    private static final String NOT_FOUND_RESPONSE = "Stream not found";

    // The media port used in the description
    private static final String DESCRIPTION_MEDIA_PORT = String.valueOf(0);

    // The address used in the description
    private static final String DESCRIPTION_ADDRESS = "127.0.0.1";

    // The owner used in the description
    private static final String DESCRIPTION_OWNER = "arena";

    // The version of the description
    private static final String DESCRIPTION_VERSION = String.valueOf(0);

    // The response when everything is fine
    private static final String OK_RESPONSE = "OK";

//    // The default value of a time when non is specified
//    private static final String DEFAULT_TIME = String.valueOf(0);
//
//    // The default name of the owner of the session
//    private static final String DEFAULT_NAME = "unknown user";

    // The log file
    private static Log logger = LogFactory.getLog(Session.class.getName());

    // The id of the session
    private String id = "";

    // The streams in the session
    private HashMap<String,Stream> streamMap = new HashMap<String,Stream>();

    // The name of the session
    private String name = "";

    // The session description
    private String description = "";

    // The end time of the session
    private long endTime = 0;

    // The database the session is loaded from
//    private Database db = null;

    // The id of the current record session if available
    private String recordSessionId = null;

    // True if the session is being imported
    private boolean isImporting = false;

    // The start time of the session
    private long startTime = 0;

    // The url of the session
    private String sessionURL = "";

    // The start time of the session requested
    private long metadataStartTime = 0;

    // The end time of the session requested
    private long metadataEndTime = 0;

    // The map of participant uris to site uris
    private HashMap<String,String> participantsURI = null;

    // A map of sites to participants
//    private HashMap participantsBySite = null;

    // A list of the sites in alphabetical order
//    private Vector<String> sitesOrdered = null;

//    // The events of the session
//    private HashMap<String,HashMap<String,Object>> events = null;
//
//    // The agenda items
//    private HashMap agenda = null;
//
//    // The documents
//    private HashMap documents = null;

    // The venue of the meeting
    private String venue = null;

    // The venue server of the meeting venue
    private String venueServer = null;

    // The creator of the meeting
    private String creator = null;

    // The uri of the session in the database
    private String uri = null;

    // The files of the session
//    private Vector files = null;

    // The username currently changing the session
    private String username = null;

//    // The password of the user currently changing the session
//    private String password = null;
//
//    // The uri of the last screen to be uploaded
//    private String lastScreen = null;

//    /**
//     * Creates a new session ready for recording
//     * @param sm The session manager of the session
//     * @param db The database that will hold the session
//     * @param id The id of the session, or null for none
//     * @param username The user adding the session
//     * @param password The password of the user
//     * @throws MalformedURLException
//     */
//    public Session(SessionManager sm, Database db, String id, String username,
//            String password) throws MalformedURLException {
//        this.db = db;
//        this.uri = sm.allocateRecordSession(id, username, password);
//        setLock(username);
//        setPassword(password);
//        //loadFromDb();
//        getUnlock();
//    }

    /**
     * Creates a new Session
     *
     * @param uri
     *            The uri of the session
     * @param db
     *            The database to load from
     */
//    public Session(String uri, Database db) {
//        this.uri = uri;
//        this.db = db;
//    }    
    public Session(String uri) {
        this.uri = uri;
    }

//    /**
//     * Creates a new Arena session using a database for the fields
//     *
//     * @param uri
//     *            The uri to fill in the fields with
//     * @param db The database to load
//     * @param user The user loading the session
//     * @param pass The password of the user
//     * @return A new Arena Session
//     */
//    public static Session loadFromDB(String uri, Database db, String user,
//            String pass) {
//
//        // Create the session to return
//        Session session = new Session(uri, db);
//
//        // Load the session
//        session.setLock(user);
//        session.setPassword(pass);
//        //session.loadFromDb();
//        session.getUnlock();
//
//        // Return the created session
//        return session;
//    }

    /**
     * Sets the username that is currently accessing the session
     * Only one user can access the session at a time, so blocks until released
     * Same user will not block themselves
     * @param username The username that will access the session
     */
    public void setLock(String username) {
        if (this.username == username) {
            logger.debug("Session " + id + " already locked for "
                    + username);
            return;
        }
        synchronized (this) {
            while (this.username != null) {
                try {
                    logger.debug("Waiting for " + this.username
                            + " to unlock session " + id);
                    wait();
                } catch (InterruptedException e) {

                    // Do Nothing
                }
            }
            this.username = username;
            logger.debug(username + " is locking " + id);
        }
    }

    /**
     * Sets the password of the user that is currently accessing the session
     * @param password The password of the user
     */
    public void setPassword(String password) {
        if (username == null) {
            logger.error(ERROR_MESSAGE,
                    new Exception("Password set without username"));
        }
//        this.password = password;
    }

    /**
     * Unlocks the session
     * @return null because it has to (java beans)
     */
    public String getUnlock() {
        synchronized (this) {
            logger.debug(username + " is unlocking " + id);
            username = null;
//            password = null;
            notify();
        }
        return null;
    }



//    /**
//     * Loads this session from a database
//     */
//    public void loadFromDb() {
//
//        // Set the session details from the database
//        HashMap details = db.getUtils().getPropertiesOfUri(uri, true, username,
//                password);
//        String end = (String) details.get(END_TIME);
//        String start = (String) details.get(START_TIME);
//        HashMap sites = db.getSites(username, password);
//        HashMap users = db.getAllUsers(username, password);
//        boolean transcribed = isTranscribed();
//        HashMap streams = getDBStreams();
//        String owner = (String) details.get(OWNER);
//        HashMap userDetails = db.getUser(db.getUserName(owner), username,
//                password);
//        String firstName = (String) userDetails.get(Person.FIRSTNAME);
//        String lastName = (String) userDetails.get(Person.SURNAME);
//
//        name = (String) details.get(NAME);
//        description = (String) details.get(DESCRIPTION);
//        if (end == null) {
//            end = DEFAULT_TIME;
//        }
//        endTime = Long.parseLong(end);
//        if (start == null) {
//            start = DEFAULT_TIME;
//        }
//        startTime = Long.parseLong(start);
//        sessionURL = (String) details.get(URL);
//        id = (String) details.get(ID);
//        start = (String) details.get(METADATA_START_TIME);
//        if (start == null) {
//            start = DEFAULT_TIME;
//        }
//        metadataStartTime = Long.parseLong(start);
//        end = (String) details.get(METADATA_END_TIME);
//        if (end == null) {
//            end = DEFAULT_TIME;
//        }
//        metadataEndTime = Long.parseLong(end);
//
//        participantsURI = new HashMap<String,String>();
//        participantsBySite = new HashMap<String,Vector>();
//        sitesOrdered = new Vector<String>();
//        this.events = getSessionLocalEvents();
//        Vector<HashMap<String,Object>> events = 
//        	new Vector<HashMap<String,Object>>(this.events.values());
//        for (int i = 0; i < events.size(); i++) {
//            HashMap<String,Object> event = (HashMap<String,Object>) events.get(i);
//            String siteURI = (String) event.get(LocalEvent.LOCATION);
//            String site = (String) sites.get(siteURI);
//            Vector<Object> parts = (Vector<Object>) event.get(LocalEvent.ATTENDEE);
//            Vector partsAtSite = (Vector) participantsBySite.get(site);
//            if (partsAtSite == null) {
//                partsAtSite = new Vector();
//            }
//            if ((site != null) && !sitesOrdered.contains(site)) {
//                sitesOrdered.add(site);
//            } else if (!sitesOrdered.contains(site)) {
//                sitesOrdered.add(UNKNOWN_SITE);
//            } else {
//                logger.debug("Duplicate site " + site);
//            }
//            for (int j = 0; j < parts.size(); j++) {
//                String part = (String) parts.get(j);
//                participantsURI.put(part, siteURI);
//                String[] name = (String[]) users.get(part);
//                if (!partsAtSite.contains(name)) {
//                    partsAtSite.add(name);
//                }
//            }
//            Collections.sort(partsAtSite, new ArrayComparator(1));
//            participantsBySite.put(site, partsAtSite);
//        }
//        Collections.sort(sitesOrdered);
//
//        if (!transcribed) {
//            agenda = getDBAgendaItems();
//            documents = getDBDocuments();
//        }
//
//        venue = (String) details.get(MEETING_VENUE);
//        if (venue == null) {
//            venue = "";
//        }
//        venueServer = (String) details.get(MEETING_VENUE_SERVER);
//        if (venueServer == null) {
//            venueServer = "";
//        }
//
//        // Get the streams from the database and add them to the session
//        if (streams != null) {
//            Iterator iterator = streams.keySet().iterator();
//            streamMap.clear();
//            while (iterator.hasNext()) {
//                String uri = (String) iterator.next();
//                HashMap values = (HashMap) streams.get(uri);
//                addStream(new Stream(id, values, uri));
//            }
//        }
//
//        creator = DEFAULT_NAME;
//        if ((firstName != null) && (lastName != null)) {
//            creator = firstName + SPACE + lastName;
//        }
//
//        files = getFilesDB();
//        Collections.sort(files, new UploadedFileSorter());
//    }
//
    /*
     * Adds a stream to the session
     * @param stream The stream to add
     */
    private void addStream(Stream stream) {
        if (stream.getSsrc() != null) {
            logger.debug("Add stream " + stream.getSessionId() + SPACE
                    + stream.getSsrc());
            streamMap.put(stream.getSsrc(), stream);
        }
    }

    /**
     * Returns the number of streams in the session
     *
     * @return the number of streams
     */
    public int countStreams() {
        return streamMap.size();
    }

    /**
     * Handles an RTSP DESCRIBE request
     *
     * @param describeRequest
     *            The request to handle
     */
    public void handleDescribeSession(RTSPDescribeRequest describeRequest) {
        Iterator<String> iter = streamMap.keySet().iterator();

        // Get the request details
        RTSPRequestPacket request = describeRequest.getRequestPacket();

        // Create a response
        RTSPResponse response =
            new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE, request);
        response.setHeader(Headers.RTSP_CONTENT_TYPE,
                Headers.RTSP_CONTENT_TYPE_SDP);
        response.bodyAppend(SDPParser.VERSION + DESCRIPTION_VERSION
                + SDPParser.EOL);
        response.bodyAppend(SDPParser.OWNER + DESCRIPTION_OWNER
                + SDPParser.SDP_SEPARATOR + id + SDPParser.SDP_SEPARATOR
                + Calendar.getInstance().getTimeInMillis()
                + SDPParser.SDP_SEPARATOR + SDPParser.INTERNET
                + SDPParser.SDP_SEPARATOR + SDPParser.IPV4_ADDRESS
                + SDPParser.SDP_SEPARATOR + DESCRIPTION_ADDRESS
                + SDPParser.EOL);
        response.bodyAppend(SDPParser.NAME + name + SDPParser.EOL);
        response.bodyAppend(SDPParser.DESCRIPTION + description
                + SDPParser.EOL);
        response.bodyAppend(SDPParser.TIME + (startTime / MS_PER_SEC)
                + SDPParser.SDP_SEPARATOR + (endTime / MS_PER_SEC)
                + SDPParser.EOL);

        logger.debug("arena session gets describeSession " + request.getUri()
                + SDPParser.SDP_SEPARATOR + request.getPath()
                + SDPParser.SDP_SEPARATOR + request.getSequence());

        while (iter.hasNext()) {
            String name = (String) iter.next();
            Stream stream = (Stream) streamMap.get(name);
            response.bodyAppend(SDPParser.MEDIA + stream.getDataType()
                    + SDPParser.SDP_SEPARATOR + DESCRIPTION_MEDIA_PORT
                    + SDPParser.SDP_SEPARATOR + SDPParser.MEDIA_RTP_AVP
                    + SDPParser.SDP_SEPARATOR + stream.getPacketTypes()
                    + SDPParser.EOL);
            response.bodyAppend(SDPParser.CONNECTION + SDPParser.INTERNET
                    + SDPParser.SDP_SEPARATOR + SDPParser.CONNECTION_URL
                    + SDPParser.SDP_SEPARATOR + request.getUri()
                    + SDPParser.URL_PATH_SEPARATOR + stream.getSsrc()
                    + SDPParser.EOL);
            response.bodyAppend(SDPParser.ATTRIBUTE
                    + SDPParser.ATTRIBUTE_CONTROL + request.getUri()
                    + SDPParser.URL_PATH_SEPARATOR + stream.getSsrc()
                    + SDPParser.EOL);
        }

        // If the session is being recorded, add the session id
        if (isRecording()) {
            response.setHeader(Headers.RTSP_SESSION, recordSessionId);
        }

        // Send the response
        response.send();
    }

    /**
     * Handles an RTSP DESCRIBE request on a specific stream
     *
     * @param describeRequest
     *            The request to handle
     * @param ssrc
     *            The stream identifier to describe
     * @throws RTSPResponse
     */
    public void handleDescribeStream(RTSPDescribeRequest describeRequest,
            String ssrc) throws RTSPResponse {

        // Get the request details
        RTSPRequestPacket request = describeRequest.getRequestPacket();

        // Get the stream to describe
        Stream stream = (Stream) streamMap.get(ssrc);
        logger.debug("arena session gets describeStream '" + ssrc + "'");

        // If the stream exists, describe it
        if (stream != null) {
            stream.handleDescribeStream(describeRequest);
        } else {

            // If the stream does not exist, return an error
            throw new RTSPResponse(Headers.RTSP_NOT_FOUND, NOT_FOUND_RESPONSE,
                    request);
        }
    }

    /**
     * Returns the id of the session
     * @return The id of the session
     */
    public String getId() {
        return id;
    }

//    /**
//     * Sets the name of the session
//     * @param name The new name
//     * @throws MalformedURLException
//     */
//    public void setName(String name) throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionName = model.createProperty(Session.NAME);
//        node.addProperty(sessionName, name);
//        db.jenaReplace(model, username, password);
//        this.name = name;
//    }

//    /**
//     * Sets the description of the session
//     *
//     * @param description
//     *            The description to give the session
//     * @throws MalformedURLException
//     */
//    public void setDescription(String description)
//            throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionDescription =
//            model.createProperty(Session.DESCRIPTION);
//        node.addProperty(sessionDescription, description);
//        db.jenaReplace(model, username, password);
//        this.description = description;
//    }

    /**
     * Sets the start time of the session
     *
     * @param startTime
     *            The start time to set in ms
     * @throws MalformedURLException
     */
//    public void setStartTime(long startTime) throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionStart = model.createProperty(Session.START_TIME);
//        node.addProperty(sessionStart, startTime);
//        db.jenaReplace(model, username, password);
//        this.startTime = startTime;
//    }
//
//    /**
//     * Sets the end time of the session
//     * @param endTime The end time of the session
//     * @throws MalformedURLException
//     */
//    public void setEndTime(long endTime) throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionEnd = model.createProperty(Session.END_TIME);
//        node.addProperty(sessionEnd, endTime);
//        db.jenaReplace(model, username, password);
//        this.endTime = endTime;
//    }

//    /**
//     * Sets the start time of the session
//     *
//     * @param startTime
//     *            The start time to set
//     * @throws MalformedURLException
//     */
//    public void setMetadataStartTime(long startTime)
//            throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionStart =
//            model.createProperty(Session.METADATA_START_TIME);
//        node.addProperty(sessionStart, startTime);
//        db.jenaReplace(model, username, password);
//        this.metadataStartTime = startTime;
//    }
//
//    /**
//     * Sets the end time of a session
//     *
//     * @param endTime
//     *            The end time to set
//     * @throws MalformedURLException
//     */
//    public void setMetadataEndTime(long endTime) throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionEnd =
//            model.createProperty(Session.METADATA_END_TIME);
//        node.addProperty(sessionEnd, endTime);
//        db.jenaReplace(model, username, password);
//        this.metadataEndTime = endTime;
//    }

//    /**
//     * Sets the venue of a session
//     * @param venue The uri of the venue
//     * @throws MalformedURLException
//     */
//    public void setVenue(String venue) throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionVenue = model.createProperty(Session.MEETING_VENUE);
//        node.addProperty(sessionVenue, venue);
//        db.jenaReplace(model, username, password);
//        this.venue = venue;
//    }

//    /**
//     * Sets the venue server of a session's venue
//     * @param venueServer The uri of the venue server (or "" for none)
//     * @throws MalformedURLException
//     */
//    public void setVenueServer(String venueServer)
//            throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionVenueServer = model.createProperty(
//                Session.MEETING_VENUE_SERVER);
//        node.addProperty(sessionVenueServer, venueServer);
//        db.jenaReplace(model, username, password);
//        this.venueServer = venueServer;
//    }

    /**
     * Sets the recording status of the session
     * @param id The record id if recording or null if stopping
     */
    public void setRecordSessionId(String id) {
        recordSessionId = id;
        if (id == null) {
            try {
                calculateTimes();
            } catch (MalformedURLException e) {
                logger.error(ERROR_MESSAGE, e);
            }
        }
    }

    /**
     * Returns the name of the session
     *
     * @return The name of the session
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the session
     *
     * @return The description of the session
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the end time of the session
     *
     * @return The end time of the session
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Stops the session from recording
     */
    public void stopRecording() {
        recordSessionId = null;
        //loadFromDb();
    }

    /**
     * Returns true if the session is recording
     *
     * @return True if the session is being recorded
     */
    public boolean isRecording() {
        return (recordSessionId != null);
    }

    /**
     * Returns the current recording session id or null if not recording
     *
     * @return the record session id or null if not recording
     */
    public String getRecordSessionId() {
        return recordSessionId;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
    }

    /**
     * Returns the streams in the session
     *
     * @return the stream objects
     */
    public Vector<Stream> getStreams() {
        return new Vector<Stream>(streamMap.values());
    }

    /**
     * Returns the stream with the given id
     * @param id The id of the stream
     * @return The stream object
     */
    public Stream getStream(String id) {
        return (Stream) streamMap.get(id);
    }

    /**
     * Returns the start time of the session
     *
     * @return the start time of the session
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Calculates the correct times of the session
     * @throws MalformedURLException
     *
     */
    public void calculateTimes()
            throws MalformedURLException {
        long startTime = -1;
        long endTime = 0;
        Iterator<String> iterator = streamMap.keySet().iterator();
        while (iterator.hasNext()) {
            String strId = (String) iterator.next();
            Stream stream = (Stream) streamMap.get(strId);
            logger.debug("Stream " + stream.getSsrc()
                    + " has start time " + stream.getStartTime());
            if ((stream.getStartTime() < startTime) || (startTime == -1)) {
                startTime = stream.getStartTime();
            }
            if (stream.getEndTime() > endTime) {
                endTime = stream.getEndTime();
            }
        }
//        setEndTime(endTime);
//        setStartTime(startTime);
    }

    /**
     * Deletes a stream from the session
     *
     * @param streamId
     *            The id of the stream to delete
     * @throws MalformedURLException
     */
//    public void deleteStream(String streamId) throws MalformedURLException {
//        permDeleteStream(streamId);
//        if (isRecording()) {
//            getStream(streamId).reset();
//        } else {
//            streamMap.remove(streamId);
//        }
//        calculateTimes();
//    }

    /**
     * Returns the URL of the server on which the session is held
     *
     * @return the url of the server
     */
    public String getURL() {
        return sessionURL;
    }

    /**
     * Sets the import status of the session
     * @param isImporting True if the session is importing
     */
    public void setImporting(boolean isImporting) {
        this.isImporting = isImporting;
    }

    /**
     * Gets the import status of the session
     *
     * @return true if the session is being imported
     */
    public boolean isImporting() {
        return this.isImporting;
    }

//   /**
//     * Gets the participants ordered by site then last name
//     *
//     * @return a vector of arrays of first name, last name, site
//     */
//    public Vector<String[]> getParticipantsSites() {
//        Vector<String[]> people = new Vector<String[]>();
//        for (int i = 0; i < sitesOrdered.size(); i++) {
//            String site = (String) sitesOrdered.get(i);
//            Vector parts = (Vector) participantsBySite.get(site);
//            if (parts != null) {
//                for (int j = 0; j < parts.size(); j++) {
//                    String[] name = (String[]) parts.get(j);
//                    String[] line = new String[]{name[0], name[1], site};
//                    people.add(line);
//                }
//            } else {
//                logger.debug("No participants found at site " + site);
//            }
//        }
//        return people;
//    }

    /**
     * Returns a map of participant to site
     *
     * @return a map of partitipant uri to site uri
     */
    public HashMap<String, String> getParticipantsSitesURI() {
        return participantsURI;
    }

//    /**
//     * Gets the agenda item titles in order
//     *
//     * @return a vector of strings
//     */
//    public Vector getAgendaItems() {
//        Vector agenda = null;
//        if (isTranscribed()) {
//            agenda = getCompendiumAgendaItems();
//        } else {
//            agenda = new Vector(this.agenda.values());
//        }
//        Collections.sort(agenda, new AgendaItemSorter());
//        Vector agendaItems = new Vector();
//        for (int i = 0; i < agenda.size(); i++) {
//            HashMap item = (HashMap) agenda.get(i);
//            agendaItems.add(item.get(AgendaItem.LABEL));
//        }
//        return agendaItems;
//    }

//    /**
//     * Gets the documents
//     *
//     * @return a vector of string arrays of doc name, doc url
//     */
//    public Vector getDocuments() {
//        Vector documents = null;
//        String nameProperty = null;
//        String urlProperty = null;
//        if (isTranscribed()) {
//            documents = getCompendiumDocuments();
//            nameProperty = Node.LABEL;
//            urlProperty = Node.REFERENCE;
//        } else {
//            documents = new Vector(this.documents.values());
//            nameProperty = rtspd.Resource.NAME;
//            urlProperty = rtspd.Resource.URL;
//        }
//        Vector docs = new Vector();
//        for (int i = 0; i < documents.size(); i++) {
//            HashMap doc = (HashMap) documents.get(i);
//            docs.add(new String[]{(String) doc.get(nameProperty),
//                    (String) doc.get(urlProperty)});
//        }
//        return docs;
//    }

    /**
     * Gets the venue
     *
     * @return the venue url
     */
    public String getVenue() {
        return venue;
    }

    /**
     * Gets the venue server of the venue
     * @return The venue server url or "" if none
     */
    public String getVenueServer() {
        return venueServer;
    }

    /**
     * Returns true if the session has been recorded
     *
     * @return true if the session is recorded
     */
    public boolean isRecorded() {
        return (countStreams() > 0) && (endTime != 0) && !isRecording();
    }

    /**
     * Returns true if the session is set up for timer recording
     *
     * @return true if the session is timer recording
     */
    public boolean isTimerRecord() {
        return !isRecorded() && (startTime > 0);
    }

//    /**
//     * Returns true if the session has been transcribed
//     *
//     * @return true if the session has compendium data
//     */
//    public boolean isTranscribed() {
//        final String dataVar = "transcription";
//        QueryResults results = null;
//        String queryString = DatabaseUtils.select(new String[]{dataVar});
//        queryString += DatabaseUtils.lastTriple(DatabaseUtils.uri(uri),
//                                    DatabaseUtils.uri(Session.TRANSCRIPTION),
//                                    DatabaseUtils.var(dataVar));
//        results = db.jenaQuery(queryString, db.getUsername(), db.getPassword());
//        return results.hasNext();
//    }

    /**
     * Returns the reported start time
     *
     * @return the record start time, or if none, the requested start time
     */
    public Date getStart() {
        if (startTime > 0) {
            return new Date(startTime);
        }
        return new Date(metadataStartTime);
    }

    /**
     * Returns the year in which the session starts
     *
     * @return the session start year
     */
    public int getStartYear() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getStart());
        return cal.get(Calendar.YEAR);
    }

    /**
     * Returns the month in which the session starts
     *
     * @return the session start month
     */
    public int getStartMonth() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getStart());
        return cal.get(Calendar.MONTH) + 1;
    }

    /**
     * Returns the day on which the session starts
     *
     * @return the session start day of the month
     */
    public int getStartDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getStart());
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns the hour at which the session starts
     *
     * @return the session start hour
     */
    public int getStartHour() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getStart());
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Returns the minute at which the session starts
     *
     * @return the session start minute
     */
    public int getStartMin() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getStart());
        return cal.get(Calendar.MINUTE);
    }

    /**
     * Returns the report end time
     *
     * @return the record end time or the requested end time if none
     */
    public Date getEnd() {
        if (endTime > 0) {
            return new Date(endTime);
        }
        return new Date(metadataEndTime);
    }

    /**
     * Returns the hour at which the session ends
     *
     * @return the session end hour
     */
    public int getEndHour() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getEnd());
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Returns the minute at which the session ends
     *
     * @return the session end minute
     */
    public int getEndMin() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getEnd());
        return cal.get(Calendar.MINUTE);
    }

    /**
     * Returns the name of the person who made the session
     *
     * @return the username that created the session
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Returns the uri of the session in the database
     *
     * @return the uri of the session
     */
    public String getUri() {
        return uri;
    }
//
//    /**
//     * Returns the files of the session
//     * @return a vector of maps of file properties
//     */
//    public Vector getFiles() {
//        return files;
//    }


    /**
     * Enables or disables the given stream
     * @param streamId The id of the stream
     * @param enabled True if the stream is enabled
     */
    public void setStreamEnabled(String streamId, boolean enabled) {
        Stream stream = (Stream) streamMap.get(streamId);
        if (stream != null) {
            stream.setEnabled(enabled);
        }
    }

//    /**
//     * Adds an address to record a session from for timer recording
//     * @param endPoints A vector of network endpoints being used for the
//     *                  recording
//     * @throws MalformedURLException
//     */
//    public void addSessionRecordAddress(Vector endPoints)
//            throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//
//        Property addressProp =
//            model.createProperty(Session.MEETING_ADDRESS);
//        Property type = model.createProperty(DatabaseUtils.RDF_TYPE);
//        Property host = model.createProperty(NetworkEndpoint.HOST);
//        Property port = model.createProperty(NetworkEndpoint.PORT);
//        Property enctype = model.createProperty(NetworkEndpoint.ENC_TYPE);
//        Property enckey = model.createProperty(NetworkEndpoint.ENC_KEY);
//        Property user = model.createProperty(Session.TIMER_RECORD_USER);
//        Property pass = model.createProperty(Session.TIMER_RECORD_PASS);
//        for (int i = 0; i < endPoints.size(); i++) {
//            NetworkEndpoint ePoint = (NetworkEndpoint) endPoints.get(i);
//            Resource address = model.createResource(DatabaseUtils.NAMESPACE
//                        + NetworkEndpoint.NAMESPACE_ADDR + id + i);
//            String encType = ePoint.getEncryptionType();
//            String encKey = ePoint.getEncryptionKey();
//            address.addProperty(type, NetworkEndpoint.TYPE);
//            address.addProperty(host, ePoint.getHost());
//            address.addProperty(port, ePoint.getPort());
//            if ((encType == null) || (encKey == null)) {
//                encType = "";
//                encKey = "";
//            }
//            address.addProperty(enctype, encType);
//            address.addProperty(enckey, encKey);
//            node.addProperty(addressProp, address);
//        }
//        node.addProperty(user, username);
//        node.addProperty(pass, password);
//
//        db.jenaAdd(model, username, password);
//    }

//    /**
//     * Clears a session recording address
//     * @throws MalformedURLException
//     */
//    public void clearSessionRecordAddress() throws MalformedURLException {
//        HashMap properties = db.getUtils().getPropertiesOfUri(uri, true,
//                username, password);
//        Vector addresses = db.getUtils().getValuesOfProperty(uri,
//                Session.MEETING_ADDRESS, true, username, password);
//        Model removeModel = db.getJenaModel();
//        Resource session = removeModel.createResource(uri);
//        Property user = removeModel.createProperty(Session.TIMER_RECORD_USER);
//        Property pass = removeModel.createProperty(Session.TIMER_RECORD_PASS);
//        String timerUser = (String) properties.get(Session.TIMER_RECORD_USER);
//        String timerPass = (String) properties.get(Session.TIMER_RECORD_PASS);
//        if (timerUser != null) {
//            session.addProperty(user, timerUser);
//        }
//        if (timerPass != null) {
//            session.addProperty(pass, timerPass);
//        }
//        db.jenaRemove(removeModel, username, password);
//        for (int i = 0; i < addresses.size(); i++) {
//            String uri = (String) addresses.get(i);
//            db.getUtils().deleteUriValuesReferences(uri, username, password);
//        }
//    }
//
//    /**
//     * Adds a file to a session
//     * @param file The file to add
//     * @param uploadUser The user that uploaded the file
//     * @param uploadtime The time of the upload
//     * @throws MalformedURLException
//     */
//    public void addFile(String file, String uploadUser, long uploadtime)
//            throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource resource = model.createResource(uri);
//        Property property = model.createProperty(Session.HAS_FILE);
//        Resource fileResource =
//            model.createResource(DatabaseUtils.NAMESPACE
//                    + UploadedFile.NAMESPACE_FILE
//                    + DatabaseUtils.generateId());
//        Property name = model.createProperty(UploadedFile.NAME);
//        Property uploader = model.createProperty(UploadedFile.UPLOADER);
//        Property uploadTime = model.createProperty(UploadedFile.UPLOADTIME);
//        String uploadUserUri = db.getUserURI(uploadUser,
//                username, password);
//        HashMap filedetails = new HashMap();
//        filedetails.put(UploadedFile.NAME, file);
//        fileResource.addProperty(name, file);
//        filedetails.put(UploadedFile.UPLOADER, uploadUserUri);
//        fileResource.addProperty(uploader, model.createResource(uploadUserUri));
//        filedetails.put(UploadedFile.UPLOADTIME, String.valueOf(uploadtime));
//        fileResource.addProperty(uploadTime, uploadtime);
//        resource.addProperty(property, fileResource);
//        files.add(filedetails);
//        db.jenaAdd(model, username, password);
//    }

//    /*
//     * Returns a list of files associated with this session
//     * @param user The user requesting the list
//     * @param pass The password of the user
//     * @return a Vector of filenames
//     */
//    private Vector getFilesDB() {
//        Vector files = new Vector();
//        long start = System.currentTimeMillis();
//        Vector uris = db.getUtils().getValuesOfProperty(uri, HAS_FILE, true,
//                username, password);
//        for (int i = 0; i < uris.size(); i++) {
//            String fileuri = (String) uris.get(i);
//            HashMap properties = db.getUtils().getPropertiesOfUri(fileuri, true,
//                    username, password);
//            files.add(properties);
//        }
//        logger.debug("Finding " + uris.size() + " files took "
//                + (System.currentTimeMillis() - start) + "ms");
//        return files;
//    }

    /**
     * Adds a new stream to a session
     *
     * @param ssrc
     *            The ssrc of the stream
     * @return The stream object added
     * @throws MalformedURLException
     */
    public Stream addStream(String ssrc) throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Resource stream = model.createResource(
//                uri + DatabaseUtils.NAMESPACE_SEP + ssrc);
//        Property type = model.createProperty(DatabaseUtils.RDF_TYPE);
//        Property sessionStream = model.createProperty(Session.STREAM);
//        Property streamSSRC = model.createProperty(Stream.SSRC);
//        stream.addProperty(type, model.createResource(Stream.TYPE));
//        stream.addProperty(streamSSRC, ssrc);
//        node.addProperty(sessionStream, stream);
//        db.jenaAdd(model, username, password);
        Stream realStream = new Stream(id, ssrc,
                uri + NAMESPACE_SEP + ssrc);
        System.out.println("addStream(ssrc) wrong one!!");
        addStream(realStream);
        return realStream;
    }

    /**
     * Sets a metadata value for a stream in a session
     *
     * @param streamId
     *            The stream identifier (ssrc)
     * @param key
     *            The name of the key to set
     * @param value
     *            The value to set the key to
     * @return True if this information replaces old information
     * @throws MalformedURLException
     */
    public boolean setStreamKeyValue(String streamId, String key,
            String value) throws MalformedURLException {
        Stream stream = (Stream) streamMap.get(streamId);
        if (stream != null) {
            boolean hasValue = (stream.getValues().get(key) != null);
            stream.set(key, value);
//            Model model = db.getJenaModel();
//            Resource node = model.createResource(stream.getUri());
//            Property streamKey = model.createProperty(key);
//            node.addProperty(streamKey, value);
//            db.jenaReplace(model, username, password);
            return hasValue;
        }
        logger.debug("Stream " + streamId
                + " not found when setting value");
        return true;
    }

    /*
     * Permanently deletes the stream from the database
     *
     * @param streamId
     *            The stream to delete
     * @throws MalformedURLException
     */
//    private void permDeleteStream(String streamId)
//            throws MalformedURLException {
//        Stream realStream = (Stream) streamMap.get(streamId);
//        Model removeModel = db.getJenaModel();
//        String stream = realStream.getUri();
//        Resource node = removeModel.createResource(stream);
//        HashMap properties = db.getUtils().getPropertiesOfUri(stream, false,
//                username, password);
//        Iterator iterator = properties.keySet().iterator();
//        while (iterator.hasNext()) {
//            String property = (String) iterator.next();
//            RDFNode value = (RDFNode) properties.get(property);
//            Property prop = removeModel.createProperty(property);
//            node.addProperty(prop, value);
//        }
//        db.jenaRemove(removeModel, username, password);
//    }



    /**
     * Sets the owners of a session
     *
     * @param owners
     *            The uris of the owners
     * @throws MalformedURLException
     */
//    public void setOwners(String[] owners)
//             throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionOwner = model.createProperty(Session.OWNER);
//
//        for (int i = 0; i < owners.length; i++) {
//            Resource owner = model.createResource(owners[i]);
//            node.addProperty(sessionOwner, owner);
//        }
//        db.jenaReplace(model, username, password);
//    }

    /**
     * Gives all the given users the given permission on a session
     *
     * @param permission
     *            The permission to give
     * @param usersGroups
     *            The users and groups (uris) to give the permission to
     * @throws MalformedURLException
     */
//    public void setUsersAndGroupsWithPermission(String permission,
//            String[] usersGroups) throws MalformedURLException {
//        if (usersGroups == null) {
//            usersGroups = new String[0];
//        }
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property prop = model.createProperty(permission);
//        for (int i = 0; i < usersGroups.length; i++) {
//            Resource dbuser = model.createResource(usersGroups[i]);
//            node.addProperty(prop, dbuser);
//        }
//        if (usersGroups.length == 0) {
//            node.addProperty(prop, "");
//        }
//        db.jenaReplace(model, username, password);
//    }
//
//    /*
//     * Returns the agenda items for a meeting
//     * @return A map of uri -> map of agenda items
//     */
//    private HashMap getDBAgendaItems() {
//        HashMap agendaItems = new HashMap();
//        Vector items = db.getUtils().getValuesOfPropertyWithType(uri,
//                Session.SUB_EVENT, AgendaItem.TYPE, true, username, password);
//        for (int i = 0; i < items.size(); i++) {
//            String agendaItem = (String) items.get(i);
//            HashMap item = db.getUtils().getPropertiesOfUri(agendaItem, true,
//                    username, password);
//            agendaItems.put(agendaItem, item);
//        }
//        return agendaItems;
//    }

//    /**
//     * Sets the agenda items for a session
//     * @param agenda The items in the agenda
//     * @throws MalformedURLException
//     */
//    public void setAgendaItems(String[] agenda) throws MalformedURLException {
//        if (isTranscribed()) {
//            return;
//        }
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property type = model.createProperty(DatabaseUtils.RDF_TYPE);
//        Property sessionSubEvent = model.createProperty(Session.SUB_EVENT);
//        Resource agendaType = model.createResource(AgendaItem.TYPE);
//        Property agendaLabel = model.createProperty(AgendaItem.LABEL);
//        Property agendaNo = model.createProperty(AgendaItem.ITEM);
//        Iterator iterator = this.agenda.keySet().iterator();
//
//        while (iterator.hasNext()) {
//            String uri = (String) iterator.next();
//            HashMap item = (HashMap) this.agenda.get(uri);
//            String label = item.get(AgendaItem.LABEL).toString();
//            String no = item.get(AgendaItem.ITEM).toString();
//            Resource eventResource = model.createResource(uri);
//            eventResource.addProperty(type, agendaType);
//            eventResource.addProperty(agendaLabel, label);
//            eventResource.addProperty(agendaNo, no);
//            node.addProperty(sessionSubEvent, eventResource);
//        }
//        db.jenaRemove(model, username, password);
//
//        this.agenda = new HashMap();
//        model.removeAll();
//        for (int i = 0; i < agenda.length; i++) {
//            String uri = DatabaseUtils.NAMESPACE + AgendaItem.NAMESPACE_AGENDA
//                + id + DatabaseUtils.NAMESPACE_SEP + i;
//            Resource agendumNode = model.createResource(uri);
//            HashMap item = new HashMap();
//            node.addProperty(sessionSubEvent, agendumNode);
//            agendumNode.addProperty(type, agendaType);
//            agendumNode.addProperty(agendaLabel, agenda[i]);
//            agendumNode.addProperty(agendaNo, i + 1);
//            item.put(AgendaItem.LABEL, agenda[i]);
//            item.put(AgendaItem.ITEM, String.valueOf(i + 1));
//            item.put(DatabaseUtils.RDF_TYPE, AgendaItem.TYPE);
//            this.agenda.put(uri, item);
//        }
//        db.jenaAdd(model, username, password);
//    }
//
//    // Gets the documents from the database
//    private HashMap getDBDocuments() {
//        HashMap documents = new HashMap();
//        Vector items = db.getUtils().getValuesOfProperty(uri, Session.RESOURCE,
//                true, username, password);
//        for (int i = 0; i < items.size(); i++) {
//            String doc = (String) items.get(i);
//            HashMap item = db.getUtils().getPropertiesOfUri(doc, true, username,
//                    password);
//            documents.put(doc, item);
//        }
//        return documents;
//    }
//
//    /**
//     * Sets the documents of the session
//     * @param documents The documents to set
//     * @throws MalformedURLException
//     */
//    public void setDocuments(HashMap documents) throws MalformedURLException {
//        if (isTranscribed()) {
//            return;
//        }
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionResource = model.createProperty(Session.RESOURCE);
//        Property resourceName = model.createProperty(rtspd.Resource.NAME);
//        Property resourceURL = model.createProperty(rtspd.Resource.URL);
//        Iterator documentIterator = documents.keySet().iterator();
//        Iterator iterator = this.documents.keySet().iterator();
//        int i = 0;
//
//        while (iterator.hasNext()) {
//            String uri = (String) iterator.next();
//            HashMap document = (HashMap) this.documents.get(uri);
//            String name = (String) document.get(rtspd.Resource.NAME);
//            String url = (String) document.get(rtspd.Resource.URL);
//            Resource documentResource = model.createResource(uri);
//            documentResource.addProperty(resourceName, name);
//            documentResource.addProperty(resourceURL, url);
//            node.addProperty(sessionResource, documentResource);
//        }
//        db.jenaRemove(model, username, password);
//
//        model.removeAll();
//        this.documents = new HashMap();
//        while (documentIterator.hasNext()) {
//            String document = (String) documentIterator.next();
//            String url = (String) documents.get(document);
//            String uri = DatabaseUtils.NAMESPACE + rtspd.Resource.NAMESPACE_DOC
//                + id + DatabaseUtils.NAMESPACE_SEP + i;
//            Resource documentNode = model.createResource(uri);
//            HashMap item = new HashMap();
//            node.addProperty(sessionResource, documentNode);
//            documentNode.addProperty(resourceName, document);
//            documentNode.addProperty(resourceURL, url);
//            item.put(rtspd.Resource.NAME, document);
//            item.put(rtspd.Resource.URL, url);
//            this.documents.put(uri, item);
//            i++;
//        }
//        db.jenaReplace(model, username, password);
//    }
//
    // Gets the local events of a session
//    private HashMap<String,HashMap<String,Object>> getSessionLocalEvents() {
//        HashMap<String,HashMap<String,Object>> localEvents = 
//        	new HashMap<String,HashMap<String,Object>>();
//        Vector<Object> events = db.getUtils().getValuesOfProperty(uri,
//                Session.LOCAL_EVENT, true, username, password);
//        for (int i = 0; i < events.size(); i++) {
//            String event = (String) events.get(i);
//            String eventSite = db.getUtils().getValueOfProperty(event,
//                    LocalEvent.LOCATION, username, password);
//            Vector<Object> people = db.getUtils().getValuesOfProperty(event,
//                    LocalEvent.ATTENDEE, true, username, password);
//            HashMap<String,Object> item = new HashMap<String,Object>();
//            item.put(LocalEvent.LOCATION, eventSite);
//            item.put(LocalEvent.ATTENDEE, people);
//            localEvents.put(event, item);
//        }
//
//        return localEvents;
//    }

    /**
     * Sets the participants of the session
     * @param participants The participants to set
     * @throws MalformedURLException
     */
//    public void setParticipants(HashMap participants)
//            throws MalformedURLException {
//        HashMap users = db.getAllUsers(username, password);
//        HashMap sites = db.getSites(username, password);
//
//        Model model = db.getJenaModel();
//        Resource node = model.createResource(uri);
//        Property sessionEvent = model.createProperty(Session.LOCAL_EVENT);
//        Property eventLocation = model.createProperty(LocalEvent.LOCATION);
//        Resource eventType = model.createResource(LocalEvent.TYPE);
//        Property eventAttendee = model.createProperty(LocalEvent.ATTENDEE);
//        Property type = model.createProperty(DatabaseUtils.RDF_TYPE);
//        Property playPermission =
//            model.createProperty(Session.PLAY_PERMISSION);
//        Property alterPermission =
//            model.createProperty(Session.ALTER_PERMISSION);
//        Property recordPermission =
//            model.createProperty(Session.RECORD_PERMISSION);
//        Iterator siteIterator = participants.values().iterator();
//        Iterator participantIterator = participants.keySet().iterator();
//        Iterator iterator = events.keySet().iterator();
//        HashMap localMap = new HashMap();
//
//        while (iterator.hasNext()) {
//            String eventURI = (String) iterator.next();
//            HashMap event = (HashMap) events.get(eventURI);
//            Resource site = model.createResource(
//                    (String) event.get(LocalEvent.LOCATION));
//            Vector people = (Vector) event.get(LocalEvent.ATTENDEE);
//            Resource eventResource = model.createResource(eventURI);
//            eventResource.addProperty(type, eventType);
//            eventResource.addProperty(eventLocation, site);
//            for (int i = 0; i < people.size(); i++) {
//                Resource person = model.createResource((String) people.get(i));
//                eventResource.addProperty(eventAttendee, person);
//                node.addProperty(playPermission, person);
//                node.addProperty(alterPermission, person);
//                node.addProperty(recordPermission, person);
//            }
//            node.addProperty(sessionEvent, eventResource);
//        }
//        db.jenaRemove(model, username, password);
//
//        participantsURI = new HashMap();
//        participantsBySite = new HashMap();
//        sitesOrdered = new Vector();
//        events = new HashMap();
//        model.removeAll();
//        while (siteIterator.hasNext()) {
//            String site = (String) siteIterator.next();
//            String localEvent = DatabaseUtils.NAMESPACE
//                                + RTPType.NAMESPACE_EVENT
//                                + DatabaseUtils.generateId();
//            Resource eventNode = model.createResource(localEvent);
//            HashMap event = new HashMap();
//            node.addProperty(sessionEvent, eventNode);
//            eventNode.addProperty(type, eventType);
//            eventNode.addProperty(eventLocation, model.createResource(site));
//            localMap.put(site, localEvent);
//            event.put(LocalEvent.LOCATION, site);
//            events.put(localEvent, event);
//        }
//        while (participantIterator.hasNext()) {
//            String participant = (String) participantIterator.next();
//            String site = (String) participants.get(participant);
//            String siteName = (String) sites.get(site);
//            String eventURI = (String) localMap.get(site);
//            Resource participantNode = model.createResource(participant);
//            Resource eventNode = model.createResource(eventURI);
//            Vector partsAtSite = (Vector) participantsBySite.get(siteName);
//            HashMap event = (HashMap) events.get(eventURI);
//            Vector eventParts = (Vector) event.get(LocalEvent.ATTENDEE);
//            eventNode.addProperty(eventAttendee, participantNode);
//            node.addProperty(playPermission, participantNode);
//            node.addProperty(recordPermission, participantNode);
//            node.addProperty(alterPermission, participantNode);
//            if ((siteName == null)) {
//                siteName = UNKNOWN_SITE;
//            }
//            if (!sitesOrdered.contains(siteName)) {
//                sitesOrdered.add(siteName);
//            }
//            participantsURI.put(participant, site);
//            if (partsAtSite == null) {
//                partsAtSite = new Vector();
//            }
//            partsAtSite.add(users.get(participant));
//            Collections.sort(partsAtSite, new ArrayComparator(1));
//            participantsBySite.put(siteName, partsAtSite);
//            if (eventParts == null) {
//                eventParts = new Vector();
//            }
//            eventParts.add(participant);
//            event.put(LocalEvent.ATTENDEE, eventParts);
//            events.put(eventURI, event);
//        }
//        Collections.sort(sitesOrdered);
//        db.jenaAdd(model, username, password);
//    }
//
//    // Loads the streams from the database
//    private HashMap getDBStreams() {
//        HashMap streams = new HashMap();
//        Vector uris = db.getUtils().getValuesOfProperty(uri, STREAM, true,
//                username, password);
//        for (int i = 0; i < uris.size(); i++) {
//            String uri = (String) uris.get(i);
//            HashMap properties = db.getUtils().getPropertiesOfUri(uri, true,
//                    username, password);
//            streams.put(uri, properties);
//        }
//        if (streams.size() == 0) {
//            return null;
//        }
//        return streams;
//    }
//
//    /*
//     * Returns the items that have been tagged as Agenda items in Compendium
//     * @return A list of maps of property to value
//     */
//    private Vector getCompendiumAgendaItems() {
//        HashMap items = db.getUtils().getTaggedNodes(uri, Event.AGENDA_ITEM_TAG,
//                username, password);
//        Vector itemList = new Vector(items.values());
//        Collections.sort(itemList, new AgendaItemSorter());
//        for (int i = 0; i < itemList.size(); i++) {
//            HashMap item = (HashMap) itemList.get(i);
//            String label = (String) item.get(Node.LABEL);
//            char first = label.charAt(0);
//            int c = 0;
//
//            // Remove spaces from the start of the string
//            while ((first == ' ') && (c < label.length())) {
//                c++;
//                if (c < label.length()) {
//                    first = label.charAt(c);
//                }
//            }
//
//            // Remove numbers from the start of the string
//            while ((c < label.length())
//                    && (((first >= '0') && (first <= '9')) || (first == '.'))) {
//                c++;
//                if (c < label.length()) {
//                    first = label.charAt(c);
//                }
//            }
//            label = label.substring(c);
//            item.put(AgendaItem.ITEM, String.valueOf(i + 1));
//            item.put(AgendaItem.LABEL, label);
//            item.put(DatabaseUtils.RDF_TYPE, AgendaItem.TYPE);
//        }
//        return itemList;
//    }
//
    /*
     * Returns a list of documents from Compendium
     * @return A list of maps of properties to values
     */
//    private Vector getCompendiumDocuments() {
//        HashMap items = db.getUtils().getTaggedNodes(uri,
//                Event.MEETING_DOCUMENT_TAG, username, password);
//        return new Vector(items.values());
//    }

    /**
     * Imports compendium metadata into the database
     * @param model The model to import
     * @throws MalformedURLException
     */
//    public void importCompendiumData(Model model) throws MalformedURLException {
//        StmtIterator iterator = model.listStatements(null,
//                model.createProperty(DatabaseUtils.RDF_TYPE),
//                model.createResource(Session.COMPENDIUM_EVENT));
//        Model linkModel = db.getJenaModel();
//        Model removeModel = db.getJenaModel();
//        Resource session = linkModel.createResource(uri);
//        Property sessionEvent = linkModel.createProperty(Session.SUB_EVENT);
//        Property useruri =
//            model.createProperty(Event.SENDER_OF_INFORMATION);
//        Property hasCompendium =
//            linkModel.createProperty(Session.TRANSCRIPTION);
//        while (iterator.hasNext()) {
//            Statement stmt = iterator.nextStatement();
//            Resource event = stmt.getSubject();
//            session.addProperty(sessionEvent, event);
//        }
//        iterator = model.listStatements(null, useruri, (Resource) null);
//        while (iterator.hasNext()) {
//            Statement stmt = iterator.nextStatement();
//            String[] sender = stmt.getObject().toString().split(
//                    SENDER_NAME_SEP);
//            String uname = db.getUserNameByName(sender[0], sender[1]);
//            String uri = null;
//            if (uname == null) {
//                uname = username;
//            }
//            uri = db.getUserURI(uname, username, password);
//            linkModel.add(stmt.getSubject(), stmt.getPredicate(),
//                    linkModel.createResource(uri));
//            removeModel.add(stmt);
//        }
//        iterator = model.listStatements(null, hasCompendium,
//                (Resource) null);
//        while (iterator.hasNext()) {
//            Statement stmt = iterator.nextStatement();
//            RDFNode object = stmt.getObject();
//            removeModel.add(stmt);
//            session.addProperty(hasCompendium, object);
//            System.err.println("Adding Compendium " + object.toString());
//        }
//        model.remove(removeModel);
//        db.jenaAdd(model, username, password);
//        db.jenaAdd(linkModel, username, password);
//    }

    /**
     * Returns the compendium events and nodes for the given session
     * @param username The user requesting the compendium
     * @param password The password of the user
     * @param convert True if the users are to be converted into strings
     * @return A model containing the events and nodes
     */
//    public Model getSessionCompendium(String username, String password,
//            boolean convert) {
//        HashMap users = db.getAllUsers(username, password);
//        Vector nodes = new Vector();
//        Iterator nodeIterator = null;
//        Vector transcriptions = db.getUtils().getValuesOfProperty(uri,
//                Session.TRANSCRIPTION, false, username, password);
//        Model events = ModelFactory.createDefaultModel();
//        Vector uris = db.getUtils().getValuesOfPropertyWithType(uri,
//                Session.SUB_EVENT, Session.COMPENDIUM_EVENT, false, username,
//                password);
//        for (int i = 0; i < uris.size(); i++) {
//            Resource event = (Resource) uris.get(i);
//            HashMap properties = db.getUtils().getPropertiesOfUri(
//                    event.getURI(), false, username, password);
//            Iterator iterator = properties.keySet().iterator();
//            while (iterator.hasNext()) {
//                String property = (String) iterator.next();
//                Property prop = events.createProperty(property);
//                Vector values = db.getUtils().getValuesOfProperty(
//                        event.getURI(), property, false, username, password);
//                for (int j = 0; j < values.size(); j++) {
//                    RDFNode value = (RDFNode) values.get(j);
//                    if (convert
//                            && property.equals(Event.SENDER_OF_INFORMATION)) {
//                        String[] names = (String[]) users.get(value.toString());
//                        value = events.createLiteral(names[0]
//                                                 + SENDER_NAME_SEP + names[1]);
//                    }
//                    events.add(event, prop, value);
//                    if (prop.toString().equals(DatabaseUtils.MEMETIC
//                            + "has-node")
//                           || prop.toString().equals(
//                                   DatabaseUtils.MEMETIC + "has-map")) {
//                        String node = value.toString();
//                        if (!nodes.contains(node)) {
//                            nodes.add(node);
//                        }
//                    }
//                }
//            }
//        }
//
//        nodeIterator = nodes.iterator();
//        while (nodeIterator.hasNext()) {
//            String node = (String) nodeIterator.next();
//            Resource nodeuri = events.createResource(node);
//            HashMap properties = db.getUtils().getPropertiesOfUri(node, false,
//                    username, password);
//            Iterator iterator = properties.keySet().iterator();
//            while (iterator.hasNext()) {
//                String property = (String) iterator.next();
//                Property nodeprop = events.createProperty(property);
//                Vector values = db.getUtils().getValuesOfProperty(node,
//                        property, false, username, password);
//                for (int i = 0; i < values.size(); i++) {
//                    RDFNode nodevalue = (RDFNode) values.get(i);
//                    events.add(nodeuri, nodeprop, nodevalue);
//                }
//            }
//        }
//
//        for (int i = 0; i < transcriptions.size(); i++) {
//            RDFNode trans = (RDFNode) transcriptions.get(i);
//            events.add(events.createResource(uri),
//                    events.createProperty(Session.TRANSCRIPTION), trans);
//        }
//        return events;
//    }

    /**
     * Adds a screen to the session
     * @param filename The name of the file
     * @param starttime The time at which the screen was changed
     * @param senderid The id of the sender
     * @param isLast True if this is the last screen
     * @throws MalformedURLException
     */
//    public void addScreen(String filename, long starttime, String senderid,
//            boolean isLast) throws MalformedURLException {
//        Model model = db.getJenaModel();
//        Property end = model.createProperty(Event.END_TIME);
//        if (lastScreen != null) {
//            Resource last = model.createResource(lastScreen);
//            last.addProperty(end, starttime);
//        }
//        if (!isLast) {
//            Resource session = model.createResource(uri);
//            String screenUri = DatabaseUtils.NAMESPACE + "Screen"
//                    + DatabaseUtils.generateId();
//            Resource screen = model.createResource(screenUri);
//            Property event = model.createProperty(SUB_EVENT);
//            Property type = model.createProperty(DatabaseUtils.RDF_TYPE);
//            Property start = model.createProperty(Event.START_TIME);
//            Property file = model.createProperty(Event.SCREEN_CAPTURE);
//            Property id = model.createProperty(Event.SENDER_ID);
//            Resource screenType = model.createResource(Event.TYPE_SCREEN);
//            screen.addProperty(type, screenType);
//            screen.addProperty(start, starttime);
//            screen.addProperty(file, filename);
//            screen.addProperty(id, senderid);
//            session.addProperty(event, screen);
//            lastScreen = screenUri;
//        } else {
//            lastScreen = null;
//        }
//        db.jenaAdd(model, username, password);
//    }

    /**
     * Gets the screen events of the session
     * @return A vector of hashmaps of each screen event
     */
//    public Vector getScreenEvents() {
//        Vector screenEvents = new Vector();
//        Vector items = db.getUtils().getValuesOfPropertyWithType(uri,
//               Session.SUB_EVENT, Event.TYPE_SCREEN, true, username, password);
//        for (int i = 0; i < items.size(); i++) {
//            String screenEvent = (String) items.get(i);
//            HashMap item = db.getUtils().getPropertiesOfUri(screenEvent, true,
//                    username, password);
//            screenEvents.add(item);
//        }
//        Collections.sort(screenEvents, new EventSorter());
//        return screenEvents;
//    }

    // Checks if a value is in an array
//    private boolean isIn(String value, String[] array) {
//        if (array == null) {
//            return false;
//        }
//        for (int i = 0; i < array.length; i++) {
//            if (value.equals(array[i])) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Gets the Compendium events of the session
     * @param eventTypes a list of compendium event types to find
     * @param nodeTypes a list of compendium node types to find
     * @return A vector of hashmaps of each compendium event
     */
//    public Vector getCompendiumEvents(String[] eventTypes, String[] nodeTypes) {
//        Vector events = new Vector();
//        Vector items = db.getUtils().getValuesOfPropertyWithType(uri,
//               Session.SUB_EVENT, Event.TYPE_COMPENDIUM, true,
//               username, password);
//        for (int i = 0; i < items.size(); i++) {
//            String event = (String) items.get(i);
//            HashMap item = db.getUtils().getPropertiesOfUri(event, true,
//                    username, password);
//            Vector eventType = db.getUtils().getValuesOfProperty(event,
//                    DatabaseUtils.RDF_TYPE, true, username, password);
//            boolean isValidEvent = false;
//            boolean isValidNode = false;
//            for (int j = 0; j < eventType.size(); j++) {
//                String type = (String) eventType.get(j);
//                if (!type.equals(Event.TYPE_COMPENDIUM)) {
//                    item.put(DatabaseUtils.RDF_TYPE, type);
//                }
//                if (isIn(type, eventTypes)) {
//                    isValidEvent = true;
//                }
//            }
//            String node = (String) item.get(Event.NODE);
//            if (node != null) {
//                HashMap nodeMap = db.getUtils().getPropertiesOfUri(node, true,
//                    username, password);
//                Vector nodeType = db.getUtils().getValuesOfProperty(node,
//                        DatabaseUtils.RDF_TYPE, true, username, password);
//                for (int j = 0; j < nodeType.size(); j++) {
//                    String type = (String) nodeType.get(j);
//                    if (!type.equals(Event.NODE_TYPE_COMPENDIUM)) {
//                        nodeMap.put(DatabaseUtils.RDF_TYPE, type);
//                    }
//                    if (isIn(type, nodeTypes)) {
//                        isValidNode = true;
//                    }
//                }
//                item.put(Event.NODE, nodeMap);
//            }
//            if (isValidEvent && isValidNode) {
//                events.add(item);
//            }
//        }
//        Collections.sort(events, new EventSorter());
//        return events;
//    }
}
