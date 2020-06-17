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

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.GraphObject;
import org.structr.core.function.search.*;
import org.structr.schema.action.ActionContext;

import java.util.HashSet;

public class PredicateBinding implements ProxyObject {

	private static final Logger logger           = LoggerFactory.getLogger(PredicateBinding.class.getName());

	private GraphObject entity                   = null;
	private ActionContext actionContext          = null;

	public PredicateBinding(final ActionContext actionContext, final GraphObject entity) {

		this.entity = entity;
		this.actionContext = actionContext;
	}

	@Override
	public Object getMember(String name) {

		switch (name) {
			case "within_distance":
				return new FunctionWrapper(actionContext, entity, new FindWithinDistanceFunction());
			case "sort":
				return new FunctionWrapper(actionContext, entity, new FindSortFunction());
			case "page":
				return new FunctionWrapper(actionContext, entity, new FindPageFunction());
			case "not":
				return new FunctionWrapper(actionContext, entity, new FindNotFunction());
			case "empty":
				return new FunctionWrapper(actionContext, entity, new FindEmptyFunction());
			case "equals":
				return new FunctionWrapper(actionContext, entity, new FindEqualsFunction());
			case "or":
				return new FunctionWrapper(actionContext, entity, new FindOrFunction());
			case "and":
				return new FunctionWrapper(actionContext, entity, new FindAndFunction());
			case "contains":
				return new FunctionWrapper(actionContext, entity, new FindContainsFunction());
		}

		return null;
	}

	@Override
	public Object getMemberKeys() {
		return new HashSet<>();
	}

	@Override
	public boolean hasMember(String key) {
		return getMember(key) != null;
	}

	@Override
	public void putMember(String key, Value value) {
	}
}
