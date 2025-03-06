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
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.DataSources;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.traits.StructrTraits;
import org.structr.flow.datasource.FlowContainerDataSource;
import org.structr.flow.impl.FlowFunction;
import org.structr.flow.impl.rels.*;
import org.structr.flow.traits.definitions.*;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.nio.file.Path;
import java.util.Set;

/**
 *
 */
public class FlowModule implements StructrModule {

	private static final Logger logger = LoggerFactory.getLogger(FlowModule.class.getName());

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		final boolean licensed = licenseManager == null || licenseManager.isModuleLicensed(getName());

		DataSources.put(licensed, getName(), "flowDataSource", new FlowContainerDataSource());

		if (licensed) {

			StructrTraits.registerRelationshipType("FlowContainerPackageFlow",         new FlowContainerPackageFlow());
			StructrTraits.registerRelationshipType("FlowContainerBaseNode",            new FlowContainerBaseNode());
			StructrTraits.registerRelationshipType("FlowContainerConfigurationFlow",   new FlowContainerConfigurationFlow());
			StructrTraits.registerRelationshipType("FlowActiveContainerConfiguration", new FlowActiveContainerConfiguration());
			StructrTraits.registerRelationshipType("FlowContainerFlowNode",            new FlowContainerFlowNode());
			StructrTraits.registerRelationshipType("DOMNodeFLOWFlowContainer",         new DOMNodeFLOWFlowContainer());

			StructrTraits.registerNodeType("FlowAction",                 new FlowActionTraitDefinition());
			StructrTraits.registerNodeType("FlowAggregate",              new FlowAggregateTraitDefinition());
			StructrTraits.registerNodeType("FlowAnd",                    new FlowAndTraitDefinition());
			StructrTraits.registerNodeType("FlowBaseNode",               new FlowBaseNodeTraitDefinition());
			StructrTraits.registerNodeType("FlowCall",                   new FlowCallTraitDefinition());
			StructrTraits.registerNodeType("FlowCollectionDataSource",   new FlowCollectionDataSourceTraitDefinition());
			StructrTraits.registerNodeType("FlowComparison",             new FlowComparisonTraitDefinition());
			StructrTraits.registerNodeType("FlowContainer",              new FlowContainerTraitDefinition());
			StructrTraits.registerNodeType("FlowContainerConfiguration", new FlowContainerConfigurationTraitDefinition());
			StructrTraits.registerNodeType("FlowContainerPackage",       new FlowContainerPackageTraitDefinition());
			StructrTraits.registerNodeType("FlowDecision",               new FlowDecisionTraitDefinition());
			StructrTraits.registerNodeType("FlowDataSource",             new FlowDataSourceTraitDefinition());
			StructrTraits.registerNodeType("FlowExceptionHandler",       new FlowExceptionHandlerTraitDefinition());
			StructrTraits.registerNodeType("FlowFilter",                 new FlowFilterTraitDefinition());
			StructrTraits.registerNodeType("FlowFirst",                  new FlowFirstTraitDefinition());
			StructrTraits.registerNodeType("FlowForEach",                new FlowForEachTraitDefinition());
			StructrTraits.registerNodeType("FlowFork",                   new FlowForkTraitDefinition());
			StructrTraits.registerNodeType("FlowForkJoin",               new FlowForkJoinTraitDefinition());
			StructrTraits.registerNodeType("FlowGetProperty",            new FlowGetPropertyTraitDefinition());
			StructrTraits.registerNodeType("FlowIsTrue",                 new FlowIsTrueTraitDefinition());
			StructrTraits.registerNodeType("FlowKeyValue",               new FlowKeyValueTraitDefinition());
			StructrTraits.registerNodeType("FlowLog",                    new FlowLogTraitDefinition());
			StructrTraits.registerNodeType("FlowLogicConditions",        new FlowLogicConditionTraitDefinition());
			StructrTraits.registerNodeType("FlowNode",                   new FlowNodeTraitDefinition());
			StructrTraits.registerNodeType("FlowNot",                    new FlowNotTraitDefinition());
			StructrTraits.registerNodeType("FlowNotEmpty",               new FlowNotEmptyTraitDefinition());
			StructrTraits.registerNodeType("FlowNotNull",                new FlowNotNullTraitDefinition());
			StructrTraits.registerNodeType("FlowObjectDataSource",       new FlowObjectDataSourceTraitDefinition());
			StructrTraits.registerNodeType("FlowOr",                     new FlowOrTraitDefinition());
		}
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new FlowFunction());
	}

	@Override
	public String getName() {
		return "flows";
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
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public boolean hasDeploymentData () {
		return true;
	}

	@Override
	public void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {
		FlowDeploymentHandler.exportDeploymentData(target, gson);
	}

	@Override
	public void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {
		FlowDeploymentHandler.importDeploymentData(source, gson);
	}
}
