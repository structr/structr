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
package org.structr.core.property;

import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;

/**
 *
 * @author Christian Morgner
 */
public class EntityIdProperty extends EntityProperty<String> {
	
	public EntityIdProperty(EntityProperty base) {
		this(null, base);
	}	
	
	public EntityIdProperty(EntityProperty base, boolean createIfNotExisting) {
		this(null, base, createIfNotExisting);
	}	
	
	public EntityIdProperty(String name, EntityProperty base) {
		this(name, base, false);
	}
	
	public EntityIdProperty(String name, EntityProperty base, boolean createIfNotExisting) {
		super(name, base, new PropertyNotion(AbstractNode.uuid, createIfNotExisting));
	}
}
