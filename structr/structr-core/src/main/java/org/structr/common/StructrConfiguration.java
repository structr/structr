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
package org.structr.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.StructrAuthenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;

/**
 *
 * @author Christian Morgner
 */
public class StructrConfiguration {
	
	private static final Logger logger = Logger.getLogger(StructrConfiguration.class.getName());
	
	private Class<? extends AbstractRelationship> genericRelationshipType = null;
	private Class<? extends AbstractNode> genericNodeType                 = null;
	private Class<? extends Authenticator> authenticatorType              = null;
	
	public StructrConfiguration(Class<? extends AbstractRelationship> genericRelationshipType, Class<? extends AbstractNode> genericNodeType, Class<? extends Authenticator> authenticatorType) {
		
		this.genericRelationshipType = genericRelationshipType;
		this.genericNodeType         = genericNodeType;
		this.authenticatorType       = authenticatorType;
	}

	public static StructrConfiguration createDefault() {
		return new StructrConfiguration(GenericRelationship.class, GenericNode.class, StructrAuthenticator.class);
	}
	
	public String genericRelationshipType() {
		return genericRelationshipType.getSimpleName();
	}
	
	public String genericNodeType() {
		return genericNodeType.getSimpleName();
	}
	
	public AbstractRelationship createGenericRelationship() {
		
		try {
			return genericRelationshipType.newInstance();
			
		} catch(Throwable t) {
			
			logger.log(Level.SEVERE, "Unable to instantiate generic relationship class {0}, using default.", genericRelationshipType.getName());
		}
		
		return new GenericRelationship();
		
	}
	
	public AbstractNode createGenericNode() {
		
		try {
			return genericNodeType.newInstance();
			
		} catch(Throwable t) {
			
			logger.log(Level.SEVERE, "Unable to instantiate generic node class {0}, using default.", genericNodeType.getName());
		}
		
		return new GenericNode();
		
	}
	
	public Authenticator createAuthenticator() {
		
		try {
			return authenticatorType.newInstance();
			
		} catch(Throwable t) {
			
			logger.log(Level.SEVERE, "Unable to instantiate authenticator class {0}, using default authenticator.", authenticatorType.getName());
		}
		
		return new StructrAuthenticator();
	}
}
