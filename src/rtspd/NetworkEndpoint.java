/*
 * @(#)NetworkEndpoint.java
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Represents an IPAddress/port/ttl on the network
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class NetworkEndpoint {
	
	public static final String MEMETIC = 
        "http://www.memetic-vre.net/ontologies/memetic-20050106-1#";

    /**
     * The type of the endpoint
     */
    public static final String TYPE = MEMETIC 
        + "Network-Endpoint";

    /**
     * The ip address field
     */
    public static final String HOST = MEMETIC + "has-ip-address";

    /**
     * The port field
     */
    public static final String PORT = MEMETIC + "has-port-number";
    
    /**
     * The media type field
     */
    public static final String MEDIATYPE = MEMETIC 
        + "has-media-type";
    
    /**
     * The encryption type field
     */
    public static final String ENC_TYPE = 
       MEMETIC + "has-encryption-type";
    
    /**
     * The encryption key
     */    
    public static final String ENC_KEY = 
        MEMETIC + "has-encryption-key";
    
    /**
     * The ttl field
     */
    public static final String TTL = MEMETIC + "has-ttl";

    /**
     * Any host
     */
    public static final String ANY_HOST = "";

    /**
     * Any port
     */
    public static final int ANY_PORT = -1;

    /**
     * Any TTL
     */
    public static final int ANY_TTL = -1;
    
    /**
     * Audio media type
     */
    public static final String AUDIO_TYPE = "audio";
    
    /**
     * Video media type
     */
    public static final String VIDEO_TYPE = "video";
    
    /**
     * ScreenStreamer media type
     */
    public static final String SCREENSTREAMER_TYPE = "video/JPEG";
    
    /**
     * Any other media type
     */
    public static final String OTHER_TYPE = "all";
    
    /**
     * The address namespace postfix
     */ 
    public static final String NAMESPACE_ADDR = "address_";
    
    // The separator between parts of the address as a string
    private static final String PART_SEPARATOR = "/";

    // The host name of the endpoint
    private String host = "";

    // The port of the endpoint
    private int port = ANY_PORT;

    // The TTL of the endpoint
    private int ttl = ANY_TTL;
    
    // The Encryption type of the endpoint
    private String encryptionType = "";
    
    // The Encryption key of the endpoint
    private String encryptionKey = "";

    // True if the host is any host
    private boolean any = false;

    /**
     * Creates a new NetworkEndpoint
     */
    public NetworkEndpoint() {
        
        // Do Nothing
    }

    /**
     * Creates a new NetworkEndpoint
     * 
     * @param host
     *            The host of the endpoint
     * @param port
     *            The port of the endpoint
     * @param ttl
     *            The ttl of the endpoint
     */
    public NetworkEndpoint(String host, int port, int ttl) {
        this.host = host;
        this.port = port;
        this.ttl = ttl;
    }

    /**
     * Creates a new NetworkEndpoint
     * 
     * @param port
     *            The port of the endpoint
     * @param ttl
     *            The ttl of the endpoint
     */
    public NetworkEndpoint(int port, int ttl) {
        this.host = ANY_HOST;
        this.port = port;
        this.ttl = ttl;
        this.any = true;
    }

    /**
     * Creates a new NetworkEndpoint
     * 
     * @param port
     *            The port of the endpoint
     */
    public NetworkEndpoint(int port) {
        this.host = ANY_HOST;
        this.port = port;
        this.any = true;
    }

    /**
     * Creates a copy of a NetworkEndpoint
     * 
     * @param ePoint
     *            The endpoint to copy
     */
    public NetworkEndpoint(NetworkEndpoint ePoint) {
        this.host = ePoint.host;
        this.port = ePoint.port;
        this.ttl = ePoint.ttl;
        this.any = ePoint.any;
    }

    /**
     * Sets the values of the endpoint. Sets a specific host (i.e. not any)
     * 
     * @param host
     *            The host of the endpoint
     * @param port
     *            The port of the endpoint
     * @param ttl
     *            The ttl of the endpoint
     */
    public void setValues(String host, int port, int ttl) {
        this.host = host;
        this.port = port;
        this.ttl = ttl;
        any = false;
    }

    /**
     * Sets the port of the endpoint. The host will be "any" and the ttl will be
     * undefined.
     * 
     * @param port
     *            The port of the endpoint
     */
    public void setPort(int port) {
        this.host = ANY_HOST;
        this.port = port;
        this.ttl = ANY_TTL;
        this.any = true;
    }
    
    /**
     * Sets the encryption of the endpoint
     * @param type The type of the encryption
     * @param key The key of the encryption
     */
    public void setEncryption(String type, String key) {
        if (key == null) {
            key = "";
        }
        if (type == null) {
            type = "";
        }
        this.encryptionType = type;
        this.encryptionKey = key;
    }

    /**
     * Gets the address/port of the endpoint for RTP
     * 
     * @return The address and port of the endpoint
     */
    public InetSocketAddress getRtpAddress() {
        if (any) {
            return new InetSocketAddress(port);
        }

        return new InetSocketAddress(host, port);
    }

    /**
     * Gets the address/port of the endpoint for RTCP
     * 
     * @return The address and port+1 of the endpoint
     */
    public InetSocketAddress getRtcpAddress() {
        if (any) {
            return new InetSocketAddress(port + 1);
        }

        return new InetSocketAddress(host, port + 1);
    }

    /**
     * Returns the ttl of the endpoint
     * @return The TTL of the endpoint
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * Returns the hostname of the endpoint
     * @return The hostname of the endpoint
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port of the endpoint
     * @return The port of the endpoint
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Returns the encryption type
     * @return The encryption type
     */
    public String getEncryptionType() {
        return encryptionType;
    }
    
    /**
     * Returns the encryption key
     * @return The encryption key
     */
    public String getEncryptionKey() {
        return encryptionKey;
    }

    /**
     * Returns true if the address is multicast
     * @return true if the address is a multicast address
     */
    public boolean isMulticastAddress() {
        try {
            return (InetAddress.getByName(host).isMulticastAddress());
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object e) {
        if (e instanceof NetworkEndpoint) {
            NetworkEndpoint endpoint = (NetworkEndpoint) e;
            if (endpoint.host.equals(host) && endpoint.port == port
                    && endpoint.ttl == ttl) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return (host + PART_SEPARATOR + port + PART_SEPARATOR + ttl).hashCode();
    }
    
    /**
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return host + PART_SEPARATOR + port + PART_SEPARATOR + ttl;
    }
}