/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.pdf;

import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.pdf.function.PDFEncryptFunction;
import org.structr.pdf.function.PDFFunction;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.util.Set;

public class PDFModule implements StructrModule {

	@Override
	public void onLoad(LicenseManager licenseManager) {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
		Functions.put(licenseManager, new PDFFunction());
		Functions.put(licenseManager, new PDFEncryptFunction());
	}

	@Override
	public String getName() {
		return "pdf";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(AbstractSchemaNode schemaNode, SourceFile buf) {

	}

	@Override
	public void insertSourceCode(AbstractSchemaNode schemaNode, SourceFile buf) {

	}

	@Override
	public void insertSaveAction(AbstractSchemaNode schemaNode, SourceFile buf, Actions.Type type) {

	}

	@Override
	public Set<String> getInterfacesForType(AbstractSchemaNode schemaNode) {
		return null;
	}
}
