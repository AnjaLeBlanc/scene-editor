/*
 * @(#)RTSPResponsePacket.java
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

package client.rtsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

import common.Headers;

/**
 * Represents a response to an RTSP request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPResponsePacket {

    // The tokens that separate the response line
    private static final String METHOD_SEPARATOR_TOKENS = " \t";

    // The RTSP response code
    private int code = 0;

    // The reason for the code
    private String reason = "";

    // The version of RTSP in this response
    private String version = "";

    // The data in the response
    private char[] body = new char[0];

    // The headers in the response
    private Headers headers = new Headers();

    // True if the response line has been seen
    private boolean gotResponseLine = false;

    // The length of the data in this packet
    private int contentLength = 0;

    /**
     * Creates a new RTSP Response Packet
     */
    public RTSPResponsePacket() {
        
        // Does Nothing
    }

    /**
     * Parses the response received into a packet
     * 
     * @param reader
     *            The reader to read the packet from
     * @return True if the parsing was successful
     * @throws IOException
     */
    public boolean parseResponse(BufferedReader reader) throws IOException {
        
        // Check if the next line is a complete header line
        boolean result = false;
        headers.reset();
        gotResponseLine = false;
        result = headers.completeHeaderLine(reader);

        // If it is ...
        if (result) {

            // If we haven't got the response line yet...
            if (!gotResponseLine()) {

                // Get the response line
                parseResponseLine(reader);

                // Read the headers
                while (headers.completeHeaderLine(reader)) {
                    headers.parseHeaderLine(reader);
                }
            } else {
                // If we have the request line, and the header line was 
                // complete ...

                // Read the headers
                while (headers.completeHeaderLine(reader)) {
                    headers.parseHeaderLine(reader);
                }
            }
        }

        // If all the headers have been read, or at least the response line ...
        if ((headers.isEndOfHeaders() || gotResponseLine())) {

            // Initialise using the remains of the data
            boolean returnValue = init(reader);
            return returnValue;
        }

        // If the request line was not read, return error
        return false;
    }

    /**
     * Returns True if the response line has been seen
     * 
     * @return True if the response line has been seen
     */
    public boolean gotResponseLine() {
        return gotResponseLine;
    }

    /**
     * Parses the first line of the response
     * 
     * @param reader
     *            The reader to read the line from
     * @throws IOException
     */
    public void parseResponseLine(BufferedReader reader) throws IOException {
        String responseLine = reader.readLine();
        StringTokenizer tokens = null;

        // Indicate that the response line has been seen
        gotResponseLine = true;

        // Read the response line
        if ((responseLine == null) || (responseLine.length() == 0)) {
            throw new IOException("Response Line was not correct: "
                    + responseLine);
        }

        // Parse the response line into version, code and reason
        tokens = new StringTokenizer(responseLine, METHOD_SEPARATOR_TOKENS);
        if (!tokens.hasMoreTokens()) {
            throw new IOException("Response line is missing version: "
                    + responseLine);
        }
        setVersion(tokens.nextToken());
        if (!tokens.hasMoreTokens()) {
            throw new IOException("Response line is missing code: "
                    + responseLine);
        }
        setCode(tokens.nextToken());
        reason = "";
        while (tokens.hasMoreTokens()) {
            reason += tokens.nextToken() + " ";
        }
    }

    /**
     * Initialises the packet with the data
     * 
     * @param reader
     *            The reader to read the data from
     * @return True if the initialisation was successful
     * @throws IOException
     */
    public boolean init(BufferedReader reader) throws IOException {
        
        // The content length
        contentLength = getContentLength();

        // Read all the data from the stream and store it
        body = new char[contentLength];
        if (contentLength > 0) {
            int chars = reader.read(body, 0, contentLength);
            int charsRead = 0;
            while ((chars < contentLength) && (charsRead >= 0)) {
                charsRead = reader.read(body, chars, contentLength - chars);
                if (charsRead > 0) {
                    chars += charsRead;
                }
            }
            if (chars < contentLength) {
                char[] temp = new char[chars];
                System.arraycopy(body, 0, temp, 0, chars);
                body = temp;
            }
        }

        // This was done sucessfully
        return true;
    }

    // Returns a particular header
    private String getHeader(String head) {
        return headers.getHeader(head);
    }

    /**
     * Returns the session header
     * @return the session header value
     */
    public String getSession() {
        return headers.getHeader(Headers.RTSP_SESSION);
    }

    /**
     * Returns the content-type header
     * @return the content type header value
     */
    public String getContentType() {
        return headers.getHeader(Headers.RTSP_CONTENT_TYPE);
    }

    /**
     * Returns the WWW-Authenticate header
     * @return the authenticate header value
     */
    public String getAuthenticate() {
        return headers.getHeader(Headers.RTSP_AUTHENTICATE);
    }

    /**
     * Returns the content-length header
     * @return The content-length header
     */
    public int getContentLength() {
        String len = getHeader(Headers.RTSP_CONTENT_LENGTH);
        if ((len == null) || (len.length() == 0)) {
            contentLength = 0;
        } else {
            contentLength = Integer.valueOf(len).intValue();
        }
        return contentLength;
    }

    /**
     * Returns the RTSP response code header
     * @return The RTSP response code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the reason for the response code
     * @return The reason for the code
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the packet data
     * @return The packet data
     */
    public char[] getData() {
        return body;
    }

    /**
     * Returns the RTP Address header
     * @return the RTP address header
     */
    public String getRTPAddress() {
        return getHeader(Headers.RTSP_ADDRESS);
    }

    /**
     * Returns the RTP Port header
     * @return the RTP port header
     */
    public int getRTPPort() {
        return Integer.valueOf(getHeader(Headers.RTSP_PORT)).intValue();
    }

    // Sets the version of the packet, ensureing it is compatible
    private boolean setVersion(String versionString) {
        version = versionString;
        if (version.equals(Headers.RTSP_VERSION)) {
            return true;
        }
        return false;
    }

    // Extracts the code for the packet
    private boolean setCode(String codeString) {
        try {
            code = Integer.valueOf(codeString).intValue();
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}