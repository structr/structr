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
public final class LocationTraitDefinition extends AbstractTraitDefinition {

	private static final Property<Double> latitudeProperty      = new DoubleProperty("latitude").indexed().partOfBuiltInSchema();
	private static final Property<Double> longitudeProperty     = new DoubleProperty("longitude").indexed().partOfBuiltInSchema();
	private static final Property<Double> altitudeProperty      = new DoubleProperty("altitude").indexed().partOfBuiltInSchema();
	private static final Property<String> countryProperty       = new StringProperty("country").indexed().partOfBuiltInSchema();
	private static final Property<String> postalCodeProperty    = new StringProperty("postalCode").indexed().partOfBuiltInSchema();
	private static final Property<String> cityProperty          = new StringProperty("city").indexed().partOfBuiltInSchema();
	private static final Property<String> streetProperty        = new StringProperty("street").indexed().partOfBuiltInSchema();
	private static final Property<String> houseNumberProperty   = new StringProperty("houseNumber").indexed().partOfBuiltInSchema();
	private static final Property<String> stateProperty         = new StringProperty("state").indexed().partOfBuiltInSchema();
	private static final Property<String> stateDistrictProperty = new StringProperty("stateDistrict").indexed().partOfBuiltInSchema();

	public LocationTraitDefinition() {
		super("Location");
	}

	/*
	public static final View defaultView = new View(Location.class, PropertyView.Public,
		latitudeProperty, longitudeProperty, altitudeProperty, countryProperty, postalCodeProperty, cityProperty,
		streetProperty, houseNumberProperty, stateProperty, stateDistrictProperty
	);

	public static final View uiView = new View(Location.class, PropertyView.Ui,
		latitudeProperty, longitudeProperty, altitudeProperty, countryProperty, postalCodeProperty, cityProperty,
		streetProperty, houseNumberProperty, stateProperty, stateDistrictProperty
	);
	*/

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of(
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
}
