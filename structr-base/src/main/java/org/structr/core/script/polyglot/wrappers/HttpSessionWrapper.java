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

import jakarta.servlet.http.HttpSession;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class HttpSessionWrapper implements ProxyObject  {
	private final ActionContext actionContext;
	private final HttpSession session;

	private static final Map<String, Function<HttpSession, Object>> staticKeywords = Map.of(
			"id", HttpSession::getId,
			"creationTime", HttpSession::getCreationTime,
			"isNew", HttpSession::isNew,
			"lastAccessedTime", HttpSession::getLastAccessedTime
	);

	public HttpSessionWrapper(final ActionContext actionContext, final HttpSession session) {

		this.actionContext = actionContext;
		this.session = session;
	}

	@Override
	public Object getMember(String key) {

		if (session == null) {
			return null;
		}

		if (staticKeywords.containsKey(key)) {
			return PolyglotWrapper.wrap(actionContext, staticKeywords.get(key).apply(session));
		}
		return PolyglotWrapper.wrap(actionContext,  session.getAttribute(key));
	}

	@Override
	public Object getMemberKeys() {

		List<String> keys = Collections.list(session.getAttributeNames());

		staticKeywords.forEach((key, value) -> {
			if (!keys.contains(key)) {
				keys.add(key);
			}
		});

		return PolyglotWrapper.wrap(actionContext, keys);
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
