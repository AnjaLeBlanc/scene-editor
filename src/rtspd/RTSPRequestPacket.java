/*
 * @(#)RTSPRequestPacket.java
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

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Represents an RTSP Request Packet
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPRequestPacket {

    /**
     * RTSP Method unknown
     */
    public static final int NO_TYPE = -1;

    /**
     * RTSP DESCRIBE Method
     */
    public static final int DESCRIBE = 0;

    /**
     * RTSP GET_PARAMETER Method
     */
    public static final int GET_PARAMETER = 1;

    /**
     * RTSP OPTIONS Method
     */
    public static final int OPTIONS = 2;

    /**
     * RTSP PAUSE Method
     */
    public static final int PAUSE = 3;

    /**
     * RTSP PLAY Method
     */
    public static final int PLAY = 4;

    /**
     * RTSP RECORD Method
     */
    public static final int RECORD = 5;

    /**
     * RTSP REDIRECT Method
     */
    public static final int REDIRECT = 6;

    /**
     * RTSP SETUP Method
     */
    public static final int SETUP = 7;

    /**
     * RTSP SET_PARAMETER Method
     */
    public static final int SET_PARAMETER = 8;

    /**
     * RTSP TEARDOWN Method
     */
    public static final int TEARDOWN = 9;

    /**
     * RTSP ANNOUNCE Method
     */
    public static final int ANNOUNCE = 10;

    /**
     * RTSP GET Method
     */
    public static final int GET = 11;

    /**
     * RTSP DELETE Method
     */
    public static final int DELETE = 12;

    /**
     * RTSP POST Method
     */
    public static final int POST = 13;

    /**
     * RTSP LISTEN Method
     */
    public static final int LISTEN = 14;

    /**
     * RTSP NEWSTREAMS Method
     */
    public static final int NEWSTREAMS = 15;

    /**
     * RTSP REPAIR Method
     */
    public static final int REPAIR = 16;

    // A mapping of method constants to strings
    private static String[] staticMethodStrings = {"DESCRIBE",
            "GET_PARAMETER", "OPTIONS", "PAUSE", "PLAY", "RECORD", "REDIRECT",
            "SETUP", "SET_PARAMETER", "TEARDOWN", "ANNOUNCE", "GET", "DELETE",
            "POST", "LISTEN", "NEWSTREAMS", "REPAIR"};

    // The log file
    private static Log logger = LogFactory.getLog(RTSPRequestPacket.class
            .getName());

    // The request
    private HttpServletRequest request = null;

    // The data of the message
    private char[] data = new char[0];

    // The method type of the request
    private int type = 0;

    // The RTSP Engine
    private RTSPEngine engine = null;

    /**
     * Creates a new RTSPRequestPacket.
     *
     * @param engine
     *            The engine of the packet
     */
    public RTSPRequestPacket(RTSPEngine engine) {
        this.engine = engine;
    }

    public RTSPRequestPacket() {
    // don't do anything
    }
    
    /**
     * Parses an RTSP request.
     * @param request The request to parse
     *
     * @return True if the request was successfully parsed
     * @throws RTSPResponse
     * @throws IOException
     */
    public boolean parseRequest(HttpServletRequest request)
            throws RTSPResponse, IOException {
        boolean returnValue = setType(request.getMethod());
        this.request = request;
        if (returnValue) {
            returnValue = init(request.getReader());
        }
        return returnValue;
    }

    /**
     * Initialises the packet
     *
     * @param reader
     *            The reader containing the data
     * @return True if sucessful
     */
    public boolean init(BufferedReader reader) {
        try {

            // The content length
            int contentLength = getContentLength();

            logger.debug("Reading " + contentLength + " chars of data");

            // Read all the data from the stream and store it
            data = new char[contentLength];
            if (contentLength > 0) {
                int chars = reader.read(data, 0, contentLength);
                while (chars < contentLength) {
                    chars += reader.read(data, chars, contentLength - chars);
                }
            }

            // This was done sucessfully
            return true;
        } catch (IOException e) {
            logger.error("Exception ", e);
            return false;
        }
    }

    /**
     * Returns the method string
     *
     * @return The method string
     */
    public String getMethod() {
        return request.getMethod();
    }

    /**
     * Returns the uri string
     *
     * @return The uri String
     */
    public String getUri() {
        return request.getRequestURI();
    }

    /**
     * Returns the sequence header
     *
     * @return The sequence header
     */
    public String getSequence() {
        logger.debug("RTSP_Request_Packet::sequence: "
                + getHeader(Headers.RTSP_CSEQ));
        return getHeader(Headers.RTSP_CSEQ);
    }

    /**
     * Returns the version of the packet
     *
     * @return The version of the packet
     */
    public String getVersion() {
        return request.getProtocol();
    }

    /**
     * Returns the path of the packet
     *
     * @return The parsed path of the packet
     */
    public String getPath() {
        return request.getPathInfo().substring(1);
    }

    /**
     * Returns the id of the play or record session
     *
     * @return The session header
     */
    public String getSession() {
        return getHeader(Headers.RTSP_SESSION);
    }

    /**
     * Returns the content type
     *
     * @return the content type
     */
    public String getContentType() {
        return getHeader(Headers.RTSP_CONTENT_TYPE);
    }

    /**
     * Returns the query part of the request
     *
     * @return The query string
     */
    public String getQueryString() {
        return request.getQueryString();
    }

    /**
     * Returns the method of the request
     *
     * @return The method type of the request
     */
    public int getType() {
        return type;
    }

    /**
     * Returns a particular header
     *
     * @param head
     *            The name of the header to get
     * @return The header
     */
    private String getHeader(String head) {
        return request.getHeader(head);
    }

    /**
     * Returns the engine being used to process the messsage
     *
     * @return The RTSP Engine
     */
    public RTSPEngine getEngine() {
        return engine;
    }

    /**
     * Returns the data in the packet
     *
     * @return The data from the packet
     */
    public char[] getPacketData() {
        return data;
    }

    /**
     * Returns the length of the data
     *
     * @return The length of the data in the packet
     */
    public int getPacketDataLength() {
        return data.length;
    }

    /**
     * Return the content-length header value
     *
     * @return The content-length header
     */
    public int getContentLength() {
        if (request.getContentLength() == -1) {
            return 0;
        }
        return request.getContentLength();
    }

    /**
     * Returns the transport header value
     *
     * @return The transport header
     */
    public String getTransport() {
        return getHeader(Headers.RTSP_TRANSPORT);
    }

    /**
     * Returns the range header
     *
     * @return the range header
     */
    public String getRange() {
        return getHeader(Headers.RTSP_RANGE);
    }

    // Sets the type of the packet from the method
    private boolean setType(String typeString) {
        type = NO_TYPE;
        for (int i = 0; i < staticMethodStrings.length; i++) {
            if (typeString.equals(staticMethodStrings[i])) {
                type = i;
                return true;
            }
        }

        if (type == NO_TYPE) {
            return false;
        }
        return false;
    }

    /**
     * Lists all the methods supported by the server
     *
     * @return A list of the methods supported by the server
     */
    public String listMethods() {
        String methods = "";
        for (int i = 0; i < staticMethodStrings.length; i++) {
            methods += staticMethodStrings[i];
            if (i + 1 < staticMethodStrings.length) {
                methods += ", ";
            }
        }
        logger.debug("RTSP_Request_Packet::list_methods(): " + methods);
        return methods;
    }

    /**
     * Returns the username of the requester
     * @return the username
     */
    public String getUsername() {
        String user = "";
//        String credentials = request.getHeader(HttpFields.__Authorization);
//
//        if (credentials != null) {
//            user = AuthenticationManager.getUsername(credentials);
//        }

        return user;
    }

    /**
     * Returns the password of the requester
     *
     * @return the password
     */
    public String getPassword() {
        String pass = "";
//        String credentials = request.getHeader(HttpFields.__Authorization);
//
//        if (credentials != null) {
//            pass = AuthenticationManager.getPassword(credentials);
//        }

        return pass;
    }

    /**
     * Returns the scale header
     *
     * @return the scale header value
     */
    public String getScale() {
        return request.getHeader(Headers.RTSP_SCALE);
    }

    /**
     * Returns the stream header
     *
     * @return the stream header value
     */
    public String getStream() {
        return getHeader(Headers.RTSP_STREAM);
    }
}
