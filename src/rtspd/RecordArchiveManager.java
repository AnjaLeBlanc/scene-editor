/*
 * @(#)RecordArchiveManager.java
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
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Manages the saving of stream errors and streams
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RecordArchiveManager extends Thread implements RTPPacketSink,
        RTCPPacketSink {

    // The error message to print in the log
    private static final String ERROR_MESSAGE = "Error";

    // The number of ms in a second
    private static final int MS_PER_SEC = 1000;

    // The time to wait
    private static final int WAIT_TIME = 1000;

    // The maximum RTP type in the RTCP - RTP conflict range
    private static final int MAX_RTCP_CONFLICT = 76;

    // The minimum RTP type in the RTCP - RTP conflict range
    private static final int MIN_RTCP_CONFLICT = 72;

    // The error response
    private static final String SERVER_ERROR_RESPONSE = "Server Error";

    // The OK response
    private static final String OK_RESPONSE = "OK";

    // The RTP port to forward packets using
    private static final int RTP_PORT = 20000;

    // The log file
    private static Log logger = LogFactory.getLog(RecordArchiveManager.class
            .getName());

    // A map of streams to StreamArchives
    private HashMap<Long,StreamArchive> streamMap = new HashMap<Long,StreamArchive>();

    // The directory containing the files
    private String directory = "";

    private String sessionName="";

    // True if we are currently recording
    private boolean recordFlag = false;

    // The time at which the recording started in milliseconds since the epoch
    private long recordStart = -1;

    // The time at which the recording stopped
    private long recordStop = -1;

    // The people to forward packets to
    private Vector<String> forwards = new Vector<String>();

    // The streams that have been stopped
    private Vector<Long> stoppedStreams = new Vector<Long>();

    // The session
    private Session session = null;

    // The owner
    private String owner = "";

    // The owner password
    private String ownerPass = "";

    // A queue of packets to be handled
    private LinkedList<ReusablePacket> queue = new LinkedList<ReusablePacket>();

    // The time that a packet was queued
    private LinkedList<Long> queueTime = new LinkedList<Long>();

    // Queues if a packet is RTP or not
    private LinkedList<Boolean> queueIsRTP = new LinkedList<Boolean>();

    // The time that the current packet was recieved
    private long packetRecievedTime = 0;

    // True if the current packet is RTP
    private boolean packetIsRTP = false;

    // True when the thread is to be stopped
    private boolean done = false;

    // True if the run method has completed
    private boolean terminated = false;

    // An object to synchronise on when the archive is finished
    private Integer finishedObject = new Integer(0);

    // A listing of streams added since the last request for new streams
    private Vector<String> newStreams = new Vector<String>();

    /**
     * Creates a new RecordArchiveManager
     *
     * @param directory
     *            The directory to store files in
     * @param session The session
     * @param owner
     *            The owner of the archive session
     * @param ownerPass
     *            The password of the owner
     */
    public RecordArchiveManager(String directory, Session session, String owner,
            String ownerPass) {
        this.sessionName= directory;
        this.directory = directory.substring(0, directory.lastIndexOf(java.io.File.separator));
        this.session = session;
        this.owner = owner;
        this.ownerPass = ownerPass;
        logger.debug("Record_Archive_Manager::Record_Archive_Manager: "
                + "creating!\n");
        start();
    }

    // Adds a packet to the queue
    private void addPacket(ReusablePacket packet, boolean isRTP, long time) {
        synchronized (queue) {
            if (!done) {
                queue.addLast(packet);
                queueTime.addLast(new Long(time));
                queueIsRTP.addLast(new Boolean(isRTP));
                queue.notifyAll();
            }
        }
    }

    // Retrieves a packet from the queue
    private ReusablePacket nextPacket() {
        ReusablePacket returnPacket = null;
        synchronized (queue) {
            while (!done && queue.isEmpty()) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {

                    // Do Nothing
                }
            }

            if (!queue.isEmpty()) {
                packetRecievedTime = ((Long) queueTime.removeFirst())
                        .longValue();
                packetIsRTP = ((Boolean) queueIsRTP.removeFirst())
                        .booleanValue();
                returnPacket = (ReusablePacket) queue.removeFirst();
            }
        }
        return returnPacket;
    }

    private void processNextPacket() {

        // Get the next packet
        ReusablePacket packet = nextPacket();

        // Process the packet, depending on its type
        if (packet != null) {
            if (packetIsRTP) {
                processRTPPacket(packet, packetRecievedTime);
            } else {
                processRTCPPacket(packet, packetRecievedTime);
            }
        }
    }

    /**
     * Processes incoming packets
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        done = false;
        synchronized (finishedObject) {
            terminated = false;
        }

        // Only stop when signalled and the queue becomes empty
        while (!done) {
            processNextPacket();
        }

        synchronized (finishedObject) {
            terminated = true;
            finishedObject.notifyAll();
        }
    }

    public void addForward(String address){
        forwards.add(address);
    }

    /**
     * Adds a person to forward to
     *
     * @param listenRequest
     *            The request to handle
     * @throws RTSPResponse
     */
    public void addListener(RTSPListenRequest listenRequest)
            throws RTSPResponse {
        RTSPResponse response = new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE,
                listenRequest.getRequestPacket());
        String address = listenRequest.getAddress();
        forwards.add(address);

        // Add the address to all created streams
        synchronized (streamMap) {
            Iterator<StreamArchive> iterator = streamMap.values().iterator();
            while (iterator.hasNext()) {
                StreamArchive archive = iterator.next();
                archive.addForwardAddress(address, RTP_PORT);
            }
        }

        // Send OK
        try {
            response.setHeader(Headers.RTSP_ADDRESS, InetAddress.getLocalHost()
                    .getHostAddress());
            response.setHeader(Headers.RTSP_PORT, String.valueOf(RTP_PORT));
        } catch (UnknownHostException e) {
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    SERVER_ERROR_RESPONSE, listenRequest.getRequestPacket());
        }
        response.send();
    }

    /**
     * Starts recording
     *
     * @param streamId
     *            The id of the stream to re-enable, or null to start all
     */
    public void record(String streamId) {
        int index = -1;
        logger.debug("Record_Archive_Manager::record()\n");
        if (streamId != null) {
            logger.debug("Restarting stream " + streamId);
            index = stoppedStreams.indexOf(Long.valueOf(streamId));
        }
        if (index == -1) {
            enableRecording();
        } else {
            stoppedStreams.remove(index);
            session.setStreamEnabled(streamId, true);
        }
    }

    /**
     * Handles an RTP packet
     *
     * @param packet The packet to handle
     */
    public void handleRTPPacket(ReusablePacket packet) {
        addPacket(packet, true, System.currentTimeMillis());
    }

    /**
     * Processes an RTP packet
     *
     * @param packet The packet to handle
     * @param packetRecievedTime The time at which the packet was recieved
     */
    public void processRTPPacket(ReusablePacket packet,
            long packetRecievedTime) {

        try {
            RTPHeader packetHeader = new RTPHeader(packet.getPacket());

            // If we are not recording, do nothing
            if (!recordFlag) {
                return;
            }
            // If this is RTP version 2 and the type is valid
            if ((packetHeader.getVersion() == RTPHeader.VERSION)
                    && (packetHeader.getPacketType() <= RTPHeader.MAX_PAYLOAD)
                            && ((packetHeader.getPacketType()
                                    < MIN_RTCP_CONFLICT)
                                    || (packetHeader.getPacketType()
                                            > MAX_RTCP_CONFLICT))) {

                // Get the stream archive for this ssrc
                StreamArchive streamArchive = null;
                boolean isStopped = false;
                synchronized (streamMap) {
                    streamArchive = (StreamArchive) streamMap
                            .get(new Long(packetHeader.getSsrc()));
                    isStopped = stoppedStreams.contains(new Long(packetHeader
                            .getSsrc()));
                }

                // If it doesn't exist and has not been stopped, create it
                if ((streamArchive == null) && !isStopped) {
                    streamArchive = createStreamArchive(packetHeader.getSsrc(),
                            session);
                    synchronized (streamMap) {
                        streamMap.put(
                              new Long(packetHeader.getSsrc()), streamArchive);
                    }
                } else if (streamArchive != null) {

                    // Let the stream archive handle the packet
                    streamArchive.handleRTPPacket(packet, packetRecievedTime);
                }
            }
        } catch (IOException e) {
            logger.error(ERROR_MESSAGE, e);
        }
    }

    /**
     * Handles an RTCP packet
     *
     * @param packet The packet to handle
     */
    public void handleRTCPPacket(ReusablePacket packet) {
        addPacket(packet, false, System.currentTimeMillis());
    }

    /**
     * Processes an RTCP packet
     *
     * @param packet The packet to process
     * @param packetRecievedTime The time at which the packet was recieved
     */
    public void processRTCPPacket(ReusablePacket packet,
            long packetRecievedTime) {

        try {
            RTCPHeader packetHeader = new RTCPHeader(packet.getPacket());

            // If we are not recording, do nothing
            if (!recordFlag) {
                return;
            }

            // If the packet is the correct version and is a sender report
            if ((packetHeader.getVersion() == RTCPHeader.VERSION)
                    && ((packetHeader.getPacketType() == RTCPHeader.PT_SR)
                           || (packetHeader.getPacketType()
                                   == RTCPHeader.PT_RR))) {

                // Get the archive
                StreamArchive streamArchive = null;
                synchronized (streamMap) {
                    streamArchive = streamMap
                            .get(new Long(packetHeader.getSsrc()));
                }

                if (streamArchive != null) {

                    // Handle the packet
                    streamArchive.handleRTCPPacket(packet, packetRecievedTime);
                }
            }
        } catch (IOException e) {
            logger.error(ERROR_MESSAGE, e);
        }
    }

    /**
     * Starts the recording of packets
     * @return true if the recording enabled correctly
     */
    public boolean enableRecording() {

        // Start the recording and note the time
        recordFlag = true;
        logger.debug("Record_Archive_Manager::enableRecording:  "
                        + "Enabling recording");
        return true;
    }

    /**
     * Stops the recording of packets
     */
    public void disableRecording() {

        // Stop recording
        recordFlag = false;
        logger.debug("Record_Archive_Manager::disableRecording:  "
                + "Disabling recording");
    }

    /**
     * Handle notification that the stream is finished
     *
     * @param streamArch
     *            The archiver of the stream
     */
    public void handleEndOfStream(StreamArchive streamArch) {
        logger.debug("Record_Archive_Manager::endOfStream: got finished for "
                        + "ssrc " + streamArch.getSsrc());
        synchronized (streamMap) {
            streamMap.remove(new Long(streamArch.getSsrc()));
        }
    }

    /**
     * Stops recording of a specific stream
     *
     * @param streamId
     *            the id of the stream to stop
     */
    public void teardown(String streamId) {
        synchronized (streamMap) {
            StreamArchive archive =
                (StreamArchive) streamMap.get(Long.valueOf(streamId));
            if (archive != null) {
                archive.terminate();
                stoppedStreams.add(Long.valueOf(streamId));
                streamMap.remove(Long.valueOf(streamId));
                session.setStreamEnabled(streamId, false);
            }
        }
    }

    /**
     * Finish with this recording
     */
    public void terminate() {
        recordStop = 0;

        // Stop recording
        logger.debug("Record_Archive_Manager::terminate:\n");
        logger.debug("\tterminating " + streamMap.size() + " Stream_Archives");

        // Finish processing of queued packets
        done = true;
        synchronized (queue) {
            queue.notifyAll();
        }

        synchronized (finishedObject) {
            while (!terminated) {
                try {
                    finishedObject.wait(WAIT_TIME);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }

        while (!queue.isEmpty()) {
            logger.debug("Waiting for queue to empty "
                    + "(size = " + queue.size() + ")");
            processNextPacket();
        }

        // Stop each of the archivers
        recordFlag = false;
        recordStart = -1;
        synchronized (streamMap) {
            Iterator<Long> iter = streamMap.keySet().iterator();
            while (iter.hasNext()) {
                Long ssrc = (Long) iter.next();
                StreamArchive archive = (StreamArchive) streamMap.get(ssrc);
                archive.terminate();
                logger.debug("Archive " + ssrc + " has start "
                        + archive.getStartTime()
                        + " and duration " + archive.getDuration());
                if ((archive.getStartTime() >= 0)
                        && ((archive.getStartTime() < recordStart)
                        || (recordStart == -1))) {
                    if (archive.getStartTime() != 0) {
                        recordStart = archive.getStartTime();
                    }
                }
                if ((archive.getStartTime() + archive.getDuration())
                        > recordStop) {
                    if (archive.getStartTime() != 0) {
                        recordStop =
                            archive.getStartTime() + archive.getDuration();
                    }
                }
            }
        }

        synchronized (newStreams) {
            newStreams.notifyAll();
        }

        // Calculate the duration of the session and store it
        if (recordStart == 0) {
            logger.debug("No Start Time found");
            recordStop = 0;
        }

        session.setLock(owner);
        session.setPassword(ownerPass);
//        try {
//            session.setEndTime(recordStop);
//            session.setStartTime(recordStart);
//        } catch (Exception e) {
//            logger.error(ERROR_MESSAGE, e);
//        }
        logger.debug("Duration: " + ((recordStop - recordStart) / MS_PER_SEC)
                + " seconds for session " + session.getId());

        session.stopRecording();
        session.getUnlock();
    }

    /**
     * Returns true if the session is being recorded
     * @return True if we are recording
     */
    public boolean isRecording() {
        return recordFlag;
    }

    // Creates a streamArchive object
    private StreamArchive createStreamArchive(long ssrc, Session session)
            throws IOException {
        StreamArchive streamArchive = new StreamArchive(this, directory, ssrc,
                session, owner, ownerPass);
        for (int i = 0; i < forwards.size(); i++) {
            streamArchive.addForwardAddress((String) forwards.get(i), RTP_PORT);
        }
        logger.debug("Record_Archive_Manager::createStreamArchive:");
        System.out.println("sessionName " + sessionName);
        File archive= new File(sessionName);
        try {
            FileWriter filewriter = new FileWriter(archive,true);
            filewriter.append(String.valueOf(ssrc));
            filewriter.append(" \n");
            filewriter.flush();
            filewriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return streamArchive;
    }

    /**
     * Adds a new stream to the archive
     * @param ssrc The ssrc of the stream
     */
    public void addStream(String ssrc) {
        synchronized (newStreams) {
            if (!newStreams.contains(ssrc)) {
                newStreams.add(ssrc);
                newStreams.notify();
            }
        }
    }

    /**
     * Handles NewStreams requests
     * @param newStreamsRequest The request for the new streams
     */
    public void handleNewStreamsRequest(
            RTSPNewStreamsRequest newStreamsRequest) {
        RTSPRequestPacket request = newStreamsRequest.getRequestPacket();
        RTSPResponse response =
            new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE, request);
        synchronized (newStreams) {
            while ((newStreams.size() == 0) && recordFlag) {
                try {
                    newStreams.wait(WAIT_TIME);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            if (recordFlag) {
                for (int i = 0; i < newStreams.size(); i++) {
                    String ssrc = (String) newStreams.get(i);
                    Stream stream = session.getStream(ssrc);
                    response = stream.handleDescribeStream(response);
                    if ((i + 1) != newStreams.size()) {
                        response.bodyAppend(Headers.EOL);
                    }
                }
                newStreams.clear();
            }
        }
        response.send();
    }

    /**
     * Adds a stream archive during a repair
     * @param archive The archive to add
     */
    public void addStreamArchive(StreamArchive archive) {
        streamMap.put(new Long(archive.getSsrc()), archive);
    }

    public void changeSessionDir(String sessiondirectory){
        this.sessionName= sessiondirectory;
        this.directory = sessiondirectory.substring(0, sessiondirectory.lastIndexOf(java.io.File.separator));
    }

    public long getRecordStart() {
        return recordStart;
    }

    public long getRecordStop() {
        return recordStop;
    }
}
