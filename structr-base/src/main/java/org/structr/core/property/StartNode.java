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
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Target;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.search.GraphSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;
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
public class StartNode<S extends NodeInterface, T extends NodeInterface> extends Property<S> implements RelationProperty<S> {

	private static final Logger logger = LoggerFactory.getLogger(StartNode.class.getName());

	// relationship members
	private Relation<S, T, OneStartpoint<S>, ? extends Target> relation = null;
	private Class<S> destType                                           = null;
	private Notion notion                                               = null;

	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given cascade delete
	 * flag.
	 *
	 * @param name
	 * @param relationClass
	 */
	public StartNode(String name, Class<? extends Relation<S, T, OneStartpoint<S>, ? extends Target>> relationClass) {
		this(name, relationClass, new ObjectNotion());
	}

	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type, the given direction,
	 * the given cascade delete flag, the given notion and the given cascade
	 * delete flag.
	 *
	 * @param name
	 * @param relationClass
	 * @param notion
	 */
	public StartNode(String name, Class<? extends Relation<S, T, OneStartpoint<S>, ? extends Target>> relationClass, Notion notion) {

		super(name);

		this.relation = Relation.getInstance(relationClass);
		this.notion   = notion;
		this.destType = relation.getSourceType();

		// configure notion
		this.notion.setType(destType);
		this.notion.setRelationProperty(this);
		this.relation.setSourceProperty(this);

		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public String typeName() {
		return "object";
	}

	@Override
	public Class valueType() {
		return relatedType();
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<S, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<S, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, S> inputConverter(SecurityContext securityContext) {
		return notion.getEntityConverter(securityContext);
	}

	@Override
	public S getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public S getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		final OneStartpoint<? extends S> startpoint = relation.getSource();

		return startpoint.get(securityContext, (NodeInterface)obj, predicate);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, S value) throws FrameworkException {

		final OneStartpoint<S> startpoint = relation.getSource();

		try {

			if (updateCallback != null) {
				updateCallback.notifyUpdated(obj, value);
			}

			return startpoint.set(securityContext, (NodeInterface)obj, value);

		} catch (RuntimeException r) {

			final Throwable cause = r.getCause();
			if (cause instanceof FrameworkException) {

				throw (FrameworkException)cause;
			}
		}

		return null;
	}

	@Override
	public Class relatedType() {
		return destType;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Property<S> indexed() {
		return this;
	}

	@Override
	public Property<S> passivelyIndexed() {
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
	public void addSingleElement(final SecurityContext securityContext, final GraphObject obj, final S s) throws FrameworkException {
		setProperty(securityContext, obj, s);
	}

	@Override
	public Class<? extends S> getTargetType() {
		return destType;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, S searchValue, boolean exactMatch, final Query query) {
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
				case Relation.TARGET_TO_SOURCE:
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
		return "in";
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
