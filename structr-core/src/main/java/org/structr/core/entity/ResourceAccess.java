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
package org.structr.core.entity;

import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.LongProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.graph.NodeService;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.validator.TypeUniquenessValidator;

/**
 * Controls access to REST resources.
 * 
 * Objects of this class act as a doorkeeper for REST resources
 * that match the signature string in the 'signature' field.
 * <p>
 * A ResourceAccess object defines access granted
 * <ul>
 * <li>to everyone (public)
 * <li>to authenticated principals
 * <li>to invidual principals (when connected to a {link @Principal} node
 * </ul>
 * 
 * 
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class ResourceAccess extends AbstractNode {

	private static final Logger logger = Logger.getLogger(ResourceAccess.class.getName());

	private String cachedResourceSignature = null;
	private Long cachedFlags     = null;
	private Integer cachedPosition = null;

	public static final Property<String>                    signature       = new StringProperty("signature");
	public static final Property<Long>                      flags           = new LongProperty("flags");
	public static final Property<Integer>                   position        = new IntProperty("position");
	public static final CollectionProperty<PropertyAccess>  propertyAccess  = new CollectionProperty<PropertyAccess>("propertyAccess", PropertyAccess.class, RelType.PROPERTY_ACCESS, Direction.OUTGOING, new PropertySetNotion(uuid, name), true);

	public static final View uiView = new View(ResourceAccess.class, PropertyView.Ui,
		signature, flags, position
	);
	
	public static final View publicView = new View(ResourceAccess.class, PropertyView.Public,
		signature, flags
	);
	
	static {

		EntityContext.registerSearchablePropertySet(ResourceAccess.class, NodeService.NodeIndex.fulltext.name(), publicView.properties());
		EntityContext.registerSearchablePropertySet(ResourceAccess.class, NodeService.NodeIndex.keyword.name(),  publicView.properties());
		
		EntityContext.registerPropertyValidator(ResourceAccess.class, signature, new TypeUniquenessValidator(ResourceAccess.class));
		
		// signature and type must be scanEntity-only
//		EntityContext.registerWriteOnceProperty(ResourceAccess.class, AbstractNode.type);
//		EntityContext.registerWriteOnceProperty(ResourceAccess.class, signature);

	}

	//~--- constant enums -------------------------------------------------

	//~--- methods --------------------------------------------------------

	@Override
	public String toString() {
		
		StringBuilder buf = new StringBuilder();
		
		buf.append("('").append(getResourceSignature()).append("', ").append(flags.jsonName()).append(": ").append(getFlags()).append("', ").append(position.jsonName()).append(": ").append(getPosition()).append(")");
		
		return buf.toString();
	}
	
	public boolean hasFlag(long flag) {
		return (getFlags() & flag) == flag;
	}
	
	public void setFlag(long flag) throws FrameworkException {
		
		// reset cached field
		cachedFlags = null;

		// set modified property
		setProperty(ResourceAccess.flags, getFlags() | flag);
	}
	
	public void clearFlag(long flag) throws FrameworkException {
		
		// reset cached field
		cachedFlags = null;

		// set modified property
		setProperty(ResourceAccess.flags, getFlags() & ~flag);
	}
	
	public long getFlags() {
		
		if (cachedFlags == null) {
			cachedFlags = getLongProperty(ResourceAccess.flags);
		}
		
		if (cachedFlags != null) {
			return cachedFlags.longValue();
		}
		
		return 0;
	}
	
	public String getResourceSignature() {
		
		if (cachedResourceSignature == null) {
			cachedResourceSignature = getProperty(ResourceAccess.signature);
		}
		
		return cachedResourceSignature;
	}

	public int getPosition() {
		
		if (cachedPosition == null) {
			cachedPosition = getIntProperty(ResourceAccess.position);
		}
		
		if (cachedPosition != null) {
			return cachedPosition.intValue();
		}
		
		return 0;
	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this,  ResourceAccess.signature, errorBuffer);
		error |= ValidationHelper.checkPropertyNotNull(this, ResourceAccess.flags, errorBuffer);
//		error |= checkPropertyNotNull(Key.city, errorBuffer);

		return !error;
	}}
