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

import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.html.Audio;
import org.structr.web.traits.wrappers.html.AudioTraitWrapper;

import java.util.Map;
import java.util.Set;

public class AudioTraitDefinition extends AbstractNodeTraitDefinition {

	/*
	public static final View htmlView = new View(Audio.class, PropertyView.Html,
		htmlSrcProperty, htmlCrossOriginProperty, htmlPreloadProperty, htmlAutoplayProperty, htmlLoopProperty, htmlMutedProperty, htmlControlsProperty
	);
	*/

	public AudioTraitDefinition() {
		super("Audio");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Audio.class, (traits, node) -> new AudioTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> htmlSrcProperty         = new StringProperty("_html_src");
		final Property<String> htmlCrossOriginProperty = new StringProperty("_html_crossorigin");
		final Property<String> htmlPreloadProperty     = new StringProperty("_html_preload");
		final Property<String> htmlAutoplayProperty    = new StringProperty("_html_autoplay");
		final Property<String> htmlLoopProperty        = new StringProperty("_html_loop");
		final Property<String> htmlMutedProperty       = new StringProperty("_html_muted");
		final Property<String> htmlControlsProperty    = new StringProperty("_html_controls");

		return Set.of(
			htmlSrcProperty,
			htmlCrossOriginProperty,
			htmlPreloadProperty,
			htmlAutoplayProperty,
			htmlLoopProperty,
			htmlMutedProperty,
			htmlControlsProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
