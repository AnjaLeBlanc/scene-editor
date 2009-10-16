/*
 * @(#)Base64.java
 * Created: 01-Nov-2006
 * Version: 1.0
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

/**
 * Base64 Encoding and decoding
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Base64 {

    /**
     * The block size of a base 64 decoded string
     */
    public static final int BASE_64_DEC_BLOCK_SIZE = 3;

    /**
     * The block size of a base 64 encoded string
     */
    public static final int BASE_64_ENC_BLOCK_SIZE = 4;

    //  Base64 encoding chars
    private static final char[] BASE64CHARS = {'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '+', '/', };

    // The 4th position in the array
    private static final int POS_4 = 3;

    // The 3rd position in the array
    private static final int POS_3 = 2;

    // The 2nd position in the array
    private static final int POS_2 = 1;

    // The 1st position in the array
    private static final int POS_1 = 0;

    // The maximum unsigned byte value
    private static final int MAX_UNSIGNED_BYTE = 0x100;

    // Base64 decoding chars
    private static final byte[] REVERSEBASE64CHARS =
        new byte[MAX_UNSIGNED_BYTE];
    static {
        for (byte i = 0; i < BASE64CHARS.length; i++) {
            REVERSEBASE64CHARS[BASE64CHARS[i]] = i;
        }
    }

    // The shift to use the last 6 bits from a byte in base 64 encoding
    private static final int BASE_64_LAST_6_BITS_SHIFT = 0;

    // The mask to get the last 2 bits from a 6-bit number
    private static final int BASE_64_LOW_2_BITS_MASK = 0x3;

    // The shift to use the first 2 bits from a byte in base 64 encoding
    private static final int BASE_64_FIRST_2_BITS_SHIFT = 6;

    // The mask to get the first 4 bits from a 6-bit number
    private static final int BASE_64_HIGH_4_BITS_MASK = 0x3c;

    // The shift to use the last 4 bits from a byte first in base 64 encoding
    private static final int BASE_64_LAST_4_BITS_SHIFT = 2;

    // The mask to get the last 4 bits from a 6-bit number
    private static final int BASE_64_LOW_4_BITS_MASK = 0xF;

    // The shift to use the first 4 bits from a byte first in base 64 encoding
    private static final int BASE_64_FIRST_4_BITS_SHIFT = 4;

    // The mask to get the first 2 bits of a 6-bit number
    private static final int BASE_64_HIGH_2_BITS_MASK = 0x30;

    // The shift to use the last 2 bits from a byte first in base 64 encoding
    private static final int BASE_64_LAST_2_BITS_SHIFT = 4;

    // The mask to get the last 6 bits from a 6 bit number
    private static final int BASE_64_LOW_6_BITS_MASK = 0x3F;

    // The shift to use the first 6 bits from a byte first in base 64 encoding
    private static final int BASE_64_FIRST_6_BITS_SHIFT = 2;

    // The padding in base 64
    private static final char BASE_64_PADDING = '=';

    /**
     * Base64 encodes the input
     *
     * @param input
     *            A series of bytes to be encoded
     * @return A base64 encoded string
     */
    public static char[] base64encode(byte[] input) {
        int length = input.length;
        int mod = length % BASE_64_DEC_BLOCK_SIZE;
        char [] output = null;
        int pos = 0;

        if (mod != 0) {
            length += BASE_64_DEC_BLOCK_SIZE - mod;
        }
        length = (length / BASE_64_DEC_BLOCK_SIZE) * BASE_64_ENC_BLOCK_SIZE;
        output = new char[length];

        for (int i = 0; i < input.length; i += BASE_64_DEC_BLOCK_SIZE) {
            output[pos++] =
                BASE64CHARS[((input[i + POS_1]
                                    >> BASE_64_FIRST_6_BITS_SHIFT)
                                    & BASE_64_LOW_6_BITS_MASK)];
            if ((i + POS_2) < input.length) {
                output[pos++] =
                    BASE64CHARS[(((input[i + POS_1]
                                         << BASE_64_LAST_2_BITS_SHIFT)
                                         & BASE_64_HIGH_2_BITS_MASK)
                               | ((input[i + POS_2]
                                         >> BASE_64_FIRST_4_BITS_SHIFT)
                                         & BASE_64_LOW_4_BITS_MASK))];

                if ((i + POS_3) < input.length) {
                    output[pos++] =
                        BASE64CHARS[((input[i + POS_2]
                                            << BASE_64_LAST_4_BITS_SHIFT)
                                            & BASE_64_HIGH_4_BITS_MASK)
                                  | ((input[i + POS_3]
                                            >> BASE_64_FIRST_2_BITS_SHIFT)
                                            & BASE_64_LOW_2_BITS_MASK)];
                    output[pos++] =
                        BASE64CHARS[(input[i + POS_3]
                                           >> BASE_64_LAST_6_BITS_SHIFT)
                                           & BASE_64_LOW_6_BITS_MASK];
                } else {
                    output[pos++] =
                        BASE64CHARS[((input[i + POS_2]
                                            << BASE_64_LAST_4_BITS_SHIFT)
                                            & BASE_64_HIGH_4_BITS_MASK)];
                    output[pos++] = BASE_64_PADDING;
                }
            } else {
                output[pos++] =
                    BASE64CHARS[((input[i + POS_1]
                                        << BASE_64_LAST_2_BITS_SHIFT)
                                        & BASE_64_HIGH_2_BITS_MASK)];
                output[pos++] = BASE_64_PADDING;
                output[pos++] = BASE_64_PADDING;
            }
        }

        return output;
    }

    /**
     * Decodes a base64 encoded string
     *
     * @param input
     *            The base64 encoded input
     * @return A decoded string
     */
    public static byte[] base64decode(char[] input) {
        int length = input.length;
        int mod = length % BASE_64_ENC_BLOCK_SIZE;
        byte[] output = null;
        int pos = 0;

        if (mod != 0) {
            length += BASE_64_ENC_BLOCK_SIZE - mod;
        }
        length = (length / BASE_64_ENC_BLOCK_SIZE) * BASE_64_DEC_BLOCK_SIZE;
        if (input[input.length - POS_3] == BASE_64_PADDING) {
            length--;
        }
        if (input[input.length - POS_2] == BASE_64_PADDING) {
            length--;
        }
        output = new byte[length];

        for (int i = 0; i < input.length; i += BASE_64_ENC_BLOCK_SIZE) {
            output[pos] =
                (byte) (REVERSEBASE64CHARS[input[i + POS_1]]
                        << BASE_64_FIRST_6_BITS_SHIFT);
            output[pos++] |=
                (byte) (REVERSEBASE64CHARS[input[i + POS_2]]
                        >> BASE_64_LAST_2_BITS_SHIFT);
            if (((i + POS_3) < input.length)
                    && (input[i + POS_3] != BASE_64_PADDING)) {
                output[pos++] =
                    (byte) ((REVERSEBASE64CHARS[input[i + POS_2]]
                            << BASE_64_FIRST_4_BITS_SHIFT)
                            | (REVERSEBASE64CHARS[input[i + POS_3]]
                            >> BASE_64_LAST_4_BITS_SHIFT));
                if (((i + POS_4) < input.length)
                        && (input[i + POS_4] != BASE_64_PADDING)) {
                    output[pos++] =
                        (byte) ((REVERSEBASE64CHARS[input[i + POS_3]]
                                << BASE_64_FIRST_2_BITS_SHIFT)
                                | (REVERSEBASE64CHARS[input[i + POS_4]]
                                >> BASE_64_LAST_6_BITS_SHIFT));
                }
            }
        }

        return output;
    }
}
