/*
 * @(#)RTSPRequest.java
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
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

import common.Base64;
import common.Base64InputStream;
import common.Base64OutputStream;
import common.Headers;
import common.RTSPResponseException;

/**
 * A generic RTSP request
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public abstract class RTSPRequest {

    // The initial sequence number
    private static final int INITIAL_SEQUENCE = 100;

    // The proportion by which a string grows when it is encoded
    private static final int BASE64_ENCODE_GROW = 4;

    // The proportion by which a string shrinks when it is decoded
    private static final int BASE64_DECODE_SHINK = 3;

    // The proxy bypass HTTP method
    private static final String RTSP_PROXY_BYPASS_METHOD = "POST";

    // The proxy bypass protocol
    private static final String RTSP_PROXY_BYPASS_PROTOCOL = "http://";

    // The page to connect to to bypass the proxy
    private static final String RTSP_PROXY_BYPASS_PAGE = "/rtsp.jsp";

    // The HTTP version to use in the bypass
    private static final String RTSP_PROXY_BYPASS_VERSION = "HTTP/1.0";

    // The current sequence number of all requests
    private static int sequenceCount = INITIAL_SEQUENCE;

    // The headers of the request
    private HashMap<String,String> headers = new HashMap<String,String>();

    // The version of the request
    private String version = Headers.RTSP_VERSION;

    // The url of the request
    private String url = "";

    // The packet data of the request
    private char[] data = new char[0];

    // The method of the request
    private String method = "";

    // The sequence number of the request
    private int sequence = 0;

    /**
     * Creates a new RTSP Request
     * 
     * @param url
     *            The URL of the request
     * @param method
     *            The method of the request
     */
    public RTSPRequest(String url, String method) {
        sequence = sequenceCount;
        sequenceCount++;
        this.url = url;
        this.method = method;
    }

    /**
     * Adds a header to the request (or changes one if it exists)
     * 
     * @param header
     *            The header to set
     * @param value
     *            The value to set to the header
     */
    public void setHeader(String header, String value) {
        headers.put(header, value);
    }

    /**
     * Sets the data of the request
     * 
     * @param data
     *            The data to send in this request
     */
    public void setData(char[] data) {
        this.data = data;
    }
    
    /**
     * Returns the length of the data
     * @return the length of the data
     */
    public int getContentLength() {
        return data.length;
    }
    
    /**
     * Returns the headers as a string
     * @return the headers to be sent
     */
    public String getHeaderString() {
        String headerString = "";
        Iterator<String> iterator = null;
        headerString += method + Headers.METHOD_SEPARATOR + url
                + Headers.METHOD_SEPARATOR + version + Headers.EOL;
        
        // If there is some data, add a content-length
        if (data.length > 0) {
            headers.put(Headers.RTSP_CONTENT_LENGTH, String
                    .valueOf(data.length));
        }

        // Add a sequence number to this request
        headers.put(Headers.RTSP_CSEQ, String.valueOf(sequence));

        // Output the headers
        iterator = headers.keySet().iterator();
        while (iterator.hasNext()) {
            String header = (String) iterator.next();
            String value = (String) headers.get(header);
            headerString += header + Headers.HEADER_SEPARATOR
                    + Headers.METHOD_SEPARATOR + value + Headers.EOL;
        }
        headerString += Headers.EOL;
        return headerString;
    }

    public String getHeaderString(String name) {
    	return headers.get(name);
    }
    
    /**
     * Sends the request using the given writer
     * 
     * @param output
     *            The writer to send the request using
     * @param useProxy
     *            True if the request will use a proxy
     * @param server
     *            The server of the request
     * @param port
     *            The port of the request
     */
    public void send(PrintWriter output, boolean useProxy, String server, 
            int port) {

        String headerString = getHeaderString();
        if (useProxy) {
            int length = headerString.length() + getContentLength();
            int mod = length % BASE64_DECODE_SHINK;
            if (mod != 0) {
                length += BASE64_DECODE_SHINK - mod;
            }
            length = (length / BASE64_DECODE_SHINK) * BASE64_ENCODE_GROW;
            output.print(RTSP_PROXY_BYPASS_METHOD + Headers.METHOD_SEPARATOR
                    + RTSP_PROXY_BYPASS_PROTOCOL + server
                    + Headers.PORT_SEPARATOR + port + RTSP_PROXY_BYPASS_PAGE
                    + Headers.METHOD_SEPARATOR + RTSP_PROXY_BYPASS_VERSION
                    + Headers.EOL);
            output.print(Headers.RTSP_CONTENT_TYPE + ": "
                    + Headers.RTSP_ENCAPSULATED + Headers.EOL);
            output.print(Headers.PRAGMA + Headers.HEADER_SEPARATOR 
                    + Headers.METHOD_SEPARATOR + Headers.NO_CACHE
                    + Headers.EOL);
            output.print(Headers.CACHE_CONTROL + Headers.HEADER_SEPARATOR
                    + Headers.METHOD_SEPARATOR + Headers.NO_CACHE
                    + Headers.EOL);
            output.print(Headers.RTSP_CONTENT_LENGTH + Headers.HEADER_SEPARATOR 
                    + Headers.METHOD_SEPARATOR + length + Headers.EOL);
            output.print(Headers.EOL);
            output = new PrintWriter(new Base64OutputStream(output));
        }
        output.write(headerString);

        // Output the data
        output.write(data);
        output.flush();
    }

    /**
     * Receives a response to a request
     * 
     * @param reader
     *            The reader from which to read the response
     * @throws IOException
     * @throws RTSPResponseException
     */
    public void recv(BufferedReader reader) throws IOException,
            RTSPResponseException {
        RTSPResponsePacket packet = new RTSPResponsePacket();
        packet.parseResponse(reader);
        if (packet.getCode() == Headers.RTSP_UNAUTHORIZED) {
            throw new RTSPAuthenticationException(packet.getAuthenticate());
        }
        if ((packet.getContentType() != null)
                && packet.getContentType().startsWith(
                        Headers.RTSP_ENCAPSULATED)) {
            reader = 
                new BufferedReader(new InputStreamReader(new Base64InputStream(
                        new CharArrayReader(packet.getData()))));
            packet = new RTSPResponsePacket();
            packet.parseResponse(reader);
        }
        handleResponse(packet);
    }

    /**
     * Sets the username and password to authenticate with
     * 
     * @param username
     *            The username
     * @param password
     *            The password
     * @throws UnsupportedEncodingException 
     * @throws UnsupportedEncodingException
     */
    public void setAuthentication(String username, String password) 
            throws UnsupportedEncodingException {
        String userPass = username + Headers.HEADER_SEPARATOR + password;
        String authorization = new String(Base64.base64encode(userPass
                .getBytes(Headers.URL_ENCODING)));
        setHeader(Headers.RTSP_AUTH, Headers.RTSP_AUTH_BASIC
                + Headers.METHOD_SEPARATOR + authorization);
    }

    /**
     * Handles the response packet
     * 
     * @param packet
     *            The packet to handle
     * @throws RTSPResponseException
     */
    public abstract void handleResponse(RTSPResponsePacket packet)
            throws RTSPResponseException;
}