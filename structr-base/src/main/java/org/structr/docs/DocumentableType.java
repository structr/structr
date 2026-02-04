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

import org.structr.autocomplete.AbstractHintProvider;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.traits.TraitsManager;
import org.structr.docs.documentables.lifecycle.LifecycleBase;
import org.structr.docs.documentables.misc.SettingDocumentable;
import org.structr.docs.ontology.ConceptType;
import org.structr.rest.resource.MaintenanceResource;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public enum DocumentableType {

	BuiltInFunction("Built-in Function", ConceptType.Function, true, true, Functions::addFunctionsAndExpressions),
	EventAction("Event Action", ConceptType.EventAction, false, false, null),
	EventNotification("Event Notification", ConceptType.EventNotification, false, false, null),
	EventBehaviour("Event Behaviour", ConceptType.EventBehaviour, false, false, null),
	Keyword("System Keyword", ConceptType.Keyword, true, true, AbstractHintProvider::addBuiltInKeywordHints),
	Method("Method", ConceptType.Method, true, true, null),
	Property("Property", ConceptType.Property, false, false, AbstractPrimitiveProperty::addProperties),
	UserDefinedFunction("User-defined Function", ConceptType.UserDefinedFunction, false, false, null),
	MaintenanceCommand("Maintenance Command", ConceptType.MaintenanceCommand, false, false, MaintenanceResource::getMaintenanceCommands),
	SystemType("System Type", ConceptType.SystemType, false, false, TraitsManager::addAllSystemTypes),
	LifecycleMethod("Lifecycle Method", ConceptType.LifecycleMethod, false, true, LifecycleBase::addAllLifecycleMethods),
	Service("Service", ConceptType.Service, false, false, Services::collectDocumentation),
	Setting("Setting", ConceptType.Setting, false, false, SettingDocumentable::collectAllSettings),
	RequestKeyword("Request Parameter", ConceptType.RequestParameter, false, false, null),
	RequestHeader("Request Header", ConceptType.RequestHeader, false, false, null),
	Constant("Constant", ConceptType.Constant, false, false, null),
	Hidden(null, null, false, false, null);

	private final Consumer<List<Documentable>> getFunction;
	private final boolean supportsLanguages;
	private final boolean supportsExamples;
	private final ConceptType type;
	private final String displayName;

	DocumentableType(final String displayName, final ConceptType type, final boolean supportsLanguages, final boolean supportsExamples, final Consumer<List<Documentable>> getFunction) {

		this.getFunction       = getFunction;
		this.supportsLanguages = supportsLanguages;
		this.supportsExamples  = supportsExamples;
		this.type              = type;
		this.displayName       = displayName;
	}

	public List<Documentable> getDocumentables() {

		final List<Documentable> documentables = new LinkedList<>();

		if (getFunction != null) {

			getFunction.accept(documentables);
		}

		return documentables;
	}

	public ConceptType getConcept() {
		return type;
	}

	public boolean supportsLanguages() {
		return supportsLanguages;
	}

	public boolean supportsExamples() {
		return supportsExamples;
	}

	// ----- interface GlossaryTerm -----
	public String getDisplayName() {
		return displayName;
	}

	public String getName() {
		return type.getIdentifier();
	}

	public String getShortDescription() {
		return "Documentable type \"" + displayName + "\"";
	}

	// ----- static methods ----
	public static DocumentableType forOntologyType(final ConceptType conceptType) {

		if (conceptType != null) {

			for (DocumentableType documentableType : DocumentableType.values()) {

				if (conceptType.equals(documentableType.type)) {

					return documentableType;
				}
			}
		}

		return DocumentableType.Hidden;
	}

	public static void collectAllDocumentables(final List<Documentable> documentables) {

		for (final DocumentableType documentableType : DocumentableType.values()) {

			documentables.addAll(documentableType.getDocumentables());
		}
	}
}
