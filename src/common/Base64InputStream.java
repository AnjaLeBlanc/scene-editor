/*
 * @(#)Base64InputStream.java
 * Created: 26-Sep-2005
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
import java.io.InputStream;
import java.io.Reader;

/**
 * An input stream for Base64 Data returning decoded data
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Base64InputStream extends InputStream {
    
    // The size of the buffer
    private static final int BUFFER_SIZE = 1024;
    
    // A buffer of decoded characters
    private byte[] buffer = new byte[BUFFER_SIZE];
    
    // The current position in the read buffer
    private int currentPos = 0;
    
    // The maximum position in the read buffer
    private int maxPos = 0;
    
    // The reader of the input stream
    private Reader reader = null;
    
    // Any spare characters that have been read
    private char[] spare = new char[0];
    
    /**
     * Creates a new Base64 input stream
     * @param in The reader to read from
     */
    public Base64InputStream(Reader in) {
        this.reader = in;
    }
    
    /**
     * 
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        if (maxPos == currentPos) {
            fillBuffer();
        }
        if (maxPos == currentPos) {
            return -1;
        }
        return buffer[currentPos++];
    }
    
    // Refills the buffer
    private void fillBuffer() throws IOException {
        int size = 0;
        int mod = 0;
        char[] inputBuffer = null;
        int bytesRead = 0;
        byte[] outputBuffer = null;
        
        for (int i = 0; i < (maxPos - currentPos); i++) {
            buffer[i] = buffer[currentPos + i];
        }
        maxPos -= currentPos;
        currentPos = 0;
        size = buffer.length - maxPos;
         mod = size % Base64.BASE_64_DEC_BLOCK_SIZE;
        if (mod != 0) {
            size -= mod;
        }
        size = (size / Base64.BASE_64_DEC_BLOCK_SIZE)
                * Base64.BASE_64_ENC_BLOCK_SIZE;
        inputBuffer = new char[size];
        if (spare.length > 0) {
            System.arraycopy(spare, 0, inputBuffer, 0, spare.length);
            bytesRead = spare.length;
            spare = new char[0];
        }
        bytesRead = reader.read(inputBuffer, 0, size);
        if (bytesRead < 0) {
            return;
        }
        if (bytesRead < size) {
            char[] encoded = null;
            if (bytesRead % Base64.BASE_64_ENC_BLOCK_SIZE != 0) {
                spare = new char[bytesRead % Base64.BASE_64_ENC_BLOCK_SIZE];
                for (int i = 0; i
                        < (bytesRead % Base64.BASE_64_ENC_BLOCK_SIZE); i++) {
                    spare[i] = inputBuffer[bytesRead - i - 1];
                }
                bytesRead -= spare.length;
            }
            encoded = new char[bytesRead];
            System.arraycopy(inputBuffer, 0, encoded, 0, bytesRead);
            inputBuffer = encoded;
        }
        outputBuffer = Base64.base64decode(inputBuffer);
        System.arraycopy(outputBuffer, 0, buffer, maxPos, 
                outputBuffer.length);
        maxPos += outputBuffer.length;
    }
}
