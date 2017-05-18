/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.common;

import java.util.Set;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;
import org.structr.util.LicenseManager;
import org.structr.web.function.AddHeaderFunction;
import org.structr.web.function.CopyFileContentsFunction;
import org.structr.web.function.EscapeHtmlFunction;
import org.structr.web.function.FromJsonFunction;
import org.structr.web.function.FromXmlFunction;
import org.structr.web.function.GetContentFunction;
import org.structr.web.function.GetRequestHeaderFunction;
import org.structr.web.function.GetSessionAttributeFunction;
import org.structr.web.function.HttpGetFunction;
import org.structr.web.function.HttpHeadFunction;
import org.structr.web.function.HttpPostFunction;
import org.structr.web.function.IncludeFunction;
import org.structr.web.function.IsLocaleFunction;
import org.structr.web.function.LogEventFunction;
import org.structr.web.function.ParseFunction;
import org.structr.web.function.RemoveSessionAttributeFunction;
import org.structr.web.function.RenderFunction;
import org.structr.web.function.SendHtmlMailFunction;
import org.structr.web.function.SendPlaintextMailFunction;
import org.structr.web.function.SetDetailsObjectFunction;
import org.structr.web.function.SetResponseHeaderFunction;
import org.structr.web.function.SetSessionAttributeFunction;
import org.structr.web.function.StripHtmlFunction;
import org.structr.web.function.ToGraphObjectFunction;
import org.structr.web.function.ToJsonFunction;

/**
 */
public class UiModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		// extend set of builtin functions
		Functions.functions.put("escape_html",              new EscapeHtmlFunction());
		Functions.functions.put("strip_html",               new StripHtmlFunction());
		Functions.functions.put("from_json",                new FromJsonFunction());
		Functions.functions.put("to_json",                  new ToJsonFunction());
		Functions.functions.put("to_graph_object",          new ToGraphObjectFunction());

		Functions.functions.put("include",                  new IncludeFunction());
		Functions.functions.put("render",                   new RenderFunction());

		Functions.functions.put("set_details_object",       new SetDetailsObjectFunction());

		// Basic Edition and up
		if (licenseManager == null || licenseManager.isEdition(LicenseManager.Basic)) {

			Functions.functions.put("send_html_mail",           new SendHtmlMailFunction());
			Functions.functions.put("send_plaintext_mail",      new SendPlaintextMailFunction());

			Functions.functions.put("get_content",              new GetContentFunction());
			Functions.functions.put("copy_file_contents",       new CopyFileContentsFunction());

			Functions.functions.put("set_session_attribute",    new SetSessionAttributeFunction());
			Functions.functions.put("get_session_attribute",    new GetSessionAttributeFunction());
			Functions.functions.put("remove_session_attribute", new RemoveSessionAttributeFunction());

			Functions.functions.put("is_locale",                new IsLocaleFunction());
		}

		// Small Business and up
		if (licenseManager == null || licenseManager.isEdition(LicenseManager.SmallBusiness)) {

			Functions.functions.put("log_event",                new LogEventFunction());
		}

		// Enterprise only
		if (licenseManager == null || licenseManager.isEdition(LicenseManager.Enterprise)) {

			Functions.functions.put("GET",                      new HttpGetFunction());
			Functions.functions.put("HEAD",                     new HttpHeadFunction());
			Functions.functions.put("POST",                     new HttpPostFunction());

			Functions.functions.put("add_header",               new AddHeaderFunction());
			Functions.functions.put("set_response_header",      new SetResponseHeaderFunction());
			Functions.functions.put("get_request_header",       new GetRequestHeaderFunction());

			Functions.functions.put("from_xml",                 new FromXmlFunction());
			Functions.functions.put("parse",                    new ParseFunction());
		}
	}

	@Override
	public String getName() {
		return "ui";
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
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}
}
