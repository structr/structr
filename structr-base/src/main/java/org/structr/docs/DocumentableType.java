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
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.HasDisplayName;
import org.structr.rest.resource.MaintenanceResource;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public enum DocumentableType implements HasDisplayName {

	BuiltInFunction("Built-in function", Concept.Type.Function, true, true, Functions::addFunctionsAndExpressions),
	Keyword("Keyword", Concept.Type.Keyword, true, true, AbstractHintProvider::addKeywordHints),
	Method("Method", Concept.Type.Method, true, true, null),
	Property("Property", Concept.Type.Property, false, false, AbstractPrimitiveProperty::addProperties),
	UserDefinedFunction("User-defined function", Concept.Type.UserDefinedFunction, false, false, null),
	MaintenanceCommand("Maintenance command", Concept.Type.MaintenanceCommand, false, false, MaintenanceResource::getMaintenanceCommands),
	SystemType("System type", Concept.Type.SystemType, false, false, TraitsManager::addAllSystemTypes),
	LifecycleMethod("Lifecycle method", Concept.Type.LifecycleMethod, false, true, LifecycleBase::addAllLifecycleMethods),
	Service("Service", Concept.Type.Service, false, false, Services::collectDocumentation),
	Setting("Setting", Concept.Type.Setting, false, false, SettingDocumentable::collectAllSettings),
	RequestKeyword("Request parameter", Concept.Type.RequestParameter, false, false, null),
	RequestHeader("Request header", Concept.Type.RequestHeader, false, false, null),
	Class("Class", Concept.Type.Class, false, false, StructrApp.getConfiguration()::addDocumentedClasses),
	Hidden(null, null, false, false, null);

	private final Consumer<List<Documentable>> getFunction;
	private final boolean supportsLanguages;
	private final boolean supportsExamples;
	private final Concept.Type type;
	private final String displayName;

	DocumentableType(final String displayName, final Concept.Type type, final boolean supportsLanguages, final boolean supportsExamples, final Consumer<List<Documentable>> getFunction) {

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

	public String getDisplayName() {
		return displayName;
	}

	public Concept.Type getConcept() {
		return type;
	}

	public boolean supportsLanguages() {
		return supportsLanguages;
	}

	public boolean supportsExamples() {
		return supportsExamples;
	}

	public static DocumentableType forOntologyType(final Concept.Type conceptType) {

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
