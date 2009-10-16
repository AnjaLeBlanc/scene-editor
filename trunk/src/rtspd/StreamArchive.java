/*
 * @(#)StreamArchive.java
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

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Stores RTP Data to Disk
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class StreamArchive {

    // The number of bytes in a short
    private static final int BYTES_PER_SHORT = 2;

    // The number of bytes in an int
    private static final int BYTES_PER_INT = 4;

    // The number of bytes in an address
    private static final int BYTES_PER_ADDRESS = 4;

    // The number of milliseconds in a second
    private static final int MS_PER_SEC = 1000;

    // The number of bytes in a word
    private static final int BYTES_PER_WORD = 4;

    // The log messsage for an error
    private static final String ERROR_MESSAGE = "Error";

    // The maximum RTP number for RTCP conflict avoidance
    private static final int MAX_RTCP_CONFLICT = 76;

    // The minimum RTP number for RTCP conflict avoidance
    private static final int MIN_RTCP_CONFLICT = 72;

    // The log message for an exception
    private static final String EXCEPTION_MESSAGE = "Exception";

    // A space
    private static final String SPACE = " ";

    // The log file
    private static Log logger = LogFactory
            .getLog(StreamArchive.class.getName());

    // The manager of this archive
    private RecordArchiveManager archiveMgr = null;

    // The start time of the stream
    private long startTime = 0;

    // The RTP identifier of the stream
    private long ssrc = 0;

    // The last RTP sequence number seen
    private int lastRtpSeq = 0;

    // The last RTP timestamp
    private long lastTimestamp = -1;

    // True if the RTP sequence number has been initialised
    private boolean rtpSeqInit = false;

    // The total number of bytes recorded
    private int totalBytes = 0;

    // The total number of packets not recorded
    private int totalMissedPackets = 0;

    // The total number of packets recorded
    private int totalPacketsSeen = 0;

    // The total size of the recorded file
    private long fileSize = 0;

    // True if some bad IO has occurred
    private boolean bBadFileIO = false;

    // The name of the file holding the stream
    private String streamFilename = "";

    // The control channel of the stream file
    private FileChannel streamFileControl = null;

    // The data channel of the stream file
    private DataOutputStream streamFile = null;

    // The name of the information file of the stream
    private String infoFilename = "";

    // The data channel for the information file
    private PrintWriter infoFile = null;

    // The name of the index of the stream
    private String indexFilename = "";

    // The data channel of the index file
    private DataOutputStream indexFile = null;

    // True if output writing has started
    private boolean writingOutput = false;

    // A list of RTP types seen in the data
    private Vector<String> typesSeen = new Vector<String>();

    // The time that the current packet was recieved
    private long packetRecievedTime = 0;

    // The packet timestamp of the current packet
    private long packetTimestamp = 0;

    // A forwarder to resend RTP packets on
    private Forwarder rtpForwarder = null;

    // A forwarder to resend RTCP packets on
    private Forwarder rtcpForwarder = null;

    // The owner
    private String owner = "";

    // The owner password
    private String ownerPass = "";

    // The SDES Items
    private HashMap<String,String> sdesInfo = new HashMap<String,String>();

    // The session to which this stream belongs
    private Session session = null;


    /**
     * Creates a new StreamArchive
     *
     * @param archiveMgr
     *            The manager of this archive
     * @param directory
     *            The directory to store in
     * @param ssrc
     *            The RTP stream id to record
     * @param session
     *            The session to which the stream belongs
     * @param owner
     *            The owner of the session
     * @param ownerPass
     *            The password of the owner
     * @throws IOException
     */
    public StreamArchive(RecordArchiveManager archiveMgr, String directory,
            long ssrc, Session session, String owner, String ownerPass)
            throws IOException {
        this.archiveMgr = archiveMgr;
        this.ssrc = ssrc;
        this.owner = owner;
        this.ownerPass = ownerPass;
        this.session = session;

        // Get the directory for the stream
        logger.debug("Stream_Archive::Stream_Archive:  for ssrc " + ssrc);
        logger.debug("Stream Archive: directory " + directory);

        // Work out the names of the files
        if (directory.length() > 0) {
            String slash = System.getProperty("file.separator");
            streamFilename = directory + slash + ssrc;
            infoFilename =
                directory + slash + ssrc + Stream.INFO_FILE_EXTENSION;
            indexFilename =
                directory + slash + ssrc + Stream.INDEX_FILE_EXTENSION;
        } else {
            logger.debug("ERROR: archiveMgr->directory().length() == 0!\n");
        }

        // Display the file names
        logger.debug("Stream_Archive::Stream_Archive: built filenames "
                + streamFilename + SPACE + infoFilename + SPACE
                + indexFilename);

        try {
            rtpForwarder = new Forwarder(new DatagramSocket());
            rtcpForwarder = new Forwarder(new DatagramSocket());
            rtpForwarder.start();
            rtcpForwarder.start();
        } catch (SocketException e) {
            logger.error(EXCEPTION_MESSAGE, e);
        }

        // If the stream file exists, this is a repair so fill in the details
        /*File streamFile = new File(streamFilename);
        if (streamFile.exists()) {
            try {
                FileInputStream fileInput = new FileInputStream(streamFile);
                DataInputStream input = new DataInputStream(fileInput);
                Stream stream = session.getStream(String.valueOf(ssrc));
                if (archiveMgr != null) {
                    archiveMgr.addStreamArchive(this);
                }
                infoFile = new PrintWriter(new FileOutputStream(infoFilename));
                startTime = stream.getStartTime();
                fileSize = streamFile.length();

                // Read the header
                input.readInt();
                input.readInt();
                input.read(new byte[BYTES_PER_ADDRESS]);
                input.readShort();

                // Read each packet
                boolean doneReading = false;
                while (!doneReading) {
                    try {
                        int length = input.readUnsignedShort();
                        int type = input.readShort();
                        long offset = input.readInt();
                        byte[] data = new byte[length];
                        input.read(data);
                        packetRecievedTime = offset + startTime;
                        if (type == Stream.RTP_PACKET) {
                            writingOutput = true;
                            RTPHeader header = new RTPHeader(
                                    data, 0, data.length);
                            totalBytes += length;
                            totalPacketsSeen += 1;
                            calculateMissedPackets(header);
                            String packetType = String.valueOf(
                                    header.getPacketType());
                            if (typesSeen.indexOf(packetType) == -1) {
                                typesSeen.add(packetType);
                            }
                        }
                    } catch (EOFException e) {
                        doneReading = true;
                    }
                }

            } catch (FileNotFoundException e) {
                // This can't happen here!
            }
        } */
    }

    /**
     * Adds an address to forward packets to
     *
     * @param address
     *            The address to forward to
     * @param rtpPort
     *            The port to forward to
     */
    public void addForwardAddress(String address, int rtpPort) {
        rtpForwarder.addListener(new InetSocketAddress(address, rtpPort));
        rtcpForwarder.addListener(new InetSocketAddress(address, rtpPort + 1));
    }

    /**
     * Stops the recording
     */
    public void terminate() {
        logger.debug("Stream_Archive::terminate:   terminating ssrc " + ssrc);
        rtpForwarder.end();
        rtcpForwarder.end();
        try {

            // Close the actual data archive file
            if (streamFile != null) {
                streamFile.close();
            } else {
                logger.debug("Stream_Archive::terminate: archive file"
                        + " already gone!");
            }

            // Write final data
            writeFinalInfo();

            // Close the information file
            if (infoFile != null) {
                infoFile.close();
            } else {
                logger.debug("Stream_Archive::terminate: info file already"
                        + " gone!");
            }

            // Close the index file
            if (indexFile != null) {
                indexFile.close();
            } else {
                logger.debug("Stream_Archive::terminate: "
                        + "index file already gone!");
            }
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
        }

//        rtpForwarder.end();
//        rtcpForwarder.end();
    }

    private void calculateMissedPackets(RTPHeader packetHeader) {
        // Initialise the RTP sequence if it hasn't been done
        if (!rtpSeqInit) {
            lastRtpSeq = packetHeader.getSequence() - 1;
            rtpSeqInit = true;
        }

        // Set the RTP sequence, taking account of wrapping
        lastRtpSeq++;
        if (lastRtpSeq > RTPHeader.MAX_SEQUENCE) {
            logger.debug("Stream_Archive::handlePacket(" + ssrc
                    + "): sequence wrap");
            lastRtpSeq = 0;
        }

        // If the packet sequence and the current sequence are not the same, we
        // missed one
        if (packetHeader.getSequence() != lastRtpSeq) {
            if (packetHeader.getSequence() < lastRtpSeq) {
                totalMissedPackets += packetHeader.getSequence()
                        + ((RTPHeader.MAX_SEQUENCE + 1) - lastRtpSeq);
            } else {
                totalMissedPackets +=
                    (packetHeader.getSequence() - lastRtpSeq) + 1;
            }
            lastRtpSeq = packetHeader.getSequence();
        }
    }

    /**
     * Handles an incoming RTP packet
     *
     * @param packet The packet to handle
     * @param time The time at which the packet arrived
     * @throws IOException
     */
    public void handleRTPPacket(ReusablePacket packet, long time)
            throws IOException {

        RTPHeader packetHeader = new RTPHeader(packet.getPacket());

        // Reject packets that have invalid data in them
        if ((packetHeader.getPacketType() >= MIN_RTCP_CONFLICT)
                && (packetHeader.getPacketType() <= MAX_RTCP_CONFLICT)) {
            return;
        }
        long offset = 0;
        String type = null;
        packetRecievedTime = time;
        if (bBadFileIO) {
            return;
        }

        // Initialization block -- we haven't started writing yet
        if (!writingOutput) {

            // Add the stream to the session
            try {
                session.setLock(owner);
                session.setPassword(ownerPass);
                session.addStream(String.valueOf(ssrc));
                if (archiveMgr != null) {
                    archiveMgr.addStream(String.valueOf(ssrc));
                }
                session.getUnlock();
            } catch (MalformedURLException e) {
                logger.error(ERROR_MESSAGE, e);
            }

            writingOutput = true;

            // This puts the initial stuff in the actual archive
            writeFileHeader((InetSocketAddress)
                    packet.getPacket().getSocketAddress());

            // This starts the info/index files
            writeInitialInfo();

            // Write out the timestamp for this first RTP packet in the file.
            setStringStreamKeyValue(Stream.STARTING_TIMESTAMP, String
                    .valueOf(packetHeader.getTimestamp()));
        }

        // Set the statistics
        totalBytes += packet.getPacket().getLength();
        totalPacketsSeen++;

        calculateMissedPackets(packetHeader);

        offset = packetRecievedTime - startTime;
        lastTimestamp = packetTimestamp;
        packetTimestamp = packetHeader.getTimestamp();

        // Forward the packet
        rtpForwarder.addPacket(packet.getPacket());

        // Store the packet
        writePacket(packet.getPacket(), Stream.RTP_PACKET, offset);

        // Add to the types of packets seen
        type = String.valueOf(packetHeader.getPacketType());
        if (typesSeen.indexOf(type) == -1) {
            String tb = "";
            typesSeen.add(type);

            // Store the types seen in the stream
            for (int i = 0; i < typesSeen.size(); i++) {
                tb += (String) typesSeen.get(i);
            }

            setStringStreamKeyValue(Stream.TYPES, tb);
        }
        packet.release();
    }

    /**
     * Handles an incoming RTCP packet
     *
     * @param packet The packet to handle
     * @param time The time the packet arrived
     */
    public void handleRTCPPacket(ReusablePacket packet, long time) {
        int offset = packet.getPacket().getOffset();
        int read = 0;
        packetRecievedTime = time;

        // Forward the packet
        rtcpForwarder.addPacket(packet.getPacket());

        // Don't do anything if there has been a file error
        if (bBadFileIO) {
            return;
        }

        // If output is being written, save the packet
        if (writingOutput) {
            long off = packetRecievedTime - startTime;
            writePacket(packet.getPacket(), Stream.RTCP_PACKET, off);
        }

        // Go through all the attached RTCP packets and handle them too
        try {
            while (offset < (packet.getPacket().getLength()
                    + packet.getPacket().getOffset())) {
                RTCPHeader header = new RTCPHeader(packet.getPacket().getData(),
                        offset, packet.getPacket().getLength() - read);
                int length = (header.getLength() + 1) * BYTES_PER_WORD;
                read += RTCPHeader.SIZE;
                offset += RTCPHeader.SIZE;
                processSubpacket(header, packet.getPacket().getData(), offset);
                offset += length - RTCPHeader.SIZE;
                read += length - RTCPHeader.SIZE;
            }
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
        }
        packet.release();
    }

    /**
     * Handles an RTCP Sub-packet
     *
     * @param packetHeader
     *            The header of the packet
     * @param packet
     *            The data of the packet
     * @param offset The offset where the data starts
     */
    private void processSubpacket(RTCPHeader packetHeader, byte packet[],
            int offset) {

        // If there is a file error, stop
        if (bBadFileIO) {
            return;
        }

        switch (packetHeader.getPacketType()) {
        // Ignore RR packets
        case RTCPHeader.PT_RR:
            break;

        // Ignore BYE packets
        case RTCPHeader.PT_BYE:
            break;

        case RTCPHeader.PT_SDES: {

            // Extract the CNAME and NAME from the source description
            // block (SDES)
            // of an RTCP packet. CNAME is the canonical name of the
            // participant
            // in the form <userid>@nnn.nnn.nnn.nnn. The SDES items that
            // follow the
            // header have the following format:
            // 1st 8 bits: SDES item id
            // 2nd 8 bits: item length in bytes
            // following bits: n bytes of data
            short length = 0;
            int curptr = offset;
            byte[] pSDES = packet;

            while (pSDES[curptr] != 0) {
                int id = pSDES[curptr];
                int itemStart = curptr + RTCPHeader.SDES_LENGTH_LENGTH
                    + RTCPHeader.SDES_TYPE_LENGTH;
                length = pSDES[curptr + RTCPHeader.SDES_TYPE_LENGTH];
                if (id < Stream.SDES_ID.length) {
                    String dbId = Stream.SDES_ID[id];
                    if (sdesInfo.get(dbId) == null) {
                        String value = new String(pSDES, itemStart, length);
                        setStringStreamKeyValue(dbId, value);
                        sdesInfo.put(dbId, value);
                    }
                }
                curptr += length + RTCPHeader.SDES_LENGTH_LENGTH
                    + RTCPHeader.SDES_TYPE_LENGTH;;
            }
        }
        break;

        // Ignore SR Packets
        case RTCPHeader.PT_SR:
            break;

        // Ignore APP Packets
        case RTCPHeader.PT_APP:
            break;

        // Ignore anything you don't know about
        default:
            logger.debug("Unknown RTCP packet type: "
                    + packetHeader.getPacketType());
            break;
        }
    }

    /**
     * Return the SSRC of the stream
     *
     * @return the ssrc
     */
    public long getSsrc() {
        return ssrc;
    }

    // Spit out info we know about the start of the stream
    private void writeInitialInfo() {
        try {
            FileOutputStream indexFileO = new FileOutputStream(indexFilename);

            // Create the information file
            FileOutputStream infoFileO = new FileOutputStream(infoFilename);
            Iterator<String> iterator = sdesInfo.keySet().iterator();
            logger.debug("Stream_Archive::writeInitialInfo: info filename "
                    + infoFilename);
            infoFile = new PrintWriter(infoFileO);

            // Store the ssrc and start time of the stream
            setStringStreamKeyValue(Stream.SSRC, String.valueOf(ssrc));
            setStringStreamKeyValue(
                    Stream.START_TIME, String.valueOf(startTime));

            // Store the sdes values if we have them
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String value = (String) sdesInfo.get(key);
                setStringStreamKeyValue(key, value);
            }

            // Start the index file
            indexFile = new DataOutputStream(indexFileO);
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            bBadFileIO = true;
        }
    }

    // Spit out the end-of-stream summary information:
    private void writeFinalInfo() {

        // Calculate the duration of the stream
        long endTime = packetRecievedTime;
        logger.debug("Stream_Archive::writeFinalInfo: info filename "
                + infoFilename);
        logger.debug("Stream_Archive::end time: " + endTime);

        // Store the types seen in the stream
        if (typesSeen.size() > 0) {
            String tb = "";

            for (int i = 0; i < typesSeen.size(); i++) {
                tb += (String) typesSeen.get(i);
            }

            setStringStreamKeyValue(Stream.TYPES, tb);
        }

        // Output the final information recorded as metadata
        setStringStreamKeyValue(Stream.END_TIME, String.valueOf(endTime));
        setStringStreamKeyValue(Stream.PACKETS_SEEN, String
                .valueOf(totalPacketsSeen));
        setStringStreamKeyValue(Stream.PACKETS_MISSED, String
                .valueOf(totalMissedPackets));
        setStringStreamKeyValue(Stream.BYTES, String.valueOf(totalBytes));
        setStringStreamKeyValue(Stream.FILE_SIZE, String.valueOf(fileSize));
    }

    // Opens the stream file for writing
    private void openFile() {
        try {
            if (streamFile == null) {
                FileOutputStream streamFileO = new FileOutputStream(
                        streamFilename);
                streamFile = new DataOutputStream(streamFileO);
                streamFileControl = streamFileO.getChannel();
            }
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            bBadFileIO = true;
        }
    }

    // Writes the index file
    private void writeIndex(long offset) {
        try {

            // If we're writing output to the stream file
            if (writingOutput && packetTimestamp != lastTimestamp) {

                // figure out where we are in the main file
                long pos = streamFileControl.position();

                // Write our index
                indexFile.writeLong(offset);
                indexFile.writeLong(pos);
                indexFile.flush();
            }
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            bBadFileIO = true;
        }
    }

    // Writes the header of the stream file
    private void writeFileHeader(InetSocketAddress source) {
        try {
            long seconds = 0;
            long uSeconds = 0;
            byte[] ipaddress = source.getAddress().getAddress();
            startTime = packetRecievedTime;
            logger.debug("Stream_Archive::writeFileHeader(" + ssrc + ")");
            openFile();

            seconds = (startTime / MS_PER_SEC);
            uSeconds = ((startTime - (seconds * MS_PER_SEC)) * MS_PER_SEC);
            streamFile.writeInt((int) seconds);
            streamFile.writeInt((int) uSeconds);
            streamFile.write(ipaddress, 0, BYTES_PER_ADDRESS);
            streamFile.writeShort(source.getPort());

            // Add to the file size 2 ints, 4 bytes and a short
            fileSize += BYTES_PER_INT + BYTES_PER_INT + BYTES_PER_ADDRESS
                + BYTES_PER_SHORT;

        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            bBadFileIO = true;
        }
    }

    // Writes a packet to disk
    private void writePacket(DatagramPacket packet, int type, long offset) {
        if (((archiveMgr != null) && !archiveMgr.isRecording())
                || (packet.getLength() == 0)) {
            return;
        }

        try {
            if (type == 0) {
                writeIndex(offset);
            }
            streamFile.writeShort(packet.getLength());
            streamFile.writeShort(type);
            streamFile.writeInt((int) offset);

            // Add 2 shorts and an int to the file
            fileSize += BYTES_PER_SHORT + BYTES_PER_SHORT + BYTES_PER_INT;

            streamFile.write(packet.getData(), packet.getOffset(),
                    packet.getLength());
            fileSize += packet.getLength();
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            bBadFileIO = true;
        }
    }

    // Stores some metadata in the infofile and database
    private void setStringStreamKeyValue(String key, String value) {
        if (infoFile != null && writingOutput) {
            try {
                session.setLock(owner);
                session.setPassword(ownerPass);
                if (!session.setStreamKeyValue(String.valueOf(ssrc), key,
                        value)) {
                    if (archiveMgr != null) {
                        archiveMgr.addStream(String.valueOf(ssrc));
                    }
                }
                session.getUnlock();
            } catch (MalformedURLException e) {
                logger.error(ERROR_MESSAGE, e);
            }
            infoFile.println(key + Stream.INFO_ITEM_SEPARATOR + value);
            infoFile.flush();
        }
    }

    /**
     * Returns the start time of the stream
     *
     * @return the start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the current duration of the stream
     *
     * @return the duration in ms
     */
    public long getDuration() {
        return packetRecievedTime - startTime;
    }
}
