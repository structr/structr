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
package org.structr.test.rest.entity;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.AddressComponent;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;

/**
 *
 *
 */
public class TestNine extends AbstractNode {

	public static final Property<Iterable<TestEight>> testEights   = new EndNodes<>("testEights", NineEightManyToMany.class);
	public static final Property<Iterable<String>>    testEightIds = new CollectionNotionProperty("testEightIds", testEights, new PropertyNotion(GraphObject.id));

	public static final Property<String>          city         = new StringProperty("city").indexed().indexedWhenEmpty();
	public static final Property<String>          street       = new StringProperty("street").indexed().indexedWhenEmpty();
	public static final Property<String>          postalCode   = new StringProperty("postalCode").indexed().indexedWhenEmpty();

	public static final Property<Double>          latitude     = new DoubleProperty("latitude");
	public static final Property<Double>          longitude    = new DoubleProperty("longitude");

	public static final View defaultView = new View(TestNine.class, PropertyView.Public,
		name, city, street, postalCode, latitude, longitude
	);

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		geocode();
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		geocode();
	}

	public void geocode() throws FrameworkException {

		Double lat              = getProperty(latitude);
		Double lon              = getProperty(longitude);

		if (lat == null || lon == null) {

			String _city       = getProperty(city);
			String _street     = getProperty(street);
			String _postalCode = getProperty(postalCode);

			GeoCodingResult geoCodingResult = GeoHelper.geocode(_street, null, _postalCode, _city, null, null);
			if (geoCodingResult == null) {

				return;
			}

			setProperty(latitude, geoCodingResult.getLatitude());
			setProperty(longitude, geoCodingResult.getLongitude());

			// set postal code if found
			AddressComponent postalCodeComponent = geoCodingResult.getAddressComponent(GeoCodingResult.Type.postal_code);
			if (postalCodeComponent != null) {

				setProperty(postalCode, postalCodeComponent.getValue());
			}
		}
	}
}
