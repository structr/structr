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
package org.structr.core.graph.search;

import org.structr.api.search.SpatialQuery;
import org.structr.core.GraphObject;

/**
 * Represents a distance search.
 *
 * Here, the key is a search string to be passed to geocoding, and value is a distance in km.
 *
 *
 */
public class DistanceSearchAttribute extends SearchAttribute implements SpatialQuery {

	private boolean needsGeocoding = true;
	private Double[] coords     = null;
	private Double distance     = null;
 	private String street       = null;
 	private String house        = null;
	private String postalCode   = null;
	private String city         = null;
	private String state        = null;
	private String country      = null;

	public DistanceSearchAttribute(final Double latitude, final Double longitude, final Double distance) {

		super(null, null);

		this.coords         = new Double[] { latitude, longitude };
		this.distance       = distance;
		this.needsGeocoding = false;
	}

	public DistanceSearchAttribute(final String street, final String house, final String postalCode, final String city, final String state, final String country, final Double distance) {

		super(null, null);

		this.street     = street;
		this.house      = house;
		this.postalCode = postalCode;
		this.city       = city;
		this.state      = state;
		this.country    = country;
		this.distance   = distance;
	}

	@Override
	public String toString() {
		return "DistanceSearchAttribute(" + street + ", " + house + ", " + postalCode + ", " + city + ", " + state + ", " + country + ", " + distance + ")";
	}

	@Override
	public Double getValue() {
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

	public boolean needsGeocoding() {
		return needsGeocoding;
	}

	@Override
	public boolean isExactMatch() {
		return true;	// ignored
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {
		return true;
	}

	public void setCoords(final Double[] coords) {
		this.coords = coords;
	}

	@Override
	public Class getQueryType() {
		return SpatialQuery.class;
	}

	@Override
	public Double[] getCoords() {
		return coords;
	}

	@Override
	public Double getDistance() {
		return distance;
	}
}
