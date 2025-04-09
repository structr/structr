/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.schema.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;

/**
 * A migration handler that removes methods with the @Export annotation which do not include the SecurityContext in their signature
 */
public class RemoveExportedMethodsWithoutSecurityContext implements MigrationHandler {

	private static final Pattern PATTERN1 = Pattern.compile("incompatible types: ([a-zA-Z0-9_\\.]+) cannot be converted to org.structr.common.SecurityContext");
	private static final Pattern PATTERN2 = Pattern.compile(".*cannot be applied to given types;.*", Pattern.DOTALL);
	private static final Pattern PATTERN3 = Pattern.compile(".*cannot find symbol.*variable ctx.*", Pattern.DOTALL);
	private static final Logger logger   = LoggerFactory.getLogger(RemoveExportedMethodsWithoutSecurityContext.class);

	@Override
	public void handleMigration(final ErrorToken errorToken) throws FrameworkException {

		final String type   = errorToken.getType();
		final String token  = errorToken.getToken();
		final String detail = (String)errorToken.getDetail();

		if ("compiler_error".equals(token)) {

			// check error detail
			Matcher matcher = PATTERN1.matcher(detail);
			if (matcher.matches()) {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					// Group
					deleteSchemaMethodWithSignature(app, "void addMember(org.structr.core.entity.Principal member) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "void removeMember(org.structr.core.entity.Principal member) throws org.structr.common.error.FrameworkException");

					// File
					deleteSchemaMethodWithSignature(app, "void doCSVImport(java.util.Map<java.lang.String, java.lang.Object> parameters) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "void doXMLImport(java.util.Map<java.lang.String, java.lang.Object> parameters) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "java.util.Map<java.lang.String, java.lang.Object> getFirstLines(java.util.Map<java.lang.String, java.lang.Object> parameters)");
					deleteSchemaMethodWithSignature(app, "java.util.Map<java.lang.String, java.lang.Object> getCSVHeaders(java.util.Map<java.lang.String, java.lang.Object> parameters) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "java.lang.String getXMLStructure() throws org.structr.common.error.FrameworkException");

					// AbstractFile
					deleteSchemaMethodWithSignature(app, "void isBinaryDataAccessible()");

					// DataFeed
					deleteSchemaMethodWithSignature(app, "void cleanUp()");
					deleteSchemaMethodWithSignature(app, "void updateIfDue()");
					deleteSchemaMethodWithSignature(app, "void updateFeed()");
					deleteSchemaMethodWithSignature(app, "void updateFeed(boolean cleanUp)");

					// PaymentNode
					deleteSchemaMethodWithSignature(app, "org.structr.core.GraphObject beginCheckout(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "void cancelCheckout(java.lang.String arg0, java.lang.String arg1) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "org.structr.core.GraphObject confirmCheckout(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws org.structr.common.error.FrameworkException");

					// ODFExporter
					deleteSchemaMethodWithSignature(app, "void exportImage(java.lang.String uuid) throws org.structr.common.error.FrameworkException");

					// MessageSubscriber
					deleteSchemaMethodWithSignature(app, "org.structr.rest.RestMethodResult onMessage(java.lang.String topic, java.lang.String message) throws org.structr.common.error.FrameworkException");

					// MessageClient
					deleteSchemaMethodWithSignature(app, "org.structr.rest.RestMethodResult sendMessage(java.lang.String topic, java.lang.String message) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "org.structr.rest.RestMethodResult subscribeTopic(java.lang.String topic) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "org.structr.rest.RestMethodResult unsubscribeTopic(java.lang.String topic) throws org.structr.common.error.FrameworkException");

					// VideoFile
					deleteSchemaMethodWithSignature(app, "void convert(java.lang.String scriptName, java.lang.String newFileName) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "void grab(java.lang.String scriptName, java.lang.String imageFileName, long timeIndex) throws org.structr.common.error.FrameworkException");

					// ODTExporter, ODSExporter
					deleteSchemaMethodWithSignature(app, "void exportAttributes(java.lang.String uuid) throws org.structr.common.error.FrameworkException");

					// VideoFile
					deleteSchemaMethodWithSignature(app, "void setMetadata(java.lang.String key, java.lang.String value) throws org.structr.common.error.FrameworkException");
					deleteSchemaMethodWithSignature(app, "void setMetadata(org.structr.core.JsonInput metadata) throws org.structr.common.error.FrameworkException");

					// Indexable, File, FeedItem, FeedItemContent, FeedItemEnclosure, RemoteDocument
					deleteSchemaMethodWithSignature(app, "void getSearchContext()");

					tx.success();

				} catch (FrameworkException fex) {
					logger.warn("Unable to correct schema compilation error: {}", fex.getMessage());
				}

			} else {

				matcher = PATTERN2.matcher(detail);
				if (matcher.matches()) {

					final App app = StructrApp.getInstance();

					try (final Tx tx = app.tx()) {

						// ODTExporter / ODSExporter
						deleteSchemaMethodWithSignature(app, "void exportAttributes(java.lang.String uuid) throws org.structr.common.error.FrameworkException");

						// ODFExporter
						deleteSchemaMethodWithSignature(app, "void createDocumentFromTemplate() throws org.structr.common.error.FrameworkException");

						// VideoFile
						deleteSchemaMethodWithSignature(app, "void setMetadata(java.lang.String key, java.lang.String value) throws org.structr.common.error.FrameworkException");
						deleteSchemaMethodWithSignature(app, "void setMetadata(org.structr.core.JsonInput metadata) throws org.structr.common.error.FrameworkException");
						deleteSchemaMethodWithSignature(app, "org.structr.rest.RestMethodResult getMetadata() throws org.structr.common.error.FrameworkException");
						deleteSchemaMethodWithSignature(app, "void updateVideoInfo()");

						tx.success();

					} catch (FrameworkException fex) {
						logger.warn("Unable to correct schema compilation error: {}", fex.getMessage());
					}

				} else {

					matcher = PATTERN3.matcher(detail);
					if (matcher.matches()) {

						final App app = StructrApp.getInstance();

						try (final Tx tx = app.tx()) {

							// LDAPGroup
							deleteSchemaMethodWithSignature(app, "void update()");

							tx.success();

						} catch (FrameworkException fex) {
							logger.warn("Unable to correct schema compilation error: {}", fex.getMessage());
						}
					}
				}
			}
		}
	}

	private void deleteSchemaMethodWithSignature (final App app, final String signature) throws FrameworkException {

		for (final NodeInterface method : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SIGNATURE_PROPERTY), signature).getAsList()) {
			app.delete(method);
		}
	}
}