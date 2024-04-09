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

import jakarta.servlet.http.HttpSession;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

public class HttpSessionWrapper implements ProxyObject  {
	private final ActionContext actionContext;
	private final HttpSession session;

	public HttpSessionWrapper(final ActionContext actionContext, final HttpSession session) {

		this.actionContext = actionContext;
		this.session = session;
	}

	@Override
	public Object getMember(String key) {
		return PolyglotWrapper.wrap(actionContext,  session.getAttribute(key));
	}

	@Override
	public Object getMemberKeys() {
		return PolyglotWrapper.wrap(actionContext, session.getAttributeNames());
	}

	@Override
	public boolean hasMember(String key) {
		return true;
	}

	@Override
	public void putMember(String key, Value value) {
		session.setAttribute(key, PolyglotWrapper.unwrap(actionContext, value));
	}
}
