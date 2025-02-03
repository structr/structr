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

import java.util.Map;
import java.util.Set;

public class A extends GenericHtmlElementTraitDefinition {

	public A() {
		super("A");
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

		return frameworkMethods;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> hrefProperty = new HtmlProperty("href");
		final PropertyKey<String> targetProperty = new HtmlProperty("target");
		final PropertyKey<String> pingProperty = new HtmlProperty("ping");
		final PropertyKey<String> relProperty = new HtmlProperty("rel");
		final PropertyKey<String> mediaProperty = new HtmlProperty("media");
		final PropertyKey<String> hreflangProperty = new HtmlProperty("hreflang");
		final PropertyKey<String> typeProperty = new HtmlProperty("type");

		return newSet(
			hrefProperty, targetProperty, pingProperty, relProperty, mediaProperty, hreflangProperty, typeProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"_html_href", "_html_target", "_html_ping", "_html_rel", "_html_media", "_html_hreflang", "_html_type"
			)
		);
	}
}
