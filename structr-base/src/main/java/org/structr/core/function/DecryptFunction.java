/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class DecryptFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "decrypt";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("value [, secret ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			/* Ticket 1601: the scheme comes first and is not optional. It used to be no choice at all -
			   a passphrase became a key by way of an unsalted MD5 digest, which is what makes a stolen
			   ciphertext worth guessing at offline. Required rather than defaulted, because a default is
			   what nobody looks at: a call written before this change no longer runs, and the message
			   says what to write instead. */
			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3);

			final String scheme = sources[0].toString();
			final String text   = sources[1].toString();
			String secret = sources.length == 3 ? sources[2].toString() : null;

			/* A key set with set_encryption_key() earlier in this evaluation, which is where that function
			   now puts it (ticket 1601). Only consulted when the call did not bring its own. */
			if (secret == null) {

				final Object keyFromContext = ctx.retrieve(CryptFunction.CONTEXT_KEY);
				if (keyFromContext != null) {

					secret = keyFromContext.toString();
				}
			}

			if (secret != null) {

				return CryptFunction.decrypt(scheme, text, secret);
			}

			/* No passphrase, so there is nothing for a scheme to derive: the global secret is 32 random
			   bytes and the guessing this protects against does not apply to it. The name is still
			   required, and still has to be one that exists. */
			if (!CryptFunction.isKnownScheme(scheme)) {

				throw new FrameworkException(422, "Unknown encryption scheme '" + scheme + "', expected one of: " + CryptFunction.getKnownSchemes());
			}

			return CryptFunction.decrypt(text);

		} catch (ArgumentNullException pe) {

			if (sources[0] == null) {

				// silently ignore case which can happen for decrypt(current.propertyThatCanBeNull[, key])

				return null;

			} else if (sources.length <= 2) {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				return null;

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				return null;
			}

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${decrypt(value[, secret])}"), Usage.javaScript("Usage: ${{ $.decrypt(value[, secret]) }}"));
	}

	@Override
	public String getShortDescription() {

		return "Decrypts a base 64 encoded AES-GCM ciphertext with the named key derivation scheme and returns the decrypted result.";
	}

	@Override
	public String getLongDescription() {

		return "The first parameter selects how a passphrase is turned into a key: '" + CryptFunction.SCHEME_PBKDF2 + "' derives it with PBKDF2 and a salt of its own per value, '" + CryptFunction.SCHEME_LEGACY + "' keeps the unsalted MD5 digest earlier versions used and is only there so that values encrypted that way stay readable. Without a third parameter the internal global encryption key from the '" + Settings.GlobalSecret.getKey() + "' setting in structr.conf is used, and the scheme makes no difference to it.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("encryptedText", "base64-encoded ciphertext to decrypt"), Parameter.optional("secret", "secret key"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${print(decrypt('aes-gcm-pbkdf2', this.encryptedString))}", "Decrypt a string with the global encryption key from structr.conf"),
			Example.structrScript("${print(decrypt('aes-gcm-pbkdf2', this.encryptedString, 'secret key'))}", "Decrypt a string that was encrypted with the passphrase 'secret key'"),
			Example.structrScript("${print(decrypt('legacy', this.encryptedString, 'secret key'))}", "Decrypt a value written by an earlier version, whose key is the unsalted MD5 digest of the passphrase")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}

	/**
	 * The value and the optional secret are credentials, and a parameter error would otherwise write them into the server log.
	 */
	@Override
	protected boolean redactParameters() {

		return true;
	}
}
