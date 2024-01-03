/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.core.script.polyglot.function;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.structr.autocomplete.BuiltinFunctionHint;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;

public class IncludeJSFunction extends BuiltinFunctionHint implements ProxyExecutable {

	private final ActionContext actionContext;

	public IncludeJSFunction(final ActionContext actionContext) {

		this.actionContext = actionContext;
	}

	@Override
	public Object execute(Value... arguments) {

		final Object[] args = Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();
		String sourceFileName;

		if (args.length > 0 && args[0] instanceof String) {

			sourceFileName = (String)args[0];

			Context.getCurrent().eval(actionContext.getJavascriptLibraryCode(sourceFileName));
		}

		return null;
	}

	@Override
	public String getName() {
		return "includeJs";
	}

	@Override
	public String shortDescription() {
		return """
**JavaScript-only**

Searches a file with the three following conditions and evaluates it in the current javascript context so that common functionality can be used in multiple places.

Conditions:

- name parameter must match file name (with extension)
- file must have flag useAsJavascriptLibrary = true
- file content type must be one of "text/javascript", "application/javascript" or "application/javascript+module"

Example:
```
${{
	$.includeJs('myLibrary.js');

	// can access all variables/functions declared in myLibrary.js
}}
```
""";
	}

	@Override
	public String getSignature() {
		return "name";
	}
}