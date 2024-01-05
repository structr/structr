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
package org.structr.core.script.polyglot.wrappers;

import jakarta.servlet.http.HttpServletRequest;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.LoggerFactory;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class HttpServletRequestWrapper implements ProxyObject {
	private final ActionContext actionContext;
	private final HttpServletRequest request;

	public HttpServletRequestWrapper(final ActionContext actionContext, final HttpServletRequest request) {

		this.actionContext = actionContext;
		this.request = request;
	}

	@Override
	public Object getMember(String key) {

		if (request != null) {
			Object value = request.getParameterValues(key);
			if (value != null && ((String[]) value).length == 1) {

				value = ((String[]) value)[0];
				return value;
			} else if (value != null && ((String[]) value).length > 1) {

				return PolyglotWrapper.wrap(actionContext, value);
			} else {

				Method[] methods = request.getClass().getMethods();
				for (Method method : methods) {

					if (method.getName().equals(key)) {

						return (ProxyExecutable) arguments -> {
							try {

								if (method.getParameterCount() == 0) {

									return PolyglotWrapper.wrap(actionContext, method.invoke(this.request));
								} else {

									return PolyglotWrapper.wrap(actionContext, method.invoke(this.request, Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray()));
								}
							} catch (InvocationTargetException | IllegalAccessException ex) {

								LoggerFactory.getLogger(HttpServletRequestWrapper.class).error("Unexpected exception while trying to invoke member function on Request.", ex);
							}

							return null;
						};
					}
				}
			}

		}

		return null;
	}

	@Override
	public Object getMemberKeys() {

		if (request != null) {

			return request.getParameterMap().keySet().toArray();
		}

		return null;
	}

	@Override
	public boolean hasMember(String key) {
		if (request != null) {
			if (request.getParameterMap().containsKey(key)) {
				return true;
			} else {
				Method[] methods = request.getClass().getMethods();
				for (Method method : methods) {

					if (method.getName().equals(key)) {

						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public void putMember(String key, Value value) {
		// Request is immutable
	}
}
