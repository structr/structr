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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class SetEncryptionKeyFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "setEncryptionKey";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("secretKey");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length == 1) {

			if (sources[0] == null) {

				CryptFunction.setEncryptionKey(null);

			} else {

				CryptFunction.setEncryptionKey(sources[0].toString());
			}

			return null;

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			// only show the error message for wrong parameter count
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${setEncryptionKey(secret)}"),
			Usage.javaScript("Usage: ${{$.setEncryptionKey(secret)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the secret key for encryt()/decrypt(), overriding the value from structr.conf.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}



	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${setEncryptionKey('MyNewSecret')}"),
				Example.javaScript("${{ $.setEncryptionKey('MyNewSecret') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("secret", "new secret key")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Please note that this function overwrites the encryption key that is stored in structr.conf.",
				"The overwritten key can be restored by using `null` as a parameter to this function, as shown in the example below."
		);
	}
}
