/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;

import java.util.Map;
import java.util.Set;

/**
 * The Location entity.
 */
public final class LocationTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String LATITUDE_PROPERTY       = "latitude";
	public static final String LONGITUDE_PROPERTY      = "longitude";
	public static final String ALTITUDE_PROPERTY       = "altitude";
	public static final String COUNTRY_PROPERTY        = "country";
	public static final String POSTAL_CODE_PROPERTY    = "postalCode";
	public static final String CITY_PROPERTY           = "city";
	public static final String STREET_PROPERTY         = "street";
	public static final String HOUSE_NUMBER_PROPERTY   = "houseNumber";
	public static final String STATE_PROPERTY          = "state";
	public static final String STATE_DISTRICT_PROPERTY = "stateDistrict";

	public LocationTraitDefinition() {
		super(StructrTraits.LOCATION);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Double> latitudeProperty      = new DoubleProperty(LATITUDE_PROPERTY).indexed();
		final Property<Double> longitudeProperty     = new DoubleProperty(LONGITUDE_PROPERTY).indexed();
		final Property<Double> altitudeProperty      = new DoubleProperty(ALTITUDE_PROPERTY).indexed();
		final Property<String> countryProperty       = new StringProperty(COUNTRY_PROPERTY).indexed();
		final Property<String> postalCodeProperty    = new StringProperty(POSTAL_CODE_PROPERTY).indexed();
		final Property<String> cityProperty          = new StringProperty(CITY_PROPERTY).indexed();
		final Property<String> streetProperty        = new StringProperty(STREET_PROPERTY).indexed();
		final Property<String> houseNumberProperty   = new StringProperty(HOUSE_NUMBER_PROPERTY).indexed();
		final Property<String> stateProperty         = new StringProperty(STATE_PROPERTY).indexed();
		final Property<String> stateDistrictProperty = new StringProperty(STATE_DISTRICT_PROPERTY).indexed();

		return newSet(
			latitudeProperty,
			longitudeProperty,
			altitudeProperty,
			countryProperty,
			postalCodeProperty,
			cityProperty,
			streetProperty,
			houseNumberProperty,
			stateProperty,
			stateDistrictProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					LATITUDE_PROPERTY, LONGITUDE_PROPERTY, ALTITUDE_PROPERTY, COUNTRY_PROPERTY, POSTAL_CODE_PROPERTY, CITY_PROPERTY,
					STREET_PROPERTY, HOUSE_NUMBER_PROPERTY, STATE_PROPERTY, STATE_DISTRICT_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					LATITUDE_PROPERTY, LONGITUDE_PROPERTY, ALTITUDE_PROPERTY, COUNTRY_PROPERTY, POSTAL_CODE_PROPERTY, CITY_PROPERTY,
					STREET_PROPERTY, HOUSE_NUMBER_PROPERTY, STATE_PROPERTY, STATE_DISTRICT_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
