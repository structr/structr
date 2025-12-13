/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.docs.formatter;

import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.autocomplete.AbstractHintProvider;
import org.structr.core.Services;
import org.structr.core.function.Functions;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.docs.Documentable;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.impl.lifecycle.*;
import org.structr.docs.impl.settings.SettingDocumentable;
import org.structr.docs.ontology.Concept;
import org.structr.rest.resource.MaintenanceResource;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MarkdownCodeSourceFormatter extends Formatter {

	@Override
	public void format(final List<String> lines, final Concept concept, final OutputSettings settings, String link, final int level) {

		final List<Documentable> documentables = new LinkedList<>();

		switch (concept.getName()) {

			case "keywords":
				AbstractHintProvider.addKeywordHints(documentables);
				break;

			case "functions":
				documentables.addAll(Functions.getFunctions());
				Functions.addExpressions(documentables);
				break;

			case "maintenance-commands":
				documentables.addAll(MaintenanceResource.getMaintenanceCommands());
				break;

			case "system-types":
				final TraitsInstance rootInstance = TraitsManager.getRootInstance();
				for (final String traitName : rootInstance.getAllTypes(t -> t.isNodeType())) {

					final Traits traits = rootInstance.getTraits(traitName);
					if (!traits.isHidden()) {

						documentables.add(traits);
					}
				}
				break;

			case "lifecycle-methods":
				// lifecycle methods
				documentables.add(new OnCreate());
				documentables.add(new OnSave());
				documentables.add(new OnDelete());
				documentables.add(new AfterCreate());
				documentables.add(new AfterSave());
				documentables.add(new AfterDelete());
				break;

			case "services":
				Services.collectDocumentation(documentables);
				break;

			case "settings":
				for (final SettingsGroup group : Settings.getGroups()) {

					for (final org.structr.api.config.Setting setting : group.getSettings()) {

						if (setting.getComment() != null) {

							documentables.add(new SettingDocumentable(setting));
						}
					}
				}
				break;
		}

		// sort
		Collections.sort(documentables, Comparator.comparing(Documentable::getName));

		// render
		for (final Documentable documentable : documentables) {
			lines.addAll(documentable.createMarkdownDocumentation(settings.getDetails(), level));
		}
	}
}
