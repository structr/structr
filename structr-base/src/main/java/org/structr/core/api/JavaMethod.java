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
package org.structr.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.script.Snippet;

/**
 */
public abstract class JavaMethod extends AbstractMethod {

	private static final Logger logger = LoggerFactory.getLogger(JavaMethod.class);

	private final Parameters parameters;
	private final boolean isPrivate;
	private final boolean isStatic;

	public JavaMethod(final String name, final boolean isPrivate, final boolean isStatic) {

		super(name, null, null);

		this.parameters = new Parameters();
		this.isPrivate  = isPrivate;
		this.isStatic   = isStatic;
	}

	@Override
	public String toString() {
		return getFullMethodName();
	}

	@Override
	public boolean isPrivate() {
		return isPrivate;
	}

	@Override
	public boolean isStatic() {
		return isStatic;
	}

	@Override
	public Snippet getSnippet() {
		return null;
	}

	@Override
	public String getHttpVerb() {
		return "POST";
	}

	@Override
	public Parameters getParameters() {
		return parameters;
	}

	@Override
	public String getFullMethodName() {
		return "Java method ‛" + name + "‛";
	}
}
