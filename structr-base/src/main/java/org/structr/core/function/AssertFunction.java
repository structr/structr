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

import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class AssertFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "assert";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("condition, statusCode, message");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final Object condition = sources.length > 0 ? sources[0] : null;
		final Object status    = sources.length > 1 ? sources[1] : null;
		final Object message   = sources.length > 2 ? sources[2] : null;

		// only the Boolean true passes, so a broken call fails closed instead of skipping the check
		if (sources.length != 3 || !(status instanceof Number) || message == null) {

			logger.warn("{}: called with {} instead of a condition, a status code and a message in '{}'. {}", getDisplayName(false), describeArguments(sources), caller, usage());
		}

		if (!(condition instanceof Boolean)) {

			logger.warn("{}: the condition is {}, not a boolean, and counts as false in '{}'. Compare it explicitly, e.g. assert(x != null, ...).", getDisplayName(false), describe(condition), caller);
		}

		if (!Boolean.TRUE.equals(condition)) {

			throw new AssertException(message != null ? message.toString() : "Unspecified assertion error", status instanceof Number number ? number.intValue() : 500);
		}

		return null;
	}

	@Override
	public String getShortDescription() {

		return "Aborts the current request unless the given condition is the boolean `true`.";
	}

	@Override
	public String getLongDescription() {

		return """
		This function allows you to check a precondition and abort the execution flow immediately if the condition is not satisfied, sending a customized error code and error message to the caller, along with all the error tokens that have accumulated in the error buffer.

		Only the boolean `true` lets the request continue. Any other value aborts it: `false`, `null`, a node, a list, a number, any string including `"true"`, and a missing argument. So compare explicitly instead of passing an object: `$.assert($.me != null, 401, ...)`, not `$.assert($.me, 401, ...)`, which never passes for a signed-in user. In JavaScript, `a && b` returns one of its operands rather than a boolean; write `$.assert(!!(a && b), ...)` or compare each part. Unlike `if()` and `is()`, `assert()` does not accept the string `"true"`.

		A condition that is not a boolean, and a call without a status code or message, are logged as a warning naming the script. Without a numeric status code, the request is aborted with 500.

		If you want to collect errors (e.g in a validation function), use `error()` which allows you to store error tokens in the context without aborting the execution flow.
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("condition", "must be the boolean `true` for the request to continue; any other value aborts it"),
			Parameter.mandatory("statusCode", "status code to send **if the request is aborted**, 500 if it is not a number"),
			Parameter.mandatory("message", "error message to send **if the request is aborted**")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("$.assert(($.me?.isAdmin === true), 422, 'Only admin users are allowed to access this resource.')", "Make sure only admin users can continue here"),
			Example.javaScript("$.assert($.me != null, 401, 'Please sign in.')", "Make sure a user is signed in; $.assert($.me, ...) would abort for every user"),
			Example.javaScript("$.assert(!$.empty(list), 404, 'Nothing found.')", "Make sure a list is not empty")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of("See also `getErrors()`, `clearError()`, `clearErrors()` and `error()`.", "Only works in schema methods, not in page rendering.");
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Validation;
	}

	// ----- private methods -----
	private String describeArguments(final Object[] sources) {

		if (sources.length != 3) {

			return sources.length + " argument" + (sources.length == 1 ? "" : "s");
		}

		if (!(sources[1] instanceof Number)) {

			return sources[1] == null ? "a null status code" : "a status code that is not a number";
		}

		return "a null message";
	}

	private String describe(final Object value) {

		if (value == null) {

			return "null";
		}

		if (value instanceof NodeInterface node) {

			return "a " + node.getType() + " node";
		}

		if (value instanceof RelationshipInterface relationship) {

			return "a " + relationship.getType() + " relationship";
		}

		if (value instanceof Iterable || value.getClass().isArray()) {

			return "a list";
		}

		if (value instanceof Map) {

			return "an object";
		}

		if (value instanceof CharSequence) {

			return "a string";
		}

		if (value instanceof Number) {

			return "a number";
		}

		return "a " + value.getClass().getSimpleName();
	}
}
