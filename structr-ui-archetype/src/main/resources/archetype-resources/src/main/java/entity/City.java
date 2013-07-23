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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.Property;
import org.structr.core.entity.AbstractNode;
import org.structr.core.validator.TypeUniquenessValidator;
import ${package}.RelType;

public class City extends AbstractNode {

	public static final Property<List<Person>> persons = new CollectionProperty<Person>("persons", Person.class, RelType.LIVES_IN, Direction.INCOMING, true);
	
	public static final View publicView = new View(Person.class, PropertyView.Public,
		name, persons
	);
	
	static {
		
		// register type uniqueness validator
		City.name.addValidator(new TypeUniquenessValidator(City.class));
		
	}
	
	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.onCreation(securityContext, errorBuffer)) {
			
			// name property must be set
			return !ValidationHelper.checkPropertyNotNull(this, name, errorBuffer);
		}
		
		return false;
	}
	
	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.onModification(securityContext, errorBuffer)) {
			
			return !ValidationHelper.checkPropertyNotNull(this, name, errorBuffer);
		}
		
		return false;
	}
}
