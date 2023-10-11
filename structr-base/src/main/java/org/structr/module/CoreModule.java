/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.*;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.util.Set;

/**
 *
 */
public class CoreModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

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
		Functions.put(licenseManager, new GetCounterFunction());
		Functions.put(licenseManager, new IncCounterFunction());
		Functions.put(licenseManager, new ResetCounterFunction());
		Functions.put(licenseManager, new MergePropertiesFunction());
		Functions.put(licenseManager, new KeysFunction());
		Functions.put(licenseManager, new ValuesFunction());
		Functions.put(licenseManager, new TimerFunction());
		Functions.put(licenseManager, new StrReplaceFunction());
		Functions.put(licenseManager, new SearchFunction());
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
		Functions.put(licenseManager, new DebugFunction());
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

		Functions.put(licenseManager, new HasCacheValueFunction());
		Functions.put(licenseManager, new GetCacheValueFunction());
		Functions.put(licenseManager, new DeleteCacheValueFunction());
		Functions.put(licenseManager, new InvalidateCacheValueFunction());

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

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}
}
