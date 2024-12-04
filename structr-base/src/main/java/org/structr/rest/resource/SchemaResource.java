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
package org.structr.rest.resource;


import org.apache.commons.lang3.StringUtils;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Multiplicity;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.*;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;

import java.lang.reflect.Modifier;
import java.util.*;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 *
 */
public class SchemaResource extends ExactMatchEndpoint {

	public static final StringProperty urlProperty                      = new StringProperty("url");
	public static final StringProperty typeProperty                     = new StringProperty("type");
	public static final StringProperty nameProperty                     = new StringProperty("name");
	public static final StringProperty classNameProperty                = new StringProperty("className");
	public static final StringProperty extendsClassNameProperty         = new StringProperty("extendsClass");
	public static final BooleanProperty isRelProperty                   = new BooleanProperty("isRel");
	public static final BooleanProperty isAbstractProperty              = new BooleanProperty("isAbstract");
	public static final BooleanProperty isInterfaceProperty             = new BooleanProperty("isInterface");
	public static final LongProperty flagsProperty                      = new LongProperty("flags");
	public static final GenericProperty viewsProperty                   = new GenericProperty("views");
	public static final GenericProperty relatedToProperty               = new GenericProperty("relatedTo");
	public static final GenericProperty relatedFromProperty             = new GenericProperty("relatedFrom");
	public static final GenericProperty possibleSourceTypesProperty     = new GenericProperty("possibleSourceTypes");
	public static final GenericProperty possibleTargetTypesProperty     = new GenericProperty("possibleTargetTypes");
	public static final BooleanProperty allSourceTypesPossibleProperty  = new BooleanProperty("allSourceTypesPossible");
	public static final BooleanProperty allTargetTypesPossibleProperty  = new BooleanProperty("allTargetTypesPossible");
	public static final BooleanProperty htmlSourceTypesPossibleProperty = new BooleanProperty("htmlSourceTypesPossible");
	public static final BooleanProperty htmlTargetTypesPossibleProperty = new BooleanProperty("htmlTargetTypesPossible");

	public enum UriPart {
		_schema
	}

	public SchemaResource() {
		super(RESTParameter.forStaticString(UriPart._schema.name(), true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {
		return new SchemaResourceHandler(call);
	}

	// ----- public static methods -----
	public static ResultStream getSchemaOverviewResult() throws FrameworkException {

		final List<GraphObjectMap> resultList = new LinkedList<>();
		final ConfigurationProvider config    = StructrApp.getConfiguration();

		// extract types from ModuleService
		final Set<String> nodeEntityKeys = config.getNodeEntities().keySet();
		final Set<String> relEntityKeys  = config.getRelationshipEntities().keySet();

		Set<String> entityKeys = new HashSet<>();
		entityKeys.addAll(nodeEntityKeys);
		entityKeys.addAll(relEntityKeys);

		for (String rawType : entityKeys) {

			// create & add schema information
			Class type            = SchemaHelper.getEntityClassForRawType(rawType);
			GraphObjectMap schema = new GraphObjectMap();
			resultList.add(schema);

			if (type != null) {

				String url = "/".concat(rawType);

				final boolean isRel = AbstractRelationship.class.isAssignableFrom(type);
				final int modifiers = type.getModifiers();

				schema.setProperty(urlProperty, url);
				schema.setProperty(typeProperty, type.getSimpleName());
				schema.setProperty(nameProperty, type.getSimpleName());
				schema.setProperty(classNameProperty, type.getName());
				schema.setProperty(extendsClassNameProperty, type.getSuperclass().getName());
				schema.setProperty(isRelProperty, isRel);
				schema.setProperty(isAbstractProperty, Modifier.isAbstract(modifiers));
				schema.setProperty(isInterfaceProperty, Modifier.isInterface(modifiers));
				schema.setProperty(flagsProperty, SecurityContext.getResourceFlags(rawType));

				// adding schema views to the global resource would blow it up immensely... rethink this
//				Set<String> propertyViews = new LinkedHashSet<>(config.getPropertyViewsForType(type));
//
//				// list property sets for all views
//				Map<String, List<String>> views = new TreeMap();
//				schema.setProperty(SchemaResource.viewsProperty, views);
//
//				for (final String view : propertyViews) {
//
//					views.put(view, SchemaHelper.getBasicPropertiesForView(SecurityContext.getSuperUserInstance(), type, view));
//				}
//
//				schema.setProperty(new GenericProperty("attributes"), SchemaHelper.getPropertiesForView(SecurityContext.getSuperUserInstance(), type, PropertyView.All));

				if (isRel) {

					schema.setProperty(new GenericProperty("relInfo"), relationToMap(config, Relation.getInstance(type)));

				} else {

//					final List<GraphObjectMap> relatedTo   = new LinkedList<>();
//					final List<GraphObjectMap> relatedFrom = new LinkedList<>();
//
//					for (final PropertyKey key : config.getPropertySet(type, PropertyView.All)) {
//
//						if (key instanceof RelationProperty) {
//
//							final RelationProperty relationProperty = (RelationProperty)key;
//							final Relation relation                 = relationProperty.getRelation();
//
//							if (!relation.isHidden()) {
//
//								switch (relation.getDirectionForType(type)) {
//
//									case OUTGOING:
//										relatedTo.add(relationPropertyToMap(config, relationProperty));
//										break;
//
//									case INCOMING:
//										relatedFrom.add(relationPropertyToMap(config, relationProperty));
//										break;
//
//									case BOTH:
//										relatedTo.add(relationPropertyToMap(config, relationProperty));
//										relatedFrom.add(relationPropertyToMap(config, relationProperty));
//										break;
//								}
//							}
//						}
//					}
//
//					schema.setProperty(relatedToProperty, relatedTo);
//					schema.setProperty(relatedFromProperty, relatedFrom);
				}
			}
		}

		return new PagingIterable<>("/_schema", resultList);
	}

	// ----- private methods -----
	public static GraphObjectMap relationPropertyToMap(final ConfigurationProvider config, final RelationProperty relationProperty) {

		return relationToMap(config, relationProperty.getRelation());
	}

	public static GraphObjectMap relationToMap(final ConfigurationProvider config, final Relation relation) {

		final GraphObjectMap map = new GraphObjectMap();

		/**
		 * what we need here:
		 * id,
		 * sourceMultiplicity,
		 * targetMultiplicity,
		 * relationshipType,
		 *
		 */

		map.put(SchemaRelationshipNode.sourceMultiplicity, multiplictyToString(relation.getSourceMultiplicity()));
		map.put(SchemaRelationshipNode.targetMultiplicity, multiplictyToString(relation.getTargetMultiplicity()));
		map.put(typeProperty, relation.getClass().getSimpleName());
		map.put(SchemaRelationshipNode.relationshipType, relation.name());

		final Class sourceType = relation.getSourceType();
		final Class targetType = relation.getTargetType();

		// select AbstractNode and SUPERCLASSES (not subclasses!)
		if (sourceType.isAssignableFrom(AbstractNode.class)) {

			map.put(allSourceTypesPossibleProperty, true);
			map.put(htmlSourceTypesPossibleProperty, true);
			map.put(possibleSourceTypesProperty, null);

		} else if ("DOMNode".equals(sourceType.getSimpleName())) {

			map.put(allTargetTypesPossibleProperty, false);
			map.put(htmlTargetTypesPossibleProperty, true);
			map.put(possibleTargetTypesProperty, null);

		} else {

			map.put(allSourceTypesPossibleProperty, false);
			map.put(htmlSourceTypesPossibleProperty, false);
			map.put(possibleSourceTypesProperty, StringUtils.join(SearchCommand.getAllSubtypesAsStringSet(sourceType.getSimpleName()), ","));
		}

		// select AbstractNode and SUPERCLASSES (not subclasses!)
		if (targetType.isAssignableFrom(AbstractNode.class)) {

			map.put(allTargetTypesPossibleProperty, true);
			map.put(htmlTargetTypesPossibleProperty, true);
			map.put(possibleTargetTypesProperty, null);

		} else if ("DOMNode".equals(targetType.getSimpleName())) {

			map.put(allTargetTypesPossibleProperty, false);
			map.put(htmlTargetTypesPossibleProperty, true);
			map.put(possibleTargetTypesProperty, null);

		} else {

			map.put(allTargetTypesPossibleProperty, false);
			map.put(htmlTargetTypesPossibleProperty, false);
			map.put(possibleTargetTypesProperty, StringUtils.join(SearchCommand.getAllSubtypesAsStringSet(targetType.getSimpleName()), ","));
		}

		return map;
	}

	private static String multiplictyToString(final Multiplicity multiplicity) {

		switch (multiplicity) {

			case One:
				return "1";

			case Many:
				return "*";
		}

		return null;
	}

	private class SchemaResourceHandler extends RESTCallHandler {

		public SchemaResourceHandler(final RESTCall call) {
			super(call);
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			return getSchemaOverviewResult();
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {
			return null;
		}

		@Override
		public boolean isCollection() {
			return true;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("GET", "OPTIONS");
		}
	}
}
