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
import org.structr.core.traits.StructrTraits;
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

	public static final String ITEMS_PROPERTY           = "items";
	public static final String URL_PROPERTY             = "url";
	public static final String FEED_TYPE_PROPERTY       = "feedType";
	public static final String DESCRIPTION_PROPERTY     = "description";
	public static final String UPDATE_INTERVAL_PROPERTY = "updateInterval";
	public static final String LAST_UPDATED_PROPERTY    = "lastUpdated";
	public static final String MAX_AGE_PROPERTY         = "maxAge";
	public static final String MAX_ITEMS_PROPERTY       = "maxItems";

	public DataFeedTraitDefinition() {
		super(StructrTraits.DATA_FEED);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> urlProperty = obj.getTraits().key(URL_PROPERTY);

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
			}

			// FIXME: why was that here twice?
//			new JavaMethod("updateFeed", false, false) {
//
//				@Override
//				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
//					entity.as(DataFeed.class).updateFeed(securityContext);
//					return null;
//				}
//			}
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

		final Property<Iterable<NodeInterface>> itemsProperty = new EndNodes(ITEMS_PROPERTY, StructrTraits.DATA_FEED_HAS_FEED_ITEMS_FEED_ITEM);
		final Property<String> urlProperty                    = new StringProperty(URL_PROPERTY).indexed().notNull();
		final Property<String> feedTypeProperty               = new StringProperty(FEED_TYPE_PROPERTY);
		final Property<String> descriptionProperty            = new StringProperty(DESCRIPTION_PROPERTY);
		final Property<Long> updateIntervalProperty           = new LongProperty(UPDATE_INTERVAL_PROPERTY);
		final Property<Date> lastUpdatedProperty              = new DateProperty(LAST_UPDATED_PROPERTY);
		final Property<Long> maxAgeProperty                   = new LongProperty(MAX_AGE_PROPERTY);
		final Property<Integer> maxItemsProperty              = new IntProperty(MAX_ITEMS_PROPERTY);

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
				URL_PROPERTY, FEED_TYPE_PROPERTY, DESCRIPTION_PROPERTY, ITEMS_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				DESCRIPTION_PROPERTY, FEED_TYPE_PROPERTY, MAX_AGE_PROPERTY, URL_PROPERTY, UPDATE_INTERVAL_PROPERTY,
				LAST_UPDATED_PROPERTY, MAX_ITEMS_PROPERTY, ITEMS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
