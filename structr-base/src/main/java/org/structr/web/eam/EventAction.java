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

@Documentation(name="Event Actions", type=ConceptType.Topic, shortDescription="Backend actions that can be executed via Event Action Mapping.", parent="Event Action Mapping")
public enum EventAction implements Documentable {

	None("none", "No action", null),
	Create("create", "Create new object", null),
	Update("update", "Update object", null),
	Delete("delete", "Delete object", null),
	AppendChild("append-child", "Append child", null),
	RemoveChild("remove-child", "Remove child", null),
	InsertHtml("insert-html", "Insert HTML", null),
	ReplaceHtml("replace-html", "Replace HTML", null),
	PrevPage("prev-page", "Previous page", null),
	NextPage("next-page", "Next page", null),
	FirstPage("first-page", "First page", null),
	LastPage("last-page", "Last page", null),
	SignIn("sign-in", "Sign in", null),
	SignOut("sign-out", "Sign out", null),
	SignUp("sign-up", "Sign up", null),
	ResetPassword("reset-password", "Reset password", null),
	Flow("flow", "Execute flow", null),
	Method("method", "Execute method", null),

	// The "unknown" action will be used for all actions that are not defined in this enum. This is because a
	// previous version of the Event Action Mapping used the action property to store the name of a method to
	// be called.
	Unknown("unknown", null, null);

	EventAction(final String identifier, final String displayName, final String shortDescription) {

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
	 * Returns an EventAction for the given identifier, or the generic "Unknown" action.
	 *
	 * @param identifier
	 * @return
	 */
	public static EventAction forName(final String identifier) {

		for (final EventAction type : EventAction.values()) {

			if (type.getName().equals(identifier)) {
				return type;
			}
		}

		return EventAction.Unknown;
	}
}
