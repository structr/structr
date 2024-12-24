/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.traits.definitions.html;

import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.entity.html.A;
import org.structr.web.traits.operations.AvoidWhitespace;
import org.structr.web.traits.wrappers.html.ATraitWrapper;

import java.util.Map;
import java.util.Set;

public class ATraitDefinition extends AbstractTraitDefinition {

	/*
	public static final View htmlView = new View(A.class, PropertyView.Html,
		htmlHrefProperty, htmlTargetProperty, htmlPingProperty, htmlRelProperty, htmlMediaProperty, htmlHrefLangProperty, htmlTypeProperty
	);
	*/

	public ATraitDefinition() {
		super("A");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			AvoidWhitespace.class,
			new AvoidWhitespace() {

				@Override
				public boolean avoidWhitespace() {
					return true;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			A.class, (traits, node) -> new ATraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> htmlHrefProperty     = new StringProperty("_html_href");
		final Property<String> htmlTargetProperty   = new StringProperty("_html_target");
		final Property<String> htmlPingProperty     = new StringProperty("_html_ping");
		final Property<String> htmlRelProperty      = new StringProperty("_html_rel");
		final Property<String> htmlMediaProperty    = new StringProperty("_html_media");
		final Property<String> htmlHrefLangProperty = new StringProperty("_html_hreflang");
		final Property<String> htmlTypeProperty     = new StringProperty("_html_type");

		return Set.of(
			htmlHrefProperty,
			htmlTargetProperty,
			htmlPingProperty,
			htmlRelProperty,
			htmlMediaProperty,
			htmlHrefLangProperty,
			htmlTypeProperty
		);
	}
}