/*
 * @(#)Stream.java
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

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;
import common.SDPParser;

/**
 * Represents a recorded stream in Arena
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Stream implements Comparable<Stream> {

    /**
     * A colon
     */
    public static final String COLON = ":";

    /**
     * The stream type
     */
    public static final String TYPE = NetworkEndpoint.MEMETIC + "Stream";

    /**
     * The time at which the stream started
     */
    public static final String STARTING_TIMESTAMP = NetworkEndpoint.MEMETIC
            + "has-first-rtp-timestamp";

    /**
     * The cname of the participant
     */
    public static final String CNAME = NetworkEndpoint.MEMETIC + "has-cname";

    /**
     * The name of the participant
     */
    public static final String NAME = Session.SUPPORT + "has-pretty-name";
    
    /**
     * The e-mail of the participant
     */
    public static final String EMAIL = NetworkEndpoint.MEMETIC + "has-email";
    
    /**
     * The phone number of the participant
     */
    public static final String PHONE = NetworkEndpoint.MEMETIC + "has-phone";
    
    /**
     * The location of the participant
     */
    public static final String LOC = NetworkEndpoint.MEMETIC + "has-location";
    
    /**
     * The media tool of the participant
     */
    public static final String TOOL = NetworkEndpoint.MEMETIC + "has-tool";
    
    /**
     * The note of the participant
     */
    public static final String NOTE = NetworkEndpoint.MEMETIC + "has-note";

    /**
     * The stream identifier
     */
    public static final String SSRC = NetworkEndpoint.MEMETIC + "has-ssrc";

    /**
     * The start time of the recording of the stream
     */
    public static final String START_TIME = NetworkEndpoint.MEMETIC
            + "has-media-start-time";

    /**
     * The types of the packets in the stream
     */
    public static final String TYPES = NetworkEndpoint.MEMETIC
        + "has-stream-type";

    /**
     * The duration of the stream
     */
    public static final String END_TIME = NetworkEndpoint.MEMETIC
            + "has-media-end-time";

    /**
     * The number of packets seen in the stream
     */
    public static final String PACKETS_SEEN = NetworkEndpoint.MEMETIC
            + "has-packets-seen";

    /**
     * The number of packets missed in the stream
     */
    public static final String PACKETS_MISSED = NetworkEndpoint.MEMETIC
            + "has-packets-missed";

    /**
     * The number of bytes in the stream
     */
    public static final String BYTES = NetworkEndpoint.MEMETIC + "has-bytes";

    /**
     * The size of the file
     */
    public static final String FILE_SIZE = NetworkEndpoint.MEMETIC 
        + "has-file-size";
    
    /**
     * Separator between info items
     */
    public static final String INFO_ITEM_SEPARATOR = COLON;

    /**
     * The extension of the info file
     */
    public static final String INFO_FILE_EXTENSION = "_info";

    /**
     * The extension of the index file
     */
    public static final String INDEX_FILE_EXTENSION = "_index";

    /**
     * The default data type
     */
    public static final String DEFAULT_DATA_TYPE = "data";

    /**
     * The packet is an RTP packet
     */
    public static final int RTP_PACKET = 0;

    /**
     * The packet is an RTCP packet
     */
    public static final int RTCP_PACKET = 1;
    
    /**
     * The SDES Info index in the database
     */  
    public static final String[] SDES_ID =
        new String[]{null, CNAME, NAME, EMAIL, PHONE, LOC, TOOL, NOTE};
    
    // The number of digits in each time element
    private static final int DIGITS_PER_TIME_ITEM = 2;

    // The number of seconds in a minute
    private static final int SECS_PER_MIN = 60;

    // The number of seconds in an hour
    private static final int SECS_PER_HOUR = 3600;

    // The number of ms in a second
    private static final int MS_PER_SEC = 1000;

    // The separator between elements of the time
    private static final String TIME_STRING_SEPARATOR = COLON;

    // The encoding of a ScreenStreamer stream
    private static final String SCREENSTREAMER_ENCODING = "Jpeg";

    // The type for a ScreenStreamer stream
    private static final String SCREENSTREAMER_TYPE = "Screen";

    // The default encoding of a stream
    private static final String DEFAULT_ENCODING = "None";

    // The default RTP type of the packets
    private static final String DEFAULT_PACKET_TYPE = String.valueOf(0);

    // The character that separates a name from a description
    private static final String NAME_SEPARATOR = "/";

    // The port for the sdp description
    private static final String DESCRIPTION_PORT = "50000";

    // The version of the sdp description
    private static final String DESCRIPTION_VERSION = String.valueOf(0);

    // The address for the sdp description
    private static final String DESCRIPTION_ADDRESS = "127.0.0.1";

    // The response sent when everything is OK
    private static final String OK_RESPONSE = "OK";

    // The default number of bytes in the stream
    private static final String DEFAULT_BYTES = String.valueOf(0);

    // The default number of packets missed in the stream
    private static final String DEFAULT_PACKETS_MISSED = String.valueOf(0);

    // The default number of packets seen in the stream
    private static final String DEFAULT_PACKETS_SEEN = String.valueOf(0);

    // The default end time of the stream
    private static final String DEFAULT_END_TIME = String.valueOf(0);
    
    // The end time if the stream is disabled
    private static final String DISABLED_END_TIME = String.valueOf(-1);

    // The default first timestamp in the stream
    private static final String DEFAULT_STARTING_TIMESTAMP = String.valueOf(0);

    // The default start time of the stream
    private static final String DEFAULT_START_TIME = String.valueOf(0);

    // The log file
    private static Log logger = LogFactory.getLog(Stream.class.getName());

    // The map of parameters
    private HashMap<String,String> parameterMap = new HashMap<String,String>();

    // The id of the session
    private String sessionId = null;

    // The SSRC of the stream
    private String ssrc = "";
    
    // True if the stream is enabled in the recording
    private boolean enabled = true;
    
    // The uri of the stream
    private String uri = null;

    /**
     * Creates a new Arena Stream
     * 
     * @param id
     *            The id of the session of which this stream is a part
     * @param ssrc
     *            The ssrc of the stream
     * @param uri
     *            The uri of the stream in the database
     */
    public Stream(String id, String ssrc, String uri) {
        this.sessionId = id;
        this.ssrc = ssrc;
        this.uri = uri;

        // Add some standard parameters
        reset();
    }

    /**
     * Creates a new Arena Stream
     * 
     * @param id
     *            The id of the session of which this stream is a part
     * @param values The parameters for the stream
     * @param uri The uri of the stream in the database
     */
    public Stream(String id, HashMap<String,String> values, String uri) {
        this.sessionId = id;
        this.ssrc = (String) values.get(SSRC);
        this.uri = uri;
        parameterMap = values;
    }
    
    /**
     * Resets the stream back to default values
     *
     */
    public void reset() {
        logger.debug("Resetting Stream " + ssrc);
        parameterMap.put(START_TIME, DEFAULT_START_TIME);
        parameterMap.put(STARTING_TIMESTAMP, DEFAULT_STARTING_TIMESTAMP);
        parameterMap.put(END_TIME, DEFAULT_END_TIME);
        parameterMap.put(PACKETS_SEEN, DEFAULT_PACKETS_SEEN);
        parameterMap.put(PACKETS_MISSED, DEFAULT_PACKETS_MISSED);
        parameterMap.put(BYTES, DEFAULT_BYTES);
    }

    /**
     * Sets a parameter for the stream
     * 
     * @param name
     *            The name of the parameter
     * @param value
     *            The value of the parameter
     */
    public void setParameter(String name, String value) {
        parameterMap.put(name, value);
    }

    /**
     * Handles an RTSP Describe Request
     * 
     * @param describeRequest
     *            The request to handle
     */
    public void handleDescribeStream(RTSPDescribeRequest describeRequest) {
        RTSPRequestPacket request = describeRequest.getRequestPacket();

        RTSPResponse response = new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE,
                request);
        response.setHeader(Headers.RTSP_CONTENT_TYPE,
                Headers.RTSP_CONTENT_TYPE_SDP);
        
        logger.debug("arena stream gets describeStream " + getSsrc());

        response = handleDescribeStream(response);

        // Send the response
        response.send();
    }
    
    /**
     * Adds the description to an existing RTSPResponse
     * @param response The response to add to
     * @return The response with the description added
     */
    public RTSPResponse handleDescribeStream(RTSPResponse response) {
        response.bodyAppend(SDPParser.VERSION + DESCRIPTION_VERSION
                + SDPParser.EOL);
        response.bodyAppend(SDPParser.OWNER + sessionId 
                + SDPParser.SDP_SEPARATOR + ssrc + SDPParser.SDP_SEPARATOR
                + Calendar.getInstance().getTimeInMillis()
                + SDPParser.SDP_SEPARATOR + SDPParser.INTERNET
                + SDPParser.SDP_SEPARATOR + SDPParser.IPV4_ADDRESS 
                + SDPParser.SDP_SEPARATOR + DESCRIPTION_ADDRESS
                + SDPParser.EOL);
        response.bodyAppend(SDPParser.NAME + getCName() + SDPParser.EOL);
        response.bodyAppend(SDPParser.DESCRIPTION + getName() + SDPParser.EOL);
        response.bodyAppend(SDPParser.MEDIA + getDataType() 
                + SDPParser.SDP_SEPARATOR + DESCRIPTION_PORT 
                + SDPParser.SDP_SEPARATOR + SDPParser.MEDIA_RTP_AVP
                + SDPParser.SDP_SEPARATOR + getPacketTypes() + SDPParser.EOL);
        response.bodyAppend(SDPParser.ATTRIBUTE + SDPParser.ATTRIBUTE_RTPMAP 
                + getPacketTypes() + SDPParser.SDP_SEPARATOR
                + getEncoding() + SDPParser.URL_PATH_SEPARATOR
                + getClockRate() + SDPParser.EOL);
        if (enabled) {
            response.bodyAppend(SDPParser.TIME + (getStartTime() / MS_PER_SEC) 
                    + SDPParser.SDP_SEPARATOR
                    + ((getEndTime()) / MS_PER_SEC) + SDPParser.EOL);
        } else {
            response.bodyAppend(SDPParser.TIME + DEFAULT_START_TIME
                    + SDPParser.SDP_SEPARATOR
                    + DISABLED_END_TIME + SDPParser.EOL);
        }
        return response;
    }

    /**
     * Returns the id of the stream
     * 
     * @return The id of the stream
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the ssrc of the stream
     * 
     * @return The ssrc of the stream
     */
    public String getSsrc() {
        return ssrc;
    }

    /**
     * Returns the start time of the stream
     * 
     * @return The start time of the stream
     */
    public long getStartTime() {
        String start = (String) parameterMap.get(START_TIME);
        if (start != null) {
            return Long.valueOf(start).longValue();
        }
        return 0;
    }

    /**
     * Returns the end time of the stream
     * 
     * @return The end time of the stream
     */
    public long getEndTime() {
        long startTime = getStartTime();
        if (startTime == 0) {
            return 0;
        }
        String end = (String) parameterMap.get(END_TIME);
        if (end != null) {
            long endTime = Long.valueOf(end).longValue();
            if (endTime > startTime) {
                return endTime;
            } 
            return startTime;
        }
        return 0;
    }

    /**
     * Returns the first timestamp in the stream
     * 
     * @return The start timestamp of the stream
     */
    public double getStartingTimestamp() {
        String start = (String) parameterMap.get(STARTING_TIMESTAMP);
        if (start != null) {
            return Double.valueOf(start).doubleValue();
        }
        return 0;
    }

    /**
     * Returns the packet types in the stream
     * 
     * @return The types of the packets of the stream
     */
    public String getPacketTypes() {
        String types = (String) parameterMap.get(TYPES);
        if (types != null) {
            return types;
        }
        return DEFAULT_PACKET_TYPE;
    }

    /**
     * Returns the length of the stream formated as hh:mm:ss
     * 
     * @return the length of the stream as a string
     */
    public String getDurationString() {
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(0);
        format.setMinimumIntegerDigits(DIGITS_PER_TIME_ITEM);
        long duration = (getEndTime() - getStartTime()) / MS_PER_SEC;
        long hours = (duration / SECS_PER_HOUR);
        duration -= (hours * SECS_PER_HOUR);
        long minutes = (duration / SECS_PER_MIN);
        duration -= (minutes * SECS_PER_MIN);
        long seconds = duration;
        return (format.format(hours) + TIME_STRING_SEPARATOR
                + format.format(minutes) + TIME_STRING_SEPARATOR 
                + format.format(seconds));
    }

    /**
     * Returns the number of packets in the stream
     * 
     * @return The number of packets in the stream
     */
    public int getNumPacketsSeen() {
        String packets = (String) parameterMap.get(PACKETS_SEEN);
        if (packets != null) {
            return Integer.valueOf(packets).intValue();
        }
        return 0;
    }

    /**
     * Returns the number of dropped packets
     * 
     * @return The number of packets dropped during recording
     */
    public int getNumPacketsMissed() {
        String packets = (String) parameterMap.get(PACKETS_MISSED);
        if (packets != null) {
            return Integer.valueOf(packets).intValue();
        }
        return 0;
    }

    /**
     * Returns the number of bytes recorded
     * 
     * @return The number of bytes in the stream
     */
    public int getNumBytes() {
        String bytes = (String) parameterMap.get(BYTES);
        if (bytes != null) {
            return Integer.valueOf(bytes).intValue();
        }
        return 0;
    }

    /**
     * Returns the name of the stream
     * 
     * @return the name of the stream
     */
    public String getName() {
        String name = (String) parameterMap.get(NAME);
        String note = (String) parameterMap.get(NOTE);
        if (name == null) {
            if (note != null) {
                return note;
            }
            return "";
        }
        if (note != null) {
            return name + NAME_SEPARATOR + note;
        }
        return name;
    }

    /**
     * Returns the CName of the stream
     * 
     * @return the CNAME of the stream
     */
    public String getCName() {
        String name = (String) parameterMap.get(CNAME);
        if (name == null) {
            return "";
        }
        return name;
    }

    /**
     * Returns the data type of the stream
     * 
     * @return the data type
     */
    public String getDataType() {
        String packetType = getPacketTypes();
        if (packetType == null) {
            packetType = DEFAULT_PACKET_TYPE;
        }
//        String type = (String) Server.getDataTypes()
//                .get(Integer.valueOf(packetType));
//        if (type != null) {
//            return type;
//        }
        return DEFAULT_DATA_TYPE;
    }

    /**
     * Returns the encoding of the stream
     * 
     * @return the encoding
     */
    public String getEncoding() {
        String packetType = getPacketTypes();
        if (packetType == null) {
            packetType = DEFAULT_PACKET_TYPE;
        }
//        String encoding = (String) Server.getMediaEncodings().get(Integer
//                .valueOf(packetType));
//        if (encoding != null) {
//            return encoding;
//        }
        return DEFAULT_ENCODING;
    }

    /**
     * Returns the clock rate of the stream
     * 
     * @return the clock rate
     */
    public int getClockRate() {
        String packetType = getPacketTypes();
        if (packetType == null) {
            packetType = DEFAULT_PACKET_TYPE;
        }
//        String clockRate = (String) Server.getClockRates().get(
//                Integer.valueOf(packetType));
//        if (clockRate != null) {
//            return Integer.parseInt(clockRate);
//        }
        return 0;
    }
    
    /**
     * Returns the size of the file stored on disk
     * 
     * @return the file size in bytes
     */
    public long getFileSize() {
        String filesize = (String) parameterMap.get(FILE_SIZE);
        if (filesize != null) {
            return Integer.valueOf(filesize).intValue();
        }
        return 0;
    }
    
    /**
     * Sets a value
     * @param key The key of the value
     * @param value The value
     */
    public void set(String key, String value) {
        parameterMap.put(key, value);
    }

    /**
     * Returns all metadata known about the stream
     * 
     * @return the metadata map
     */
    public HashMap<String,String> getValues() {
        return parameterMap;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Stream a) {
//        if (!(a instanceof Stream)) {
//            return 0;
//        }
        Stream s1 = (Stream) a;
        Stream s2 = this;

        String name1 = s1.getName();
        String name2 = s2.getName();
        String cname1 = s1.getCName();
        String cname2 = s2.getCName();
        String type1 = s1.getPacketTypes();
        String type2 = s2.getPacketTypes();

        int diff = cname1.compareTo(cname2);
        if (diff == 0) {
            diff = name1.compareTo(name2);
            if (diff == 0) {
                return type1.compareTo(type2);
            }

            return diff;
        }
        return diff;
    }
    
    /**
     * Returns true if the stream is enabled
     * 
     * @return true if the stream is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets the enabled status of the stream
     * @param enabled True if the stream is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Returns the description of the stream
     * @return the description
     */
    public String getDescription() {
        String name = getName();
        int slash = name.indexOf(NAME_SEPARATOR);
        if (slash != -1) {
            name = name.substring(0, slash);
        }
        if (name.equals("")) {
            name = getCName();
        }
        if (name.equals("")) {
            name = getSsrc();
        }
        return name;
    }
    
    /**
     * Returns the type (Audio, Video, Screen, None) of a stream
     * 
     * @return the type
     */
    public String getType() {
        String type = getDataType();
        type = type.substring(0, 1).toUpperCase()
            + type.substring(1).toLowerCase();
        if (getEncoding().equalsIgnoreCase(SCREENSTREAMER_ENCODING)) {
            type = SCREENSTREAMER_TYPE;
        }
        return type;
    }
    
    /**
     * Returns the uri of the stream in the database
     * @return The uri of the stream
     */
    public String getUri() {
        return uri;
    }
}