/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.core.EntityContext;
import org.structr.core.converter.DoubleConverter;
import org.structr.core.node.NodeService.NodeIndex;

/**
 * The Location entity.
 * 
 * @author Axel Morgner
 */
public class Location extends AbstractNode {


	public enum Key implements PropertyKey {
		latitude, longitude, altitude;
	}

	static {


		// ----- initialize property sets -----
		EntityContext.registerPropertySet(Location.class, PropertyView.All,		Key.values());

		// ----- initialize property converters -----
		EntityContext.registerPropertyConverter(Location.class, Location.Key.latitude,	DoubleConverter.class);
		EntityContext.registerPropertyConverter(Location.class, Location.Key.longitude,	DoubleConverter.class);
		EntityContext.registerPropertyConverter(Location.class, Location.Key.altitude,	DoubleConverter.class);
		
		// ----- initialize validators -----

		// ----- initialize searchable properties
		EntityContext.registerSearchablePropertySet(Location.class, NodeIndex.fulltext.name(), Key.values());
		EntityContext.registerSearchablePropertySet(Location.class, NodeIndex.keyword.name(), Key.values());
		
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

//		error |= ValidationHelper.checkPropertyNotNull(this, Key.latitude, errorBuffer);
//		error |= ValidationHelper.checkPropertyNotNull(this, Key.longitude, errorBuffer);

		return !error;
	}
	
	@Override
	public String getIconSrc() {
		return null;
	}
}
