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
package org.structr.autocomplete.keywords;

import org.structr.autocomplete.PageKeywordHint;

public class VisibilityMappingHint extends PageKeywordHint {

	@Override
	public String getName() {

		return "visibilityMapping";
	}

	@Override
	public String getShortDescription() {

		return "Refers to the VisibilityMapping of the enclosing partial.";
	}

	@Override
	public String getLongDescription() {

		return "The `visibilityMapping` keyword returns the VisibilityMapping bound to this node, or to the "
			+ "closest ancestor that has one -- the same upward traversal `component` does. Use it to read what "
			+ "a partial is bound to without a query, e.g. `$.visibilityMapping.boundStep.bpmnName` for the "
			+ "current name of the BPMN step a partial renders. Returns null when nothing in the chain is bound.";
	}
}
