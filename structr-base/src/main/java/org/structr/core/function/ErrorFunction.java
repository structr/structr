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
import org.structr.common.error.SemanticErrorToken;
import org.structr.core.GraphObject;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class ErrorFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "error";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("propertyName, errorToken [, errorMessage]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		String type = StructrTraits.GRAPH_OBJECT;

		if (caller != null && caller instanceof GraphObject o) {

			type = o.getType();
		}

		try {

			if (sources == null) {
				throw new IllegalArgumentException();
			}

			switch (sources.length) {

				case 2: {
					ctx.raiseError(422, new SemanticErrorToken(type, (String)sources[0], (String)sources[1]));

					break;
				}

				case 3: {
					ctx.raiseError(422, new SemanticErrorToken(type, (String)sources[0], (String)sources[1]).withDetail(sources[2]));

					break;
				}

				default:
					throw new IllegalArgumentException();

			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${$.error(propertyName, errorToken[, errorMessage])}. Example: ${$.error('name', 'alreadyTaken'[, 'Another node with that name already exists'])}"),
			Usage.structrScript("Usage: ${error(propertyName, errorToken[, errorMessage])}. Example: ${error('name', 'alreadyTaken'[, 'Another node with that name already exists'])}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Stores error tokens in the current context causing the transaction to fail at the end of the request.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function allows you to store error tokens in the current context without aborting the execution flow. Errors that have accumulated in the error buffer can be fetched with `getErrors()` and cleared with either `clearError()` or `clearErrors()`.

		If there are still error tokens in the error buffer at the end of the transaction, the transaction is rolled back. If the calling context was an HTTP request, the HTTP status code 422 Unprocessable Entity will be sent to the client together with the error tokens.

		This function is mainly used in entity callback functions like `onCreate()` or `onSave()` to allow the user to implement custom validation logic.

		If you want to abort the execution flow immediately, use `assert()`.
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("propertyName", "name of the property that caused the error, will end up in the `property` field of the response"),
			Parameter.mandatory("errorToken", "arbitrary string that represents the error, will end up in the `token` field of the response"),
			Parameter.optional("errorMessage", "more detailed description of the error for humans to read, will end up in the `detail` field of the response")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also `getErrors()`, `clearError()`, `clearErrors()` and `assert()`."
		);
	}
}
