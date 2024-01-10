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
package org.structr.flow;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.flow.deployment.FlowLegacyDeploymentHandler;
import org.structr.flow.deployment.FlowTreeDeploymentHandler;

import java.io.File;
import java.nio.file.Path;

public abstract class FlowDeploymentHandler {

	private static final Logger logger = LoggerFactory.getLogger(FlowDeploymentHandler.class.getName());

	public static void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {
		new FlowTreeDeploymentHandler().doExport(target, gson);
	}


	public static void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {

		final File flowDir = new File(source.resolve(FlowTreeDeploymentHandler.FLOW_DEPLOYMENT_TREE_BASE_FOLDER).toAbsolutePath().toString());

		if (flowDir.isDirectory()) {

			new FlowTreeDeploymentHandler().doImport(source, gson);
		} else {

			new FlowLegacyDeploymentHandler().doImport(source, gson);
		}
	}

}
