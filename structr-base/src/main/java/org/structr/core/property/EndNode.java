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
package org.structr.core.property;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.OneEndpoint;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Source;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.search.GraphSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;
import org.structr.core.traits.GraphTrait;
import org.structr.core.traits.NodeTrait;
import org.structr.core.traits.Trait;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.openapi.common.OpenAPIAnyOf;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIStructrTypeSchemaOutput;

import java.util.Collections;
import java.util.Map;

/**
 * A property that defines a relationship with the given parameters between two nodes.
 *
 *
 */
public class EndNode<S extends NodeTrait, T extends NodeTrait> extends Property<T> implements RelationProperty<T> {

	private static final Logger logger = LoggerFactory.getLogger(EndNode.class.getName());

	private Relation<S, T, ? extends Source, OneEndpoint<T>> relation = null;
	private Notion notion                                             = null;
	private Trait<T> destType                                         = null;

	/**
	 * Constructs an entity property with the given name.
	 *
	 * @param name
	 * @param fqcn
	 */
	public EndNode(final String name, final String fqcn) {
		this(name, getClass(fqcn), new ObjectNotion());
	}

	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given cascade delete
	 * flag.
	 *
	 * @param name
	 * @param relationClass
	 */
	public EndNode(String name, Class<? extends Relation<S, T, ? extends Source, OneEndpoint<T>>> relationClass) {
		this(name, relationClass, new ObjectNotion());
	}

	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given notion.
	 *
	 * @param name
	 * @param relationClass
	 * @param notion
	 */
	public EndNode(String name, Class<? extends Relation<S, T, ? extends Source, OneEndpoint<T>>> relationClass, Notion notion) {

		super(name);

		this.relation  = Relation.getInstance(relationClass);
		this.notion    = notion;
		this.destType  = relation.getTargetType();

		this.notion.setType(destType);
		this.notion.setRelationProperty(this);
		this.relation.setTargetProperty(this);

		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public String typeName() {
		return "object";
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphTrait entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return notion.getEntityConverter(securityContext);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphTrait obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphTrait obj, boolean applyConverter, final Predicate<GraphTrait> predicate) {

		OneEndpoint<T> endpoint  = relation.getTarget();

		return endpoint.get(securityContext, (NodeTrait)obj, predicate);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphTrait obj, T value) throws FrameworkException {

		final OneEndpoint<T> endpoint = relation.getTarget();

		try {

			if (updateCallback != null) {
				updateCallback.notifyUpdated(obj, value);
			}

			return endpoint.set(securityContext, (NodeTrait)obj, value);

		} catch (RuntimeException r) {

			final Throwable cause = r.getCause();
			if (cause instanceof FrameworkException) {

				throw (FrameworkException)cause;
			}
		}

		return null;
	}

	@Override
	public Trait relatedType() {
		return destType;
	}

	@Override
	public Class valueType() {
		return relatedType().getClass();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Property<T> indexed() {
		return this;
	}

	@Override
	public Property<T> passivelyIndexed() {
		return this;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public boolean isIndexed() {
		return false;
	}

	@Override
	public boolean isPassivelyIndexed() {
		return false;
	}

	// ----- interface RelationProperty -----
	@Override
	public Notion getNotion() {
		return notion;
	}

	@Override
	public void addSingleElement(final SecurityContext securityContext, final GraphTrait obj, final T t) throws FrameworkException {
		setProperty(securityContext, obj, t);
	}

	@Override
	public Trait<T> getTargetType() {
		return destType;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, T searchValue, boolean exactMatch, final Query query) {
		return new GraphSearchAttribute<>(this, searchValue, occur, exactMatch);
	}

	@Override
	public Relation getRelation() {
		return relation;
	}

	@Override
	public boolean doAutocreate() {

		if (relation != null) {

			switch (relation.getAutocreationFlag()) {

				case Relation.ALWAYS:
				case Relation.SOURCE_TO_TARGET:
					return true;
			}
		}

		return false;
	}

	@Override
	public String getAutocreateFlagName() {

		if (relation != null) {
			return Relation.CASCADING_DESCRIPTIONS[relation.getAutocreationFlag()];
		}

		return Relation.CASCADING_DESCRIPTIONS[0];
	}

	@Override
	public String getDirectionKey() {
		return "out";
	}

	// ----- private methods -----
	private static Class getClass(final String fqcn) {

		try {

			return Class.forName(fqcn);

		} catch (Throwable t) {

			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final String destTypeName = destType.getName();

		if ("org.structr.core.graph.NodeInterface".equals(destTypeName) || "org.structr.flow.impl.FlowContainer".equals(destTypeName) ) {

			final ConfigurationProvider configuration = StructrApp.getConfiguration();

			destType = configuration.getNodeEntityClass(AbstractNode.class.getSimpleName());
			if (destType == null) {

				final Map<String, Class> interfaces = configuration.getInterfaces();
				destType = interfaces.get(AbstractNode.class.getSimpleName());
			}
		}

		return new OpenAPIStructrTypeSchemaOutput(destType, viewName, level + 1);
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		if (level > 4) {
			return Collections.EMPTY_MAP;
		}

		return new OpenAPIAnyOf(
			Map.of("type", "string", "example", NodeServiceCommand.getNextUuid(), "description", "The UUID of an existing object"),
			new OpenAPIObjectSchema("An existing object, referenced by its UUID",
				Map.of("id", Map.of("type", "string", "example", NodeServiceCommand.getNextUuid()))
			)
		);
	}
}
