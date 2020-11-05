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
package org.structr.core.script.polyglot.wrappers;

import org.apache.commons.lang3.ArrayUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticTypeWrapper implements ProxyObject {
	private final static Logger logger = LoggerFactory.getLogger(StaticTypeWrapper.class);
	private final App app;
	private final Class referencedClass;
	private final ActionContext actionContext;

	public StaticTypeWrapper(final ActionContext actionContext, final Class referencedClass) {

		this.actionContext = actionContext;
		this.app = StructrApp.getInstance();
		this.referencedClass = referencedClass;
	}


	@Override
	public Object getMember(String key) {

		Map<String, Method> methods = StructrApp.getConfiguration().getAnnotatedMethods(referencedClass, Export.class);

		if (methods.containsKey(key)) {
			Method method = methods.get(key);

			// Ensure that  method is static
			try (final Tx tx = app.tx()) {
				SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(referencedClass.getSimpleName()).getFirst();

				if (schemaNode != null) {

					List<SchemaMethod> schemaMethods = Iterables.toList(schemaNode.getSchemaMethods());
					boolean matchingStaticMethodFound = false;

					for (SchemaMethod m : schemaMethods) {

						if (m.getName().equals(key) && m.isStaticMethod()) {

							matchingStaticMethodFound = true;
							break;
						}
					}

					if (!matchingStaticMethodFound) {

						return null;
					}
				}

				tx.success();
			} catch (FrameworkException ex) {

				logger.warn("Unexpected exception while trying to look up schema method.", ex);
				return null;
			}

			return (ProxyExecutable) arguments -> {

				try {

					int paramCount = method.getParameterCount();

					// FixMe: Ensure that static schema methods get compiled to static java methods and pass null as caller object instead new instance
					Object newInstance = null;
					try {
						newInstance = referencedClass.newInstance();
					} catch (Exception ex) {
						logger.warn("Could not instantiate new instance of class " + referencedClass.getSimpleName());
					}

					if (paramCount == 0) {

						return PolyglotWrapper.wrap(actionContext, method.invoke(newInstance));
					} else if (paramCount == 1) {

						return PolyglotWrapper.wrap(actionContext, method.invoke(newInstance, actionContext.getSecurityContext()));
					} else if (paramCount == 2 && arguments.length == 0) {

						return PolyglotWrapper.wrap(actionContext, method.invoke(newInstance, actionContext.getSecurityContext(), new HashMap<String, Object>()));
					} else if (arguments.length == 0) {

						return PolyglotWrapper.wrap(actionContext, method.invoke(newInstance, actionContext.getSecurityContext()));
					} else {

						return PolyglotWrapper.wrap(actionContext, method.invoke(newInstance, ArrayUtils.add(Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray(), 0, actionContext.getSecurityContext())));
					}

				} catch (IllegalAccessException ex) {

					logger.error("Unexpected exception while trying to get GraphObject member.", ex);
				} catch (InvocationTargetException ex) {

					if (ex.getTargetException() instanceof FrameworkException) {

						throw new RuntimeException(ex.getTargetException());
					}
					logger.error("Unexpected exception while trying to get GraphObject member.", ex);
				}

				return null;

			};

		}

		return null;
	}

	@Override
	public Object getMemberKeys() {
		return null;
	}

	@Override
	public boolean hasMember(String key) {
		return getMember(key) != null;
	}

	@Override
	public void putMember(String key, Value value) {

	}
}
