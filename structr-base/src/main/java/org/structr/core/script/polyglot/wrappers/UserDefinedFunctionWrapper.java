/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserDefinedFunctionWrapper implements ProxyObject {

	private static Logger logger = LoggerFactory.getLogger(UserDefinedFunctionWrapper.class);
	private final Map<String, AbstractMethod> methods;
	private final ActionContext actionContext;

	public UserDefinedFunctionWrapper(final ActionContext actionContext) {

		this.methods       = Methods.getAllMethods(null);
		this.actionContext = actionContext;
	}

	@Override
	public Object getMember(String key) {

		return getExecutable(key);
	}

	@Override
	public Object getMemberKeys() {
		return methods.values().stream().map(AbstractMethod::getName).toList();
	}

	@Override
	public boolean hasMember(final String key) {
		return methods.containsKey(key);
	}

	@Override
	public void putMember(String key, Value value) {
	}

	private ProxyExecutable getExecutable(final String methodName) {

		final List<ProxyExecutable> executables = getExecutables(methodName);

		if (!executables.isEmpty()) {

			return executables.get(0);
		}

		return null;
	}

	private List<ProxyExecutable> getExecutables(final String methodName) {

		final AbstractMethod method = methods.get(methodName);
		if (method != null) {

			return List.of(method.getProxyExecutable(actionContext, null));
		}

		return new ArrayList<>();
	}
}
