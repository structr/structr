/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.docs.Documentation;
import org.structr.docs.ontology.HasDisplayName;

/**
 */

@Documentation(name="Request headers", shortDescription="Structr's HTTP API supports a number of custom request headers to influence the behaviour of the endpoints.")
public enum RequestHeaders implements HasDisplayName {

	Accept("Accept"),
	AccessControlRequestHeaders("Access-Control-Request-Headers"),
	AccessControlRequestMethod("Access-Control-Request-Method"),
	Authorization("Authorization"),
	CacheControl("Cache-Control"),
	ContentType("Content-Type"),
	Expires("Expires"),
	IfModifiedSince("If-Modified-Since"),
	LastModified("Last-Modified"),
	Origin("Origin"),
	Pragma("Pragma"),
	RefreshToken("Refresh-Token"),
	Range("Range"),
	ReturnDetailsForCreatedObjects("Structr-Return-Details-For-Created-Objects"),
	StructrWebsocketBroadcast("Structr-Websocket-Broadcast"),
	StructrCascadingDelete("Structr-Cascading-Delete"),
	StructrForceMergeOfNestedProperties("Structr-Force-Merge-Of-Nested-Properties"),
	Vary("Vary"),
	XForwardedFor("X-Forwarded-For"),
	XUser("X-User"),
	XPassword("X-Password"),
	XStructrEdition("X-Structr-Edition"),
	XStructrClusterNode("X-Structr-Cluster-Node"),
	XStructrSessionToken("X-StructrSessionToken"),

	;

	private String headerName = null;

	RequestHeaders(final String headerName) {
		this.headerName = headerName;
	}

	public String getHeaderName() {
		return headerName;
	}

	@Override
	public String getDisplayName() {
		return headerName;
	}
}
