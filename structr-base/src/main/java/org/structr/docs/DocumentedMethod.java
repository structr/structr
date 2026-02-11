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
package org.structr.docs;

import org.structr.core.api.AbstractMethod;

public class DocumentedMethod {

	private final String name;
	private final String description;

	private DocumentedMethod(final String name, final String description) {

		this.name         = name;
		this.description  = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public static DocumentedMethod of(final AbstractMethod method) {

		final String name        = method.getName() + "()";
		final String description = method.getDescription();

		return new DocumentedMethod(name, description);
	}
}
