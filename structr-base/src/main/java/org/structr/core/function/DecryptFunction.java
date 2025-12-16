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

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			String secret = null;
			String text   = null;

			switch (sources.length) {

				case 2:
					secret = sources[1].toString();
				case 1:
					text = sources[0].toString();
			}

			if (secret != null) {

				return CryptFunction.decrypt(text, secret);

			} else {

				return CryptFunction.decrypt(text);
			}

		} catch (ArgumentNullException pe) {

			if (sources[0] == null) {

				// silently ignore case which can happen for decrypt(current.propertyThatCanBeNull[, key])
				return "";

			} else if (sources.length <= 2) {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				return "";

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				// only show the error message for wrong parameter count
				return usage(ctx.isJavaScriptContext());
			}

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			// only show the error message for wrong parameter count
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${decrypt(value[, secret])}"),
			Usage.javaScript("Usage: ${{Structr.decrypt(value[, secret])}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Decrypts a base 64 encoded AES ciphertext and returns the decrypted result.";
	}

	@Override
	public String getLongDescription() {
		return "This function either uses the internal global encryption key from the '" + Settings.GlobalSecret.getKey() + "' setting in structr.conf, or the optional second parameter.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("encryptedText", "base64-encoded ciphertext to decrypt"),
			Parameter.optional("secret", "secret key")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${print(decrypt(this.encryptedString))}", "Decrypt a string with the global encryption key from structr.conf"),
			Example.structrScript("${print(decrypt(this.encryptedString'), 'secret key')}", "Decrypt a string with the key 'secret key'")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}
}
