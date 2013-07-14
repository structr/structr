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
package org.structr.common.geo;

import java.util.List;

/**
 *
 * @author Christian Morgner
 */
public interface GeoCodingResult {

	public enum Type {

		street_number,
		route,
		sublocality,
		locality,
		/**
		 * Bundesland
		 */
		administrative_area_level_1,
		/**
		 * Regierungsbezirk
		 */
		administrative_area_level_2,
		/**
		 * Stadt
		 */
		administrative_area_level_3,
		postal_code,
		country,
		political
	}

	String getAddress();

	AddressComponent getAddressComponent(Type... types);

	List<AddressComponent> getAddressComponents();

	//~--- get methods --------------------------------------------
	/**
	 * @return the latitude
	 */
	double getLatitude();

	/**
	 * @return the longitude
	 */
	double getLongitude();

	void setAddress(String address);

	//~--- set methods --------------------------------------------
	/**
	 * @param latitude the latitude to set
	 */
	void setLatitude(double latitude);

	/**
	 * @param longitude the longitude to set
	 */
	void setLongitude(double longitude);

	Double[] toArray();
}
