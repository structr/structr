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
package org.structr.docs.documentables.misc;

import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.api.util.Category;
import org.structr.docs.*;
import org.structr.docs.ontology.ConceptType;

import java.util.LinkedList;
import java.util.List;

public class SettingDocumentable implements Documentable {

	private final SettingsGroup parentGroup;
	private final Setting setting;

	public SettingDocumentable(final SettingsGroup parentGroup, final Setting setting) {

		this.parentGroup = parentGroup;
		this.setting     = setting;
	}

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.Setting;
	}

	@Override
	public Category getCategory() {
		return parentGroup;
	}

	@Override
	public String getName() {
		return setting.getKey();
	}

	@Override
	public String getShortDescription() {
		return setting.getComment();
	}

	@Override
	public String getLongDescription() {
		return null;
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
		return List.of(ConceptReference.of(ConceptType.Topic, parentGroup.getDisplayName()));
	}

	public List<Link> getLinkedConcepts() {

		final List<Link> links = new LinkedList<>();

		return links;
	}

	public static void collectAllSettings(final List<Documentable> documentables) {

		for (final SettingsGroup group : Settings.getGroups()) {

			for (final org.structr.api.config.Setting setting : group.getSettings()) {

				if (setting.getComment() != null) {

					documentables.add(new SettingDocumentable(group, setting));
				}
			}
		}
	}
}












