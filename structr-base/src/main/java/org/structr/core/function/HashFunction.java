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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.List;
import java.util.TreeSet;

public class HashFunction extends CoreFunction {

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String algorithm = (String) sources[0];
			final String text = (String) sources[1];

			final byte[] bytes = MessageDigest.getInstance(algorithm).digest(text.getBytes(StandardCharsets.UTF_8));
			final BigInteger bigInteger = new BigInteger(1, bytes);

			return String.format("%0" + (bytes.length << 1) + "x", bigInteger);

		} catch (NoSuchAlgorithmException e) {

			logger.warn("{}: Given algorithm \"{}\" not available - the following algorithms are available: \"{}\"", getReplacement(), sources[0], String.join(", ", new TreeSet(Security.getAlgorithms("MessageDigest"))));

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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${hash(algorithm, value)}. Example: ${hash(\"SHA-256\", \"test\")}"),
			Usage.javaScript("Usage: ${{ $.hash(algorithm, value); }}. Example: ${{ $.hash(\"SHA-256\", \"test\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the hash (as a hexadecimal string) of a given string, using the given algorithm (if available via the underlying JVM).";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("algorithm, value");
	}

	@Override
	public String getName() {
		return "hash";
	}
}
