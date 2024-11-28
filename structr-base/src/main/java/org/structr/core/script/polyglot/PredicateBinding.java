/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.script.polyglot;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.core.script.polyglot.wrappers.FunctionWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.Map;

public class PredicateBinding implements ProxyObject {

	private static final Logger logger           = LoggerFactory.getLogger(PredicateBinding.class.getName());

	private GraphObject entity                   = null;
	private ActionContext actionContext          = null;

	private final Map<String, Function<Object, Object>> predicateBindings = Map.ofEntries(
		Map.entry("within_distance", Functions.get("find.within_distance")),
		Map.entry("withinDistance",  Functions.get("find.within_distance")),
		Map.entry("sort",            Functions.get("find.sort")),
		Map.entry("page",            Functions.get("find.page")),
		Map.entry("not",             Functions.get("find.not")),
		Map.entry("empty",           Functions.get("find.empty")),
		Map.entry("equals",          Functions.get("find.equals")),
		Map.entry("or",              Functions.get("find.or")),
		Map.entry("and",             Functions.get("find.and")),
		Map.entry("contains",        Functions.get("find.contains")),
		Map.entry("starts_with",     Functions.get("find.starts_with")),
		Map.entry("startsWith",      Functions.get("find.starts_with")),
		Map.entry("ends_with",       Functions.get("find.ends_with")),
		Map.entry("endsWith",        Functions.get("find.ends_with")),
		Map.entry("range",           Functions.get("find.range")),
		Map.entry("lt",              Functions.get("find.lt")),
		Map.entry("lte",             Functions.get("find.lte")),
		Map.entry("gte",             Functions.get("find.gte")),
		Map.entry("gt",              Functions.get("find.gt"))
	);

	public PredicateBinding(final ActionContext actionContext, final GraphObject entity) {

		this.entity = entity;
		this.actionContext = actionContext;
	}

	@Override
	public Object getMember(String name) {

		if (hasMember(name)) {
			return new FunctionWrapper(actionContext, entity, predicateBindings.get(name));
		}

		return null;
	}

	@Override
	public Object getMemberKeys() {

		return predicateBindings.keySet();
	}

	@Override
	public boolean hasMember(String key) {

		return predicateBindings.containsKey(key);
	}

	@Override
	public void putMember(String key, Value value) {
	}
}