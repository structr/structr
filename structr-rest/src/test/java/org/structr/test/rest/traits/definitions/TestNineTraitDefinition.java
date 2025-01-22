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
package org.structr.test.rest.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

public class TestNineTraitDefinition extends AbstractNodeTraitDefinition {

	public TestNineTraitDefinition() {
		super("TestNine");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> testEights = new EndNodes("testEights", "NineEightManyToMany");
		final Property<Iterable<String>>    testEightIds   = new CollectionNotionProperty("testEightIds", "TestNine", "testEights", "TestEight", new PropertyNotion("id"));
		final Property<String> city                        = new StringProperty("city").indexed().indexedWhenEmpty();
		final Property<String> street                      = new StringProperty("street").indexed().indexedWhenEmpty();
		final Property<String> postalCode                  = new StringProperty("postalCode").indexed().indexedWhenEmpty();
		final Property<Double> latitude                    = new DoubleProperty("latitude");
		final Property<Double> longitude                   = new DoubleProperty("longitude");

		return Set.of(
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			Set.of("name", "city", "street", "postalCode", "latitude", "longitude")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*

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
	*/
}
