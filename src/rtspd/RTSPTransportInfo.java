/*
 * @(#)RTSPTransportInfo.java
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

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Represents the RTSP Transport header
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPTransportInfo {

    /**
     * Record Mode
     */
    public static final int MODE_RECORD = 0;

    /**
     * Play Mode
     */
    public static final int MODE_PLAY = 1;

    // The quote character
    private static final String QUOTE = "\"";

    // The error returned when an unknown token is seen
    private static final String UNKNOWN_TOKEN_ERROR = 
        "Unknown token in transport header: ";

    // The message sent when the mode is not valid
    private static final String INVALID_MODE = "Mode is incorrect";

    // The allowed modes regular expression
    private static final String ALLOWED_MODES = "\"?(record|play)\"?";

    // The message sent when the port is not valid
    private static final String INVALID_PORT = "Port format is incorrect";

    // The allowed ports regular expression
    private static final String ALLOWED_PORTS = "\\d{1,5}(-\\d{1,5})?";

    // The message sent when the layers are invalid
    private static final String INVALID_LAYERS_RESPONSE = 
        "Only a single address is currently supported";

    // The message sent when the ttl is invalid
    private static final String INVALID_TTL_RESPONSE = 
        "TTL format is incorrect";

    // The allowed ttl regular expression
    private static final String TTL_ALLOWED = "\\d{1,3}";

    // The message sent when the transport length is invalid
    private static final String TRANSPORT_LENGTH_ERROR_MESSAGE = 
        "Transport header length is 0";

    // The log file
    private static Log logger = LogFactory.getLog(RTSPTransportInfo.class
            .getName());

    // The transport protocol (RTP)
    private String transportProtocol = Headers.RTSP_TRANSPORT_PROTOCOL_RTP;

    // The profile (AVP)
    private String profile = Headers.RTSP_TRANSPORT_PROFILE_AVP;

    // The lower transport (UDP by default)
    private String lowerTransport = Headers.RTSP_TRANSPORT_LOWER_UDP;

    // True if the transport is multicast
    private boolean isMulticast = false;

    // Other paramters
    private HashMap<String,String> parameters = new HashMap<String,String>();

    // The mode selected
    private int mode = MODE_PLAY;

    // True if a recording is to be appended
    private boolean append = false;

    /**
     * Parses the given header
     * @param packet The packet to parse
     * 
     * @return True if the parsing was successful
     * @throws RTSPResponse 
     */
    public boolean parse(RTSPRequestPacket packet) throws RTSPResponse {
        try {
            
            // Get the header
            String header = packet.getTransport();

            // Split the header up
            StringTokenizer tokens = new StringTokenizer(header,
                    Headers.RTSP_TRANSPORT_SEPARATOR);

            // Assume that initially we have not got the first part of the
            // transport header
            boolean haveProfile = false;

            if (header.length() == 0) {
                throw new RTSPResponse(
                        Headers.RTSP_INVALID_HEADER_FIELD,
                        TRANSPORT_LENGTH_ERROR_MESSAGE,
                        packet);
            }

            // The mode is play unless otherwise specified
            parameters.put(Headers.RTSP_TRANSPORT_MODE,
                    Headers.RTSP_TRANSPORT_MODE_PLAY);

            // While there are parts of the header to be examined...
            while (tokens.hasMoreTokens()) {

                // Get the next part
                String token = tokens.nextToken();

                // If we don't have the first part of the header, get it now
                if (!haveProfile) {
                    StringTokenizer parts = new StringTokenizer(token,
                            Headers.RTSP_TRANSPORT_PROFILE_SEPARATOR);
                    transportProtocol = parts.nextToken();
                    profile = parts.nextToken();
                    if (parts.hasMoreTokens()) {
                        lowerTransport = parts.nextToken();
                    }

                    // Make sure the header is valid and can be recognised
                    if (!transportProtocol.equalsIgnoreCase(
                            Headers.RTSP_TRANSPORT_PROTOCOL_RTP)
                            && !profile.equalsIgnoreCase(
                                    Headers.RTSP_TRANSPORT_PROFILE_AVP)
                            && !(lowerTransport.equalsIgnoreCase(
                                    Headers.RTSP_TRANSPORT_LOWER_UDP)
                                    || lowerTransport.equalsIgnoreCase(
                                        Headers.RTSP_TRANSPORT_LOWER_TCP))) {
                        logger.warn("Transport would not parse: " + token);
                        return false;
                    }

                    // Don't try to get this again
                    haveProfile = true;
                } else if (token.equalsIgnoreCase(
                        Headers.RTSP_TRANSPORT_UNICAST)) {

                    // Detect the unicast token
                    isMulticast = false;
                } else if (token
                        .equalsIgnoreCase(Headers.RTSP_TRANSPORT_MULTICAST)) {

                    // Detect the multicast token
                    isMulticast = true;
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_DESTINATION
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract the desired destination
                    parameters.put(Headers.RTSP_TRANSPORT_DESTINATION, 
                            token.substring(
                                    Headers.RTSP_TRANSPORT_DESTINATION.length()
                                    + 1));
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_INTERLEAVED
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Fail on interleaved token
                    throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                            "Cannot handle interleaved in transport", packet);
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_APPEND)) {

                    // Detect if a recording should be appended
                    append = true;
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_TTL
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract the ttl
                    if (!token.toLowerCase().matches(
                            Headers.RTSP_TRANSPORT_TTL
                            + Headers.RTSP_TRANSPORT_VAR_VAL_SEP
                            + TTL_ALLOWED)) {
                        throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                                INVALID_TTL_RESPONSE, packet);
                    }
                    parameters
                            .put(Headers.RTSP_TRANSPORT_TTL, token
                                    .substring(Headers.RTSP_TRANSPORT_TTL
                                            .length() + 1));
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_LAYERS
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Fail on layers token
                    throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                            INVALID_LAYERS_RESPONSE,
                            packet);
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_PORT
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract the port token
                    if (!token.toLowerCase().matches(
                            Headers.RTSP_TRANSPORT_PORT
                                    + Headers.RTSP_TRANSPORT_VAR_VAL_SEP
                                    + ALLOWED_PORTS)) {
                        throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                                INVALID_PORT, packet);
                    }
                    parameters.put(Headers.RTSP_TRANSPORT_PORT,
                            token.substring(Headers.RTSP_TRANSPORT_PORT
                                    .length() + 1));
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_CLIENT_PORT 
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract the client port token, assuming it to be the same
                    // as port
                    if (!token.toLowerCase().matches(
                            Headers.RTSP_TRANSPORT_CLIENT_PORT
                            + Headers.RTSP_TRANSPORT_VAR_VAL_SEP
                            + ALLOWED_PORTS)) {
                        throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                                INVALID_PORT, packet);
                    }
                    parameters.put(Headers.RTSP_TRANSPORT_PORT, token
                            .substring(Headers.RTSP_TRANSPORT_CLIENT_PORT
                                    .length() + 1));
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_SERVER_PORT
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract the server port token
                    if (!token.toLowerCase().matches(
                            Headers.RTSP_TRANSPORT_SERVER_PORT
                            + Headers.RTSP_TRANSPORT_VAR_VAL_SEP
                            + ALLOWED_PORTS)) {
                        throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                                INVALID_PORT, packet);
                    }
                    parameters.put(Headers.RTSP_TRANSPORT_SERVER_PORT, token
                            .substring(Headers.RTSP_TRANSPORT_SERVER_PORT
                                    .length() + 1));
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_SSRC
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract the ssrc token
                    parameters.put(Headers.RTSP_TRANSPORT_SSRC,
                            token.substring(Headers.RTSP_TRANSPORT_SSRC.length()
                                    + 1));
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_MODE 
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract and parse the mode token
                    String theMode = null;
                    if (!token.toLowerCase().matches(
                            Headers.RTSP_TRANSPORT_MODE
                                    + Headers.RTSP_TRANSPORT_VAR_VAL_SEP
                                    + ALLOWED_MODES)) {
                        throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                                INVALID_MODE, packet);
                    }
                    token = token.substring(Headers.RTSP_TRANSPORT_MODE
                            .length() + 1);
                    if (token.endsWith(QUOTE)) {
                        token = token.substring(0, token.length() - 1);
                    }
                    if (token.startsWith(QUOTE)) {
                        token = token.substring(1);
                    }
                    parameters.put(
                            Headers.RTSP_TRANSPORT_MODE, token.toUpperCase());
                    theMode = (String) parameters.get(
                            Headers.RTSP_TRANSPORT_MODE);

                    if (theMode.equals(Headers.RTSP_TRANSPORT_MODE_RECORD)) {
                        mode = MODE_RECORD;
                    } else if (theMode.equals(
                            Headers.RTSP_TRANSPORT_MODE_PLAY)) {
                        mode = MODE_PLAY;
                    }
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_ENC_TYPE 
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {

                    // Extract the encryption type
                    parameters.put(Headers.RTSP_TRANSPORT_ENC_TYPE,
                            token.substring(
                                    Headers.RTSP_TRANSPORT_ENC_TYPE.length()
                                    + 1));
                } else if (token.toLowerCase().startsWith(
                        Headers.RTSP_TRANSPORT_ENC_KEY
                        + Headers.RTSP_TRANSPORT_VAR_VAL_SEP)) {


                    // Extract the encryption key
                    parameters.put(Headers.RTSP_TRANSPORT_ENC_KEY,
                            token.substring(
                                    Headers.RTSP_TRANSPORT_ENC_KEY.length()
                                    + 1));
                } else {

                    // Fail on unrecognised token
                    throw new RTSPResponse(Headers.RTSP_INVALID_PARAMETER,
                            UNKNOWN_TOKEN_ERROR + token,
                            packet);
                }
            }
        } catch (NoSuchElementException e) {

            // Fail if we run out of tokens before time
            logger.error("Exception ", e);
            return false;
        }

        // If we got no errors, succeed
        return true;
    }

    /**
     * Returns the transport protocol
     * 
     * @return The transport protocol (RTP)
     */
    public String getTransportProtocol() {
        return transportProtocol;
    }

    /**
     * Returns the profile
     * 
     * @return The profile (AVP)
     */
    public String getProfile() {
        return profile;
    }

    /**
     * Returns the underlying transport
     * 
     * @return The underlying transport (UDP or TCP)
     */
    public String getLowerTransport() {
        return lowerTransport;
    }

    /**
     * Returns the parameters
     * 
     * @return The set of parameters
     */
    public HashMap<String, String> getParameters() {
        return parameters;
    }

    /**
     * Gets the named parameter
     * 
     * @param param
     *            The parameter to get
     * @return The value of the parameter
     */
    public String getParameter(String param) {
        return (String) parameters.get(param);
    }

    /**
     * Returns the mode constant
     * 
     * @return The mode of the recording
     */
    public int getMode() {
        return mode;
    }

    /**
     * Returns true if the mode is play
     * 
     * @return True if the mode is play
     */
    public boolean isPlay() {
        return (mode == MODE_PLAY);
    }

    /**
     * Returns true if the mode is record
     * 
     * @return True if the mode is record
     */
    public boolean isRecord() {
        return (mode == MODE_RECORD);
    }

    /**
     * Returns true if the address is a multicast address
     * 
     * @return True if the address is multicast
     */
    public boolean isMulticast() {
        return isMulticast;
    }

    /**
     * Returns true if the request is to append the session
     * 
     * @return true if the request is to append
     */
    public boolean isAppend() {
        return append;
    }
}