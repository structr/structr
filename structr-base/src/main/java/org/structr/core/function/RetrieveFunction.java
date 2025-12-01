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
import org.structr.docs.Documentable;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class RetrieveFunction extends CoreFunction {

	@Override
	public String getName() {
		return "retrieve";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("key");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				return ctx.retrieve(sources[0].toString());
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${retrieve(key)}."),
			Usage.javaScript("Usage: ${{ $.retrieve(key) }}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the value associated with the given key from the temporary store.";
	}

	@Override
	public String getLongDescription() {
		return """
		Retrieves the value previously stored under the given key in the current request context. 
		This method can be used to obtain the results of a previous computation step etc. and is often used to provide 
		some sort of "variables" in the scripting context. See `store()` for the inverse operation.
		Additionally, the `retrieve()` function is used to retrieve the parameters supplied to the execution of a custom method.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript(" ${retrieve('tmpUser')}"),
				Example.javaScript("${{ $.retrieve('tmpUser') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("url", "key to retrieve")
				);
	}

	@Override
	public List<Documentable> getContextHints(final String lastToken) {

		// this might be the place where information about the execution context
		// of a function etc. can be used, but not yet.
		return null;
	}
}
