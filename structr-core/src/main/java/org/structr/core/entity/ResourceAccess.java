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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.LongProperty;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.relationship.Access;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.Endpoints;
import org.structr.core.property.IntProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
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

	private static final Map<String, ResourceAccess> grantCache = new ConcurrentHashMap<String, ResourceAccess>();
	private static final Logger logger                          = Logger.getLogger(ResourceAccess.class.getName());

	public static final Property<String>                    signature       = new StringProperty("signature", new TypeUniquenessValidator(ResourceAccess.class)).indexed();
	public static final Property<Long>                      flags           = new LongProperty("flags").indexed();
	public static final Property<Integer>                   position        = new IntProperty("position").indexed();
	
	public static final Endpoints<ResourceAccess, PropertyAccess>  propertyAccess  = new Endpoints<ResourceAccess, PropertyAccess>("propertyAccess", Access.class, new PropertySetNotion(uuid, name), true);

	public static final View uiView = new View(ResourceAccess.class, PropertyView.Ui,
		signature, flags, position
	);
	
	public static final View publicView = new View(ResourceAccess.class, PropertyView.Public,
		signature, flags
	);

	// non-static members
	private String cachedResourceSignature = null;
	private Long cachedFlags               = null;
	private Integer cachedPosition         = null;
	
	@Override
	public String toString() {
		
		StringBuilder buf = new StringBuilder();
		
		buf.append("('").append(getResourceSignature()).append("', ").append(flags.jsonName()).append(": ").append(getFlags()).append("', ").append(position.jsonName()).append(": ").append(getPosition()).append(")");
		
		return buf.toString();
	}
	
	public boolean hasFlag(long flag) {
		return (getFlags() & flag) == flag;
	}
	
	public void setFlag(final long flag) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// reset cached field
				cachedFlags = null;

				// set modified property
				setProperty(ResourceAccess.flags, getFlags() | flag);
				
				return null;
			}
		});
	}
	
	public void clearFlag(final long flag) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// reset cached field
				cachedFlags = null;

				// set modified property
				setProperty(ResourceAccess.flags, getFlags() & ~flag);
				
				return null;
			}
		});
	}
	
	public long getFlags() {
		
		if (cachedFlags == null) {
			cachedFlags = getProperty(ResourceAccess.flags);
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
			cachedPosition = getProperty(ResourceAccess.position);
		}
		
		if (cachedPosition != null) {
			return cachedPosition.intValue();
		}
		
		return 0;
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) {
		grantCache.clear();
		return true;
	}
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this,  ResourceAccess.signature, errorBuffer);
		error |= ValidationHelper.checkPropertyNotNull(this, ResourceAccess.flags, errorBuffer);
//		error |= checkPropertyNotNull(Key.city, errorBuffer);

		return !error;
	}
	
	@Override
	public void afterCreation(SecurityContext securityContext) {
		grantCache.clear();
	}
	
	@Override
	public void afterModification(SecurityContext securityContext) {
		grantCache.clear();
	}
	
	public static ResourceAccess findGrant(String signature) throws FrameworkException {

		ResourceAccess grant = grantCache.get(signature);
		if (grant == null) {

			SecurityContext securityContext        = SecurityContext.getSuperUserInstance();
			SearchNodeCommand search               = Services.command(securityContext, SearchNodeCommand.class);
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andExactType(ResourceAccess.class));
			searchAttributes.add(Search.andExactProperty(securityContext, ResourceAccess.signature, signature));

			Result result = search.execute(searchAttributes);

			if (result.isEmpty()) {

				logger.log(Level.WARNING, "No resource access object found for {0}", signature);

			} else {

				final AbstractNode node = (AbstractNode) result.get(0);

				if (node instanceof ResourceAccess) {

					grant = (ResourceAccess) node;
					
					grantCache.put(signature, grant);

				} else {

					logger.log(Level.SEVERE, "Grant for URI {0} has wrong type!", new Object[] { signature, node.getClass().getName() });

				}

				if (result.size() > 1) {

					logger.log(Level.SEVERE, "Found {0} grants for URI {1}!", new Object[] { result.size(), signature });

				}
			}
		}
		
		return grant;
	}
}
