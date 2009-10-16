/*
 * @(#)Base64OutputStream.java
 * Created: 28-Feb-2006
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * An output stream to write to Base 64
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Base64OutputStream extends OutputStream {
    
    // The size of the buffer
    private static final int BUFFER_SIZE = 1024;
    
    // A buffer of decoded characters
    private byte[] buffer = new byte[BUFFER_SIZE];
    
    // The current position in the write buffer
    private int currentPos = 0;
    
    // The writer of the output stream
    private Writer writer = null;
    
    // True if the stream has been flushed
    private boolean flushed = false;
    
    /**
     * Creates a new Base64OutputStream
     * @param writer The writer to write to
     */
    public Base64OutputStream(Writer writer) {
        this.writer = writer;
    }

    /**
     * 
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException {
        if (currentPos == BUFFER_SIZE) {
            emptyBuffer();
        }
        if (currentPos == BUFFER_SIZE) {
            throw new IOException("Could not empty buffer");
        }
        buffer[currentPos++] = (byte) b;
    }
    
    // Attempts to write as many bytes as possible to the output
    private void emptyBuffer() throws IOException {
        int maxPos = (currentPos / Base64.BASE_64_DEC_BLOCK_SIZE)
                      * Base64.BASE_64_DEC_BLOCK_SIZE;
        byte[] decoded = buffer;
        char [] outputBuffer = null;
        if (maxPos < currentPos) {
            decoded = new byte[maxPos];
            System.arraycopy(buffer, 0, decoded, 0, maxPos);
        }
        outputBuffer = Base64.base64encode(decoded);
        writer.write(outputBuffer);
        for (int i = maxPos; i < currentPos; i++) {
            buffer[i - maxPos] = buffer[i];
        }
        currentPos = (currentPos - maxPos);
    }
    
    /**
     * Finalises the writing of the Base64 data (including padding)
     * @throws IOException 
     * @see java.io.Flushable#flush()
     */
    public void flush() throws IOException {
        if (!flushed) {
            byte[] decoded = new byte[currentPos];
            char[] outputBuffer = null;
            System.arraycopy(buffer, 0, decoded, 0, currentPos);
            outputBuffer = Base64.base64encode(decoded);
            writer.write(outputBuffer);
            currentPos = 0;
            writer.flush();
            flushed = true;
        }
    }
    
    /**
     * 
     * @see java.io.Closeable#close()
     */
    public void close() throws IOException {
        flush();
        writer.flush();
        writer.close();
    }

}
