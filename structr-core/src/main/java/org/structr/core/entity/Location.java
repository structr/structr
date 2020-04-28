/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.net.URI;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;

/**
 * The Location entity.
 */
public interface Location extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Location");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Location"));
		type.setCategory("core");

		type.addNumberProperty("latitude",      PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addNumberProperty("longitude",     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addNumberProperty("altitude",      PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("country",       PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("postalCode",    PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("city",          PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("street",        PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("houseNumber",   PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("state",         PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("stateDistrict", PropertyView.Public, PropertyView.Ui).setIndexed(true);
	}}

	/*
	public static final Property<Double> latitude     = new DoubleProperty("latitude").cmis().passivelyIndexed();	// these need to be indexed at the end
	public static final Property<Double> longitude    = new DoubleProperty("longitude").cmis().passivelyIndexed();	// of the transaction so the spatial
	public static final Property<Double> altitude     = new DoubleProperty("altitude").cmis().passivelyIndexed();	// indexer sees all properties at once
	public static final Property<String> country      = new StringProperty("country");
	public static final Property<String> postalCode   = new StringProperty("postalCode");
	public static final Property<String> city         = new StringProperty("city");
	public static final Property<String> street       = new StringProperty("street");
	public static final Property<String> houseNumber  = new StringProperty("houseNumber");
	public static final Property<String> state        = new StringProperty("state");
	public static final Property<String> stateDistrict = new StringProperty("stateDistrict");

	public static final View publicView = new View(Location.class, PropertyView.Public,
		latitude, longitude, altitude, country, postalCode, city, street, houseNumber, state, stateDistrict
	);

	public static final View uiView = new View(Location.class, PropertyView.Ui,
		latitude, longitude, altitude, country, postalCode, city, street, houseNumber, state, stateDistrict
	);

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		notifyLocatables(errorBuffer);
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		notifyLocatables(errorBuffer);
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
		notifyLocatables(null);
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
		notifyLocatables(null);

	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		notifyLocatables(errorBuffer);

		return valid;

	}

	static void notifyLocatables(final Location thisLocation, final ErrorBuffer errorBuffer) {

		for(RelationshipInterface rel : this.getRelationships(NodeHasLocation.class)) {

			NodeInterface otherNode = rel.getOtherNode(this);
			if(otherNode != null && otherNode instanceof Locatable) {

				// notify other node of location change
				((Locatable)otherNode).locationChanged(errorBuffer);
			}
		}
	}
	*/
}
