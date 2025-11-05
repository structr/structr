/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.coverage.processing.Operation;
import org.geotools.api.parameter.ParameterValueGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.ArgumentTypeException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import javax.media.jai.Histogram;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GetWCSHistogramFunction extends AbstractGeoserverFunction {

	private static final Logger logger       = LoggerFactory.getLogger(GetWCSHistogramFunction.class.getName());
	public static final String ERROR_MESSAGE = "usage: get_wcs_histogram(baseUrl, coverageId, boundingBox, [numBins, lowValue])";

	@Override
	public String getName() {
		return "get_wcs_histogram";
	}

	@Override
	public String getSignature() {
		return "baseUrl, coverageId, bBox [, bins, lowValue ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndTypes(sources, 3, String.class, String.class, Geometry.class, Number.class, Number.class, Number.class);

			final String baseUrl              = (String)sources[0];
			final String coverageId           = (String)sources[1];
			final Geometry geometry           = (Geometry)sources[2];
			final GridCoverage2D coverage     = getWCSCoverage(baseUrl, coverageId, geometry);
			final CoverageProcessor processor = CoverageProcessor.getInstance();
			final Operation operation         = processor.getOperation("Histogram");
			final ParameterValueGroup params  = operation.getParameters();
			final int numBins                 = sources.length > 3 ? parseInt(sources[3], 256) : 256;
			final double lowValue             = sources.length > 4 ? parseDouble(sources[4], 0.0) : 0.0;
			final double cutoffPercentage     = sources.length > 5 ? parseDouble(sources[5], 0.005) : 0.005;
			final double[] extrema            = getExtrema(coverage, 0);

			params.parameter("source0").setValue(coverage);
			params.parameter("numBins").setValue(new int[] { numBins });
			params.parameter("lowValue").setValue(new double[] { lowValue });
			params.parameter("highValue").setValue(new double[] { extrema[1] });

			final NumberFormat format      = new DecimalFormat("#,###,##0.00");
			final GridCoverage2D result    = (GridCoverage2D)processor.doOperation(params);
			final Histogram histogram      = (Histogram)result.getProperty("histogram");
			final int[] bins               = histogram.getBins(0);
			final Double binWidth          = histogram.getHighValue()[0] / (double)numBins;
			final Map<String, Object> map  = new LinkedHashMap<>();


			List<String> binNames    = new LinkedList<>();
			List<Integer> binData    = new LinkedList<>();

			int lastIndex    = 0;
			int restBin      = 0;
			double maxCount  = 0;

			for (int i=0; i<bins.length; i++) {

				final double d = i;

				binNames.add(format.format(binWidth * d));
				binData.add(bins[i]);

				if (bins[i] > maxCount) {
					maxCount = bins[i];
				}
			}

			// combine all bins whose value is below a given threshold (maxCount * cutoffPercentage)
			int threshold = Double.valueOf(maxCount * cutoffPercentage).intValue();
			for (int i=0; i<bins.length; i++) {

				// find index of last bin with more than x elements
				if (bins[i] > threshold) {
					lastIndex = i;
				}
			}

			// collect the sum of all the bins below the threshold
			for (int i=lastIndex; i<bins.length; i++) {
				restBin += bins[i];
			}

			binNames = binNames.subList(0, lastIndex);
			binData  = binData.subList(0, lastIndex);

			// add a single bin with all the rest
			binNames.add("> " + format.format(binWidth * lastIndex));
			binData.add(restBin);

			// remove all bins that have less than threshold elements
			map.put("names", binNames);
			map.put("bins", binData);

			return map;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (ArgumentTypeException te) {

			logParameterError(caller, sources, te.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return ERROR_MESSAGE;
	}

	@Override
	public String shortDescription() {
		return "Reads coverage data from a WCS endpoint and returns it";
	}
}