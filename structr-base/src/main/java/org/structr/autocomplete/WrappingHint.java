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
package org.structr.autocomplete;

import org.structr.docs.*;

import java.util.List;

public class WrappingHint extends AbstractHint {

	private final Documentable documentable;
	private final String name;

	public WrappingHint(final Documentable documentable, final String name) {

		this.documentable = documentable;
		this.name         = name;
	}

	@Override
	public DocumentableType getDocumentableType() {
		return documentable.getDocumentableType();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getShortDescription() {
		return documentable.getShortDescription();
	}

	@Override
	public String getLongDescription() {
		return documentable.getLongDescription();
	}

	@Override
	public List<Parameter> getParameters() {
		return documentable.getParameters();
	}

	@Override
	public List<Example> getExamples() {
		return documentable.getExamples();
	}

	@Override
	public List<String> getNotes() {
		return documentable.getNotes();
	}

	@Override
	public List<Signature> getSignatures() {
		return documentable.getSignatures();
	}

	@Override
	public List<Language> getLanguages() {
		return documentable.getLanguages();
	}

	@Override
	public List<Usage> getUsages() {
		return documentable.getUsages();
	}

	@Override
	public boolean isHidden() {
		return documentable.isHidden();
	}

	@Override
	public List<Property> getProperties() {
		return documentable.getProperties();
	}

	@Override
	public List<Setting> getSettings() {
		return documentable.getSettings();
	}
}
