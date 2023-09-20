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
package org.structr.core.script.polyglot.wrappers;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.rest.resource.SchemaMethodResource;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalSchemaMethodWrapper implements ProxyObject {
	private static Logger logger = LoggerFactory.getLogger(GlobalSchemaMethodWrapper.class);
	private final ActionContext actionContext;

	public GlobalSchemaMethodWrapper(final ActionContext actionContext) {
		this.actionContext = actionContext;
	}

	@Override
	public Object getMember(String key) {

		return getExecutable(key);
	}

	@Override
	public Object getMemberKeys() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			List<String> result =  app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null).sort(AbstractNode.name).getAsList().stream().map(AbstractNode::getName).toList();
			tx.success();
		} catch (FrameworkException ex) {

			logger.error(ExceptionUtils.getStackTrace(ex));
		}

		return null;
	}

	@Override
	public boolean hasMember(String key) {
		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final boolean exists = app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null).andName(key).getFirst() != null;
			tx.success();

			return exists;
		} catch (FrameworkException ex) {

			logger.error(ExceptionUtils.getStackTrace(ex));
		}

		return false;
	}

	@Override
	public void putMember(String key, Value value) {
	}

	private ProxyExecutable getExecutable(final String methodName) {
		final List<ProxyExecutable> executables = getExecutables(methodName);

		if (executables.size() > 0) {

			return executables.get(0);
		}

		return null;
	}

	private List<ProxyExecutable> getExecutables(final String methodName) {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			Query<SchemaMethod> query = app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null);

			if (methodName != null) {

				query = query.andName(methodName);
			}

			final List<SchemaMethod> methods = query.getAsList();

			tx.success();

			if (!methods.isEmpty()) {

				return methods.stream().map(this::getExecutable).collect(Collectors.toList());
			}

		} catch (FrameworkException fex) {

			logger.error(ExceptionUtils.getStackTrace(fex));
		}

		return new ArrayList<>();
	}

	private ProxyExecutable getExecutable(final SchemaMethod method) {

		return arguments -> {

			Object[] args = Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();

			Map<String, Object> paramMap;

			if (args.length > 0 && args[0] instanceof Map params) {

				paramMap = params;
			} else {

				paramMap = new HashMap<>();
			}

			try {

				final String source = method.getProperty(SchemaMethod.source);
				return PolyglotWrapper.wrap(actionContext, Actions.execute(actionContext.getSecurityContext(), null, "${" + source.trim() + "}", paramMap, method.getName(), source));
			} catch (FrameworkException e) {

				throw new RuntimeException(e);
			}
		};
	}
}
