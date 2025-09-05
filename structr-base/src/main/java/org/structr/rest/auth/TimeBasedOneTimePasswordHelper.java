/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * Based on https://github.com/j256/two-factor-auth but extended to support more than 6 digits for the OTP length
 * Also, the ability to switch between SHA1, SHA256 and SHA512 was implemented
 *
 * ISC License
 *
 * Copyright 2017, Gray Watson
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * Java implementation for the Time-based One-Time Password (TOTP) two factor authentication algorithm. To get this to
 * work you:
 *
 * <ol>
 * <li>Use generateBase32Secret() to generate a secret key for a user.</li>
 * <li>Store the secret key in the database associated with the user account.</li>
 * <li>Display the QR image URL returned by qrImageUrl(...) to the user.</li>
 * <li>User uses the image to load the secret key into his authenticator application.</li>
 * </ol>
 *
 * <p>
 * Whenever the user logs in:
 * </p>
 *
 * <ol>
 * <li>The user enters the number from the authenticator application into the login form.</li>
 * <li>Read the secret associated with the user account from the database.</li>
 * <li>The server compares the user input with the output from generateCurrentNumber(...).</li>
 * <li>If they are equal then the user is allowed to log in.</li>
 * </ol>
 *
 * <p>
 * See: https://github.com/j256/two-factor-auth
 * </p>
 *
 * <p>
 * For more details of this magic algorithm, see: http://en.wikipedia.org/wiki/Time-based_One-time_Password_Algorithm
 * </p>
 *
 * @author graywatson
 */
public class TimeBasedOneTimePasswordHelper {

	/** set to the number of digits to control 0 prefix, set to 0 for no prefix */
	private static int NUM_DIGITS_OUTPUT = 6;

	/**
	 * Generate and return a 16-character secret key in base32 format (A-Z2-7) using {@link SecureRandom}. Could be used
	 * to generate the QR image to be shared with the user. Other lengths should use {@link #generateBase32Secret(int)}.
	 */
	public static String generateBase32Secret() {
		return generateBase32Secret(16);
	}

	/**
	 * Similar to {@link #generateBase32Secret()} but specifies a character length.
	 */
	public static String generateBase32Secret(int length) {
		StringBuilder sb = new StringBuilder(length);
		Random random = new SecureRandom();
		for (int i = 0; i < length; i++) {
			int val = random.nextInt(32);
			if (val < 26) {
				sb.append((char) ('A' + val));
			} else {
				sb.append((char) ('2' + (val - 26)));
			}
		}
		return sb.toString();
	}

	public static String generateCurrentNumberString(String base32Secret, String cryptoAlgorithm, int timeStepSeconds, Integer length) throws GeneralSecurityException {

		byte[] key = decodeBase32(base32Secret);

		byte[] data = new byte[8];
		long value = System.currentTimeMillis() / 1000 / timeStepSeconds;
		for (int i = 7; value > 0; i--) {
			data[i] = (byte) (value & 0xFF);
			value >>= 8;
		}

		// encrypt the data with the key and return the encrypted hex value
		SecretKeySpec signKey = new SecretKeySpec(key, cryptoAlgorithm);
		// if this is expensive, could put in a thread-local
		Mac mac = Mac.getInstance(cryptoAlgorithm);
		mac.init(signKey);
		byte[] hash = mac.doFinal(data);

		// take the 4 least significant bits from the encrypted string as an offset
		int offset = hash[hash.length - 1] & 0xF;

		// We're using a long because Java hasn't got unsigned int.
		long truncatedHash = 0;
		for (int i = offset; i < offset + 4; ++i) {
			truncatedHash <<= 8;
			// get the 4 bytes at the offset
			truncatedHash |= (hash[i] & 0xFF);
		}
		// cut off the top bit
		truncatedHash &= 0x7FFFFFFF;

		// the token is then the last <length> digits in the number
		truncatedHash %= (int)Math.pow(10, length);

		return zeroPrepend(truncatedHash, length);
	}

	/**
	 * Return the string prepended with 0s.
	 */
	static String zeroPrepend(long num, int digits) {
		String numStr = Long.toString(num);
		if (numStr.length() >= digits) {
			return numStr;
		} else {
			return String.format("%0" + digits + "d", num);
		}
	}

	/**
	 * Decode base-32 method. I didn't want to add a dependency to Apache Codec just for this decode method. Exposed for
	 * testing.
	 */
	static byte[] decodeBase32(String str) {
		// each base-32 character encodes 5 bits
		int numBytes = ((str.length() * 5) + 7) / 8;
		byte[] result = new byte[numBytes];
		int resultIndex = 0;
		int which = 0;
		int working = 0;
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			int val;
			if (ch >= 'a' && ch <= 'z') {
				val = ch - 'a';
			} else if (ch >= 'A' && ch <= 'Z') {
				val = ch - 'A';
			} else if (ch >= '2' && ch <= '7') {
				val = 26 + (ch - '2');
			} else if (ch == '=') {
				// special case
				which = 0;
				break;
			} else {
				throw new IllegalArgumentException("Invalid base-32 character: " + ch);
			}
			/*
			 * There are probably better ways to do this but this seemed the most straightforward.
			 */
			switch (which) {
				case 0:
					// all 5 bits is top 5 bits
					working = (val & 0x1F) << 3;
					which = 1;
					break;
				case 1:
					// top 3 bits is lower 3 bits
					working |= (val & 0x1C) >> 2;
					result[resultIndex++] = (byte) working;
					// lower 2 bits is upper 2 bits
					working = (val & 0x03) << 6;
					which = 2;
					break;
				case 2:
					// all 5 bits is mid 5 bits
					working |= (val & 0x1F) << 1;
					which = 3;
					break;
				case 3:
					// top 1 bit is lowest 1 bit
					working |= (val & 0x10) >> 4;
					result[resultIndex++] = (byte) working;
					// lower 4 bits is top 4 bits
					working = (val & 0x0F) << 4;
					which = 4;
					break;
				case 4:
					// top 4 bits is lowest 4 bits
					working |= (val & 0x1E) >> 1;
					result[resultIndex++] = (byte) working;
					// lower 1 bit is top 1 bit
					working = (val & 0x01) << 7;
					which = 5;
					break;
				case 5:
					// all 5 bits is mid 5 bits
					working |= (val & 0x1F) << 2;
					which = 6;
					break;
				case 6:
					// top 2 bits is lowest 2 bits
					working |= (val & 0x18) >> 3;
					result[resultIndex++] = (byte) working;
					// lower 3 bits of byte 6 is top 3 bits
					working = (val & 0x07) << 5;
					which = 7;
					break;
				case 7:
					// all 5 bits is lower 5 bits
					working |= (val & 0x1F);
					result[resultIndex++] = (byte) working;
					which = 0;
					break;
			}
		}
		if (which != 0) {
			result[resultIndex++] = (byte) working;
		}
		if (resultIndex != result.length) {
			result = Arrays.copyOf(result, resultIndex);
		}
		return result;
	}
}
