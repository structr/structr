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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.ArrayList;

/**
 * A namespace of built-in functions that has no function of its own, as seen from JavaScript.
 *
 * Functions registered as {@code documents.extractContent}, {@code documents.splitContent} and so on
 * are reachable as {@code $.documents.extractContent(...)}, while {@code $.documents} itself is not
 * callable. A namespace that is also a function, like {@code log}, is a {@link FunctionWrapper}
 * instead.
 *
 * StructrScript needs none of this: its parser resolves a dotted name directly.
 */
public class FunctionNamespaceWrapper implements ProxyObject {

	private final ActionContext actionContext;
	private final GraphObject entity;
	private final String namespace;

	public FunctionNamespaceWrapper(final ActionContext actionContext, final GraphObject entity, final String namespace) {

		this.actionContext = actionContext;
		this.entity        = entity;
		this.namespace     = namespace;
	}

	@Override
	public Object getMember(final String key) {

		final String name                       = namespace + "." + key;
		final Function<Object, Object> function = Functions.get(name);

		if (function != null) {

			return new FunctionWrapper<>(actionContext, entity, function);
		}

		if (Functions.isNamespace(name)) {

			return new FunctionNamespaceWrapper(actionContext, entity, name);
		}

		return null;
	}

	@Override
	public boolean hasMember(final String key) {

		// GraalJS asks this before it reads, so it has to stay in step with getMember

		final String name = namespace + "." + key;

		return Functions.get(name) != null || Functions.isNamespace(name);
	}

	@Override
	public Object getMemberKeys() {

		final String prefix = namespace + ".";

		return new ArrayList<>(Functions.getNames().stream().filter(name -> name.startsWith(prefix)).map(name -> name.substring(prefix.length()).split("\\.")[0]).distinct().toList());
	}

	@Override
	public void putMember(final String key, final Value value) {

		throw new UnsupportedOperationException("Cannot add members to the function namespace " + namespace + ".");
	}
}
