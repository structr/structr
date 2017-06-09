/**
 * Copyright (C) 2010-2017 Structr GmbH
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
import org.structr.core.function.AncestorTypesFunction;
import org.structr.core.function.AndFunction;
import org.structr.core.function.AppendFunction;
import org.structr.core.function.CallFunction;
import org.structr.core.function.CallPrivilegedFunction;
import org.structr.core.function.CapitalizeFunction;
import org.structr.core.function.CeilFunction;
import org.structr.core.function.ChangelogFunction;
import org.structr.core.function.CleanFunction;
import org.structr.core.function.ComplementFunction;
import org.structr.core.function.ConcatFunction;
import org.structr.core.function.ConfigFunction;
import org.structr.core.function.ContainsFunction;
import org.structr.core.function.CopyPermissionsFunction;
import org.structr.core.function.CreateFunction;
import org.structr.core.function.CreateRelationshipFunction;
import org.structr.core.function.CypherFunction;
import org.structr.core.function.DateFormatFunction;
import org.structr.core.function.DeleteFunction;
import org.structr.core.function.DisableNotificationsFunction;
import org.structr.core.function.DivFunction;
import org.structr.core.function.DoubleSumFunction;
import org.structr.core.function.EmptyFunction;
import org.structr.core.function.EnableNotificationsFunction;
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
import org.structr.core.function.GetOrNullFunction;
import org.structr.core.function.GetOutgoingRelationshipsFunction;
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
import org.structr.core.function.PrintFunction;
import org.structr.core.function.PrivilegedFindFunction;
import org.structr.core.function.PropertyInfoFunction;
import org.structr.core.function.QuotFunction;
import org.structr.core.function.RInterpreterFunction;
import org.structr.core.function.RandomFunction;
import org.structr.core.function.ReadFunction;
import org.structr.core.function.ReplaceFunction;
import org.structr.core.function.ResetCounterFunction;
import org.structr.core.function.RetrieveFunction;
import org.structr.core.function.RevokeFunction;
import org.structr.core.function.RintFunction;
import org.structr.core.function.RoundFunction;
import org.structr.core.function.SearchFunction;
import org.structr.core.function.ServerLogFunction;
import org.structr.core.function.SetFunction;
import org.structr.core.function.SetPrivilegedFunction;
import org.structr.core.function.SizeFunction;
import org.structr.core.function.SortFunction;
import org.structr.core.function.SplitFunction;
import org.structr.core.function.SplitRegexFunction;
import org.structr.core.function.StoreFunction;
import org.structr.core.function.StrReplaceFunction;
import org.structr.core.function.SubstringFunction;
import org.structr.core.function.SubtFunction;
import org.structr.core.function.TemplateFunction;
import org.structr.core.function.TimerFunction;
import org.structr.core.function.TitleizeFunction;
import org.structr.core.function.ToDateFunction;
import org.structr.core.function.TypeInfoFunction;
import org.structr.core.function.UnlockReadonlyPropertiesFunction;
import org.structr.core.function.UnlockSystemPropertiesFunction;
import org.structr.core.function.UnwindFunction;
import org.structr.core.function.UpperFunction;
import org.structr.core.function.UrlEncodeFunction;
import org.structr.core.function.ValuesFunction;
import org.structr.core.function.WriteFunction;
import org.structr.core.function.XPathFunction;
import org.structr.core.function.XmlFunction;
import org.structr.schema.action.Actions;

/**
 *
 * @author Christian Morgner
 */
public class CoreModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		if (licenseManager == null || licenseManager.isEdition(LicenseManager.Basic)) {

			Functions.functions.put("error", new ErrorFunction());
			Functions.functions.put("config", new ConfigFunction());
			Functions.functions.put("changelog", new ChangelogFunction());
			Functions.functions.put("serverlog", new ServerLogFunction());

			Functions.functions.put("grant", new GrantFunction());
			Functions.functions.put("revoke", new RevokeFunction());
			Functions.functions.put("is_allowed", new IsAllowedFunction());

			Functions.functions.put("localize", new LocalizeFunction());

			Functions.functions.put("call", new CallFunction());
			Functions.functions.put("call_privileged", new CallPrivilegedFunction());
			Functions.functions.put("exec", new ExecFunction());
			Functions.functions.put("exec_binary", new ExecBinaryFunction());

			Functions.functions.put("unlock_readonly_properties_once", new UnlockReadonlyPropertiesFunction());
			Functions.functions.put("unlock_system_properties_once", new UnlockSystemPropertiesFunction());
			Functions.functions.put("set_privileged", new SetPrivilegedFunction());
			Functions.functions.put("find_privileged", new PrivilegedFindFunction());

			Functions.functions.put("read", new ReadFunction());
			Functions.functions.put("write", new WriteFunction());
			Functions.functions.put("append", new AppendFunction());
			Functions.functions.put("xml", new XmlFunction());
			Functions.functions.put("xpath", new XPathFunction());
			Functions.functions.put("geocode", new GeocodeFunction());

			Functions.functions.put("instantiate", new InstantiateFunction());

			Functions.functions.put("property_info", new PropertyInfoFunction());
			Functions.functions.put("type_info", new TypeInfoFunction());
			Functions.functions.put("enum_info", new EnumInfoFunction());
			Functions.functions.put("disable_notifications", new DisableNotificationsFunction());
			Functions.functions.put("enable_notifications", new EnableNotificationsFunction());
			Functions.functions.put("r", new RInterpreterFunction());
			Functions.functions.put("evaluate_script", new EvaluateScriptFunction());
			Functions.functions.put("ancestor_types", new AncestorTypesFunction());
			Functions.functions.put("inheriting_types", new InheritingTypesFunction());

			Functions.functions.put("template", new TemplateFunction());
		}

		Functions.functions.put("cypher", new CypherFunction());
		Functions.functions.put("md5", new MD5Function());
		Functions.functions.put("upper", new UpperFunction());
		Functions.functions.put("lower", new LowerFunction());
		Functions.functions.put("join", new JoinFunction());
		Functions.functions.put("concat", new ConcatFunction());
		Functions.functions.put("split", new SplitFunction());
		Functions.functions.put("split_regex", new SplitRegexFunction());
		Functions.functions.put("abbr", new AbbrFunction());
		Functions.functions.put("capitalize", new CapitalizeFunction());
		Functions.functions.put("titleize", new TitleizeFunction());
		Functions.functions.put("num", new NumFunction());
		Functions.functions.put("int", new IntFunction());
		Functions.functions.put("random", new RandomFunction());
		Functions.functions.put("rint", new RintFunction());
		Functions.functions.put("index_of", new IndexOfFunction());
		Functions.functions.put("contains", new ContainsFunction());
		Functions.functions.put("copy_permissions", new CopyPermissionsFunction());
		Functions.functions.put("substring", new SubstringFunction());
		Functions.functions.put("length", new LengthFunction());
		Functions.functions.put("replace", new ReplaceFunction());
		Functions.functions.put("clean", new CleanFunction());
		Functions.functions.put("urlencode", new UrlEncodeFunction());
		Functions.functions.put("escape_javascript", new EscapeJavascriptFunction());
		Functions.functions.put("escape_json", new EscapeJsonFunction());
		Functions.functions.put("empty", new EmptyFunction());
		Functions.functions.put("equal", new EqualFunction());
		Functions.functions.put("eq", new EqualFunction());
		Functions.functions.put("add", new AddFunction());
		Functions.functions.put("double_sum", new DoubleSumFunction());
		Functions.functions.put("int_sum", new IntSumFunction());
		Functions.functions.put("is_collection", new IsCollectionFunction());
		Functions.functions.put("is_entity", new IsEntityFunction());
		Functions.functions.put("extract", new ExtractFunction());
		Functions.functions.put("merge", new MergeFunction());
		Functions.functions.put("merge_unique", new MergeUniqueFunction());
		Functions.functions.put("complement", new ComplementFunction());
		Functions.functions.put("unwind", new UnwindFunction());
		Functions.functions.put("sort", new SortFunction());
		Functions.functions.put("lt", new LtFunction());
		Functions.functions.put("gt", new GtFunction());
		Functions.functions.put("lte", new LteFunction());
		Functions.functions.put("gte", new GteFunction());
		Functions.functions.put("subt", new SubtFunction());
		Functions.functions.put("mult", new MultFunction());
		Functions.functions.put("quot", new QuotFunction());
		Functions.functions.put("div", new DivFunction());
		Functions.functions.put("mod", new ModFunction());
		Functions.functions.put("floor", new FloorFunction());
		Functions.functions.put("ceil", new CeilFunction());
		Functions.functions.put("round", new RoundFunction());
		Functions.functions.put("max", new MaxFunction());
		Functions.functions.put("min", new MinFunction());
		Functions.functions.put("date_format", new DateFormatFunction());
		Functions.functions.put("parse_date", new ParseDateFunction());
		Functions.functions.put("to_date", new ToDateFunction());
		Functions.functions.put("number_format", new NumberFormatFunction());
		Functions.functions.put("not", new NotFunction());
		Functions.functions.put("and", new AndFunction());
		Functions.functions.put("or", new OrFunction());
		Functions.functions.put("get", new GetFunction());
		Functions.functions.put("get_or_null", new GetOrNullFunction());
		Functions.functions.put("size", new SizeFunction());
		Functions.functions.put("first", new FirstFunction());
		Functions.functions.put("last", new LastFunction());
		Functions.functions.put("nth", new NthFunction());
		Functions.functions.put("get_counter", new GetCounterFunction());
		Functions.functions.put("inc_counter", new IncCounterFunction());
		Functions.functions.put("reset_counter", new ResetCounterFunction());
		Functions.functions.put("merge_properties", new MergePropertiesFunction());
		Functions.functions.put("keys", new KeysFunction());
		Functions.functions.put("values", new ValuesFunction());
		Functions.functions.put("timer", new TimerFunction());
		Functions.functions.put("str_replace", new StrReplaceFunction());
		Functions.functions.put("search", new SearchFunction());
		Functions.functions.put("incoming", new IncomingFunction());
		Functions.functions.put("outgoing", new OutgoingFunction());
		Functions.functions.put("has_relationship", new HasRelationshipFunction());
		Functions.functions.put("has_outgoing_relationship", new HasOutgoingRelationshipFunction());
		Functions.functions.put("has_incoming_relationship", new HasIncomingRelationshipFunction());
		Functions.functions.put("get_relationships", new GetRelationshipsFunction());
		Functions.functions.put("get_outgoing_relationships", new GetOutgoingRelationshipsFunction());
		Functions.functions.put("get_incoming_relationships", new GetIncomingRelationshipsFunction());
		Functions.functions.put("retrieve", new RetrieveFunction());
		Functions.functions.put("store", new StoreFunction());
		Functions.functions.put("print", new PrintFunction());
		Functions.functions.put("log", new LogFunction());
		Functions.functions.put("find", new FindFunction());
		Functions.functions.put("find_relationship", new FindRelationshipFunction());

		// ----- BEGIN functions with side effects -----
		Functions.functions.put("set", new SetFunction());
		Functions.functions.put("create", new CreateFunction());
		Functions.functions.put("delete", new DeleteFunction());
		Functions.functions.put("create_relationship", new CreateRelationshipFunction());
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
