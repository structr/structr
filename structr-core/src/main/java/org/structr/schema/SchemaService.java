/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.schema;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StructrConf;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.Command;
import org.structr.core.Service;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.graph.Tx;
import org.structr.schema.compiler.NodeExtender;

/**
 *
 * @author Christian Morgner
 */
public class SchemaService implements Service {

	private static final Logger logger           = Logger.getLogger(SchemaService.class.getName());
	private static final AtomicBoolean compiling = new AtomicBoolean(false);

	private static ClassLoader lastClassLoader   = null;

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public void initialize(final StructrConf config) {
		reloadSchema(new ErrorBuffer());
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer) {

		boolean success = true;

		// compiling must only be done once
		if (compiling.compareAndSet(false, true)) {

			// this is a very critical section :)
			synchronized (SchemaService.class) {

				final Set<String> dynamicViews  = new LinkedHashSet<>();
				final NodeExtender nodeExtender = new NodeExtender();

				try (final Tx tx = StructrApp.getInstance().tx()) {

					// collect node classes
					for (final SchemaNode schemaNode : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {
						nodeExtender.addClass(schemaNode.getClassName(), schemaNode.getSource(errorBuffer));
						dynamicViews.addAll(schemaNode.getViews());
					}

					// collect relationship classes
					for (final SchemaRelationship schemaRelationship : StructrApp.getInstance().relationshipQuery(SchemaRelationship.class).getAsList()) {
						nodeExtender.addClass(schemaRelationship.getClassName(), schemaRelationship.getSource(errorBuffer));
						dynamicViews.addAll(schemaRelationship.getViews());
					}

					// compile all classes at once
					for (final Class newType : nodeExtender.compile(errorBuffer).values()) {
						Services.getInstance().getConfigurationProvider().registerEntityType(newType);
					}

					success = !errorBuffer.hasError();

					// inject views in configuration provider
					if (success) {

						Services.getInstance().getConfigurationProvider().registerDynamicViews(dynamicViews);
						// TODO: add all views
					}

					lastClassLoader = nodeExtender.getClassLoader();

				} catch (Throwable t) {

					logger.log(Level.SEVERE, "Unable to compile dynamic schema.", t);

					success = false;
				}
			}

			// compiling done
			compiling.set(false);
		}

		return success;
	}

	public static ClassLoader getClassLoader() {
		return lastClassLoader;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String getName() {
		return SchemaService.class.getName();
	}

	@Override
	public boolean isRunning() {
		return true;
	}
}
