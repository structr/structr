/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.flow.deployment.FlowLegacyDeploymentHandler;
import org.structr.flow.deployment.FlowTreeDeploymentHandler;
import org.structr.flow.impl.*;
import org.structr.flow.impl.rels.*;
import org.structr.module.api.DeployableEntity;
import org.structr.schema.SchemaHelper;
import org.structr.web.common.AbstractMapComparator;

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
