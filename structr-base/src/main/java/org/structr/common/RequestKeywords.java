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

import java.util.Set;
import java.util.TreeSet;

/**
 */

public enum RequestKeywords {

	// pagination
	PageNumber("page"),
	PageSize("pageSize"),
	SortKey("sort"),
	SortOrder("order"),

	// search
	Inexact_Deprecated("loose"),
	Inexact("inexact"),

	// distance search
	LatLon("latlon"),
	Location("location"),
	State("state"),
	House("house"),
	Country("country"),
	PostalCode("postalCode"),
	City("city"),
	Street("street"),
	Distance("distance"),

	// special settings
	OutputDepth("outputNestingDepth"),
	DebugLogging("debugLoggingEnabled"),
	ForceResultCount("forceResultCount"),
	DisableSoftLimit("disableSoftLimit"),
	ParallelizeJsonOutput("parallelizeJsonOutput"),
	SerializeNulls("serializeNulls"),
	BatchSize("batchSize"),
	OutputReductionDepth("outputReductionDepth"),

	// edit mode
	EditMode("edit"),

	// locale
	Locale("locale")

	;

	private String keyword = null;

	RequestKeywords(final String keyword) {
		this.keyword = keyword;
	}

	@Override
	public String toString() {
		return keyword();
	}

	public String keyword() {

		if (Settings.RequestParameterLegacyMode.getValue(false)) {
			return keyword;
		}

		return "_" + keyword;
	}

	public static Set<String> keywords() {

		final Set<String> keywords = new TreeSet<>();

		for (final RequestKeywords keyword : RequestKeywords.values()) {

			keywords.add(keyword.keyword());
		}

		return keywords;
	}
}
