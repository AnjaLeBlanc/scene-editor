/*
 * @(#)DESCrypt.java
 * Created: 11-Jul-2005
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

import java.security.MessageDigest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts and Decrypts using the DES Algorithm
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class DESCrypt implements Crypt {

    /**
     * The DES encryption type
     */
    public static final String TYPE = "DES";

    // The java encryption type
    private static final String JAVA_ENC_TYPE = "DES/CBC/NoPadding";

    // The shift to shift by 2
    private static final int SHIFT_BY_2 = 2;

    // The shift to shift by 4
    private static final int SHIFT_BY_4 = 4;

    // The mask for the key in generation
    private static final int KEY_MASK = 0xfe;

    // The conversion mask for a byte to an int
    private static final int BYTE_TO_INT = 0xFF;

    // The size of the key
    private static final int KEY_SIZE = 8;

    // An object for decrypting data
    private Cipher decrypter = null;

    // An object for encrypting data
    private Cipher encrypter = null;

    // The password
    private String password = null;

    /**
     * Creates a new DESCrypt
     *
     * @param password
     *            The password to crypt with
     */
    public DESCrypt(String password) {
        try {

            // Generate the key by taking the MD5 of the password
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] md5Key = md5.digest(password.getBytes("UTF-8"));

            // Generate the key by compressing 8 bytes to 7
            byte[] inKey = new byte[KEY_SIZE];
            for (int i = 0; i < inKey.length; i++) {
                int firstVal = 0;
                int secondVal = 0;
                if ((i - 1) >= 0) {
                    firstVal = (md5Key[i - 1] & BYTE_TO_INT) << (KEY_SIZE - i);
                }
                if (i < (inKey.length - 1)) {
                    secondVal = ((md5Key[i] & BYTE_TO_INT) >> i) & BYTE_TO_INT;
                }
                inKey[i] = (byte) ((firstVal | secondVal) & BYTE_TO_INT);
            }

            // Add DES Key Padding
            for (int i = 0; i < KEY_SIZE; ++i) {
                int k = inKey[i] & KEY_MASK;
                int j = k;
                j ^= j >>> SHIFT_BY_4;
                j ^= j >>> SHIFT_BY_2;
                j ^= j >>> 1;
                j = (j & 1) ^ 1;
                inKey[i] = (byte) ((k | j) & BYTE_TO_INT);
            }

            SecretKeySpec secretKey = new SecretKeySpec(inKey, TYPE);

            decrypter = Cipher.getInstance(JAVA_ENC_TYPE);
            encrypter = Cipher.getInstance(JAVA_ENC_TYPE);

            // IV is all 0s for RTP DES
            IvParameterSpec iv = new IvParameterSpec(new byte[] {0, 0, 0, 0,
                    0, 0, 0, 0});

            decrypter.init(Cipher.DECRYPT_MODE, secretKey, iv);
            encrypter.init(Cipher.ENCRYPT_MODE, secretKey, iv);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @see memetic.crypto.Crypt#getName()
     */
    public String getName() {
        return TYPE;
    }

    /**
     *
     * @see memetic.crypto.Crypt#getPassword()
     */
    public String getPassword() {
        return password;
    }

    /**
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @see memetic.crypto.Crypt#encrypt(byte[], int, int, byte[], int)
     */
    public int encrypt(byte[] input, int offset, int length, byte[] output,
            int outOffset) throws ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {
        int val = 0;
        synchronized (encrypter) {
            val = encrypter.doFinal(input, offset, length, output, outOffset);
        }
        return val;
    }

    /**
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @see memetic.crypto.Crypt#decrypt(byte[], int, int, byte[], int)
     */
    public int decrypt(byte[] input, int offset, int length, byte[] output,
            int outOffset) throws ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {
        int val = 0;
        synchronized (decrypter) {
            val = decrypter.doFinal(input, offset, length, output, outOffset);
        }
        return val;
    }

    /**
     * @see memetic.crypto.Crypt#getEncryptOutputSize(int)
     */
    public int getEncryptOutputSize(int inputSize) {
        int val = 0;
        synchronized (encrypter) {
            val = encrypter.getOutputSize(inputSize);
        }
        return val;
    }

    /**
     * @see memetic.crypto.Crypt#getDecryptOutputSize(int)
     */
    public int getDecryptOutputSize(int inputSize) {
        int val = 0;
        synchronized (decrypter) {
            val = decrypter.getOutputSize(inputSize);
        }
        return val;
    }

    /**
     * @see memetic.crypto.Crypt#getBlockSize()
     */
    public int getBlockSize() {
        return encrypter.getBlockSize();
    }
}