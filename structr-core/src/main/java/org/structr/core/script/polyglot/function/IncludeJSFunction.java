/*
 * Copyright (C) 2010-2021 Structr GmbH
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
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;

public class IncludeJSFunction implements ProxyExecutable {
	private final ActionContext actionContext;

	public IncludeJSFunction(final ActionContext actionContext) {

		this.actionContext = actionContext;
	}

	@Override
	public Object execute(Value... arguments) {
		Object[] args = Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();
		String sourceFileName;

		if (args.length > 0 && args[0] instanceof String) {
			sourceFileName = (String)args[0];
			Context.getCurrent().eval("js", actionContext.getJavascriptLibraryCode(sourceFileName));
		}

		return null;
	}
}
