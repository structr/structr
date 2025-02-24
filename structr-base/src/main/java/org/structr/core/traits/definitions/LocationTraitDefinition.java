/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import java.util.Map;
import java.util.Set;

/**
 * The Location entity.
 */
public final class LocationTraitDefinition extends AbstractNodeTraitDefinition {

	public LocationTraitDefinition() {
		super("Location");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Double> latitudeProperty      = new DoubleProperty("latitude").indexed();
		final Property<Double> longitudeProperty     = new DoubleProperty("longitude").indexed();
		final Property<Double> altitudeProperty      = new DoubleProperty("altitude").indexed();
		final Property<String> countryProperty       = new StringProperty("country").indexed();
		final Property<String> postalCodeProperty    = new StringProperty("postalCode").indexed();
		final Property<String> cityProperty          = new StringProperty("city").indexed();
		final Property<String> streetProperty        = new StringProperty("street").indexed();
		final Property<String> houseNumberProperty   = new StringProperty("houseNumber").indexed();
		final Property<String> stateProperty         = new StringProperty("state").indexed();
		final Property<String> stateDistrictProperty = new StringProperty("stateDistrict").indexed();

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
			"latitude", "longitude", "altitude", "country", "postalCode", "city",
				"street", "houseNumber", "state", "stateDistrict"
			),
			PropertyView.Ui,
			newSet(
				"latitude", "longitude", "altitude", "country", "postalCode", "city",
				"street", "houseNumber", "state", "stateDistrict"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
