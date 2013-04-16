/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph.search;

import org.structr.core.property.StringProperty;
import org.structr.core.graph.NodeAttribute;

/**
 * Represents a distance search.
 *
 * Here, the key is a search string to be passed to geocoding, and value is a
 * distance in km.
 *
 * <p> Used in {
 *
 * @see SearchNodeCommand}.
 *
 * @author Axel Morgner
 */
public class DistanceSearchAttribute extends SearchAttribute {

	private Double distance   = null;
	private String street     = null;
	private String house      = null;
	private String postalCode = null;
	private String city       = null;
	private String state      = null;
	private String country    = null;
		
	public DistanceSearchAttribute(final String street, final String house, final String postalCode, final String city, final String state, final String country, final Double distance, final SearchOperator searchOp) {

		this.street     = street;
		this.house      = house;
		this.postalCode = postalCode;
		this.city       = city;
		this.state      = state;
		this.country    = country;

		this.distance   = distance;
		
		setSearchOperator(searchOp);
	}
//	
//	public DistanceSearchAttribute(final String searchString, final Double distance, final SearchOperator searchOp) {
//		
//		nodeAttribute = new NodeAttribute(new StringProperty(searchString), distance);
//		setSearchOperator(searchOp);
//	}

	@Override
	public Double getValue() {
		return distance;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getHouse() {
		return house;
	}

	public void setHouse(String house) {
		this.house = house;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}
	
	
}
