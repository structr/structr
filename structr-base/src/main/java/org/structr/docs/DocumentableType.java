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
import org.structr.docs.ontology.HasDisplayName;
import org.structr.rest.api.RESTEndpoints;
import org.structr.rest.resource.MaintenanceResource;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public enum DocumentableType implements HasDisplayName {

	BuiltInFunction("Built-in function", "function", true, true, Functions::addFunctionsAndExpressions),
	Keyword("Keyword", "keyword", true, true, AbstractHintProvider::addKeywordHints),
	Method("Method", "method", true, true, null),
	Property("Property", "property", false, false, AbstractPrimitiveProperty::addProperties),
	UserDefinedFunction("User-defined function", "user-defined-function", false, false, null),
	MaintenanceCommand("Maintenance command", "maintenance-command", false, false, MaintenanceResource::getMaintenanceCommands),
	SystemType("System type", "system-type", false, false, TraitsManager::addAllSystemTypes),
	LifecycleMethod("Lifecycle method", "lifecycle-method", false, true, LifecycleBase::addAllLifecycleMethods),
	Service("Service", "service", false, false, Services::collectDocumentation),
	Setting("Setting", "setting", false, false, SettingDocumentable::collectAllSettings),
	RequestKeyword("Request parameter", "request-parameter", false, false, null),
	RequestHeader("Request header", "request-header", false, false, null),
	Class("Class", "class", false, false, StructrApp.getConfiguration()::addDocumentedClasses),
	Hidden(null, null, false, false, null);

	private final Consumer<List<Documentable>> getFunction;
	private final boolean supportsLanguages;
	private final boolean supportsExamples;
	private final String ontologyType;
	private final String displayName;

	DocumentableType(final String displayName, final String ontologyType, final boolean supportsLanguages, final boolean supportsExamples, final Consumer<List<Documentable>> getFunction) {

		this.getFunction       = getFunction;
		this.supportsLanguages = supportsLanguages;
		this.supportsExamples  = supportsExamples;
		this.ontologyType      = ontologyType;
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

	public String getOntologyType() {
		return ontologyType;
	}

	public boolean supportsLanguages() {
		return supportsLanguages;
	}

	public boolean supportsExamples() {
		return supportsExamples;
	}

	public static DocumentableType forOntologyType(final String ontologyType) {

		if (ontologyType != null) {

			for (DocumentableType documentableType : DocumentableType.values()) {

				if (ontologyType.equals(documentableType.ontologyType)) {

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
