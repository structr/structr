/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.geo;

import java.util.List;

/**
 *
 *
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

	AddressComponent getAddressComponent(Type type);

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
