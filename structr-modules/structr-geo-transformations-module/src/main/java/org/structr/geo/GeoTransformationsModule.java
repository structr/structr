/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.geo;

import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.util.Set;

/**
 *
 */
public class GeoTransformationsModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new LatLonToUTMFunction());
		Functions.put(licenseManager, new UTMToLatLonFunction());
		Functions.put(licenseManager, new ImportGPXFunction());
		Functions.put(licenseManager, new ReadShapefileFunction());
		Functions.put(licenseManager, new WKTToPolygonsFunction());
		Functions.put(licenseManager, new WKTToGeometryFunction());
		Functions.put(licenseManager, new MakePolygonValidFunction());
		Functions.put(licenseManager, new GetWFSDataFunction());
		Functions.put(licenseManager, new GetWCSDataFunction());
		Functions.put(licenseManager, new GetWCSHistogramFunction());

		Functions.put(licenseManager, new CoordsToPointFunction());
		Functions.put(licenseManager, new CoordsToMultipointFunction());
		Functions.put(licenseManager, new CoordsToLineStringFunction());
		Functions.put(licenseManager, new CoordsToPolygonFunction());

		Functions.put(licenseManager, new GeoAzimuthFunction());
		Functions.put(licenseManager, new GeoDistanceFunction());
		Functions.put(licenseManager, new LineSegmentFunction());
		Functions.put(licenseManager, new LineStringsToPolygonsFunction());
		Functions.put(licenseManager, new ConvertGeometryFunction());
		Functions.put(licenseManager, new CoordsFunction());
	}

	@Override
	public String getName() {
		return "geo-transformations";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}
}