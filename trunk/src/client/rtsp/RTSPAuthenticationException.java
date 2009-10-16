/*
 * @(#)RTSPAuthenticationException.java
 * Created: 15-Jun-2005
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

import java.util.StringTokenizer;

import common.RTSPResponseException;

/**
 * An exception thrown if an RTSP request does not authenticate
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTSPAuthenticationException extends RTSPResponseException {

	private static final long serialVersionUID = 1L;

	// The error message to pass
    private static final String ERROR_MESSAGE = "Unauthorized";

    // The realm of the authentication
    private String realm = "";

    // The realm of the authentication
    private String type = "";

    /**
     * Creates a new RTSPAuthenticationException
     * 
     * @param header
     *            The contents of the WWW-Authenticate header
     */
    public RTSPAuthenticationException(String header) {
        super(ERROR_MESSAGE);
        StringTokenizer tokens = new StringTokenizer(header, " ");
        if (tokens.hasMoreTokens()) {
            type = tokens.nextToken();
        }
        if (tokens.hasMoreTokens()) {
            realm = tokens.nextToken();
        }
    }

    /**
     * Returns the type of authentication requested
     * @return the type of the authentication
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the realm for Basic authentication
     * @return the realm of the Basic authentication
     */
    public String getRealm() {
        return realm;
    }
}
