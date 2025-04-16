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
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

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

			final Object value = request.getParameterValues(key);

			if (value != null && ((String[]) value).length == 1) {

				return ((String[]) value)[0];

			} else if (value != null && ((String[]) value).length > 1) {

				return PolyglotWrapper.wrap(actionContext, value);
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

			return request.getParameterMap().containsKey(key);
		}

		return false;
	}

	@Override
	public void putMember(String key, Value value) {
		// Request is immutable
	}
}
