/*
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.core.function.RangeFunction;
import org.structr.core.function.search.*;
import org.structr.core.script.polyglot.wrappers.FunctionWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class PredicateBinding implements ProxyObject {

	private static final Logger logger           = LoggerFactory.getLogger(PredicateBinding.class.getName());

	private GraphObject entity                   = null;
	private ActionContext actionContext          = null;

	private final Map<String, Function<Object, Object>> predicateBindings = Map.ofEntries(
		Map.entry("within_distance", Functions.getByClass(FindWithinDistanceFunction.class)),
		Map.entry("sort",            Functions.getByClass(FindSortFunction.class)),
		Map.entry("page",            Functions.getByClass(FindPageFunction.class)),
		Map.entry("not",             Functions.getByClass(FindNotFunction.class)),
		Map.entry("empty",           Functions.getByClass(FindEmptyFunction.class)),
		Map.entry("equals",          Functions.getByClass(FindEqualsFunction.class)),
		Map.entry("or",              Functions.getByClass(FindOrFunction.class)),
		Map.entry("and",             Functions.getByClass(FindAndFunction.class)),
		Map.entry("contains",        Functions.getByClass(FindContainsFunction.class)),
		Map.entry("range",           Functions.getByClass(RangeFunction.class))
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