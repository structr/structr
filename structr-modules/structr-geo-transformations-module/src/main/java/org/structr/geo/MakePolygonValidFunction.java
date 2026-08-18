/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MakePolygonValidFunction extends GeoFunction {

	private static final Logger logger = LoggerFactory.getLogger(MakePolygonValidFunction.class.getName());

	@Override
	public String getName() {

		return "makePolygonValid";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("polygon");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof Polygon polygon) {

				return JTS.makeValid(polygon, false);

			} else {

				logger.warn("{}(): Invalid parameter, expected polygon, got {}", getName(), sources[0].getClass().getSimpleName());
			}

			return null;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of();
	}

	@Override
	public String getShortDescription() {

		return "Makes a polygon valid.";
	}

	@Override
	public String getLongDescription() {

		return "";
	}
}
