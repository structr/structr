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
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.HtmlProperty;
import org.structr.web.traits.operations.AvoidWhitespace;
import org.structr.web.traits.operations.IsVoidElement;

import java.util.Map;
import java.util.Set;

public class Img extends GenericHtmlElementTraitDefinition {

	public Img() {
		super("Img");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> frameworkMethods = super.getFrameworkMethods();

		frameworkMethods.put(

			AvoidWhitespace.class,
			new AvoidWhitespace() {

				@Override
				public boolean avoidWhitespace() {
					return true;
				}
			}
		);

		frameworkMethods.put(

			IsVoidElement.class,
			new IsVoidElement() {

				@Override
				public boolean isVoidElement() {
					return true;
				}
			}
		);

		return frameworkMethods;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> altProperty = new HtmlProperty("alt");
		final PropertyKey<String> srcProperty = new HtmlProperty("src");
		final PropertyKey<String> crossoriginProperty = new HtmlProperty("crossorigin");
		final PropertyKey<String> usemapProperty = new HtmlProperty("usemap");
		final PropertyKey<String> ismapProperty = new HtmlProperty("ismap");
		final PropertyKey<String> widthProperty = new HtmlProperty("width");
		final PropertyKey<String> heightProperty = new HtmlProperty("height");

		return newSet(
			altProperty, srcProperty, crossoriginProperty, usemapProperty, ismapProperty, widthProperty, heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"_html_alt", "_html_src", "_html_crossorigin", "_html_usemap", "_html_ismap", "_html_width", "_html_height"
			)
		);
	}
}
