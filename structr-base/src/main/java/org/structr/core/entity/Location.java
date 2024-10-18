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
package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 * The Location entity.
 */
public class Location extends AbstractNode {

	public static final Property<Double> latitudeProperty      = new DoubleProperty("latitude").indexed().partOfBuiltInSchema();
	public static final Property<Double> longitudeProperty     = new DoubleProperty("longitude").indexed().partOfBuiltInSchema();
	public static final Property<Double> altitudeProperty      = new DoubleProperty("altitude").indexed().partOfBuiltInSchema();
	public static final Property<String> countryProperty       = new StringProperty("country").indexed().partOfBuiltInSchema();
	public static final Property<String> postalCodeProperty    = new StringProperty("postalCode").indexed().partOfBuiltInSchema();
	public static final Property<String> cityProperty          = new StringProperty("city").indexed().partOfBuiltInSchema();
	public static final Property<String> streetProperty        = new StringProperty("street").indexed().partOfBuiltInSchema();
	public static final Property<String> houseNumberProperty   = new StringProperty("houseNumber").indexed().partOfBuiltInSchema();
	public static final Property<String> stateProperty         = new StringProperty("state").indexed().partOfBuiltInSchema();
	public static final Property<String> stateDistrictProperty = new StringProperty("stateDistrict").indexed().partOfBuiltInSchema();

	public static final View defaultView = new View(Location.class, PropertyView.Public,
		latitudeProperty, longitudeProperty, altitudeProperty, countryProperty, postalCodeProperty, cityProperty,
		streetProperty, houseNumberProperty, stateProperty, stateDistrictProperty
	);

	public static final View uiView = new View(Location.class, PropertyView.Ui,
		latitudeProperty, longitudeProperty, altitudeProperty, countryProperty, postalCodeProperty, cityProperty,
		streetProperty, houseNumberProperty, stateProperty, stateDistrictProperty
	);
}
