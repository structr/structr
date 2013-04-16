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
