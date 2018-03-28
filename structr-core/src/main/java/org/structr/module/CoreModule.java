/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import org.structr.core.function.ValuesFunction;
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
		Functions.put(licensed, LicenseManager.Basic, "error", new ErrorFunction());
		Functions.put(licensed, LicenseManager.Basic, "config", new ConfigFunction());
		Functions.put(licensed, LicenseManager.Basic, "changelog", new ChangelogFunction());
		Functions.put(licensed, LicenseManager.Basic, "serverlog", new ServerLogFunction());

		Functions.put(licensed, LicenseManager.Basic, "grant", new GrantFunction());
		Functions.put(licensed, LicenseManager.Basic, "revoke", new RevokeFunction());
		Functions.put(licensed, LicenseManager.Basic, "is_allowed", new IsAllowedFunction());
		Functions.put(licensed, LicenseManager.Basic, "add_to_group", new AddToGroupFunction());
		Functions.put(licensed, LicenseManager.Basic, "remove_from_group", new RemoveFromGroupFunction());
		Functions.put(licensed, LicenseManager.Basic, "is_in_group", new IsInGroupFunction());

		Functions.put(licensed, LicenseManager.Basic, "localize", new LocalizeFunction());

		Functions.put(licensed, LicenseManager.Basic, "call", new CallFunction());
		Functions.put(licensed, LicenseManager.Basic, "call_privileged", new CallPrivilegedFunction());
		Functions.put(licensed, LicenseManager.Basic, "exec", new ExecFunction());
		Functions.put(licensed, LicenseManager.Basic, "exec_binary", new ExecBinaryFunction());

		Functions.put(licensed, LicenseManager.Basic, "unlock_readonly_properties_once", new UnlockReadonlyPropertiesFunction());
		Functions.put(licensed, LicenseManager.Basic, "unlock_system_properties_once", new UnlockSystemPropertiesFunction());
		Functions.put(licensed, LicenseManager.Basic, "set_privileged", new SetPrivilegedFunction());
		Functions.put(licensed, LicenseManager.Basic, "find_privileged", new PrivilegedFindFunction());

		Functions.put(licensed, LicenseManager.Basic, "read", new ReadFunction());
		Functions.put(licensed, LicenseManager.Basic, "write", new WriteFunction());
		Functions.put(licensed, LicenseManager.Basic, "append", new AppendFunction());
		Functions.put(licensed, LicenseManager.Basic, "xml", new XmlFunction());
		Functions.put(licensed, LicenseManager.Basic, "xpath", new XPathFunction());
		Functions.put(licensed, LicenseManager.Basic, "geocode", new GeocodeFunction());

		Functions.put(licensed, LicenseManager.Basic, "instantiate", new InstantiateFunction());

		Functions.put(licensed, LicenseManager.Basic, "property_info", new PropertyInfoFunction());
		Functions.put(licensed, LicenseManager.Basic, "type_info", new TypeInfoFunction());
		Functions.put(licensed, LicenseManager.Basic, "enum_info", new EnumInfoFunction());
		Functions.put(licensed, LicenseManager.Basic, "disable_notifications", new DisableNotificationsFunction());
		Functions.put(licensed, LicenseManager.Basic, "enable_notifications", new EnableNotificationsFunction());
		Functions.put(licensed, LicenseManager.Basic, "r", new RInterpreterFunction());
		Functions.put(licensed, LicenseManager.Basic, "evaluate_script", new EvaluateScriptFunction());
		Functions.put(licensed, LicenseManager.Basic, "ancestor_types", new AncestorTypesFunction());
		Functions.put(licensed, LicenseManager.Basic, "inheriting_types", new InheritingTypesFunction());

		Functions.put(licensed, LicenseManager.Basic, "template", new TemplateFunction());
		Functions.put(licensed, LicenseManager.Basic, "jdbc", new JdbcFunction());

		// Community Edition
		Functions.put(true, LicenseManager.Community, "cypher", new CypherFunction());
		Functions.put(true, LicenseManager.Community, "md5", new MD5Function());
		Functions.put(true, LicenseManager.Community, "upper", new UpperFunction());
		Functions.put(true, LicenseManager.Community, "lower", new LowerFunction());
		Functions.put(true, LicenseManager.Community, "join", new JoinFunction());
		Functions.put(true, LicenseManager.Community, "concat", new ConcatFunction());
		Functions.put(true, LicenseManager.Community, "split", new SplitFunction());
		Functions.put(true, LicenseManager.Community, "split_regex", new SplitRegexFunction());
		Functions.put(true, LicenseManager.Community, "abbr", new AbbrFunction());
		Functions.put(true, LicenseManager.Community, "capitalize", new CapitalizeFunction());
		Functions.put(true, LicenseManager.Community, "titleize", new TitleizeFunction());
		Functions.put(true, LicenseManager.Community, "num", new NumFunction());
		Functions.put(true, LicenseManager.Community, "int", new IntFunction());
		Functions.put(true, LicenseManager.Community, "random", new RandomFunction());
		Functions.put(true, LicenseManager.Community, "rint", new RintFunction());
		Functions.put(true, LicenseManager.Community, "index_of", new IndexOfFunction());
		Functions.put(true, LicenseManager.Community, "contains", new ContainsFunction());
		Functions.put(true, LicenseManager.Community, "copy_permissions", new CopyPermissionsFunction());
		Functions.put(true, LicenseManager.Community, "substring", new SubstringFunction());
		Functions.put(true, LicenseManager.Community, "length", new LengthFunction());
		Functions.put(true, LicenseManager.Community, "replace", new ReplaceFunction());
		Functions.put(true, LicenseManager.Community, "trim", new TrimFunction());
		Functions.put(true, LicenseManager.Community, "clean", new CleanFunction());
		Functions.put(true, LicenseManager.Community, "coalesce", new CoalesceFunction());
		Functions.put(true, LicenseManager.Community, "urlencode", new UrlEncodeFunction());
		Functions.put(true, LicenseManager.Community, "escape_javascript", new EscapeJavascriptFunction());
		Functions.put(true, LicenseManager.Community, "escape_json", new EscapeJsonFunction());
		Functions.put(true, LicenseManager.Community, "empty", new EmptyFunction());
		Functions.put(true, LicenseManager.Community, "equal", new EqualFunction());
		Functions.put(true, LicenseManager.Community, "eq", new EqualFunction());
		Functions.put(true, LicenseManager.Community, "add", new AddFunction());
		Functions.put(true, LicenseManager.Community, "double_sum", new DoubleSumFunction());
		Functions.put(true, LicenseManager.Community, "int_sum", new IntSumFunction());
		Functions.put(true, LicenseManager.Community, "is_collection", new IsCollectionFunction());
		Functions.put(true, LicenseManager.Community, "is_entity", new IsEntityFunction());
		Functions.put(true, LicenseManager.Community, "extract", new ExtractFunction());
		Functions.put(true, LicenseManager.Community, "merge", new MergeFunction());
		Functions.put(true, LicenseManager.Community, "merge_unique", new MergeUniqueFunction());
		Functions.put(true, LicenseManager.Community, "complement", new ComplementFunction());
		Functions.put(true, LicenseManager.Community, "unwind", new UnwindFunction());
		Functions.put(true, LicenseManager.Community, "sort", new SortFunction());
		Functions.put(true, LicenseManager.Community, "lt", new LtFunction());
		Functions.put(true, LicenseManager.Community, "gt", new GtFunction());
		Functions.put(true, LicenseManager.Community, "lte", new LteFunction());
		Functions.put(true, LicenseManager.Community, "gte", new GteFunction());
		Functions.put(true, LicenseManager.Community, "subt", new SubtFunction());
		Functions.put(true, LicenseManager.Community, "mult", new MultFunction());
		Functions.put(true, LicenseManager.Community, "quot", new QuotFunction());
		Functions.put(true, LicenseManager.Community, "div", new DivFunction());
		Functions.put(true, LicenseManager.Community, "mod", new ModFunction());
		Functions.put(true, LicenseManager.Community, "floor", new FloorFunction());
		Functions.put(true, LicenseManager.Community, "ceil", new CeilFunction());
		Functions.put(true, LicenseManager.Community, "round", new RoundFunction());
		Functions.put(true, LicenseManager.Community, "max", new MaxFunction());
		Functions.put(true, LicenseManager.Community, "min", new MinFunction());
		Functions.put(true, LicenseManager.Community, "date_format", new DateFormatFunction());
		Functions.put(true, LicenseManager.Community, "parse_date", new ParseDateFunction());
		Functions.put(true, LicenseManager.Community, "to_date", new ToDateFunction());
		Functions.put(true, LicenseManager.Community, "number_format", new NumberFormatFunction());
		Functions.put(true, LicenseManager.Community, "parse_number", new ParseNumberFunction());
		Functions.put(true, LicenseManager.Community, "not", new NotFunction());
		Functions.put(true, LicenseManager.Community, "and", new AndFunction());
		Functions.put(true, LicenseManager.Community, "or", new OrFunction());
		Functions.put(true, LicenseManager.Community, "get", new GetFunction());
		Functions.put(true, LicenseManager.Community, "get_or_null", new GetOrNullFunction());
		Functions.put(true, LicenseManager.Community, "size", new SizeFunction());
		Functions.put(true, LicenseManager.Community, "first", new FirstFunction());
		Functions.put(true, LicenseManager.Community, "last", new LastFunction());
		Functions.put(true, LicenseManager.Community, "nth", new NthFunction());
		Functions.put(true, LicenseManager.Community, "get_counter", new GetCounterFunction());
		Functions.put(true, LicenseManager.Community, "inc_counter", new IncCounterFunction());
		Functions.put(true, LicenseManager.Community, "reset_counter", new ResetCounterFunction());
		Functions.put(true, LicenseManager.Community, "merge_properties", new MergePropertiesFunction());
		Functions.put(true, LicenseManager.Community, "keys", new KeysFunction());
		Functions.put(true, LicenseManager.Community, "values", new ValuesFunction());
		Functions.put(true, LicenseManager.Community, "timer", new TimerFunction());
		Functions.put(true, LicenseManager.Community, "str_replace", new StrReplaceFunction());
		Functions.put(true, LicenseManager.Community, "search", new SearchFunction());
		Functions.put(true, LicenseManager.Community, "incoming", new IncomingFunction());
		Functions.put(true, LicenseManager.Community, "outgoing", new OutgoingFunction());
		Functions.put(true, LicenseManager.Community, "has_relationship", new HasRelationshipFunction());
		Functions.put(true, LicenseManager.Community, "has_outgoing_relationship", new HasOutgoingRelationshipFunction());
		Functions.put(true, LicenseManager.Community, "has_incoming_relationship", new HasIncomingRelationshipFunction());
		Functions.put(true, LicenseManager.Community, "get_relationships", new GetRelationshipsFunction());
		Functions.put(true, LicenseManager.Community, "get_outgoing_relationships", new GetOutgoingRelationshipsFunction());
		Functions.put(true, LicenseManager.Community, "get_incoming_relationships", new GetIncomingRelationshipsFunction());
		Functions.put(true, LicenseManager.Community, "retrieve", new RetrieveFunction());
		Functions.put(true, LicenseManager.Community, "store", new StoreFunction());
		Functions.put(true, LicenseManager.Community, "print", new PrintFunction());
		Functions.put(true, LicenseManager.Community, "log", new LogFunction());
		Functions.put(true, LicenseManager.Community, "find", new FindFunction());
		Functions.put(true, LicenseManager.Community, "get_or_create", new GetOrCreateFunction());
		Functions.put(true, LicenseManager.Community, "find_relationship", new FindRelationshipFunction());
		Functions.put(true, LicenseManager.Community, "starts_with", new StartsWithFunction());
		Functions.put(true, LicenseManager.Community, "ends_with", new EndsWithFunction());
		Functions.put(true, LicenseManager.Community, "base64encode", new Base64EncodeFunction());
		Functions.put(true, LicenseManager.Community, "base64decode", new Base64DecodeFunction());

		// ----- BEGIN functions with side effects -----
		Functions.put(true, LicenseManager.Community, "set", new SetFunction());
		Functions.put(true, LicenseManager.Community, "create", new CreateFunction());
		Functions.put(true, LicenseManager.Community, "delete", new DeleteFunction());
		Functions.put(true, LicenseManager.Community, "create_relationship", new CreateRelationshipFunction());
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
