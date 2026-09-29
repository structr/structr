/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.advanced;

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.attribute.Name;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.Site;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.SiteTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertEquals;

public class Deployment7Test extends DeploymentTestBase {

	@Test
	public void test70ExportComponentConfigurationWithoutComponent() {

		// A ComponentConfiguration whose related DOM node (component) has been
		// deleted leaves the "domNode" relationship empty. The deployment export
		// must not fail with a NullPointerException in that case (see
		// DeployCommand.exportComponentConfigurations).

		String configUuid = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface node   = app.create(StructrTraits.COMPONENT_CONFIGURATION, "test70");
			final ComponentConfiguration config = node.as(ComponentConfiguration.class);

			configUuid = config.getUuid();

			// sanity check: this configuration has no related component
			assertNull("Test setup should create a component configuration without a component", config.getComponent());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		assertNotNull(configUuid);

		Path exportPath = null;

		try {

			// this used to throw a NullPointerException because the export
			// dereferenced config.getComponent().getUuid() unconditionally
			exportPath = doExport();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Deployment export must not fail for a component configuration without a component: " + t.getMessage());

		} finally {

			if (exportPath != null) {

				try {

					deleteExportAt(exportPath);
				} catch (Exception ignore) {}
			}
		}
	}

	@Test
	public void test71PagesWithIdenticalNames() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page1 = Page.createSimplePage(securityContext, "test01");
			final Page page2 = Page.createSimplePage(securityContext, "test01");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			assertEquals(2, app.nodeQuery(StructrTraits.PAGE).name("test01").getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}
	}

	@Test
	public void test72PageWithNullName() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page1 = Page.createSimplePage(securityContext, "test01");

			page1.setName(null);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			assertEquals(1, app.nodeQuery(StructrTraits.PAGE).name(null).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}
	}

	@Test
	public void test73PagesWithIdenticalNamesLinkedToSites() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page1 = Page.createSimplePage(securityContext, "test01");
			final Page page2 = Page.createSimplePage(securityContext, "test01");
			final Site site = app.create(StructrTraits.SITE, "mysite").as(Site.class);

			site.setProperty(site.getTraits().key(SiteTraitDefinition.PAGES_PROPERTY), List.of(page1, page2));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			assertEquals(2, Iterables.count(app.nodeQuery(StructrTraits.SITE).name("mysite").getFirst().as(Site.class).getPages()));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}
	}

	@Test
	public void test74PagesLinkedToFilesAndPages() {

		// setup schema
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonObjectType page = (JsonObjectType)schema.getType(StructrTraits.PAGE);
			final JsonObjectType file = (JsonObjectType)schema.getType(StructrTraits.FILE);

			page.relate(page, "LINKS_TO_PAGE", Cardinality.ManyToMany, "linkingPages", "linkedPages");
			page.relate(file, "LINKS_TO_FILE", Cardinality.ManyToMany, "linkingPages", "linkedFiles");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final Page page1 = Page.createSimplePage(securityContext, "test01");
			final Page page2 = Page.createSimplePage(securityContext, "test02");

			page1.setProperty(Traits.of(StructrTraits.PAGE).key("linkedPages"), List.of(page2));

			final NodeInterface templateFile = FileHelper.createFile(securityContext, """
				blah blah
				""".getBytes(), "text/plain", StructrTraits.FILE, "myfile.txt", true);

			templateFile.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			page1.setProperty(Traits.of(StructrTraits.PAGE).key("linkedFiles"), List.of(templateFile));
			page2.setProperty(Traits.of(StructrTraits.PAGE).key("linkedFiles"), List.of(templateFile));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

		} catch (IOException e) {

			fail("Unexpected IOException.");
		}

		// test
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			// assert that the link is still there after the deployment roundtrip
			final PropertyKey<Iterable<NodeInterface>> linkedPagesKey  = Traits.of(StructrTraits.PAGE).key("linkedPages");
			final PropertyKey<Iterable<NodeInterface>> linkingPagesKey = Traits.of(StructrTraits.PAGE).key("linkingPages");
			final PropertyKey<Iterable<NodeInterface>> linkingFilesFromPagesKey = Traits.of(StructrTraits.PAGE).key("linkedFiles");
			final PropertyKey<Iterable<NodeInterface>> linkingPagesFromFileKey = Traits.of(StructrTraits.FILE).key("linkingPages");

			final NodeInterface page1 = app.nodeQuery(StructrTraits.PAGE).name("test01").getFirst();
			final NodeInterface page2 = app.nodeQuery(StructrTraits.PAGE).name("test02").getFirst();
			final NodeInterface file  = app.nodeQuery(StructrTraits.FILE).name("myfile.txt").getFirst();

			assertNotNull(page1);
			assertNotNull(page2);
			assertNotNull(file);

			final List<NodeInterface> linkedPages  = Iterables.toList(page1.getProperty(linkedPagesKey));
			final List<NodeInterface> linkingPages = Iterables.toList(page2.getProperty(linkingPagesKey));

			assertEquals(1, linkedPages.size());
			assertEquals(page2.getUuid(), linkedPages.getFirst().getUuid());

			assertEquals(1, linkingPages.size());
			assertEquals(page1.getUuid(), linkingPages.getFirst().getUuid());

			final List<NodeInterface> linkedFiles1 = Iterables.toList(page1.getProperty(linkingFilesFromPagesKey));
			final List<NodeInterface> linkedFiles2 = Iterables.toList(page2.getProperty(linkingFilesFromPagesKey));
			final List<NodeInterface> linkedPagesFromFile = Iterables.toList(file.getProperty(linkingPagesFromFileKey));

			assertEquals(1, linkedFiles1.size());
			assertEquals(file.getUuid(), linkedFiles1.getFirst().getUuid());

			assertEquals(1, linkedFiles2.size());
			assertEquals(file.getUuid(), linkedFiles2.getFirst().getUuid());

			assertEquals(2, linkedPagesFromFile.size());

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}
	}
}