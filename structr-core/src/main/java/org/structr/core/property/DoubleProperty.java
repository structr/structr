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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import org.neo4j.index.lucene.ValueContext;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.search.DoubleSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;

/**
* A property that stores and retrieves a simple Double value.
 *
 * @author Christian Morgner
 */
public class DoubleProperty extends AbstractPrimitiveProperty<Double> {
	
	public DoubleProperty(String name) {
		super(name);
		
		if (name.equals("latitude") || name.equals("longitude")) {
			
			// add layer node index and make
			// this property be indexed at the
			// end of the transaction instead
			// of on setProperty
			nodeIndices.add(NodeIndex.layer);
			passivelyIndexed();
		}
	}
	
	@Override
	public String typeName() {
		return "Double";
	}

	@Override
	public Integer getSortType() {
		return SortField.DOUBLE;
	}
	
	@Override
	public PropertyConverter<Double, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Double, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Double> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}
	
	protected class InputConverter extends PropertyConverter<Object, Double> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext);
		}
		
		@Override
		public Object revert(Double source) throws FrameworkException {
			return source;
		}

		@Override
		public Double convert(Object source) {
			
			// FIXME: be more strict when dealing with "wrong" input types
			if (source != null) {
				
				if (source instanceof Number) {

					return ((Number)source).doubleValue();
					
				}
				
				if (source instanceof String) {
					
					return Double.parseDouble(source.toString());
				}
			}
			
			return null;
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		
		if (value != null) {
			
			if (value instanceof Double) {
				return value;
			}
			
			if (value instanceof Number) {
				return ((Number)value).doubleValue();
			}
			
			try {
				
				return Double.parseDouble(value.toString());
				
			} catch (Throwable t) {
				
				// no chance, give up..
			}
		}
		
		return null;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, Double searchValue, boolean exactMatch) {
		return new DoubleSearchAttribute(this, searchValue, occur, exactMatch);
	}
	
	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, value != null ? ValueContext.numeric((Number)value) : value);
	}
}

/*
			
			if ((dbNode.hasProperty(Location.latitude.dbName())) && (dbNode.hasProperty(Location.longitude.dbName()))) {
				
				// Before indexing, check properties for correct type
				Object lat = dbNode.getProperty(Location.latitude.dbName());
				Object lon = dbNode.getProperty(Location.longitude.dbName());
				
				if (lat instanceof Double && lon instanceof Double && !((Double) lat).isNaN() && !((Double) lon).isNaN()) {

					LayerNodeIndex layerIndex = (LayerNodeIndex) indices.get(NodeService.NodeIndex.layer.name());

					try {

						synchronized (layerIndex) {

							layerIndex.add(dbNode, "", "");
						}

						// If an exception is thrown here, the index was deleted
						// and has to be recreated.
					} catch (NotFoundException nfe) {

						logger.log(Level.SEVERE, "Could not add node to layer index because the db could not find the node", nfe);

					} catch (Throwable t) {

						logger.log(Level.SEVERE, "Could not add node to layer index", t);
					}
				
				}

			}

 */