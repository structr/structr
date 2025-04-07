/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HMACFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_HMAC = "Usage: ${hmac(value, secret [, hashAlgorithm ])}. Example: ${hmac(\"testpayload\", \"hashSecret\", \"SHA256\")}";
	public static final String ERROR_MESSAGE_HMAC_JS = "Usage: ${{Structr.hmac(value, secret [, hashAlgorithm ])}}. Example: ${{Structr.hmac(\"testpayload\", \"hashSecret\", \"SHA256\")}}";

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String algorithmPrefix = "Hmac";

			final String value = (String) sources[0];
			final String secret = (String) sources[1];

			String algorithm = null;
			if (sources.length > 2) {
				algorithm = (String) sources[2];
			} else {
				algorithm = "SHA256";
			}
			algorithm = algorithmPrefix.concat(algorithm);

			Boolean returnRawHash = false;
			if (sources.length > 3) {
				returnRawHash = true;
			}

			SecretKeySpec key = new SecretKeySpec((secret).getBytes("UTF-8"), algorithm);
			Mac mac = Mac.getInstance(algorithm);
			mac.init(key);

			byte[] rawBytesHash = mac.doFinal(value.getBytes("UTF-8"));

			if (returnRawHash) {
				return rawBytesHash;
			}

			// Create Hex String and fill with 0 if needed
			StringBuffer hash = new StringBuffer();
			for (final byte b : rawBytesHash) {
				String hex = Integer.toHexString(0xFF & b);
				if (hex.length() == 1) {
					hash.append('0');
				}
				hash.append(hex);
			}

			return hash.toString();

		} catch (UnsupportedEncodingException e) {
			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		} catch (NoSuchAlgorithmException e) {
			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		} catch (InvalidKeyException e) {
			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		} catch (ArgumentNullException pe) {
			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		} catch (ArgumentCountException pe) {
			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_HMAC_JS : ERROR_MESSAGE_HMAC);
	}

	@Override
	public String shortDescription() {
		return "Returns a keyed-hash message authentication code generated out of the given payload, secret and hash algorithm,";
	}

	@Override
	public String getSignature() {
		return "value, secret [, hashAlgorithm ]";
	}

	@Override
	public String getName() {
		return "hmac";
	}
}
