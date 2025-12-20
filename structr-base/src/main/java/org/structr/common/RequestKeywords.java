/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common;

import org.structr.api.config.Settings;
import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.Documentation;

import java.util.Set;
import java.util.TreeSet;

/**
 */

@Documentation(name="Request parameters", shortDescription="Structr's HTTP API supports a number of custom request parameters to influence the behaviour of the endpoints.")
public enum RequestKeywords implements Documentable {

	// pagination
	PageNumber("page", "Page number", "Request parameter used for pagination, sets the page number, value can be any positive integer > 0."),
	PageSize("pageSize", "Page size", "Request parameter used for pagination, sets the page size."),
	SortKey("sort", "Sort key", "Request parameter used for sorting, sets the sort key."),
	SortOrder("order", "Sort order", "Request parameter used for sorting, sets the sort order, value can be 'asc' or 'desc' for ascending or descending order."),

	// search
	Inexact_Deprecated("loose", null, null),
	Inexact("inexact", "Search type", "Request parameter that activates inexact search."),

	// distance search
	LatLon("latlon", "Latitude/Longitude", "Request parameter used for distance search, specifies the center point of the distance search in the form `latitude, longitude`."),
	Location("location", "Location (country, city, street)", "Request parameter used for distance search, specifies the center point of the distance search in the form `country, city, street`."),
	State("state", null, null),
	House("house", null, null),
	Country("country", "Country", "Request parameter used for distance search, specifies the center point of the search circle together with `postalCode`, `city`, `street`."),
	PostalCode("postalCode", "Postal code", "Request parameter used for distance search, specifies the center point of the search circle together with `country`, `city`, `street`."),
	City("city", "City", "Request parameter used for distance search, specifies the center point of the search circle together with `country`, `postalCode`, `street`."),
	Street("street", "Street", "Request parameter used for distance search, specifies the center point of the search circle together with `country`, `postalCode`, `city`."),
	Distance("distance", "Distance in kilometers", "Request parameter used for distance search, specifies the **radius** of the search circle."),

	// special settings
	OutputDepth("outputNestingDepth", null, null),
	DebugLogging("debugLoggingEnabled", null, null),
	ForceResultCount("forceResultCount", null, null),
	DisableSoftLimit("disableSoftLimit", null, null),
	ParallelizeJsonOutput("parallelizeJsonOutput", null, null),
	SerializeNulls("serializeNulls", null, null),
	BatchSize("batchSize", null, null),
	OutputReductionDepth("outputReductionDepth", null, null),

	// edit mode
	EditMode("edit", null, null),

	// locale
	Locale("locale", "Locale", "Request parameter that overrides the locale for the current request.")

	;

	private final String identifier;
	private final String displayName;
	private final String shortDescription;

	RequestKeywords(final String keyword, final String displayName, final String shortDescription) {

		this.shortDescription = shortDescription;
		this.displayName      = displayName;
		this.identifier       = keyword;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.RequestKeyword;
	}

	@Override
	public String getName() {

		if (Settings.RequestParameterLegacyMode.getValue(false)) {
			return identifier;
		}

		return "_" + identifier;
	}

	@Override
	public String getDisplayName() {
		// we always return the same format and do not change it based on the legacy parameters setting
		//return "_" + identifier;
		return displayName;
	}

	@Override
	public String getShortDescription() {
		return shortDescription;
	}

	public static Set<String> getIdentifiers() {

		final Set<String> keywords = new TreeSet<>();

		for (final RequestKeywords keyword : RequestKeywords.values()) {

			keywords.add(keyword.getName());
		}

		return keywords;
	}
}
