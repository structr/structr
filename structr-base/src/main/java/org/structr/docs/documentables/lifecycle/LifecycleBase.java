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
package org.structr.docs.documentables.lifecycle;

import org.structr.docs.*;
import org.structr.docs.ontology.ConceptType;

import java.util.List;

public abstract class LifecycleBase implements Documentable {

	private final String name;

	public LifecycleBase(final String name) {
		this.name = name;
	}

	@Override
	public final DocumentableType getDocumentableType() {
		return DocumentableType.LifecycleMethod;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public List<Parameter> getParameters() {
		return null;
	}

	@Override
	public List<Example> getExamples() {
		return null;
	}

	@Override
	public List<String> getNotes() {
		return null;
	}

	@Override
	public List<Signature> getSignatures() {
		return null;
	}

	@Override
	public List<Language> getLanguages() {
		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return null;
	}

	@Override
	public List<ConceptReference> getParentConcepts() {

		final List<ConceptReference> parentConcepts = Documentable.super.getParentConcepts();

		parentConcepts.add(ConceptReference.of(ConceptType.Topic, "Lifecycle methods"));

		return parentConcepts;
	}

	/**
	 *
	 * @param documentables
	 */
	public static void addAllLifecycleMethods(final List<Documentable> documentables) {

		documentables.add(new OnCreate());
		documentables.add(new OnSave());
		documentables.add(new OnDelete());
		documentables.add(new AfterCreate());
		documentables.add(new AfterSave());
		documentables.add(new AfterDelete());
		documentables.add(new OnNodeCreation());

		// File
		documentables.add(new OnUpload());
		documentables.add(new OnDownload());

		// User
		documentables.add(new OnOAuthLogin());


		// global
		documentables.add(new OnStructrLogin());
		documentables.add(new OnStructrLogout());
		documentables.add(new OnAcmeChallenge());


	}
}
