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
package org.structr.web.eam;

import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;

@Documentation(name="Parameter types", type=ConceptType.Topic, shortDescription="Possible types of Event Action Mapping parameters.", parent="Event Action Mapping")
public enum ParameterType implements Documentable {

	UserInput("user-input", "User input", null),
	ConstantValue("constant-value", "Constant value", null),
	ScriptExpression("script-expression", "Script expression", null),
	PageParam("page-param", "Page parameter", null),
	PageSizeParam("pagesize-param", "Page size parameter", null),
	Unknown("unknown", null, null)

	;

	ParameterType(final String identifier, final String displayName, final String shortDescription) {

		this.identifier       = identifier;
		this.displayName      = displayName;
		this.shortDescription = shortDescription;
	}

	private final String identifier, displayName, shortDescription;

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.EventAction;
	}

	public String getName() {
		return identifier;
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return displayName;
	}

	@Override
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Returns a ParameterType for the given identifier, or the generic "Unknown" action.
	 *
	 * @param identifier
	 * @return
	 */
	public static ParameterType forName(final String identifier) {

		for (final ParameterType type : ParameterType.values()) {

			if (type.getName().equals(identifier)) {
				return type;
			}
		}

		return ParameterType.Unknown;
	}
}
