/*
 * @(#)SDPParser.java
 * Created: 03-Jun-2005
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
package common;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class SDPParser {

    /**
     * A slash
     */
    public static final String SLASH = "/";

    /**
     * The version line prefix
     */
    public static final String VERSION = "v=";
    
    /**
     * The owner line prefix
     */
    public static final String OWNER = "o=";
    
    /**
     * The name prefix
     */
    public static final String NAME = "s=";
    
    /**
     * The description prefix
     */
    public static final String DESCRIPTION = "i=";
    
    /**
     * The time prefix
     */
    public static final String TIME = "t=";
    
    /**
     * The media prefix
     */
    public static final String MEDIA = "m=";
    
    /**
     * The connection prefix
     */
    public static final String CONNECTION = "c=";
    
    /**
     * The attribute prefix
     */
    public static final String ATTRIBUTE = "a=";
    
    /**
     * The rtptype attribute
     */
    public static final String ATTRIBUTE_RTPMAP = "rtpmap:";
    
    /**
     * The control attribute
     */
    public static final String ATTRIBUTE_CONTROL = "control:";
    
    /**
     * The RTP/AVP Media type
     */
    public static final String MEDIA_RTP_AVP = "RTP/AVP";
    
    /**
     * The URL connection type
     */
    public static final String CONNECTION_URL = "URL";
    
    /**
     * The URL separator
     */
    public static final String URL_PATH_SEPARATOR = SLASH;
    
    /**
     * The Separator between SDP Elements
     */
    public static final String SDP_SEPARATOR = " ";

    /**
     * An IPV4 Address Identifier
     */
    public static final String IPV4_ADDRESS = "IP4";

    /**
     * An Internet Address Identifier
     */
    public static final String INTERNET = "IN";

    /**
     * The End-Of-Line string
     */
    public static final String EOL = "\r\n";    

    // The string that separates elements of the data type
    private static final String DATATYPE_SEPARATOR = SLASH;

    // A list of the streams if this is a session describe request
    private Vector<String> streams = new Vector<String>();

    // The name of the session / stream
    private String name = "";

    // The description of the session
    private String description = "";

    // The type value of the RTP stream
    private String type = "";

    // The ssrc of the stream
    private String ssrc = "";

    // The type of data in the stream (audio/video/data)
    private String dataType = "";

    // The encoding of the stream
    private String encoding = "";

    // The owner of the session
    private String owner = "";

    // The ip address creator of the session
    private String address = "";

    // The starttime of the session
    private String startTime = String.valueOf(0);

    // The endtime of the session
    private String endTime = String.valueOf(0);

    /**
     * Creates a new SDPParser
     */
    public SDPParser() {
        // Does Nothing
    }
    
    /**
     * Parses SDP Data
     * 
     * @param data
     *            The data to parse
     * @throws RTSPResponseException 
     */
    public void parse(char[] data) throws RTSPResponseException {
        parse(data, 0, data.length);
    }

    /**
     * Parses SDP Data
     * 
     * @param data
     *            The data to parse
     * @param offset The offset of the valid data
     * @param length The length of the valid data
     * 
     * @throws RTSPResponseException 
     */
    public void parse(char[] data, int offset, int length) 
            throws RTSPResponseException {

        // Prepare to read the data from the packet as an SDP description
        BufferedReader reader = new BufferedReader(
                new CharArrayReader(data, offset, length));
        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                
                if (line.startsWith(NAME)) {

                    // The SDP name
                    name = line.substring(NAME.length());
                } else if (line.startsWith(DESCRIPTION)) {
                    
                    // The SDP information
                    description = line.substring(DESCRIPTION.length());
                } else if (line.startsWith(MEDIA)) {
                    
                    // An SDP media element
                    StringTokenizer tokens = null;
                    line = line.substring(MEDIA.length());
                    tokens = new StringTokenizer(line, SDP_SEPARATOR);
                    dataType = tokens.nextToken();
                    tokens.nextToken(); // Read the port
                    tokens.nextToken(); // Read the Media Type
                    type = tokens.nextToken();
                } else if (line.startsWith(CONNECTION)) {
                    
                    // An SDP connection element
                    StringTokenizer tokens = null;
                    line = line.substring(CONNECTION.length());
                    tokens = new StringTokenizer(line, SDP_SEPARATOR);
                    if (tokens.nextToken().equals(INTERNET)
                            && tokens.nextToken().equals(CONNECTION_URL)) {
                        String url = tokens.nextToken();
                        if (!streams.contains(url)) {
                            streams.add(url);
                        }
                    } else {
                        throw new RTSPResponseException(
                                "Unrecognized field in describe response");
                    }
                } else if (line.startsWith(ATTRIBUTE + ATTRIBUTE_CONTROL)) {
                    String url = line.substring((ATTRIBUTE
                            + ATTRIBUTE_CONTROL).length());
                    if (!streams.contains(url)) {
                        streams.add(url);
                    }
                } else if (line.startsWith(ATTRIBUTE + ATTRIBUTE_RTPMAP)) {
                    StringTokenizer tokens = null;
                    String encodingRate = null;
                    line = line.substring((ATTRIBUTE
                            + ATTRIBUTE_RTPMAP).length());
                    tokens = new StringTokenizer(line, SDP_SEPARATOR);
                    tokens.nextToken(); // Read the RTP Mapping
                    encodingRate = tokens.nextToken();
                    tokens = 
                        new StringTokenizer(encodingRate, URL_PATH_SEPARATOR);
                    encoding = tokens.nextToken();
                    tokens.nextToken(); // The clock rate
                } else if (line.startsWith(OWNER)) {
                    
                    // The SDP ssrc
                    StringTokenizer tokens = null;
                    line = line.substring(OWNER.length());
                    tokens = new StringTokenizer(line, SDP_SEPARATOR);
                    owner = tokens.nextToken();
                    ssrc = tokens.nextToken();
                    tokens.nextToken(); // Read the version
                    tokens.nextToken(); // Read the network
                    tokens.nextToken(); // Read the address type
                    address = tokens.nextToken();
                } else if (line.startsWith(TIME)) {
                    
                    // The SDP time
                    StringTokenizer tokens = null;
                    line = line.substring(TIME.length());
                    tokens = new StringTokenizer(line, SDP_SEPARATOR);
                    startTime = tokens.nextToken();
                    endTime = tokens.nextToken();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RTSPResponseException("Parse Error:" + e.getMessage());
        }
    }

    /**
     * Retrieves the streams if this is a session
     * 
     * @return A vector of String URLs
     */
    public Vector<String> getStreams() {
        return streams;
    }

    /**
     * Retrieves the name of this session
     * 
     * @return The name of the session
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the description of the session
     * 
     * @return The session description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retrieves the MIME Type of the session
     * 
     * @return The MIME Type of the session
     */
    public String getType() {
        if (encoding != null) {
            return dataType + DATATYPE_SEPARATOR + encoding;
        }
        return dataType + DATATYPE_SEPARATOR + type;
    }

    /**
     * Retrieves the SSRC of the stream
     * 
     * @return The SSRC of the stream
     */
    public String getSSRC() {
        return ssrc;
    }

    /**
     * Returns the owner of the session
     * 
     * @return the username of the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns the address of the session
     * 
     * @return the address 
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the start time of the session
     * 
     * @return the start time as a string
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time of the session
     * 
     * @return the end time as a string
     */
    public String getEndTime() {
        return endTime;
    }
}