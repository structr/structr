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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Multiplicity;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.*;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;

import java.lang.reflect.Modifier;
import java.util.*;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 *
 */
public class SchemaResource extends RESTEndpoint {

	private static final StringProperty urlProperty                      = new StringProperty("url");
	private static final StringProperty typeProperty                     = new StringProperty("type");
	private static final StringProperty nameProperty                     = new StringProperty("name");
	private static final StringProperty classNameProperty                = new StringProperty("className");
	private static final StringProperty extendsClassNameProperty         = new StringProperty("extendsClass");
	private static final BooleanProperty isRelProperty                   = new BooleanProperty("isRel");
	private static final BooleanProperty isAbstractProperty              = new BooleanProperty("isAbstract");
	private static final BooleanProperty isInterfaceProperty             = new BooleanProperty("isInterface");
	private static final LongProperty flagsProperty                      = new LongProperty("flags");
	private static final GenericProperty relatedToProperty               = new GenericProperty("relatedTo");
	private static final GenericProperty relatedFromProperty             = new GenericProperty("relatedFrom");
	private static final GenericProperty possibleSourceTypesProperty     = new GenericProperty("possibleSourceTypes");
	private static final GenericProperty possibleTargetTypesProperty     = new GenericProperty("possibleTargetTypes");
	private static final BooleanProperty allSourceTypesPossibleProperty  = new BooleanProperty("allSourceTypesPossible");
	private static final BooleanProperty allTargetTypesPossibleProperty  = new BooleanProperty("allTargetTypesPossible");
	private static final BooleanProperty htmlSourceTypesPossibleProperty = new BooleanProperty("htmlSourceTypesPossible");
	private static final BooleanProperty htmlTargetTypesPossibleProperty = new BooleanProperty("htmlTargetTypesPossible");

	public enum UriPart {
		_schema
	}

	public SchemaResource() {
		super(RESTParameter.forStaticString(UriPart._schema.name()));
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {
		return new SchemaResourceHandler(securityContext, call);
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

				if (!isRel) {

					final List<GraphObjectMap> relatedTo   = new LinkedList<>();
					final List<GraphObjectMap> relatedFrom = new LinkedList<>();

					for (final PropertyKey key : config.getPropertySet(type, PropertyView.All)) {

						if (key instanceof RelationProperty) {

							final RelationProperty relationProperty = (RelationProperty)key;
							final Relation relation                 = relationProperty.getRelation();

							if (!relation.isHidden()) {

								switch (relation.getDirectionForType(type)) {

									case OUTGOING:
										relatedTo.add(relationPropertyToMap(config, relationProperty));
										break;

									case INCOMING:
										relatedFrom.add(relationPropertyToMap(config, relationProperty));
										break;

									case BOTH:
										relatedTo.add(relationPropertyToMap(config, relationProperty));
										relatedFrom.add(relationPropertyToMap(config, relationProperty));
										break;
								}
							}
						}
					}

					if (!relatedTo.isEmpty()) {
						schema.setProperty(relatedToProperty, relatedTo);
					}

					if (!relatedFrom.isEmpty()) {
						schema.setProperty(relatedFromProperty, relatedFrom);
					}
				}

			}

		}

		return new PagingIterable<>("/_schema", resultList);

	}

	// ----- private methods -----
	private static GraphObjectMap relationPropertyToMap(final ConfigurationProvider config, final RelationProperty relationProperty) {

		final GraphObjectMap map = new GraphObjectMap();
		final Relation relation  = relationProperty.getRelation();

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

		public SchemaResourceHandler(final SecurityContext securityContext, final RESTCall call) {
			super(securityContext, call);
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			return getSchemaOverviewResult();
		}

		@Override
		public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("POST not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("PUT not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doDelete() throws FrameworkException {
			throw new IllegalMethodException("DELETE not allowed on " + getURL());
		}

		@Override
		public Class getEntityClass() {
			return null;
		}

		@Override
		public boolean isCollection() {
			return true;
		}
	}
}
