/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.feed.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.feed.entity.DataFeed;
import org.structr.feed.traits.wrappers.DataFeedTraitWrapper;
import org.structr.schema.action.EvaluationHints;

import java.util.Date;
import java.util.Map;
import java.util.Set;


public class DataFeedTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DESCRIPTION_PROPERTY         = "description";

	public DataFeedTraitDefinition() {
		super("DataFeed");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> urlProperty = obj.getTraits().key("url");

					return ValidationHelper.isValidPropertyNotNull(obj, urlProperty, errorBuffer);
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					graphObject.as(DataFeed.class).updateFeed(securityContext);
				}
			}
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("cleanUp", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					entity.as(DataFeed.class).cleanUp(securityContext);
					return null;
				}
			},

			new JavaMethod("updateIfDue", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					entity.as(DataFeed.class).updateIfDue(securityContext);
					return null;
				}
			},

			new JavaMethod("updateFeed", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					entity.as(DataFeed.class).updateFeed(securityContext);
					return null;
				}
			},

			new JavaMethod("updateFeed", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					entity.as(DataFeed.class).updateFeed(securityContext);
					return null;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataFeed.class, (traits, node) -> new DataFeedTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> itemsProperty = new EndNodes("items", "DataFeedHAS_FEED_ITEMSFeedItem");
		final Property<String> urlProperty                    = new StringProperty("url").indexed().notNull();
		final Property<String> feedTypeProperty               = new StringProperty("feedType");
		final Property<String> descriptionProperty            = new StringProperty(DESCRIPTION_PROPERTY);
		final Property<Long> updateIntervalProperty           = new LongProperty("updateInterval");
		final Property<Date> lastUpdatedProperty              = new DateProperty("lastUpdated");
		final Property<Long> maxAgeProperty                   = new LongProperty("maxAge");
		final Property<Integer> maxItemsProperty              = new IntProperty("maxItems");

		return newSet(
			itemsProperty,
			urlProperty,
			feedTypeProperty,
			descriptionProperty,
			updateIntervalProperty,
			lastUpdatedProperty,
			maxAgeProperty,
			maxItemsProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"url", "feedType", DESCRIPTION_PROPERTY, "items"
			),
			PropertyView.Ui,
			newSet(
				DESCRIPTION_PROPERTY, "feedType", "maxAge", "url", "updateInterval",
				"lastUpdated", "maxItems", "maxItems", "items"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
