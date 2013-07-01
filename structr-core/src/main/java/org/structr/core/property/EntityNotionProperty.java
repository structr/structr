/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.BooleanClause.Occur;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SourceSearchAttribute;
import org.structr.core.notion.Notion;
import org.structr.core.notion.PropertyNotion;

/**
* A property that wraps a {@link PropertyNotion} with the given notion around an {@link EntityProperty}.
 *
 * @author Christian Morgner
 */


public class EntityNotionProperty<S extends GraphObject, T> extends Property<T> {
	
	private static final Logger logger = Logger.getLogger(EntityIdProperty.class.getName());
	
	private Property<S> base    = null;
	private Notion<S, T> notion = null;
	
	public EntityNotionProperty(String name, Property<S> base, Notion<S, T> notion) {
		
		super(name);
		
		this.notion = notion;
		this.base   = base;
		
		notion.setType(base.relatedType());
		
		// set indexed flag
		indexed();
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public String typeName() {
		return "";
	}

	@Override
	public Integer getSortType() {
		return null;
	}
	
	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		try {
			return notion.getAdapterForGetter(securityContext).adapt(base.getProperty(securityContext, obj, applyConverter));
			
		} catch (FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to apply notion of type {0} to property {1}", new Object[] { notion.getClass(), this } );
		}
		
		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		
		if (value != null) {
	
			base.setProperty(securityContext, obj, notion.getAdapterForSetter(securityContext).adapt(value));
			
		} else {
			
			base.setProperty(securityContext, obj, null);
		}
	}

	@Override
	public Class relatedType() {
		return base.relatedType();
	}

	@Override
	public boolean isCollection() {
		return false;
	}
	
	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occur occur, T searchValue, boolean exactMatch) {
		
		SourceSearchAttribute attr            = new SourceSearchAttribute(occur);
		Set<GraphObject> intersectionResult   = new LinkedHashSet<GraphObject>();
		EntityProperty entityProperty         = (EntityProperty)base;
		boolean alreadyAdded                  = false;
		
		try {

			Result<AbstractNode> result = Services.command(securityContext, SearchNodeCommand.class).execute(
				
				Search.andExactType(base.relatedType().getSimpleName()),
				Search.andExactProperty(securityContext, notion.getPrimaryPropertyKey(), searchValue)
			);
			
			for (AbstractNode node : result.getResults()) {

				switch (occur) {

					case MUST:

						if (!alreadyAdded) {

							// the first result is the basis of all subsequent intersections
							intersectionResult.addAll(entityProperty.getRelatedNodesReverse(securityContext, node, declaringClass));
							
							// the next additions are intersected with this one
							alreadyAdded = true;

						} else {

							intersectionResult.retainAll(entityProperty.getRelatedNodesReverse(securityContext, node, declaringClass));
						}

						break;

					case SHOULD:
						intersectionResult.addAll(entityProperty.getRelatedNodesReverse(securityContext, node, declaringClass));
						break;

					case MUST_NOT:
						break;
				}
			}

			attr.setResult(new LinkedList<GraphObject>(intersectionResult));
						
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}
		
		return attr;
	}
	
	@Override
	public void index(GraphObject entity, Object value) {
		// no direct indexing
	}
}
