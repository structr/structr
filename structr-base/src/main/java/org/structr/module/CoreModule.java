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
package org.structr.module;

import org.structr.api.service.LicenseManager;
import org.structr.core.function.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.definitions.*;
import org.structr.core.traits.relationships.*;
import org.structr.web.function.ScheduleFunction;

import java.util.Set;

public class CoreModule implements StructrModule {

	@Override
	public void onLoad() {

		final TraitDefinition propertyContainer     = new PropertyContainerTraitDefinition();
		final TraitDefinition graphObject           = new GraphObjectTraitDefinition();
		final TraitDefinition accessControllable    = new AccessControllableTraitDefinition();

		final TraitDefinition nodeInterface         = new NodeInterfaceTraitDefinition();
		final TraitDefinition relationshipInterface = new RelationshipInterfaceTraitDefinition();

		// common base types for nodes and relationships
		StructrTraits.registerTrait(propertyContainer);
		StructrTraits.registerTrait(graphObject);
		StructrTraits.registerTrait(accessControllable);

		StructrTraits.registerTrait(new PrincipalOwnsNodeDefinition());
		StructrTraits.registerTrait(new SecurityRelationshipDefinition());

		StructrTraits.registerTrait(relationshipInterface);

		StructrTraits.registerRelationshipType(StructrTraits.PRINCIPAL_OWNS_NODE,                   StructrTraits.PRINCIPAL_OWNS_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.SECURITY,                              StructrTraits.SECURITY);

		StructrTraits.registerTrait(nodeInterface);

		StructrTraits.registerBaseType(propertyContainer);
		StructrTraits.registerBaseType(graphObject);
		StructrTraits.registerBaseType(accessControllable);

		// relationship traits
		StructrTraits.registerTrait(new PrincipalSchemaGrantRelationshipDefinition());
		StructrTraits.registerTrait(new GroupContainsPrincipalDefinition());
		StructrTraits.registerTrait(new SchemaExcludedViewPropertyDefinition());
		StructrTraits.registerTrait(new SchemaGrantSchemaNodeRelationshipDefinition());
		StructrTraits.registerTrait(new SchemaMethodParametersDefinition());
		StructrTraits.registerTrait(new SchemaNodeExtendsSchemaNodeDefinition());
		StructrTraits.registerTrait(new SchemaNodeMethodDefinition());
		StructrTraits.registerTrait(new SchemaNodePropertyDefinition());
		StructrTraits.registerTrait(new SchemaNodeViewDefinition());
		StructrTraits.registerTrait(new SchemaRelationshipSourceNodeDefinition());
		StructrTraits.registerTrait(new SchemaRelationshipTargetNodeDefinition());
		StructrTraits.registerTrait(new SchemaViewPropertyDefinition());

		// relationship types
		StructrTraits.registerRelationshipType(StructrTraits.PRINCIPAL_SCHEMA_GRANT_RELATIONSHIP,   StructrTraits.PRINCIPAL_SCHEMA_GRANT_RELATIONSHIP);
		StructrTraits.registerRelationshipType(StructrTraits.GROUP_CONTAINS_PRINCIPAL,              StructrTraits.GROUP_CONTAINS_PRINCIPAL);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_EXCLUDED_VIEW_PROPERTY,         StructrTraits.SCHEMA_EXCLUDED_VIEW_PROPERTY);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP, StructrTraits.SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_METHOD_PARAMETERS,              StructrTraits.SCHEMA_METHOD_PARAMETERS);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_NODE_EXTENDS_SCHEMA_NODE,       StructrTraits.SCHEMA_NODE_EXTENDS_SCHEMA_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_NODE_METHOD,                    StructrTraits.SCHEMA_NODE_METHOD);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_NODE_PROPERTY,                  StructrTraits.SCHEMA_NODE_PROPERTY);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_NODE_VIEW,                      StructrTraits.SCHEMA_NODE_VIEW);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_RELATIONSHIP_SOURCE_NODE,       StructrTraits.SCHEMA_RELATIONSHIP_SOURCE_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_RELATIONSHIP_TARGET_NODE,       StructrTraits.SCHEMA_RELATIONSHIP_TARGET_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.SCHEMA_VIEW_PROPERTY,                  StructrTraits.SCHEMA_VIEW_PROPERTY);

		StructrTraits.registerNodeType(StructrTraits.NODE_INTERFACE, StructrTraits.NODE_INTERFACE);
		StructrTraits.registerRelationshipType(StructrTraits.RELATIONSHIP_INTERFACE, StructrTraits.RELATIONSHIP_INTERFACE);

		StructrTraits.registerTrait(new PrincipalTraitDefinition());
		StructrTraits.registerTrait(new GroupTraitDefinition());
		StructrTraits.registerTrait(new LocalizationTraitDefinition());
		StructrTraits.registerTrait(new LocationTraitDefinition());
		StructrTraits.registerTrait(new MailTemplateTraitDefinition());
		StructrTraits.registerTrait(new SessionDataNodeTraitDefinition());
		StructrTraits.registerTrait(new SchemaReloadingNodeTraitDefinition());
		StructrTraits.registerTrait(new SchemaGrantTraitDefinition());
		StructrTraits.registerTrait(new AbstractSchemaNodeTraitDefinition());
		StructrTraits.registerTrait(new SchemaNodeTraitDefinition());
		StructrTraits.registerTrait(new SchemaMethodTraitDefinition());
		StructrTraits.registerTrait(new SchemaMethodParameterTraitDefinition());
		StructrTraits.registerTrait(new SchemaRelationshipNodeTraitDefinition());
		StructrTraits.registerTrait(new SchemaPropertyTraitDefinition());
		StructrTraits.registerTrait(new SchemaViewTraitDefinition());
		StructrTraits.registerTrait(new CorsSettingTraitDefinition());
		StructrTraits.registerTrait(new ResourceAccessTraitDefinition(StructrTraits.RESOURCE_ACCESS));
		StructrTraits.registerTrait(new SessionDataNodeTraitDefinition());

		// node types
		StructrTraits.registerNodeType(StructrTraits.GENERIC_NODE);
		StructrTraits.registerNodeType(StructrTraits.PRINCIPAL,                StructrTraits.PRINCIPAL);
		StructrTraits.registerNodeType(StructrTraits.GROUP,                    StructrTraits.PRINCIPAL, StructrTraits.GROUP);
		StructrTraits.registerNodeType(StructrTraits.LOCALIZATION,             StructrTraits.LOCALIZATION);
		StructrTraits.registerNodeType(StructrTraits.LOCATION,                 StructrTraits.LOCATION);
		StructrTraits.registerNodeType(StructrTraits.MAIL_TEMPLATE,            StructrTraits.MAIL_TEMPLATE);
		StructrTraits.registerNodeType(StructrTraits.SESSION_DATA_NODE,        StructrTraits.SESSION_DATA_NODE);
		StructrTraits.registerNodeType(StructrTraits.SCHEMA_GRANT,             StructrTraits.SCHEMA_RELOADING_NODE, StructrTraits.SCHEMA_GRANT);
		StructrTraits.registerNodeType(StructrTraits.ABSTRACT_SCHEMA_NODE,     StructrTraits.ABSTRACT_SCHEMA_NODE);
		StructrTraits.registerNodeType(StructrTraits.SCHEMA_NODE,              StructrTraits.SCHEMA_RELOADING_NODE, StructrTraits.ABSTRACT_SCHEMA_NODE, StructrTraits.SCHEMA_NODE);
		StructrTraits.registerNodeType(StructrTraits.SCHEMA_METHOD,            StructrTraits.SCHEMA_RELOADING_NODE, StructrTraits.SCHEMA_METHOD);
		StructrTraits.registerNodeType(StructrTraits.SCHEMA_METHOD_PARAMETER,  StructrTraits.SCHEMA_RELOADING_NODE, StructrTraits.SCHEMA_METHOD_PARAMETER);
		StructrTraits.registerNodeType(StructrTraits.SCHEMA_RELATIONSHIP_NODE, StructrTraits.SCHEMA_RELOADING_NODE, StructrTraits.ABSTRACT_SCHEMA_NODE, StructrTraits.SCHEMA_RELATIONSHIP_NODE);
		StructrTraits.registerNodeType(StructrTraits.SCHEMA_PROPERTY,          StructrTraits.SCHEMA_RELOADING_NODE, StructrTraits.SCHEMA_PROPERTY);
		StructrTraits.registerNodeType(StructrTraits.SCHEMA_VIEW,              StructrTraits.SCHEMA_RELOADING_NODE, StructrTraits.SCHEMA_VIEW);
		StructrTraits.registerNodeType(StructrTraits.CORS_SETTING,             StructrTraits.CORS_SETTING);
		StructrTraits.registerNodeType(StructrTraits.RESOURCE_ACCESS,          StructrTraits.RESOURCE_ACCESS);
		StructrTraits.registerNodeType(StructrTraits.SESSION_DATA_NODE,        StructrTraits.SESSION_DATA_NODE);

	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new RollbackTransactionFunction());
		Functions.put(licenseManager, new RemoteCypherFunction());
		Functions.put(licenseManager, new CypherFunction());
		Functions.put(licenseManager, new MD5Function());
		Functions.put(licenseManager, new UpperFunction());
		Functions.put(licenseManager, new LowerFunction());
		Functions.put(licenseManager, new JoinFunction());
		Functions.put(licenseManager, new ConcatFunction());
		Functions.put(licenseManager, new SplitFunction());
		Functions.put(licenseManager, new SplitRegexFunction());
		Functions.put(licenseManager, new AbbrFunction());
		Functions.put(licenseManager, new CapitalizeFunction());
		Functions.put(licenseManager, new TitleizeFunction());
		Functions.put(licenseManager, new NumFunction());
		Functions.put(licenseManager, new LongFunction());
		Functions.put(licenseManager, new IntFunction());
		Functions.put(licenseManager, new RandomFunction());
		Functions.put(licenseManager, new RintFunction());
		Functions.put(licenseManager, new IndexOfFunction());
		Functions.put(licenseManager, new ContainsFunction());
		Functions.put(licenseManager, new CopyPermissionsFunction());
		Functions.put(licenseManager, new SubstringFunction());
		Functions.put(licenseManager, new LengthFunction());
		Functions.put(licenseManager, new ReplaceFunction());
		Functions.put(licenseManager, new TrimFunction());
		Functions.put(licenseManager, new CleanFunction());
		Functions.put(licenseManager, new CoalesceFunction());
		Functions.put(licenseManager, new CoalesceObjectsFunction());
		Functions.put(licenseManager, new FormUrlEncodeFunction());
		Functions.put(licenseManager, new UrlEncodeFunction());
		Functions.put(licenseManager, new EscapeJavascriptFunction());
		Functions.put(licenseManager, new EscapeJsonFunction());
		Functions.put(licenseManager, new EmptyFunction());
		Functions.put(licenseManager, new EqualFunction());
		Functions.put(licenseManager, new AddFunction());
		Functions.put(licenseManager, new DoubleSumFunction());
		Functions.put(licenseManager, new IntSumFunction());
		Functions.put(licenseManager, new IsCollectionFunction());
		Functions.put(licenseManager, new IsEntityFunction());
		Functions.put(licenseManager, new ExtractFunction());
		Functions.put(licenseManager, new MergeFunction());
		Functions.put(licenseManager, new MergeUniqueFunction());
		Functions.put(licenseManager, new ComplementFunction());
		Functions.put(licenseManager, new UnwindFunction());
		Functions.put(licenseManager, new SortFunction());
		Functions.put(licenseManager, new LtFunction());
		Functions.put(licenseManager, new GtFunction());
		Functions.put(licenseManager, new LteFunction());
		Functions.put(licenseManager, new GteFunction());
		Functions.put(licenseManager, new SubtFunction());
		Functions.put(licenseManager, new MultFunction());
		Functions.put(licenseManager, new QuotFunction());
		Functions.put(licenseManager, new DivFunction());
		Functions.put(licenseManager, new ModFunction());
		Functions.put(licenseManager, new FloorFunction());
		Functions.put(licenseManager, new CeilFunction());
		Functions.put(licenseManager, new RoundFunction());
		Functions.put(licenseManager, new MaxFunction());
		Functions.put(licenseManager, new MinFunction());
		Functions.put(licenseManager, new SetLocaleFunction());
		Functions.put(licenseManager, new DateFormatFunction());
		Functions.put(licenseManager, new DateAddFunction());
		Functions.put(licenseManager, new ParseDateFunction());
		Functions.put(licenseManager, new WeekDaysFunction());
		Functions.put(licenseManager, new ToDateFunction());
		Functions.put(licenseManager, new NumberFormatFunction());
		Functions.put(licenseManager, new ParseNumberFunction());
		Functions.put(licenseManager, new NotFunction());
		Functions.put(licenseManager, new AndFunction());
		Functions.put(licenseManager, new OrFunction());
		Functions.put(licenseManager, new GetFunction());
		Functions.put(licenseManager, new GetOrNullFunction());
		Functions.put(licenseManager, new SizeFunction());
		Functions.put(licenseManager, new FirstFunction());
		Functions.put(licenseManager, new LastFunction());
		Functions.put(licenseManager, new NthFunction());
		Functions.put(licenseManager, new MergePropertiesFunction());
		Functions.put(licenseManager, new TimerFunction());
		Functions.put(licenseManager, new StrReplaceFunction());
		Functions.put(licenseManager, new SearchFunction());
		Functions.put(licenseManager, new SearchFulltextFunction());
		Functions.put(licenseManager, new SearchRelationshipsFulltextFunction());
		Functions.put(licenseManager, new IncomingFunction());
		Functions.put(licenseManager, new OutgoingFunction());
		Functions.put(licenseManager, new HasRelationshipFunction());
		Functions.put(licenseManager, new HasOutgoingRelationshipFunction());
		Functions.put(licenseManager, new HasIncomingRelationshipFunction());
		Functions.put(licenseManager, new GetRelationshipsFunction());
		Functions.put(licenseManager, new GetOutgoingRelationshipsFunction());
		Functions.put(licenseManager, new GetIncomingRelationshipsFunction());
		Functions.put(licenseManager, new RetrieveFunction());
		Functions.put(licenseManager, new StoreFunction());
		Functions.put(licenseManager, new PrintFunction());
		Functions.put(licenseManager, new LogFunction());
		Functions.put(licenseManager, new StackDumpFunction());
		Functions.put(licenseManager, new FindFunction());
		Functions.put(licenseManager, new GetOrCreateFunction());
		Functions.put(licenseManager, new CreateOrUpdateFunction());
		Functions.put(licenseManager, new FindRelationshipFunction());
		Functions.put(licenseManager, new StartsWithFunction());
		Functions.put(licenseManager, new EndsWithFunction());
		Functions.put(licenseManager, new Base64EncodeFunction());
		Functions.put(licenseManager, new Base64DecodeFunction());
		Functions.put(licenseManager, new SleepFunction());
		Functions.put(licenseManager, new RandomUUIDFunction());
		Functions.put(licenseManager, new HMACFunction());
		Functions.put(licenseManager, new OneFunction());
		Functions.put(licenseManager, new HashFunction());
		Functions.put(licenseManager, new PrefetchFunction());
		Functions.put(licenseManager, new Prefetch2Function());
		Functions.put(licenseManager, new AddLabelsFunction());
		Functions.put(licenseManager, new RemoveLabelsFunction());
		Functions.put(licenseManager, new ScheduleFunction());
		Functions.put(licenseManager, new GetErrorsFunction());
		Functions.put(licenseManager, new ClearErrorsFunction());
		Functions.put(licenseManager, new ClearErrorFunction());

		Functions.put(licenseManager, new HasCacheValueFunction());
		Functions.put(licenseManager, new GetCacheValueFunction());
		Functions.put(licenseManager, new InvalidateCacheValueFunction());

		Functions.put(licenseManager, new SetLogLevelFunction());
		Functions.put(licenseManager, new IsValidUuidFunction());

		// ----- BEGIN functions with side effects -----
		Functions.put(licenseManager, new SetFunction());
		Functions.put(licenseManager, new CreateFunction());
		Functions.put(licenseManager, new DeleteFunction());
		Functions.put(licenseManager, new CreateRelationshipFunction());
	}

	@Override
	public String getName() {
		return "core";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
