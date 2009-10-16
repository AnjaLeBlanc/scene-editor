/*
 * @(#)RTPCrypt.java
 * Created: 11-Aug-2005
 * Version: 1-1-alpha3
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

package memetic.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 * Performs RTP Encryption using a given encryption standard
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class RTPCrypt {

    private static final int PACKET_LENGTH_POS = 2;

    private static final int BYTE_TO_INT_MASK = 0xFF;

    private static final int BYTES_PER_WORD = 4;

    private static final int SHORT_BYTE_2_MASK = 0xFF;

    private static final int SHORT_BYTE_1_MASK = 0xFF00;

    private static final int SHORT_BYTE_1_SHIFT = 8;

    private static final int RANDOM_DATA_POS_4 = 3;

    private static final int RANDOM_DATA_POS_3 = 2;

    private static final int RANDOM_DATA_POS_2 = 1;

    private static final int RANDOM_DATA_POS_1 = 0;

    private static final int MAX_BYTE = 255;

    private static final int RANDOM_DATA_LENGTH = 4;

    // The packet length offset from the start of an RTCP packet
    private static final int PACKET_LENGTH_START = 2;

    // The padding field in the RTCP packet
    private static final int PADDING = 0x20;

    // A crypt implementation
    private Crypt crypter = null;

    /**
     * Creates a new RTPCrypt
     *
     * @param crypter
     *            The encryption standard to use
     */
    public RTPCrypt(Crypt crypter) {
        this.crypter = crypter;
    }

    /**
     * Gets the encryption algorithm name
     * @return the algorithm name
     */
    public String getAlgorithmName() {
        return crypter.getName();
    }

    /**
     * Gets the key used for the encryption
     * @return The key
     */
    public String getKey() {
        return crypter.getPassword();
    }

    /**
     * Encrypts the given RTCP data
     *
     * @param input
     * @param offset
     * @param length
     * @param output
     * @param outOffset
     * @return The length of the output stored in the output buffer
     * @throws InvalidAlgorithmParameterException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @throws InvalidKeyException
     */
    public int encryptCtrl(byte[] input, int offset, int length, byte[] output,
            int outOffset) throws InvalidKeyException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {

        // Write four bytes of random data to the start of the packet
        int blockSize = crypter.getBlockSize();
        int padding = blockSize - ((length + RANDOM_DATA_LENGTH) % blockSize);
        if (padding == blockSize) {
            padding = 0;
        }
        int sendLength = length + RANDOM_DATA_LENGTH + padding;
        byte[] sendData = new byte[sendLength];
        System.arraycopy(input, offset, sendData, RANDOM_DATA_LENGTH, length);
        sendData[RANDOM_DATA_POS_1] = (byte) (Math.random() * MAX_BYTE);
        sendData[RANDOM_DATA_POS_2] = (byte) (Math.random() * MAX_BYTE);
        sendData[RANDOM_DATA_POS_3] = (byte) (Math.random() * MAX_BYTE);
        sendData[RANDOM_DATA_POS_4] = (byte) (Math.random() * MAX_BYTE);
        int sendOffset = 0;

        // Add padding to the correct block size
        if (padding != 0) {

            // Find the Last RTCP packet in the group (starting
            // after the 4-byte padding
            int position = RANDOM_DATA_LENGTH;
            int lastPacket = 0;
            int packetLength = 0;
            while (position < (sendLength - padding)) {
                lastPacket = position;
                packetLength =
                    ((sendData[position + PACKET_LENGTH_START]
                               << SHORT_BYTE_1_SHIFT) & SHORT_BYTE_1_MASK)
                        | (sendData[position + PACKET_LENGTH_START + 1]
                                    & SHORT_BYTE_2_MASK);
                packetLength += 1;
                packetLength *= BYTES_PER_WORD;
                position += packetLength;
            }

            // Add the padding
            boolean padded = (sendData[lastPacket] & PADDING) != 0;
            int paddingLength = 0;
            if (padded) {
                paddingLength = sendData[sendLength - 1] & BYTE_TO_INT_MASK;
                sendData[sendLength - 1] = 0;
            }
            paddingLength += padding;
            packetLength += padding;
            packetLength /= BYTES_PER_WORD;
            packetLength -= 1;

            // Set the parameters of the packet to reflect the
            // changes
            sendData[lastPacket] |= PADDING;
            for (int i = sendLength - paddingLength; i < sendLength; i++) {
                sendData[i] = (byte) (paddingLength & BYTE_TO_INT_MASK);
            }
            sendData[lastPacket + PACKET_LENGTH_START] =
                (byte) ((packetLength >> SHORT_BYTE_1_SHIFT)
                        & BYTE_TO_INT_MASK);
            sendData[lastPacket + PACKET_LENGTH_START + 1] =
                (byte) (packetLength & BYTE_TO_INT_MASK);
        }

        // Encode the data
        return crypter.encrypt(sendData, sendOffset, sendLength, output,
                outOffset);
    }

    /**
     * Encrypts the given RTP data
     *
     * @param input
     * @param offset
     * @param length
     * @param output
     * @param outOffset
     * @return The length of the output stored in the output buffer
     * @throws InvalidAlgorithmParameterException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @throws InvalidKeyException
     */
    public int encryptData(byte[] input, int offset, int length, byte[] output,
            int outOffset) throws InvalidKeyException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {

        byte[] sendData = input;
        int sendOffset = offset;
        int sendLength = length;

        // Add any padding
        int blockSize = crypter.getBlockSize();
        int padding = sendLength % blockSize;
        if (padding != 0) {

            // Create an array big enough for the padding
            padding = blockSize - padding;
            byte[] oldSendData = sendData;
            sendData = new byte[sendLength + padding];
            System.arraycopy(oldSendData, sendOffset, sendData, 0, sendLength);

            // Get the current padding stats
            boolean padded = (sendData[0] & PADDING) != 0;
            int paddingLength = 0;
            if (padded) {
                paddingLength = sendData[sendLength - 1] & BYTE_TO_INT_MASK;
                sendData[sendLength - 1] = 0;
            }

            // Add the padding
            paddingLength += padding;
            sendLength += padding;

            // Alter the packet to reflect the changes
            sendData[0] |= PADDING;
            for (int i = sendLength - paddingLength; i < sendLength; i++) {
                sendData[i] = (byte) (paddingLength & BYTE_TO_INT_MASK);
            }
        }

        // Encode the data
        return crypter.encrypt(sendData, sendOffset, sendLength, output,
                outOffset);
    }

    /**
     * Decrypts the given RTCP data
     *
     * @param input
     * @param offset
     * @param length
     * @param output
     * @param outOffset
     * @return The length of the output stored in the output buffer
     * @throws InvalidAlgorithmParameterException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @throws InvalidKeyException
     */
    public int decryptCtrl(byte[] input, int offset, int length, byte[] output,
            int outOffset) throws InvalidKeyException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {

        int readLength = crypter.decrypt(input, offset, length, output,
                outOffset);

        // Remove the additional four bytes
        readLength -= RANDOM_DATA_LENGTH;
        for (int i = outOffset; i < readLength; i++) {
            output[outOffset + i] = output[outOffset + i + RANDOM_DATA_LENGTH];
        }

        // Find the last packet
        int position = outOffset;
        int lastPacket = 0;
        int packetLength = 0;
        while (position < (outOffset + readLength)) {
            lastPacket = position;
            packetLength =
                ((output[position + PACKET_LENGTH_START] << SHORT_BYTE_1_SHIFT)
                        & SHORT_BYTE_1_MASK)
                    | (output[position + PACKET_LENGTH_START]
                              & SHORT_BYTE_2_MASK);
            packetLength += 1;
            packetLength *= BYTES_PER_WORD;
            position += packetLength;
        }

        // Remove the padding from the packet
        boolean padded = (output[lastPacket] & PADDING) != 0;
        int paddingLength = 0;
        if (padded) {
            paddingLength = output[outOffset + readLength - 1]
                                   & BYTE_TO_INT_MASK;
            output[lastPacket] = (byte) (output[lastPacket] & (~PADDING));
            packetLength -= paddingLength;
            packetLength /= BYTES_PER_WORD;
            packetLength -= 1;
            output[lastPacket + PACKET_LENGTH_POS] =
                (byte) ((packetLength >> SHORT_BYTE_1_SHIFT)
                        & BYTE_TO_INT_MASK);
            output[lastPacket + PACKET_LENGTH_POS + 1] =
                (byte) (packetLength & BYTE_TO_INT_MASK);
        }
        readLength -= paddingLength;
        return readLength;
    }

    /**
     * Decrypts the given RTP data
     *
     * @param input
     * @param offset
     * @param length
     * @param output
     * @param outOffset
     * @return The length of the output stored in the output buffer
     * @throws InvalidAlgorithmParameterException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @throws InvalidKeyException
     */
    public int decryptData(byte[] input, int offset, int length, byte[] output,
            int outOffset) throws InvalidKeyException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {

        int readLength = crypter.decrypt(input, offset, length, output,
                outOffset);

        // Remove the padding
        boolean padded = (output[outOffset] & PADDING) != 0;
        int paddingLength = 0;
        if (padded) {
            paddingLength = output[outOffset + readLength - 1]
                                   & BYTE_TO_INT_MASK;
            output[outOffset] = (byte) (output[outOffset] & (~PADDING));
        }
        readLength -= paddingLength;
        return readLength;
    }

    /**
     * Returns the size of the encrypted data
     * @param inputSize The input size of the decrypted data
     * @return The size of the resulting output data
     */
    public int getEncryptOutputSize(int inputSize) {
        inputSize += RANDOM_DATA_LENGTH;
        int padding = getBlockSize() - (inputSize % getBlockSize());
        return crypter.getEncryptOutputSize(inputSize + padding);
    }

    /**
     * Returns the size of the decrypted data
     * @param inputSize The input size of the encrypted data
     * @return The size of the resulting output data
     */
    public int getDecryptOutputSize(int inputSize) {
        return crypter.getDecryptOutputSize(inputSize);
    }

    /**
     * Returns the algorithm block size
     * @return the block size
     */
    public int getBlockSize() {
        return crypter.getBlockSize();
    }
}
