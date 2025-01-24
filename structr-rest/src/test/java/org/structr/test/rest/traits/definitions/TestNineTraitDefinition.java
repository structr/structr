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
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.AddressComponent;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;

import java.util.Map;
import java.util.Set;

public class TestNineTraitDefinition extends AbstractNodeTraitDefinition {

	public TestNineTraitDefinition() {
		super("TestNine");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					geocode(graphObject);
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
					geocode(graphObject);
				}
			}
		);
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

		return newSet(
			testEights,
			testEightIds,
			city,
			street,
			postalCode,
			latitude,
			longitude
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet("name", "city", "street", "postalCode", "latitude", "longitude")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	private void geocode(final GraphObject node) throws FrameworkException {

		final Traits traits                  = node.getTraits();
		final PropertyKey<Double> latitude   = traits.key("latitude");
		final PropertyKey<Double> longitude  = traits.key("longitude");
		final PropertyKey<String> city       = traits.key("city");
		final PropertyKey<String> street     = traits.key("street");
		final PropertyKey<String> postalCode = traits.key("postalCode");

		Double lat              = node.getProperty(latitude);
		Double lon              = node.getProperty(longitude);

		if (lat == null || lon == null) {

			String _city       = node.getProperty(city);
			String _street     = node.getProperty(street);
			String _postalCode = node.getProperty(postalCode);

			GeoCodingResult geoCodingResult = GeoHelper.geocode(_street, null, _postalCode, _city, null, null);
			if (geoCodingResult == null) {

				return;
			}

			node.setProperty(latitude, geoCodingResult.getLatitude());
			node.setProperty(longitude, geoCodingResult.getLongitude());

			// set postal code if found
			AddressComponent postalCodeComponent = geoCodingResult.getAddressComponent(GeoCodingResult.Type.postal_code);
			if (postalCodeComponent != null) {

				node.setProperty(postalCode, postalCodeComponent.getValue());
			}
		}
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
	*/
}
