/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.schema;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StructrConf;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Service;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.Tx;
import org.structr.schema.compiler.NodeExtender;

/**
 *
 * @author Christian Morgner
 */
public class SchemaService implements Service {

	private static final Logger logger                            = Logger.getLogger(SchemaService.class.getName());
	private static final AtomicBoolean compiling                  = new AtomicBoolean(false);
	private static final Map<String, String> builtinTypeMap       = new LinkedHashMap<>();

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public void initialize(final StructrConf config) {
		reloadSchema(new ErrorBuffer());
	}

	public static void registerBuiltinTypeOverride(final String type, final String fqcn) {
		builtinTypeMap.put(type, fqcn);
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		boolean success = true;

		// compiling must only be done once
		if (compiling.compareAndSet(false, true)) {

			// this is a very critical section :)
			synchronized (SchemaService.class) {

				final Set<String> dynamicViews  = new LinkedHashSet<>();
				final NodeExtender nodeExtender = new NodeExtender();

				try (final Tx tx = StructrApp.getInstance().tx()) {

					SchemaService.ensureBuiltinTypesExist();

					// collect node classes
					for (final SchemaNode schemaNode : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {

						nodeExtender.addClass(schemaNode.getClassName(), schemaNode.getSource(errorBuffer));

						final String auxSource = schemaNode.getAuxiliarySource();
						if (auxSource != null) {

							nodeExtender.addClass("_" + schemaNode.getClassName() + "Helper", auxSource);
						}

						dynamicViews.addAll(schemaNode.getViews());
					}

					// collect relationship classes
					for (final SchemaRelationshipNode schemaRelationship : StructrApp.getInstance().nodeQuery(SchemaRelationshipNode.class).getAsList()) {

						nodeExtender.addClass(schemaRelationship.getClassName(), schemaRelationship.getSource(errorBuffer));

						final String auxSource = schemaRelationship.getAuxiliarySource();
						if (auxSource != null) {

							nodeExtender.addClass("_" + schemaRelationship.getClassName() + "Helper", auxSource);
						}

						dynamicViews.addAll(schemaRelationship.getViews());
					}

					// compile all classes at once and register
					Map<String, Class> newTypes = nodeExtender.compile(errorBuffer);
					for (final Class newType : newTypes.values()) {

						// do full reload
						config.unregisterEntityType(newType.getName());
						config.registerEntityType(newType);

						// instantiate classes to execute
						// static initializer of helpers
						try { newType.newInstance(); } catch (Throwable t) {}
					}


					// create properties and views etc.
					for (final SchemaNode schemaNode : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {
						schemaNode.createBuiltInSchemaEntities(errorBuffer);
					}


					success = !errorBuffer.hasError();

					// inject views in configuration provider
					if (success) {

						config.registerDynamicViews(dynamicViews);
						tx.success();
					}

				} catch (Throwable t) {

					logger.log(Level.SEVERE, "Unable to compile dynamic schema.", t);
					t.printStackTrace();

					success = false;
				}
			}

			// compiling done
			compiling.set(false);
		}

		return success;
	}

	@Override
	public void initialized() {
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

	// ----- private methods -----
	public static void ensureBuiltinTypesExist() throws FrameworkException {

		final App app = StructrApp.getInstance();

		for (final Entry<String, String> entry : builtinTypeMap.entrySet()) {

			final String type = entry.getKey();
			final String fqcn = entry.getValue();

			SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(type).getFirst();
			if (schemaNode == null) {

				schemaNode = app.create(SchemaNode.class, type);
			}

			schemaNode.setProperty(SchemaNode.extendsClass, fqcn);
			schemaNode.unlockReadOnlyPropertiesOnce();
			schemaNode.setProperty(SchemaNode.isBuiltinType, true);
		}
	}

	@Override
	public boolean isVital() {
		return true;
	}
}
