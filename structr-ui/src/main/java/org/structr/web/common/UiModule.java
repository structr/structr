/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;
import org.structr.web.function.AddHeaderFunction;
import org.structr.web.function.AppendContentFunction;
import org.structr.web.function.CopyFileContentsFunction;
import org.structr.web.function.CreateArchiveFunction;
import org.structr.web.function.EscapeHtmlFunction;
import org.structr.web.function.FromJsonFunction;
import org.structr.web.function.FromXmlFunction;
import org.structr.web.function.GetContentFunction;
import org.structr.web.function.GetRequestHeaderFunction;
import org.structr.web.function.GetSessionAttributeFunction;
import org.structr.web.function.HttpDeleteFunction;
import org.structr.web.function.HttpGetFunction;
import org.structr.web.function.HttpHeadFunction;
import org.structr.web.function.HttpPostFunction;
import org.structr.web.function.HttpPutFunction;
import org.structr.web.function.IncludeFunction;
import org.structr.web.function.IsLocaleFunction;
import org.structr.web.function.LogEventFunction;
import org.structr.web.function.ParseFunction;
import org.structr.web.function.RemoveSessionAttributeFunction;
import org.structr.web.function.RenderFunction;
import org.structr.web.function.ScheduleFunction;
import org.structr.web.function.SendHtmlMailFunction;
import org.structr.web.function.SendPlaintextMailFunction;
import org.structr.web.function.SetContentFunction;
import org.structr.web.function.SetDetailsObjectFunction;
import org.structr.web.function.SetResponseHeaderFunction;
import org.structr.web.function.SetSessionAttributeFunction;
import org.structr.web.function.StripHtmlFunction;
import org.structr.web.function.ToGraphObjectFunction;
import org.structr.web.function.ToJsonFunction;
import org.structr.web.function.UnescapeHtmlFunction;

/**
 */
public class UiModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		final boolean basicEdition         = licenseManager == null || licenseManager.isEdition(LicenseManager.Basic);
		final boolean smallBusinessEdition = licenseManager == null || licenseManager.isEdition(LicenseManager.SmallBusiness);
		final boolean enterpriseEdition    = licenseManager == null || licenseManager.isEdition(LicenseManager.Enterprise);

		// Community Edition
		Functions.put(true, LicenseManager.Community, "escape_html",              new EscapeHtmlFunction());
		Functions.put(true, LicenseManager.Community, "unescape_html",            new UnescapeHtmlFunction());
		Functions.put(true, LicenseManager.Community, "strip_html",               new StripHtmlFunction());
		Functions.put(true, LicenseManager.Community, "from_json",                new FromJsonFunction());
		Functions.put(true, LicenseManager.Community, "to_json",                  new ToJsonFunction());
		Functions.put(true, LicenseManager.Community, "to_graph_object",          new ToGraphObjectFunction());
		Functions.put(true, LicenseManager.Community, "include",                  new IncludeFunction());
		Functions.put(true, LicenseManager.Community, "render",                   new RenderFunction());
		Functions.put(true, LicenseManager.Community, "set_details_object",       new SetDetailsObjectFunction());

		// Basic Edition and up
		Functions.put(basicEdition, LicenseManager.Basic, "send_html_mail",           new SendHtmlMailFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "send_plaintext_mail",      new SendPlaintextMailFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "get_content",              new GetContentFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "set_content",              new SetContentFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "append_content",           new AppendContentFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "copy_file_contents",       new CopyFileContentsFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "set_session_attribute",    new SetSessionAttributeFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "get_session_attribute",    new GetSessionAttributeFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "remove_session_attribute", new RemoveSessionAttributeFunction());
		Functions.put(basicEdition, LicenseManager.Basic, "is_locale",                new IsLocaleFunction());

		// Small Business and up
		Functions.put(smallBusinessEdition, LicenseManager.SmallBusiness, "log_event",                new LogEventFunction());

		// Enterprise only
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "GET",                      new HttpGetFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "HEAD",                     new HttpHeadFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "POST",                     new HttpPostFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "PUT",                      new HttpPutFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "DELETE",                   new HttpDeleteFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "add_header",               new AddHeaderFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "set_response_header",      new SetResponseHeaderFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "get_request_header",       new GetRequestHeaderFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "from_xml",                 new FromXmlFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "parse",                    new ParseFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "create_archive",           new CreateArchiveFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, "schedule",                 new ScheduleFunction());
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
