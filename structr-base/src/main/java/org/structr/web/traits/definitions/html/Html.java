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

import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.traits.operations.OpeningTag;

import java.util.Map;
import java.util.Set;

public class Html extends GenericHtmlElementTraitDefinition {

	public static final String MANIFEST_PROPERTY           = getPrefixedHTMLAttributeName("manifest");
	public static final String CUSTOM_OPENING_TAG_PROPERTY = "customOpeningTag";

	public Html() {
		super(StructrTraits.HTML);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> frameworkMethods = super.getFrameworkMethods();

		frameworkMethods.put(

			OpeningTag.class,
			new OpeningTag() {

				@Override
				public void openingTag(final DOMElement node, final AsyncBuffer out, final String tag, final RenderContext.EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

					final String custTag = node.getProperty(node.getTraits().key(CUSTOM_OPENING_TAG_PROPERTY));
					if (custTag != null) {

						// fixme: not sure if this works at all...
						out.append(custTag);

					} else {

						getSuper().openingTag(node, out, tag, editMode, renderContext, depth);
					}
				}
			}
		);

		return frameworkMethods;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> manifestProperty = new StringProperty(MANIFEST_PROPERTY);
		final PropertyKey<String> customOpeningTag = new StringProperty(CUSTOM_OPENING_TAG_PROPERTY);

		return newSet(
			manifestProperty, customOpeningTag
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
				MANIFEST_PROPERTY, CUSTOM_OPENING_TAG_PROPERTY
			),
			PropertyView.Html,
			newSet(
				MANIFEST_PROPERTY
			)
		);
	}
}
