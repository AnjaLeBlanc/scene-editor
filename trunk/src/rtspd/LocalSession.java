/*
 * @(#)LocalSession.java
 * Created: 2008-01-16
 * Version: 0-1-alpha
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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author zzptmba
 *
 */
public class LocalSession extends Session {

//	 The id of the session
    private String id = "";
public LocalSession(String uri) {
		super(uri);
		id = String.valueOf(Math.random());
	}
//public LocalSession(String uri, Database db) {
//		super(uri, db);
//		id=String.valueOf(Math.random());
//	}

	//	 A Space
    private static final String SPACE = " ";
//	 The log file
    private static Log logger = LogFactory.getLog(LocalSession.class.getName());

//	 The streams in the session
    private HashMap<String, Stream> streamMap = new HashMap<String, Stream>();

    /**
     * Enables or disables the given stream
     * @param streamId The id of the stream
     * @param enabled True if the stream is enabled
     */
    public void setStreamEnabled(String streamId, boolean enabled) {
        Stream stream = (Stream) streamMap.get(streamId);
        if (stream != null) {
            stream.setEnabled(enabled);
        }
    }

    /**
     * Adds a stream to the session
     * @param stream The stream to add
     */
    public void addStream(Stream stream) {
    	System.out.println("addStream(stream) called");
        if (stream.getSsrc() != null) {
            logger.debug("Add stream " + stream.getSessionId() + SPACE
                    + stream.getSsrc());
            streamMap.put(stream.getSsrc(), stream);
        }
    }

    /**
     * Returns the number of streams in the session
     *
     * @return the number of streams
     */
    public int countStreams() {
        return streamMap.size();
    }

    /**
     * Returns the streams in the session
     *
     * @return the stream objects
     */
    public Vector<Stream> getStreams() {
        return new Vector<Stream>(streamMap.values());
    }

    /**
     * Adds a new stream to a session
     *
     * @param ssrc
     *            The ssrc of the stream
     * @return The stream object added
     * @throws MalformedURLException
     */
    public Stream addStream(String ssrc) throws MalformedURLException {
    	System.out.println("addStream(ssrc) called");
        Stream realStream = new Stream(id, ssrc,
                Session.NAMESPACE_SEP + ssrc);
        addStream(realStream);
        return realStream;
    }

    /**
     * Returns the stream with the given id
     * @param id The id of the stream
     * @return The stream object
     */
    public Stream getStream(String id) {
        return (Stream) streamMap.get(id);
    }

}
