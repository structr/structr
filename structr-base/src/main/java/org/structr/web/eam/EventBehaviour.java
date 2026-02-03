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

@Documentation(name="Follow-up actions", type=ConceptType.Topic, shortDescription="Automated follow-up actions that can be executed after an action was executed.", parent="Event Action Mapping")
public enum EventBehaviour implements Documentable {

	PartialRefresh("partial-refresh", "Partial refresh", null),
	PartialRefreshLinked("partial-refresh-linked", "Partial refresh linked", "Partial refresh of a linked element."),
	NavigateToUrl("navigate-to-url", "Navigate to URL", null),
	FireEvent("fire-event", "Fire event", null),
	FullPageReload("full-page-reload", "Full page reload", null),
	SignOut("sign-out", "Sign out", null),
	None("none", "None", null),
	Unknown("unknown", null, null);

	EventBehaviour(final String identifier, final String displayName, final String shortDescription) {

		this.identifier       = identifier;
		this.displayName      = displayName;
		this.shortDescription = shortDescription;
	}

	private final String identifier, displayName, shortDescription;

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.EventBehaviour;
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
	 * Returns an EventAction for the given identifier, or the generic "Unknown" action.
	 *
	 * @param identifier
	 * @return
	 */
	public static EventBehaviour forName(final String identifier) {

		for (final EventBehaviour type : EventBehaviour.values()) {

			if (type.getName().equals(identifier)) {
				return type;
			}
		}

		return EventBehaviour.Unknown;
	}
}
