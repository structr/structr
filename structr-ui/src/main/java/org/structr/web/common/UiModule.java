/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import org.structr.core.datasources.DataSources;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;
import org.structr.web.datasource.CypherGraphDataSource;
import org.structr.web.datasource.FunctionDataSource;
import org.structr.web.datasource.IdRequestParameterGraphDataSource;
import org.structr.web.datasource.RestDataSource;
import org.structr.web.datasource.XPathGraphDataSource;
import org.structr.web.function.AddHeaderFunction;
import org.structr.web.function.AppendContentFunction;
import org.structr.web.function.BarcodeFunction;
import org.structr.web.function.ConfirmationKeyFunction;
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
import org.structr.web.function.IncludeChildFunction;
import org.structr.web.function.IncludeFunction;
import org.structr.web.function.IsLocaleFunction;
import org.structr.web.function.LogEventFunction;
import org.structr.web.function.MaintenanceFunction;
import org.structr.web.function.ParseFunction;
import org.structr.web.function.RemoveSessionAttributeFunction;
import org.structr.web.function.RenderFunction;
import org.structr.web.function.ScheduleFunction;
import org.structr.web.function.SendHtmlMailFunction;
import org.structr.web.function.SendPlaintextMailFunction;
import org.structr.web.function.SetContentFunction;
import org.structr.web.function.SetDetailsObjectFunction;
import org.structr.web.function.SetResponseCodeFunction;
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
		Functions.put(true, LicenseManager.Community, new EscapeHtmlFunction());
		Functions.put(true, LicenseManager.Community, new UnescapeHtmlFunction());
		Functions.put(true, LicenseManager.Community, new StripHtmlFunction());
		Functions.put(true, LicenseManager.Community, new FromJsonFunction());
		Functions.put(true, LicenseManager.Community, new ToJsonFunction());
		Functions.put(true, LicenseManager.Community, new ToGraphObjectFunction());
		Functions.put(true, LicenseManager.Community, new IncludeFunction());
		Functions.put(true, LicenseManager.Community, new IncludeChildFunction());
		Functions.put(true, LicenseManager.Community, new RenderFunction());
		Functions.put(true, LicenseManager.Community, new SetDetailsObjectFunction());
		Functions.put(true, LicenseManager.Community, new ConfirmationKeyFunction());

		DataSources.put(true, LicenseManager.Community, "idRequestParameterDataSource", new IdRequestParameterGraphDataSource("nodeId"));
		DataSources.put(true, LicenseManager.Community, "restDataSource",               new RestDataSource());
		DataSources.put(true, LicenseManager.Community, "cypherDataSource",             new CypherGraphDataSource());
		DataSources.put(true, LicenseManager.Community, "functionDataSource",           new FunctionDataSource());
		DataSources.put(true, LicenseManager.Community, "xpathDataSource",              new XPathGraphDataSource());


		// Basic Edition and up
		Functions.put(basicEdition, LicenseManager.Basic, new SendHtmlMailFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new SendPlaintextMailFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new GetContentFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new SetContentFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new AppendContentFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new CopyFileContentsFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new SetSessionAttributeFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new GetSessionAttributeFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new RemoveSessionAttributeFunction());
		Functions.put(basicEdition, LicenseManager.Basic, new IsLocaleFunction());

		// Small Business and up
		Functions.put(smallBusinessEdition, LicenseManager.SmallBusiness, new LogEventFunction());

		// Enterprise only
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new HttpGetFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new HttpHeadFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new HttpPostFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new HttpPutFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new HttpDeleteFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new AddHeaderFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new SetResponseHeaderFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new SetResponseCodeFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new GetRequestHeaderFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new FromXmlFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new ParseFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new CreateArchiveFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new ScheduleFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new MaintenanceFunction());
		Functions.put(enterpriseEdition, LicenseManager.Enterprise, new BarcodeFunction());

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
