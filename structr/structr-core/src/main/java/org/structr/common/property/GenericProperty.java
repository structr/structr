/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common.property;

import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- JDK imports ------------------------------------------------------------

import java.lang.reflect.ParameterizedType;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class GenericProperty<T> extends Property<T> {

	private Boolean isCollection;
	private Class<? extends GraphObject> relatedType;

	//~--- constructors ---------------------------------------------------

	public GenericProperty(String name) {

		this(name, name);

	}

	public GenericProperty(String jsonName, String dbName) {

		super(jsonName, dbName);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public String typeName() {

		ParameterizedType pType = (ParameterizedType) getClass().getGenericSuperclass();

		if ("T".equals(pType.getRawType().toString())) {

			Class<? extends GraphObject> relType = relatedType();

			return relType != null
			       ? relType.getSimpleName()
			       : null;

		}

		return pType.getRawType().toString();

	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		return null;

	}

	@Override
	public Class<? extends GraphObject> relatedType() {

		if (relatedType != null) {

			return relatedType;
		}

		RelationClass relType = EntityContext.getRelationClass(getDeclaringClass(), this);

		if (relType != null) {

			relatedType  = relType.getDestType();
			isCollection = (Cardinality.ManyToMany.equals(relType.getCardinality()) || Cardinality.OneToMany.equals(relType.getCardinality()));

		}

		return relatedType;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isCollection() {

		if (isCollection != null) {

			return isCollection;
		}

		RelationClass relType = EntityContext.getRelationClass(getDeclaringClass(), this);

		if (relType != null) {

			relatedType  = relType.getDestType();
			isCollection = (Cardinality.ManyToMany.equals(relType.getCardinality()) || Cardinality.OneToMany.equals(relType.getCardinality()));
			return isCollection;

		}

		return super.isCollection();

	}

}
