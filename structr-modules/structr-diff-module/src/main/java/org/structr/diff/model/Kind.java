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
package org.structr.diff.model;

/**
 * The entity kinds a deployment export decomposes into.
 *
 * Deliberately a String constant holder rather than an enum: the set is open. A module that
 * adds a file to the export adds a kind, and a kind this version does not know about must be
 * reportable rather than fatal. Nothing here switches exhaustively on kind for that reason.
 */
public final class Kind {

	public static final String PAGE              = "Page";
	public static final String COMPONENT         = "Component";
	public static final String TEMPLATE          = "Template";
	public static final String DOM_ELEMENT       = "DomElement";
	public static final String CONTENT           = "Content";
	public static final String FILE              = "File";
	public static final String FILE_PAYLOAD      = "FilePayload";
	public static final String SCHEMA_TYPE       = "SchemaType";
	public static final String SCHEMA_PROPERTY   = "SchemaProperty";
	public static final String SCHEMA_METHOD     = "SchemaMethod";
	public static final String GLOBAL_METHOD     = "GlobalMethod";
	public static final String SCHEMA_SOURCE     = "SchemaSource";
	public static final String LOCALIZATION      = "Localization";
	public static final String WIDGET            = "Widget";
	public static final String APP_CONFIG        = "AppConfig";
	public static final String SCRATCHPAD        = "Scratchpad";
	public static final String ACTION_MAPPING    = "ActionMapping";
	public static final String PARAMETER_MAPPING = "ParameterMapping";
	public static final String RESOURCE_ACCESS   = "ResourceAccess";
	public static final String SCHEMA_GRANT      = "SchemaGrant";
	public static final String LLM_TOOL          = "LLMTool";
	public static final String LLM_AGENT         = "LLMAgent";
	public static final String BPMN_NODE         = "BpmnNode";
	public static final String CONFIG_FILE       = "ConfigFile";

	/** A data export: one record of an application type, and one link between two of them. */
	public static final String RECORD            = "Record";
	public static final String RECORD_LINK       = "RecordLink";

	private Kind() {}
}
