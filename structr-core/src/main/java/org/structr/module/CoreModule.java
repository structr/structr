/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import org.structr.core.function.AddToGroupFunction;
import org.structr.core.function.AncestorTypesFunction;
import org.structr.core.function.AndFunction;
import org.structr.core.function.AppendFunction;
import org.structr.core.function.Base64DecodeFunction;
import org.structr.core.function.Base64EncodeFunction;
import org.structr.core.function.CallFunction;
import org.structr.core.function.CallPrivilegedFunction;
import org.structr.core.function.CapitalizeFunction;
import org.structr.core.function.CeilFunction;
import org.structr.core.function.ChangelogFunction;
import org.structr.core.function.CleanFunction;
import org.structr.core.function.CoalesceFunction;
import org.structr.core.function.CoalesceObjectsFunction;
import org.structr.core.function.ComplementFunction;
import org.structr.core.function.ConcatFunction;
import org.structr.core.function.ConfigFunction;
import org.structr.core.function.ContainsFunction;
import org.structr.core.function.CopyPermissionsFunction;
import org.structr.core.function.CreateFunction;
import org.structr.core.function.CreateRelationshipFunction;
import org.structr.core.function.CypherFunction;
import org.structr.core.function.DateFormatFunction;
import org.structr.core.function.DebugFunction;
import org.structr.core.function.DeleteFunction;
import org.structr.core.function.DisableCascadingDeleteFunction;
import org.structr.core.function.DisableNotificationsFunction;
import org.structr.core.function.DivFunction;
import org.structr.core.function.DoubleSumFunction;
import org.structr.core.function.EmptyFunction;
import org.structr.core.function.EnableCascadingDeleteFunction;
import org.structr.core.function.EnableNotificationsFunction;
import org.structr.core.function.EndsWithFunction;
import org.structr.core.function.EnumInfoFunction;
import org.structr.core.function.EqualFunction;
import org.structr.core.function.ErrorFunction;
import org.structr.core.function.EscapeJavascriptFunction;
import org.structr.core.function.EscapeJsonFunction;
import org.structr.core.function.EvaluateScriptFunction;
import org.structr.core.function.ExecBinaryFunction;
import org.structr.core.function.ExecFunction;
import org.structr.core.function.ExtractFunction;
import org.structr.core.function.FindFunction;
import org.structr.core.function.FindRelationshipFunction;
import org.structr.core.function.FirstFunction;
import org.structr.core.function.FloorFunction;
import org.structr.core.function.Functions;
import org.structr.core.function.GeocodeFunction;
import org.structr.core.function.GetCounterFunction;
import org.structr.core.function.GetFunction;
import org.structr.core.function.GetIncomingRelationshipsFunction;
import org.structr.core.function.GetOrCreateFunction;
import org.structr.core.function.GetOrNullFunction;
import org.structr.core.function.GetOutgoingRelationshipsFunction;
import org.structr.core.function.GetRelationshipTypesFunction;
import org.structr.core.function.GetRelationshipsFunction;
import org.structr.core.function.GrantFunction;
import org.structr.core.function.GtFunction;
import org.structr.core.function.GteFunction;
import org.structr.core.function.HasIncomingRelationshipFunction;
import org.structr.core.function.HasOutgoingRelationshipFunction;
import org.structr.core.function.HasRelationshipFunction;
import org.structr.core.function.IncCounterFunction;
import org.structr.core.function.IncomingFunction;
import org.structr.core.function.IndexOfFunction;
import org.structr.core.function.InheritingTypesFunction;
import org.structr.core.function.InstantiateFunction;
import org.structr.core.function.IntFunction;
import org.structr.core.function.IntSumFunction;
import org.structr.core.function.IsAllowedFunction;
import org.structr.core.function.IsCollectionFunction;
import org.structr.core.function.IsEntityFunction;
import org.structr.core.function.IsInGroupFunction;
import org.structr.core.function.JdbcFunction;
import org.structr.core.function.JoinFunction;
import org.structr.core.function.KeysFunction;
import org.structr.core.function.LastFunction;
import org.structr.core.function.LengthFunction;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.function.LogFunction;
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
import org.structr.core.function.PrivilegedFindFunction;
import org.structr.core.function.PropertyInfoFunction;
import org.structr.core.function.QuotFunction;
import org.structr.core.function.RInterpreterFunction;
import org.structr.core.function.RandomFunction;
import org.structr.core.function.ReadFunction;
import org.structr.core.function.RemoveFromGroupFunction;
import org.structr.core.function.ReplaceFunction;
import org.structr.core.function.ResetCounterFunction;
import org.structr.core.function.RetrieveFunction;
import org.structr.core.function.RevokeFunction;
import org.structr.core.function.RintFunction;
import org.structr.core.function.RoundFunction;
import org.structr.core.function.SearchFunction;
import org.structr.core.function.ServerLogFunction;
import org.structr.core.function.SetFunction;
import org.structr.core.function.SetLocaleFunction;
import org.structr.core.function.SetPrivilegedFunction;
import org.structr.core.function.SizeFunction;
import org.structr.core.function.SortFunction;
import org.structr.core.function.SplitFunction;
import org.structr.core.function.SplitRegexFunction;
import org.structr.core.function.StartsWithFunction;
import org.structr.core.function.StoreFunction;
import org.structr.core.function.StrReplaceFunction;
import org.structr.core.function.SubstringFunction;
import org.structr.core.function.SubtFunction;
import org.structr.core.function.TemplateFunction;
import org.structr.core.function.TimerFunction;
import org.structr.core.function.TitleizeFunction;
import org.structr.core.function.ToDateFunction;
import org.structr.core.function.TrimFunction;
import org.structr.core.function.TypeInfoFunction;
import org.structr.core.function.UnlockReadonlyPropertiesFunction;
import org.structr.core.function.UnlockSystemPropertiesFunction;
import org.structr.core.function.UnwindFunction;
import org.structr.core.function.UpperFunction;
import org.structr.core.function.UrlEncodeFunction;
import org.structr.core.function.UserChangelogFunction;
import org.structr.core.function.ValuesFunction;
import org.structr.core.function.WeekDaysFunction;
import org.structr.core.function.WriteFunction;
import org.structr.core.function.XPathFunction;
import org.structr.core.function.XmlFunction;
import org.structr.schema.action.Actions;

/**
 *
 */
public class CoreModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		final boolean licensed = licenseManager == null || licenseManager.isEdition(LicenseManager.Basic);

		// Basic Edition
		Functions.put(licensed, LicenseManager.Basic, new ErrorFunction());
		Functions.put(licensed, LicenseManager.Basic, new ConfigFunction());
		Functions.put(licensed, LicenseManager.Basic, new ChangelogFunction());
		Functions.put(licensed, LicenseManager.Basic, new UserChangelogFunction());
		Functions.put(licensed, LicenseManager.Basic, new ServerLogFunction());

		Functions.put(licensed, LicenseManager.Basic, new GrantFunction());
		Functions.put(licensed, LicenseManager.Basic, new RevokeFunction());
		Functions.put(licensed, LicenseManager.Basic, new IsAllowedFunction());
		Functions.put(licensed, LicenseManager.Basic, new AddToGroupFunction());
		Functions.put(licensed, LicenseManager.Basic, new RemoveFromGroupFunction());
		Functions.put(licensed, LicenseManager.Basic, new IsInGroupFunction());

		Functions.put(licensed, LicenseManager.Basic, new LocalizeFunction());

		Functions.put(licensed, LicenseManager.Basic, new CallFunction());
		Functions.put(licensed, LicenseManager.Basic, new CallPrivilegedFunction());
		Functions.put(licensed, LicenseManager.Basic, new ExecFunction());
		Functions.put(licensed, LicenseManager.Basic, new ExecBinaryFunction());

		Functions.put(licensed, LicenseManager.Basic, new UnlockReadonlyPropertiesFunction());
		Functions.put(licensed, LicenseManager.Basic, new UnlockSystemPropertiesFunction());
		Functions.put(licensed, LicenseManager.Basic, new SetPrivilegedFunction());
		Functions.put(licensed, LicenseManager.Basic, new PrivilegedFindFunction());

		Functions.put(licensed, LicenseManager.Basic, new ReadFunction());
		Functions.put(licensed, LicenseManager.Basic, new WriteFunction());
		Functions.put(licensed, LicenseManager.Basic, new AppendFunction());
		Functions.put(licensed, LicenseManager.Basic, new XmlFunction());
		Functions.put(licensed, LicenseManager.Basic, new XPathFunction());
		Functions.put(licensed, LicenseManager.Basic, new GeocodeFunction());

		Functions.put(licensed, LicenseManager.Basic, new InstantiateFunction());

		Functions.put(licensed, LicenseManager.Basic, new PropertyInfoFunction());
		Functions.put(licensed, LicenseManager.Basic, new TypeInfoFunction());
		Functions.put(licensed, LicenseManager.Basic, new EnumInfoFunction());
		Functions.put(licensed, LicenseManager.Basic, new DisableCascadingDeleteFunction());
		Functions.put(licensed, LicenseManager.Basic, new EnableCascadingDeleteFunction());
		Functions.put(licensed, LicenseManager.Basic, new DisableNotificationsFunction());
		Functions.put(licensed, LicenseManager.Basic, new EnableNotificationsFunction());
		Functions.put(licensed, LicenseManager.Basic, new RInterpreterFunction());
		Functions.put(licensed, LicenseManager.Basic, new EvaluateScriptFunction());
		Functions.put(licensed, LicenseManager.Basic, new AncestorTypesFunction());
		Functions.put(licensed, LicenseManager.Basic, new InheritingTypesFunction());

		Functions.put(licensed, LicenseManager.Basic, new TemplateFunction());
		Functions.put(licensed, LicenseManager.Basic, new JdbcFunction());

		Functions.put(licensed, LicenseManager.Enterprise, new GetRelationshipTypesFunction());

		// Community Edition
		Functions.put(true, LicenseManager.Community, new CypherFunction());
		Functions.put(true, LicenseManager.Community, new MD5Function());
		Functions.put(true, LicenseManager.Community, new UpperFunction());
		Functions.put(true, LicenseManager.Community, new LowerFunction());
		Functions.put(true, LicenseManager.Community, new JoinFunction());
		Functions.put(true, LicenseManager.Community, new ConcatFunction());
		Functions.put(true, LicenseManager.Community, new SplitFunction());
		Functions.put(true, LicenseManager.Community, new SplitRegexFunction());
		Functions.put(true, LicenseManager.Community, new AbbrFunction());
		Functions.put(true, LicenseManager.Community, new CapitalizeFunction());
		Functions.put(true, LicenseManager.Community, new TitleizeFunction());
		Functions.put(true, LicenseManager.Community, new NumFunction());
		Functions.put(true, LicenseManager.Community, new IntFunction());
		Functions.put(true, LicenseManager.Community, new RandomFunction());
		Functions.put(true, LicenseManager.Community, new RintFunction());
		Functions.put(true, LicenseManager.Community, new IndexOfFunction());
		Functions.put(true, LicenseManager.Community, new ContainsFunction());
		Functions.put(true, LicenseManager.Community, new CopyPermissionsFunction());
		Functions.put(true, LicenseManager.Community, new SubstringFunction());
		Functions.put(true, LicenseManager.Community, new LengthFunction());
		Functions.put(true, LicenseManager.Community, new ReplaceFunction());
		Functions.put(true, LicenseManager.Community, new TrimFunction());
		Functions.put(true, LicenseManager.Community, new CleanFunction());
		Functions.put(true, LicenseManager.Community, new CoalesceFunction());
		Functions.put(true, LicenseManager.Community, new CoalesceObjectsFunction());
		Functions.put(true, LicenseManager.Community, new UrlEncodeFunction());
		Functions.put(true, LicenseManager.Community, new EscapeJavascriptFunction());
		Functions.put(true, LicenseManager.Community, new EscapeJsonFunction());
		Functions.put(true, LicenseManager.Community, new EmptyFunction());
		Functions.put(true, LicenseManager.Community, new EqualFunction());
		Functions.put(true, LicenseManager.Community, new AddFunction());
		Functions.put(true, LicenseManager.Community, new DoubleSumFunction());
		Functions.put(true, LicenseManager.Community, new IntSumFunction());
		Functions.put(true, LicenseManager.Community, new IsCollectionFunction());
		Functions.put(true, LicenseManager.Community, new IsEntityFunction());
		Functions.put(true, LicenseManager.Community, new ExtractFunction());
		Functions.put(true, LicenseManager.Community, new MergeFunction());
		Functions.put(true, LicenseManager.Community, new MergeUniqueFunction());
		Functions.put(true, LicenseManager.Community, new ComplementFunction());
		Functions.put(true, LicenseManager.Community, new UnwindFunction());
		Functions.put(true, LicenseManager.Community, new SortFunction());
		Functions.put(true, LicenseManager.Community, new LtFunction());
		Functions.put(true, LicenseManager.Community, new GtFunction());
		Functions.put(true, LicenseManager.Community, new LteFunction());
		Functions.put(true, LicenseManager.Community, new GteFunction());
		Functions.put(true, LicenseManager.Community, new SubtFunction());
		Functions.put(true, LicenseManager.Community, new MultFunction());
		Functions.put(true, LicenseManager.Community, new QuotFunction());
		Functions.put(true, LicenseManager.Community, new DivFunction());
		Functions.put(true, LicenseManager.Community, new ModFunction());
		Functions.put(true, LicenseManager.Community, new FloorFunction());
		Functions.put(true, LicenseManager.Community, new CeilFunction());
		Functions.put(true, LicenseManager.Community, new RoundFunction());
		Functions.put(true, LicenseManager.Community, new MaxFunction());
		Functions.put(true, LicenseManager.Community, new MinFunction());
		Functions.put(true, LicenseManager.Community, new SetLocaleFunction());
		Functions.put(true, LicenseManager.Community, new DateFormatFunction());
		Functions.put(true, LicenseManager.Community, new ParseDateFunction());
		Functions.put(true, LicenseManager.Community, new WeekDaysFunction());
		Functions.put(true, LicenseManager.Community, new ToDateFunction());
		Functions.put(true, LicenseManager.Community, new NumberFormatFunction());
		Functions.put(true, LicenseManager.Community, new ParseNumberFunction());
		Functions.put(true, LicenseManager.Community, new NotFunction());
		Functions.put(true, LicenseManager.Community, new AndFunction());
		Functions.put(true, LicenseManager.Community, new OrFunction());
		Functions.put(true, LicenseManager.Community, new GetFunction());
		Functions.put(true, LicenseManager.Community, new GetOrNullFunction());
		Functions.put(true, LicenseManager.Community, new SizeFunction());
		Functions.put(true, LicenseManager.Community, new FirstFunction());
		Functions.put(true, LicenseManager.Community, new LastFunction());
		Functions.put(true, LicenseManager.Community, new NthFunction());
		Functions.put(true, LicenseManager.Community, new GetCounterFunction());
		Functions.put(true, LicenseManager.Community, new IncCounterFunction());
		Functions.put(true, LicenseManager.Community, new ResetCounterFunction());
		Functions.put(true, LicenseManager.Community, new MergePropertiesFunction());
		Functions.put(true, LicenseManager.Community, new KeysFunction());
		Functions.put(true, LicenseManager.Community, new ValuesFunction());
		Functions.put(true, LicenseManager.Community, new TimerFunction());
		Functions.put(true, LicenseManager.Community, new StrReplaceFunction());
		Functions.put(true, LicenseManager.Community, new SearchFunction());
		Functions.put(true, LicenseManager.Community, new IncomingFunction());
		Functions.put(true, LicenseManager.Community, new OutgoingFunction());
		Functions.put(true, LicenseManager.Community, new HasRelationshipFunction());
		Functions.put(true, LicenseManager.Community, new HasOutgoingRelationshipFunction());
		Functions.put(true, LicenseManager.Community, new HasIncomingRelationshipFunction());
		Functions.put(true, LicenseManager.Community, new GetRelationshipsFunction());
		Functions.put(true, LicenseManager.Community, new GetOutgoingRelationshipsFunction());
		Functions.put(true, LicenseManager.Community, new GetIncomingRelationshipsFunction());
		Functions.put(true, LicenseManager.Community, new RetrieveFunction());
		Functions.put(true, LicenseManager.Community, new StoreFunction());
		Functions.put(true, LicenseManager.Community, new PrintFunction());
		Functions.put(true, LicenseManager.Community, new LogFunction());
		Functions.put(true, LicenseManager.Community, new DebugFunction());
		Functions.put(true, LicenseManager.Community, new FindFunction());
		Functions.put(true, LicenseManager.Community, new GetOrCreateFunction());
		Functions.put(true, LicenseManager.Community, new FindRelationshipFunction());
		Functions.put(true, LicenseManager.Community, new StartsWithFunction());
		Functions.put(true, LicenseManager.Community, new EndsWithFunction());
		Functions.put(true, LicenseManager.Community, new Base64EncodeFunction());
		Functions.put(true, LicenseManager.Community, new Base64DecodeFunction());

		// ----- BEGIN functions with side effects -----
		Functions.put(true, LicenseManager.Community, new SetFunction());
		Functions.put(true, LicenseManager.Community, new CreateFunction());
		Functions.put(true, LicenseManager.Community, new DeleteFunction());
		Functions.put(true, LicenseManager.Community, new CreateRelationshipFunction());
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
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}
}
