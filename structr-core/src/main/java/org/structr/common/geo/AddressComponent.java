package org.structr.common.geo;

import java.util.Set;
import org.structr.common.geo.GeoCodingResult.Type;

/**
 *
 * @author Christian Morgner
 */

public interface AddressComponent {

	String getLongValue();

	String getShortValue();

	Set<Type> getTypes();
	
}
