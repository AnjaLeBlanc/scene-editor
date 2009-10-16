/*
 * @(#)Headers.java
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

package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Set;

/**
 * Handles the parsing of RTSP header lines.
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Headers {

    /**
     * A space
     */
    public static final String SPACE = " ";

    /**
     * An equals char
     */
    public static final String EQUALS = "=";

    /**
     * A semi-colon!
     */
    public static final String SEMI_COLON = ";";

    /**
     * A Colon!
     */
    public static final String COLON = ":";

    /**
     * The RTSP (added) Stream header
     */
    public static final String RTSP_STREAM = "Stream";
    
    /**
     * The RTSP Version
     */
    public static final String RTSP_VERSION = "RTSP/1.0";

    /**
     * The HTTP Version
     */
    public static final String HTTP_VERSION = "HTTP/1.1";
    
    /**
     * The encapsulated RTSP content type
     */
    public static final String RTSP_ENCAPSULATED = 
        "application/x-rtsp-tunnelled";

    /**
     * The RTSP Authenticate Header
     */
    public static final String RTSP_AUTHENTICATE = "WWW-Authenticate";

    /**
     * The RTSP Authorization Header
     */
    public static final String RTSP_AUTH = "Authorization";

    /**
     * The RTSP Basic Authorization Type
     */
    public static final String RTSP_AUTH_BASIC = "Basic";

    /**
     * The RTSP CSeq Header
     */
    public static final String RTSP_CSEQ = "CSeq";

    /**
     * The RTSP Session Header
     */
    public static final String RTSP_SESSION = "Session";

    /**
     * The RTSP Content Length
     */
    public static final String RTSP_CONTENT_LENGTH = "Content-Length";

    /**
     * The RTSP Content Type
     */
    public static final String RTSP_CONTENT_TYPE = "Content-Type";

    /**
     * The RTSP RTP Address Header
     */
    public static final String RTSP_ADDRESS = "Address";

    /**
     * The RTSP RTP Port Header
     */
    public static final String RTSP_PORT = "Port";
    
    /**
     * The pragma header
     */
    public static final String PRAGMA = "Pragma";
    
    /**
     * The pragma no-cache header
     */
    public static final String NO_CACHE = "no-cache";
    
    /**
     * The Cache control header
     */
    public static final String CACHE_CONTROL = "Cache-Control";

    /**
     * The RTSP Transport
     */
    public static final String RTSP_TRANSPORT = "Transport";

    /**
     * The RTSP Range header
     */
    public static final String RTSP_RANGE = "Range";

    /**
     * The RTSP Scale header
     */
    public static final String RTSP_SCALE = "Scale";

    /**
     * The RTSP Range header separator
     */
    public static final String RTSP_RANGE_SEPARATOR = SEMI_COLON;

    /**
     * The RTSP ntp time identifier
     */
    public static final String RTSP_NTP_TIME = "ntp=";

    /**
     * The RTSP time of now
     */
    public static final String RTSP_TIME_NOW = "now";
    
    /**
     * The RTSP Transport separator between port numbers
     */
    public static final String RTSP_TRANSPORT_PORT_SEP = "-";
    
    /**
     * The RTSP Transport separator between variables and values 
     */
    public static final String RTSP_TRANSPORT_VAR_VAL_SEP = EQUALS;

    /**
     * The RTSP Transport Separator
     */
    public static final String RTSP_TRANSPORT_SEPARATOR = SEMI_COLON;

    /**
     * The RTSP Transport Profile Separator
     */
    public static final String RTSP_TRANSPORT_PROFILE_SEPARATOR = "/";

    /**
     * The RTSP RTP Transport Type
     */
    public static final String RTSP_TRANSPORT_PROTOCOL_RTP = "RTP";

    /**
     * The RTSP AVP Profile Type
     */
    public static final String RTSP_TRANSPORT_PROFILE_AVP = "AVP";

    /**
     * The RTSP Lower Transport UDP
     */
    public static final String RTSP_TRANSPORT_LOWER_UDP = "UDP";

    /**
     * The RTSP Lower Transport TCP
     */
    public static final String RTSP_TRANSPORT_LOWER_TCP = "TCP";

    /**
     * The RTSP Transport Mode subheader
     */
    public static final String RTSP_TRANSPORT_MODE = "mode";

    /**
     * The RTSP Transport PLAY mode
     */
    public static final String RTSP_TRANSPORT_MODE_PLAY = "PLAY";

    /**
     * The RTSP Transport RECORD mode
     */
    public static final String RTSP_TRANSPORT_MODE_RECORD = "RECORD";

    /**
     * The RTSP Transport Unicast subheader
     */
    public static final String RTSP_TRANSPORT_UNICAST = "unicast";

    /**
     * The RTSP Transport Multicast subheader
     */
    public static final String RTSP_TRANSPORT_MULTICAST = "multicast";

    /**
     * The RTSP Transport Destination subheader
     */
    public static final String RTSP_TRANSPORT_DESTINATION = "destination";

    /**
     * The RTSP Transport Interleaved subheader
     */
    public static final String RTSP_TRANSPORT_INTERLEAVED = "interleaved";

    /**
     * The RTSP Transport Append subheader
     */
    public static final String RTSP_TRANSPORT_APPEND = "append";

    /**
     * The RTSP Transport TTL subheader
     */
    public static final String RTSP_TRANSPORT_TTL = "ttl";

    /**
     * The RTSP Transport Layers subheader
     */
    public static final String RTSP_TRANSPORT_LAYERS = "layers";

    /**
     * The RTSP Transport Port subheader
     */
    public static final String RTSP_TRANSPORT_PORT = "port";

    /**
     * The RTSP Transport Client-port subheader
     */
    public static final String RTSP_TRANSPORT_CLIENT_PORT = "client_port";

    /**
     * The RTSP Transport Server-port subheader
     */
    public static final String RTSP_TRANSPORT_SERVER_PORT = "server_port";

    /**
     * The RTSP Transport SSRC subheader
     */
    public static final String RTSP_TRANSPORT_SSRC = "ssrc";

    /**
     * The RTSP Transport encryption type subheader
     */
    public static final String RTSP_TRANSPORT_ENC_TYPE = "encryption_type";

    /**
     * The RTSP Transport encryption key subheader
     */
    public static final String RTSP_TRANSPORT_ENC_KEY = "encryption_key";

    /**
     * The application/sdp content type
     */
    public static final String RTSP_CONTENT_TYPE_SDP = "application/sdp";

    /**
     * The text/html content type
     */
    public static final String RTSP_CONTENT_TYPE_HTML = "text/html";

    /**
     * The text/plain content type
     */
    public static final String RTSP_CONTENT_TYPE_TEXT = "text/plain";

    /**
     * The text/parameters content type
     */
    public static final String RTSP_CONTENT_TYPE_PARAMTERS = "text/parameters";

    /**
     * The application/x-www-form-urlencoded content type
     */
    public static final String RTSP_CONTENT_TYPE_URLENC =
        "application/x-www-form-urlencoded";

    /**
     * The separation character between a header and its value
     */
    public static final String HEADER_SEPARATOR = COLON;

    /**
     * The playback stream prefix
     */
    public static final String STREAM_PREFIX = "stream_";
    
    /**
     * The prefix of the boundary in a multipart file
     */
    public static final String MULTIPART_BOUNDARY_PREFIX = "--";
    
    /**
     * The multipart boundary start
     */
    public static final String MULTIPART_BOUNDARY = "boundary=";
    
    /**
     * The RTSP OK Response
     */
    public static final int RTSP_OK = 200;

    /**
     * The RTSP Bad Request Response
     */
    public static final int RTSP_BAD_REQUEST = 400;

    /**
     * The RTSP Unauthorized Response
     */
    public static final int RTSP_UNAUTHORIZED = 401;

    /**
     * The RTSP Forbidden Response
     */
    public static final int RTSP_FORBIDDEN = 403;

    /**
     * The RTSP Not Found Response
     */
    public static final int RTSP_NOT_FOUND = 404;

    /**
     * The RTSP Not Acceptable response
     */
    public static final int RTSP_NOT_ACCEPTABLE = 406;

    /**
     * The RTSP Unrecognized media response
     */
    public static final int RTSP_BAD_MEDIA = 415;

    /**
     * The RTSP Invalid parameter response
     */
    public static final int RTSP_INVALID_PARAMETER = 451;

    /**
     * The RTSP Method not valid in state response
     */
    public static final int RTSP_INVALID_STATE = 455;

    /**
     * The RTSP Invalid header field response
     */
    public static final int RTSP_INVALID_HEADER_FIELD = 456;

    /**
     * The RTSP Server Error response
     */
    public static final int RTSP_SERVER_ERROR = 500;

    /**
     * The RTSP Not Implemented response
     */
    public static final int RTSP_NOT_IMPLEMENTED = 501;
    
    /**
     * The path separator in URIs
     */
    public static final char URI_PATH_SEPARATOR = '/';

    /**
     * The encoding used by URL query variables
     */
    public static final String URL_ENCODING = "UTF-8";

    /**
     * The string that separates query values from variables
     */
    public static final String QUERY_VAL_SEPARATOR = EQUALS;

    /**
     * The string that separates query variables
     */
    public static final String QUERY_VAR_SEPARATOR = "&";
    
    /**
     * End of line string
     */
    public static final String EOL = "\r\n";
    
    /**
     * The string that separates the method from the url
     */
    public static final String METHOD_SEPARATOR = SPACE;
    
    /**
     * The string that separates the port from the server
     */
    public static final String PORT_SEPARATOR = COLON;
    
    // The number of parts in a header
    private static final int PARTS_PER_HEADER = 2;

    // The string that joins a multi-line header
    private static final String HEADER_JOIN_CHAR = SPACE;

    // The number of characters that we will need to go back by
    private static final int MARK_VALUE = 8196;

    // A map of the headers
    private HashMap<String,String> map = new HashMap<String,String>();

    // True if the headers have been read
    private boolean done = false;

    /**
     * Reads a header line from the stream
     * 
     * @param reader
     *            The reader to read the line from
     * @throws IOException
     */
    public void parseHeaderLine(BufferedReader reader) throws IOException {

        // Get the first line
        String line = reader.readLine();
        String nextLine = null;
        String[] parts = null;
        String header = null;
        String value = null;

        // Read the next line, ensuring we can go back if needed
        reader.mark(MARK_VALUE);
        nextLine = reader.readLine();

        // While the next line starts with linear white space, fold the
        // header back up
        while ((nextLine.length() > 0)
                && Character.isWhitespace(nextLine.charAt(0))) {
            line += HEADER_JOIN_CHAR + nextLine.trim();
            reader.mark(MARK_VALUE);
            nextLine = reader.readLine();
        }

        // If the next line is empty, the headers have been read
        if (nextLine.trim().length() == 0) {
            done = true;
        } else {
            
            // If the next is not empty, it is another header, so go back and
            // read it again
            reader.reset();
        }

        // Get the header and value part of the line
        parts = line.split(HEADER_SEPARATOR, PARTS_PER_HEADER);
        header = parts[0].trim();
        value = parts[1].trim();

        // If the header exists and was chosen to be recognized, store it
        if (header.length() > 0) {
            map.put(header, value);
        }
    }

    /**
     * Determine if the next line is a complete header or not
     * 
     * @param reader
     *            The reader to read the line from
     * @return True if there is a header to read
     * @throws IOException 
     */
    public boolean completeHeaderLine(BufferedReader reader) 
            throws IOException {
        if (done) {
            return false;
        }
        try {
            String line = null;
            reader.mark(MARK_VALUE);
            line = reader.readLine();
            if (line == null) {
                throw new SocketException("End of Stream");
            } else if (line.trim().length() == 0) {
                done = true;
                return false;
            } else {
                reader.reset();
                return true;
            }
        } catch (SocketTimeoutException e) {
            done = true;
            return false;
        }
    }

    /**
     * Returns true if the headers have all been read
     * @return True if all the headers have been read
     */
    public boolean isEndOfHeaders() {
        return done;
    }

    /**
     * Retrieves a header
     * 
     * @param header
     *            The header to get
     * @return The value of the header
     */
    public String getHeader(String header) {
        return (String) map.get(header);
    }

    /**
     * Resets the headers to be used again
     * 
     */
    public void reset() {
        done = false;
    }
    
    /**
     * Returns the set of headers
     * @return the set of headers
     */
    public Set<String> getKeySet() {
        return map.keySet();
    }
    
    /**
     * Returns the header value
     * @param o the header to get the value of
     * @return the value of the header
     */
    public Object get(Object o) {
        return map.get(o);
    }
}