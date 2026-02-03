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

import java.util.*;

/**
 */

@Documentation(name="Request parameters", shortDescription="Structr's HTTP API supports a number of custom request parameters to influence the behaviour of the endpoints.")
public enum RequestParameters implements Documentable {

	// pagination
	PageNumber("page", "Page number", "Request parameter used for pagination, sets the page number, value can be any positive integer > 0."),
	PageSize("pageSize", "Page size", "Request parameter used for pagination, sets the page size."),
	SortKey("sort", "Sort key", "Request parameter used for sorting, sets the sort key."),
	SortOrder("order", "Sort order", "Request parameter used for sorting, sets the sort order, value can be 'asc' or 'desc' for ascending or descending order."),

	// search
	Inexact_Deprecated("loose", null, null),
	Inexact("inexact", "Search type", "Request parameter that activates inexact search."),

	// distance search
	LatLon("latlon", "Latitude/Longitude", "Request parameter used for distance search, specifies the center point of the distance search in the form `latitude,longitude`."),
	Location("location", "Location (country, city, street)", "Request parameter used for distance search, specifies the center point of the distance search in the form `country,city,street`."),
	State("state", null, null),
	House("house", null, null),
	Country("country", "Country", "Request parameter used for distance search, specifies the center point of the search circle together with `_postalCode`, `_city`, `_street`."),
	PostalCode("postalCode", "Postal code", "Request parameter used for distance search, specifies the center point of the search circle together with `_country`, `_city`, `_street`."),
	City("city", "City", "Request parameter used for distance search, specifies the center point of the search circle together with `_country`, `_postalCode`, `_street`."),
	Street("street", "Street", "Request parameter used for distance search, specifies the center point of the search circle together with `_country`, `_postalCode`, `_city`."),
	Distance("distance", "Distance in kilometers", "Request parameter used for distance search, specifies the **radius** of the search circle."),

	// special settings
	// link to JSON output
	OutputDepth("outputNestingDepth", "JSON Nesting Depth", "You can control how deeply nested objects are serialized in REST responses using the `_outputNestingDepth` request parameter. By default, Structr serializes nested objects to a depth of three levels, but increasing the nesting depth includes more levels of related objects in the response. This is useful when you need to access deeply nested relationships without making multiple requests."),
	ForceResultCount("forceResultCount", null, null),
	DisableSoftLimit("disableSoftLimit", null, null),
	ParallelizeJsonOutput("parallelizeJsonOutput", null, null),
	SerializeNulls("serializeNulls", null, null),
	BatchSize("batchSize", null, null),
	OutputReductionDepth("outputReductionDepth", null, null),

	// HtmlServlet
	// link to "File download"
	DownloadAsFilename("filename", "Download filename", "Request parameter that sets the 'filename' part of the `Content-Disposition: attachment` response header.", false),
	DownloadAsDataUrl("as-data-url", "Download as data URL", "Request parameter that controls the response format of a file download. If set (with any value), it causes the file data to be returned in Base64 format ready to be used in a data URL.", false),

	// edit mode
	EditMode("edit", null, null),

	// locale
	Locale("locale", "Locale", "Request parameter that overrides the locale for the current request.")

	;

	private final boolean noPrefix;
	private final String identifier;
	private final String displayName;
	private final String shortDescription;

	RequestParameters(final String identifier, final String displayName, final String shortDescription) {
		this(identifier, displayName, shortDescription, false);
	}

	RequestParameters(final String identifier, final String displayName, final String shortDescription, final boolean noPrefix) {

		this.noPrefix         = noPrefix;
		this.shortDescription = shortDescription;
		this.displayName      = displayName;
		this.identifier       = identifier;
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

		if (Settings.RequestParameterLegacyMode.getValue(false) || noPrefix) {
			return identifier;
		}

		return "_" + identifier;
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		// we always return the same format and do not change it based on the legacy parameters setting
		//return "_" + identifier;
		return displayName;
	}

	@Override
	public String getShortDescription() {
		return shortDescription;
	}

	@Override
	public Map<String, String> getTableHeaders() {

		final Map<String, String> headers = new LinkedHashMap<>();

		headers.put("Key", "`name`");
		headers.put("Name", "displayName");
		headers.put("Description", "shortDescription");

		return headers;
	}

	public static Set<String> getIdentifiers() {

		final Set<String> keywords = new TreeSet<>();

		for (final RequestParameters keyword : RequestParameters.values()) {

			keywords.add(keyword.getName());
		}

		return keywords;
	}
}
