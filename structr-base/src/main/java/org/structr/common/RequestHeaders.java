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

import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.Documentation;

@Documentation(name="Request headers", shortDescription="Structr's HTTP API supports a number of custom request headers to influence the behaviour of the endpoints.")
public enum RequestHeaders implements Documentable {

	Accept("Accept", "HTTP Accept header", null),
	AccessControlRequestHeaders("Access-Control-Request-Headers", "HTTP CORS headers header", null),
	AccessControlRequestMethod("Access-Control-Request-Method", "HTTP CORS methods header", null),
	Authorization("Authorization", "HTTP Authorization header", null),
	CacheControl("Cache-Control", "HTTP Cache-Control header", null),
	ContentType("Content-Type",  "HTTP Content-Type header", null),
	Expires("Expires",  "HTTP Expires header", null),
	IfModifiedSince("If-Modified-Since", "HTTP If-Modified-Since header", null),
	LastModified("Last-Modified", "HTTP Last-Modified header", null),
	Origin("Origin", "HTTP Origin header", null),
	Pragma("Pragma", "HTTP Pragma header", null),
	RefreshToken("Refresh-Token", "HTTP Refresh-Token header", null),
	Range("Range", "HTTP Range header", null),
	ReturnDetailsForCreatedObjects("Structr-Return-Details-For-Created-Objects", null, null),
	StructrWebsocketBroadcast("Structr-Websocket-Broadcast", null, null),
	StructrCascadingDelete("Structr-Cascading-Delete", null, null),
	StructrForceMergeOfNestedProperties("Structr-Force-Merge-Of-Nested-Properties", null, null),
	Vary("Vary", "HTTP Vary header", null),
	XForwardedFor("X-Forwarded-For", "HTTP X-Forwarded-For header", null),
	XUser("X-User", "Structr request authentication header: username", "Custom request header to authenticate single requests with username / password, used in header-based authentication."),
	XPassword("X-Password", "Structr request authentication header: password", "Custom request header to authenticate single requests with username / password, used in header-based authentication."),
	XStructrSessionToken("X-StructrSessionToken", "Structr request authentication header: sessionToken", "Custom request header to authenticate single requests with sessionToken, used in header-based authentication."),
	XStructrEdition("X-Structr-Edition", "Structr response header", "Custom response header sent by some Structr versions to indicate the Structr Edition that is running."),
	XStructrClusterNode("X-Structr-Cluster-Node", null, null),

	;

	private final String shortDescription;
	private final String identifier;
	private final String displayName;

	RequestHeaders(final String identifier, final String displayName, final String shortDescription) {

		this.shortDescription = shortDescription;
		this.identifier       = identifier;
		this.displayName      = displayName;
	}

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.RequestHeader;
	}

	@Override
	public String getName() {
		return identifier;
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return identifier;
	}

	@Override
	public String getShortDescription() {
		return shortDescription;
	}
}
