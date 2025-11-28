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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
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

			logger.warn("{}: Given algorithm \"{}\" not available - the following algorithms are available: \"{}\"", getDisplayName(), sources[0], String.join(", ", new TreeSet(Security.getAlgorithms("MessageDigest"))));

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
	public String getLongDescription() {
		return """
		Returns the hash (as a hexadecimal string) of a given string, using the given algorithm (if available via the underlying JVM).
		Currently, the SUN provider makes the following hashes/digests available: MD2, MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512, SHA-512/224, SHA-512/256, SHA3-224, SHA3-256, SHA3-384, SHA3-512
		If an algorithm does not exist, an error message with all available algorithms will be logged and a null value will be returned.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${hash('SHA-512', 'Hello World!')}"),
				Example.javaScript("${{ $.hash('SHA-512', 'Hello World!') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("algorithm", "Hash algorithm that will be used to convert the string"),
				Parameter.mandatory("value", "String that will be converted to hash string")
				);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("algorithm, value");
	}

	@Override
	public String getName() {
		return "hash";
	}
}
