/*
 * @(#)RTSPAnnounceRequest.java
 * Created: 2005-06-03
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
import java.util.HashMap;
import java.util.Vector;

import common.Headers;
import common.RTSPResponseException;

/**
 * A request to get parameters
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPGetParamRequest extends RTSPRequest {

    // The number of parts in an item
    private static final int PARTS_PER_ITEM = 2;

    // The RTSP Method of the request
    private static final String METHOD = "GET_PARAMETER";

    // The parameters
    private HashMap<String, String> parameters = new HashMap<String, String>();

    /**
     * Create a new get parameter request
     *
     * @param url
     *            The url of the request
     * @param parameters The parameters to request
     */
    public RTSPGetParamRequest(String url, Vector<String> parameters) {
        super(url, METHOD);
        setHeader(Headers.RTSP_CONTENT_TYPE,
                Headers.RTSP_CONTENT_TYPE_PARAMTERS);

        String content = "";
        for (int i = 0; i < parameters.size(); i++) {
            content += parameters.get(i);
        }
    }

    /**
     * Handles the response to a get parameter request
     *
     * @see client.rtsp.RTSPRequest#handleResponse(
     *     client.rtsp.RTSPResponsePacket)
     */
    public void handleResponse(RTSPResponsePacket packet)
            throws RTSPResponseException {

        // If the code is not OK, fail
        if (packet.getCode() != Headers.RTSP_OK) {
            throw new RTSPResponseException(packet.getReason());
        } else if (!packet.getContentType().equals(
                Headers.RTSP_CONTENT_TYPE_PARAMTERS)) {
            throw new RTSPResponseException("Unknown content type in response");
        } else {
            try {
                BufferedReader reader = new BufferedReader(new CharArrayReader(
                        packet.getData()));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(Headers.HEADER_SEPARATOR,
                            PARTS_PER_ITEM);
                    String parameter = parts[0].trim();
                    String value = parts[1].trim();
                    parameters.put(parameter, value);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RTSPResponseException(
                        "Error reading response from server");
            }
        }
    }

    /**
     * Returns the parameters
     * @return a HashMap of parameters returned
     */
    public HashMap<String, String> getParameters() {
        return parameters;
    }
}
