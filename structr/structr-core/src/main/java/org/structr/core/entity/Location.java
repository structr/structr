
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.core.entity;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.EntityContext;
import org.structr.core.converter.DoubleConverter;
import org.structr.core.node.NodeService.NodeIndex;

//~--- classes ----------------------------------------------------------------

/**
 * The Location entity.
 *
 * @author Axel Morgner
 */
public class Location extends AbstractNode {

	static {

		// ----- initialize property sets -----
		EntityContext.registerPropertySet(Location.class, PropertyView.All, Key.values());

		// ----- initialize property converters -----
		EntityContext.registerPropertyConverter(Location.class, Location.Key.latitude, DoubleConverter.class);
		EntityContext.registerPropertyConverter(Location.class, Location.Key.longitude, DoubleConverter.class);
		EntityContext.registerPropertyConverter(Location.class, Location.Key.altitude, DoubleConverter.class);

		// ----- initialize validators -----
		// ----- initialize searchable properties
		EntityContext.registerSearchablePropertySet(Location.class, NodeIndex.fulltext.name(), Key.values());
		EntityContext.registerSearchablePropertySet(Location.class, NodeIndex.keyword.name(), Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ latitude, longitude, altitude; }

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		return isValid(errorBuffer);
	}
	
	private  boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

//              error |= ValidationHelper.checkPropertyNotNull(this, Key.latitude, errorBuffer);
//              error |= ValidationHelper.checkPropertyNotNull(this, Key.longitude, errorBuffer);
		return !error;

	}

}
