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
package org.structr.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Relation;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.wrappers.MailTemplateTraitWrapper;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class MailTemplateTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String TEXT_PROPERTY        = "text";
	public static final String DESCRIPTION_PROPERTY = "description";
	public static final String LOCALE_PROPERTY      = "locale";

	public MailTemplateTraitDefinition() {
		super(StructrTraits.MAIL_TEMPLATE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			MailTemplate.class, (traits, node) -> new MailTemplateTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String> textProperty        = new StringProperty(TEXT_PROPERTY).description("Text content of this template.");
		final Property<String> descriptionProperty = new StringProperty(DESCRIPTION_PROPERTY).indexed().description("Description of this template.");
		final Property<String> localeProperty      = new StringProperty(LOCALE_PROPERTY).indexed().description("Locale for this template.");

		return newSet(
			textProperty,
			descriptionProperty,
			localeProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
					TEXT_PROPERTY, DESCRIPTION_PROPERTY, LOCALE_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					TEXT_PROPERTY, DESCRIPTION_PROPERTY, LOCALE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public boolean includeInDocumentation() {
		return true;
	}
}
