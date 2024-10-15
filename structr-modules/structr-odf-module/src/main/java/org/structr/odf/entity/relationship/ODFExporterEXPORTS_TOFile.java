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
package org.structr.odf.entity.relationship;

import org.structr.core.entity.OneToOne;
import org.structr.odf.entity.ODFExporter;
import org.structr.web.entity.File;

public class ODFExporterEXPORTS_TOFile extends OneToOne<ODFExporter, File> {

	@Override
	public Class<ODFExporter> getSourceType() {
		return ODFExporter.class;
	}

	@Override
	public Class<File> getTargetType() {
		return File.class;
	}

	@Override
	public String name() {
		return "EXPORTS_TO";
	}
}
