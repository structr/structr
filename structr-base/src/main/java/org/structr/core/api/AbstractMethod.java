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
package org.structr.core.api;

import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.EvaluationHints;

/**
 *
 */
public abstract class AbstractMethod {

	protected String description = null;
	protected String summary     = null;
	protected String name        = null;

	public AbstractMethod(final String name, final String summary, final String description) {

		this.description = description;
		this.summary     = summary;
		this.name        = name;
	}

	/*
- Konvertierung von REST-Parametern
- Definition von Methodensignaturen (Typ usw.)
- dynamische Page-Pfade




- Minimalziel: String-Inputs, die wir per REST reinkriegen, z.B. in Date konvertieren zu können
- eine Art von automatischer Validierung beim Aufruf

	*/

	public abstract boolean isStatic();
	public abstract Map<String, String> getParameters();
	public abstract Object execute(final SecurityContext securityContext, final Map<String, Object> arguments, final EvaluationHints hints) throws FrameworkException;

	public String getName() {
		return name;
	}

	public String getSummary() {
		return summary;
	}

	public String getDescription() {
		return description;
	}
}
