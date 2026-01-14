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

		final Property<String> textProperty        = new StringProperty(TEXT_PROPERTY).description("text content of this template");
		final Property<String> descriptionProperty = new StringProperty(DESCRIPTION_PROPERTY).indexed().description("description of this template");
		final Property<String> localeProperty      = new StringProperty(LOCALE_PROPERTY).indexed().description("locale for this template");

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
	public String getShortDescription() {
		return "This type represents customizable email templates.";
	}

	@Override
	public String getLongDescription() {
		return """
		### How It Works
		A MailTemplate is a node with a name, text content and locale that you can use to send customized emails. The text content can contain Script Expressions that are evaluated before the next processing step, and MailTemplates with pre-defined keys are used in internal processes in Structr.
		
		### Common Use Cases
		- MailTemplates are used to customize emails that are sent to the new users, if you configure Structr to allow User Self-Registration.
		
		### Notes
		- To send emails from Structr, the appropriate settings must be made in structr.conf.
		""";
	}
}
