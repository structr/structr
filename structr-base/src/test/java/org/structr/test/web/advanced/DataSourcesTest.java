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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.datasources.Channel;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Components need information about the type of object that the data source returns. This information
 * can either be determined by following the channel / controller hierarchy or based on the expected
 * type of the object from the component configuration.
 *
 * We need to make sure that the channel / controller hierarchy is evaluated correctly and that the
 * fallback based on the expected type attribute of a component configuration works as intended.
 */
public class DataSourcesTest extends WidgetsTest {


	/*********
	 * data sources to test:
	 *  node:Project (schema type)
	 *  channel:current (current channel)
	 *  channel:project (request parameter channel)
	 *  parent (parent of nested data source)
	 *  stacked data sources
	 *  unsupported combinations (edit form with collection source etc.)
	 *     - collection (list, table) with a single element selection (projects.parent)
	 *     - single element with a collection selection (current.tasks)
	 *
	 *
	 */

	@Test
	public void testTypeResolution1() {

		// test case: a single page with a list component and an edit form, with the list configured as a
		// controller for the "current" channel, and the edit form as a subscriber for that channel, with
		// no selection.

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testTypeResolution1", "List", "Edit Form");

		try (final Tx tx = app.tx()) {

			final RenderContext renderContext          = new RenderContext(securityContext);
			final Page                   page          = app.nodeQuery(StructrTraits.PAGE).name("testTypeResolution1").getFirst().as(Page.class);

			{
				final DOMNode                listComponent = getDOMNode(page, "List");
				final ComponentConfiguration config       = listComponent.getComponentConfiguration();
				final Traits                 configTraits = config.getTraits();
				final String                 channelName  = "node:Project";

				// enable data source
				config.setProperty(configTraits.key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), channelName);

				// fetch data source
				final Channel<GraphObject> dataSource = config.getDataSource();

				// check existence
				assertNotNull(dataSource, "Data source " + channelName + " was not created");

				// check dimensionality of component configuration (will be checked against the component and used for error reporting)
				assertEquals("Component configuration does not return the correct dimensions for the given configuration", 1, dataSource.getDimension());

				// check data type
				assertEquals("Data source " + channelName + " did not return the correct data type", "Project", dataSource.getDataType(renderContext));

				// setup for next step: list component must be configured as a controller for the "current" channel so we can follow the hierarchy
				config.setProperty(configTraits.key(ComponentConfigurationTraitDefinition.ROLE_PROPERTY), "controller");
				config.setProperty(configTraits.key(ComponentConfigurationTraitDefinition.SELECTION_CHANNEL_PROPERTY), "current");
			}

			{

				final DOMNode                editForm = getDOMNode(page, "Edit Form");
				final ComponentConfiguration config   = editForm.getComponentConfiguration();
				final String channelName              = "channel:current";

				// enable data source
				config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), channelName);

				// fetch data source
				final Channel<GraphObject> dataSource = config.getDataSource();

				// check existence
				assertNotNull(dataSource, "Data source " + channelName + " was not created");

				// check dimensionality of component configuration (will be checked against the component and used for error reporting)
				assertEquals("Component configuration does not return the correct dimensions for the given configuration", 0, dataSource.getDimension());

				// check data type
				assertEquals("Data source " + channelName + " did not return the correct data type", "Project", dataSource.getDataType(renderContext));
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception");
		}
	}

	@Test
	public void testTypeResolution2() {

		// test case: a single page with an edit form that has a COLLECTION data source.
		// This cannot work – the system must reject this combination with a 422 error.

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testTypeResolution2", "Edit Form");

		try (final Tx tx = app.tx()) {

			final Page                   page   = app.nodeQuery(StructrTraits.PAGE).name("testTypeResolution2").getFirst().as(Page.class);
			final DOMNode                editForm = getDOMNode(page, "Edit Form");
			final ComponentConfiguration config   = editForm.getComponentConfiguration();

			// "node:Project" is a collection (dim=1) – an edit form (dim=0) cannot accept it
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "node:Project");

			tx.success();

			Assert.fail("Expected FrameworkException: edit form with collection data source must be rejected");

		} catch (FrameworkException fex) {

			assertEquals("Expected HTTP 422 for incompatible data source on edit form", 422, fex.getStatus());
		}
	}


	@Test
	public void testTypeResolution3() {

		// test case: a single page with an edit form and NO controller, so we need to configure the data type
		// manually.

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testTypeResolution3", "Edit Form");

		try (final Tx tx = app.tx()) {

			final ActionContext          actionContext = new ActionContext(securityContext);
			final Page                   page          = app.nodeQuery(StructrTraits.PAGE).name("testTypeResolution3").getFirst().as(Page.class);

			{
				final DOMNode                editForm = getDOMNode(page, "Edit Form");
				final ComponentConfiguration config   = editForm.getComponentConfiguration();
				final String channelName              = "channel:current";

				// enable data source
				config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), channelName);
				config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.EXPECTED_DATA_TYPE_PROPERTY), "Project");

				// fetch data source
				final Channel<GraphObject> dataSource = config.getDataSource();

				// check existence
				assertNotNull(dataSource, "Data source " + channelName + " was not created");

				// check data type
				assertEquals("Data source " + channelName + " did not return the correct data type", "Project", dataSource.getDataType(actionContext));
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception");
		}
	}

	// -----------------------------------------------------------------------
	// Compatibility checks – valid combinations (no exception expected)
	// -----------------------------------------------------------------------

	@Test
	public void testCompatibilityOk_ListComponentWithListDatasource() {

		// list component (dim=1) + list data source (dim=1) + no transform => ok

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testCompatibilityOk1", "List");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityOk1").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "List").getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "node:Project");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception: list component with list data source should be valid");
		}
	}

	@Test
	public void testCompatibilityOk_ListComponentWithObjectDatasource() {

		// list component (dim=1) + single-object data source (dim=0) + no transform => ok

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testCompatibilityOk2", "List");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityOk2").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "List").getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "channel:current");
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.EXPECTED_DATA_TYPE_PROPERTY), "Project");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception: list component with single-object data source should be valid");
		}
	}

	@Test
	public void testCompatibilityOk_ListComponentWithObjectDatasourceAndListTransform() {

		// list component (dim=1) + single-object data source (dim=0) + list transform => ok
		// e.g. current.tasks where tasks is a one-to-many relation

		setupUserAndWidgets();
		createProjectAndTaskTypes();
		createPageWithWidgets("testCompatibilityOk3", "List");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityOk3").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "List").getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "channel:current");
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.EXPECTED_DATA_TYPE_PROPERTY), "Project");
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.TRANSFORM_PROPERTY), "tasks");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception: list component with object datasource and list transform should be valid");
		}
	}

	@Test
	public void testCompatibilityOk_EditFormWithObjectDatasource() {

		// edit form (dim=0) + single-object data source (dim=0) + no transform => ok

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testCompatibilityOk4", "Edit Form");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityOk4").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "Edit Form").getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "channel:current");
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.EXPECTED_DATA_TYPE_PROPERTY), "Project");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception: edit form with single-object data source should be valid");
		}
	}

	@Test
	public void testCompatibilityOk_EditFormWithObjectDatasourceAndObjectTransform() {

		// edit form (dim=0) + single-object data source (dim=0) + single-object transform => ok
		// e.g. current.project where project is the one-side of a relation (single object)

		setupUserAndWidgets();
		createProjectAndTaskTypes();
		createPageWithWidgets("testCompatibilityOk5", "Edit Form");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityOk5").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "Edit Form").getComponentConfiguration();

			// task.project is the one-side (single object), so this should be valid on an edit form
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "channel:current");
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.EXPECTED_DATA_TYPE_PROPERTY), "Task");
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.TRANSFORM_PROPERTY), "project");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception: edit form with object datasource and single-object transform should be valid");
		}
	}

	// -----------------------------------------------------------------------
	// Compatibility checks – invalid combinations (FrameworkException expected)
	// -----------------------------------------------------------------------

	@Test
	public void testCompatibilityError_ListDatasourceWithTransform() {

		// list data source (dim=1) + any transform => error
		// "cannot navigate a property on a collection"

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testCompatibilityErr1", "List");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityErr1").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "List").getComponentConfiguration();

			// first set datasource successfully
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "node:Project");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception while setting up list datasource");
		}

		// now set a transform on a list datasource – this must fail
		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityErr1").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "List").getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.TRANSFORM_PROPERTY), "name");

			tx.success();

			Assert.fail("Expected FrameworkException: list datasource with transform is not allowed");

		} catch (FrameworkException fex) {

			assertEquals("Expected HTTP 422 for invalid combination", 422, fex.getStatus());
		}
	}

	@Test
	public void testCompatibilityError_EditFormWithListDatasource() {

		// edit form (dim=0) + list data source (dim=1) => error
		// "edit form needs a single element"

		setupUserAndWidgets();
		createDataType("Project");
		createPageWithWidgets("testCompatibilityErr2", "Edit Form");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityErr2").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "Edit Form").getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "node:Project");

			tx.success();

			Assert.fail("Expected FrameworkException: edit form with list datasource is not allowed");

		} catch (FrameworkException fex) {

			assertEquals("Expected HTTP 422 for invalid combination", 422, fex.getStatus());
		}
	}

	@Test
	public void testCompatibilityError_EditFormWithListTransform() {

		// edit form (dim=0) + single-object datasource (dim=0) + list transform => error
		// e.g. current.tasks expands to a collection, but the edit form needs a single element

		setupUserAndWidgets();
		createProjectAndTaskTypes();
		createPageWithWidgets("testCompatibilityErr3", "Edit Form");

		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityErr3").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "Edit Form").getComponentConfiguration();

			// set datasource + expectedDataType first (both valid for edit form individually)
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "channel:current");
			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.EXPECTED_DATA_TYPE_PROPERTY), "Project");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			Assert.fail("Unexpected exception while setting up edit form datasource");
		}

		// adding a list transform must fail on an edit form
		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("testCompatibilityErr3").getFirst().as(Page.class);
			final ComponentConfiguration config = getDOMNode(page, "Edit Form").getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.TRANSFORM_PROPERTY), "tasks");

			tx.success();

			Assert.fail("Expected FrameworkException: edit form with list transform is not allowed");

		} catch (FrameworkException fex) {

			assertEquals("Expected HTTP 422 for invalid combination", 422, fex.getStatus());
		}
	}

	// -----------------------------------------------------------------------
	// Helper
	// -----------------------------------------------------------------------

	/**
	 * Creates two types – Project and Task – with a one-to-many relation so that
	 * Project.tasks is a RelationProperty (collection) and Task.project is a
	 * single-object back-reference.
	 */
	private void createProjectAndTaskTypes() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema         = StructrSchema.createEmptySchema();
			final JsonObjectType projectType = (JsonObjectType) schema.addType("Project");
			final JsonObjectType taskType    = (JsonObjectType) schema.addType("Task");

			projectType.relate(taskType, "HAS_TASK", Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			Assert.fail("Unexpected exception while creating Project/Task schema");
		}
	}
}
