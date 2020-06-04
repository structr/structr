/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.Set;
import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.AbbrFunction;
import org.structr.core.function.AddFunction;
import org.structr.core.function.AndFunction;
import org.structr.core.function.Base64DecodeFunction;
import org.structr.core.function.Base64EncodeFunction;
import org.structr.core.function.CapitalizeFunction;
import org.structr.core.function.CeilFunction;
import org.structr.core.function.CleanFunction;
import org.structr.core.function.CoalesceFunction;
import org.structr.core.function.CoalesceObjectsFunction;
import org.structr.core.function.ComplementFunction;
import org.structr.core.function.ConcatFunction;
import org.structr.core.function.ContainsFunction;
import org.structr.core.function.CopyPermissionsFunction;
import org.structr.core.function.CreateFunction;
import org.structr.core.function.CreateRelationshipFunction;
import org.structr.core.function.CypherFunction;
import org.structr.core.function.DateAddFunction;
import org.structr.core.function.DateFormatFunction;
import org.structr.core.function.DebugFunction;
import org.structr.core.function.DeleteCacheValueFunction;
import org.structr.core.function.DeleteFunction;
import org.structr.core.function.DivFunction;
import org.structr.core.function.DoubleSumFunction;
import org.structr.core.function.EmptyFunction;
import org.structr.core.function.EndsWithFunction;
import org.structr.core.function.EqualFunction;
import org.structr.core.function.EscapeJavascriptFunction;
import org.structr.core.function.EscapeJsonFunction;
import org.structr.core.function.ExtractFunction;
import org.structr.core.function.FindFunction;
import org.structr.core.function.FindRelationshipFunction;
import org.structr.core.function.FirstFunction;
import org.structr.core.function.FloorFunction;
import org.structr.core.function.Functions;
import org.structr.core.function.GetCacheValueFunction;
import org.structr.core.function.GetCounterFunction;
import org.structr.core.function.GetFunction;
import org.structr.core.function.GetIncomingRelationshipsFunction;
import org.structr.core.function.GetOrCreateFunction;
import org.structr.core.function.GetOrNullFunction;
import org.structr.core.function.GetOutgoingRelationshipsFunction;
import org.structr.core.function.GetRelationshipsFunction;
import org.structr.core.function.GtFunction;
import org.structr.core.function.GteFunction;
import org.structr.core.function.HasCacheValueFunction;
import org.structr.core.function.HasIncomingRelationshipFunction;
import org.structr.core.function.HasOutgoingRelationshipFunction;
import org.structr.core.function.HasRelationshipFunction;
import org.structr.core.function.IncCounterFunction;
import org.structr.core.function.IncomingFunction;
import org.structr.core.function.IndexOfFunction;
import org.structr.core.function.IntFunction;
import org.structr.core.function.IntSumFunction;
import org.structr.core.function.IsCollectionFunction;
import org.structr.core.function.IsEntityFunction;
import org.structr.core.function.JoinFunction;
import org.structr.core.function.KeysFunction;
import org.structr.core.function.LastFunction;
import org.structr.core.function.LengthFunction;
import org.structr.core.function.LogFunction;
import org.structr.core.function.LongFunction;
import org.structr.core.function.LowerFunction;
import org.structr.core.function.LtFunction;
import org.structr.core.function.LteFunction;
import org.structr.core.function.MD5Function;
import org.structr.core.function.MaxFunction;
import org.structr.core.function.MergeFunction;
import org.structr.core.function.MergePropertiesFunction;
import org.structr.core.function.MergeUniqueFunction;
import org.structr.core.function.MinFunction;
import org.structr.core.function.ModFunction;
import org.structr.core.function.MultFunction;
import org.structr.core.function.NotFunction;
import org.structr.core.function.NthFunction;
import org.structr.core.function.NumFunction;
import org.structr.core.function.NumberFormatFunction;
import org.structr.core.function.OrFunction;
import org.structr.core.function.OutgoingFunction;
import org.structr.core.function.ParseDateFunction;
import org.structr.core.function.ParseNumberFunction;
import org.structr.core.function.PrintFunction;
import org.structr.core.function.QuotFunction;
import org.structr.core.function.RandomFunction;
import org.structr.core.function.RemoteCypherFunction;
import org.structr.core.function.ReplaceFunction;
import org.structr.core.function.ResetCounterFunction;
import org.structr.core.function.RetrieveFunction;
import org.structr.core.function.RintFunction;
import org.structr.core.function.RoundFunction;
import org.structr.core.function.SearchFunction;
import org.structr.core.function.SetFunction;
import org.structr.core.function.SetLocaleFunction;
import org.structr.core.function.SizeFunction;
import org.structr.core.function.SortFunction;
import org.structr.core.function.SplitFunction;
import org.structr.core.function.SplitRegexFunction;
import org.structr.core.function.StartsWithFunction;
import org.structr.core.function.StoreFunction;
import org.structr.core.function.StrReplaceFunction;
import org.structr.core.function.SubstringFunction;
import org.structr.core.function.SubtFunction;
import org.structr.core.function.TimerFunction;
import org.structr.core.function.TitleizeFunction;
import org.structr.core.function.ToDateFunction;
import org.structr.core.function.TrimFunction;
import org.structr.core.function.UnwindFunction;
import org.structr.core.function.UpperFunction;
import org.structr.core.function.UrlEncodeFunction;
import org.structr.core.function.ValuesFunction;
import org.structr.core.function.WeekDaysFunction;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

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
		Functions.put(licenseManager, new DebugFunction());
		Functions.put(licenseManager, new FindFunction());
		Functions.put(licenseManager, new GetOrCreateFunction());
		Functions.put(licenseManager, new FindRelationshipFunction());
		Functions.put(licenseManager, new StartsWithFunction());
		Functions.put(licenseManager, new EndsWithFunction());
		Functions.put(licenseManager, new Base64EncodeFunction());
		Functions.put(licenseManager, new Base64DecodeFunction());

		Functions.put(licenseManager, new HasCacheValueFunction());
		Functions.put(licenseManager, new GetCacheValueFunction());
		Functions.put(licenseManager, new DeleteCacheValueFunction());

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
