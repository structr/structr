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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class EmptyFunction extends CoreFunction {

	@Override
	public String getName() {
		return "empty";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		if (sources.length == 0 || sources[0] == null || StringUtils.isEmpty(sources[0].toString())) {

			return true;

		} else if (sources[0] instanceof Collection) {

			return ((Collection) sources[0]).isEmpty();

		} else if (sources[0] instanceof Iterable) {

			return !((Iterable) sources[0]).iterator().hasNext();

		} else if (sources[0].getClass().isArray()) {

			return (((Object[]) sources[0]).length == 0);

		} else if (sources[0] instanceof Map) {

			return (((Map) sources[0]).isEmpty());

		} else {

			return false;
		}
	}


	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.empty(string|array|collection)}}. Example: ${{if($.empty(possibleEmptyString), \"empty\", \"non-empty\")}}"),
			Usage.structrScript("Usage: ${empty(string|array|collection)}. Example: ${if(empty(possibleEmptyString), \"empty\", \"non-empty\")}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a boolean value that indicates whether the given object is null or empty.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function works for all sorts of objects: strings, collections, variables, etc., with different semantics depending on the input object.
		
		| Input Type | Behaviour |
		| --- | --- |
		| string | Returns `true` if the string is non-null and not empty. A string with length > 0 is non-empty, even if it contains only whitespace. |
		| collection | Returns `true` if the collection is non-null and contains at least one object (even if the object itself might be null). |
		| variable | Returns `true` if the variable is neither null nor undefined nor the empty string. |
		
		This function is the go-to replacement for more complex checks in both JavaScript and StructrScript for null references, undefined variables, empty strings etc., since you can simply use `!$.empty(..)` on all objects.
		""";
	}

	@Override
	public List<Parameter> getParameters() {
		return super.getParameters();
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${empty('')}", "Returns `true`"),
			Example.structrScript("${empty('test')}", "Returns `false`"),
			Example.structrScript("${empty(find('Project'))}", "Returns `false` if there are Project entites in the database"),
			Example.structrScript("${empty(find('NonExistentType'))}", "WARNING: the call in this example returns `false`  because the error message returned by the `find()` call is non-empty.")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Logic;
	}
}
