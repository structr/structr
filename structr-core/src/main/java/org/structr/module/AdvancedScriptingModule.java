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
import org.structr.core.function.AddToGroupFunction;
import org.structr.core.function.AncestorTypesFunction;
import org.structr.core.function.AppendFunction;
import org.structr.core.function.AssertFunction;
import org.structr.core.function.CallFunction;
import org.structr.core.function.CallPrivilegedFunction;
import org.structr.core.function.ChangelogFunction;
import org.structr.core.function.ConfigFunction;
import org.structr.core.function.DecryptFunction;
import org.structr.core.function.DisableCascadingDeleteFunction;
import org.structr.core.function.DisablePreventDuplicateRelationshipsFunction;
import org.structr.core.function.DisableNotificationsFunction;
import org.structr.core.function.EnableCascadingDeleteFunction;
import org.structr.core.function.EnableNotificationsFunction;
import org.structr.core.function.EncryptFunction;
import org.structr.core.function.EnumInfoFunction;
import org.structr.core.function.ErrorFunction;
import org.structr.core.function.EvaluateScriptFunction;
import org.structr.core.function.ExecBinaryFunction;
import org.structr.core.function.ExecFunction;
import org.structr.core.function.Functions;
import org.structr.core.function.GeocodeFunction;
import org.structr.core.function.GetRelationshipTypesFunction;
import org.structr.core.function.GrantFunction;
import org.structr.core.function.HasErrorFunction;
import org.structr.core.function.InheritingTypesFunction;
import org.structr.core.function.InstantiateFunction;
import org.structr.core.function.IsAllowedFunction;
import org.structr.core.function.IsInGroupFunction;
import org.structr.core.function.JdbcFunction;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.function.PrivilegedFindFunction;
import org.structr.core.function.PropertyInfoFunction;
import org.structr.core.function.RInterpreterFunction;
import org.structr.core.function.RangeFunction;
import org.structr.core.function.ReadFunction;
import org.structr.core.function.RemoveFromGroupFunction;
import org.structr.core.function.RevokeFunction;
import org.structr.core.function.ServerLogFunction;
import org.structr.core.function.SetEncryptionKeyFunction;
import org.structr.core.function.SetPrivilegedFunction;
import org.structr.core.function.TemplateFunction;
import org.structr.core.function.TypeInfoFunction;
import org.structr.core.function.UnlockReadonlyPropertiesFunction;
import org.structr.core.function.UnlockSystemPropertiesFunction;
import org.structr.core.function.UserChangelogFunction;
import org.structr.core.function.WriteFunction;
import org.structr.core.function.XPathFunction;
import org.structr.core.function.XmlFunction;
import org.structr.core.function.search.FindAndFunction;
import org.structr.core.function.search.FindWithinDistanceFunction;
import org.structr.core.function.search.FindContainsFunction;
import org.structr.core.function.search.FindEmptyFunction;
import org.structr.core.function.search.FindEqualsFunction;
import org.structr.core.function.search.FindNotFunction;
import org.structr.core.function.search.FindOrFunction;
import org.structr.core.function.search.FindPageFunction;
import org.structr.core.function.search.FindSortFunction;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

/**
 *
 */
public class AdvancedScriptingModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new AssertFunction());
		Functions.put(licenseManager, new ErrorFunction());
		Functions.put(licenseManager, new HasErrorFunction());
		Functions.put(licenseManager, new ConfigFunction());
		Functions.put(licenseManager, new ChangelogFunction());
		Functions.put(licenseManager, new UserChangelogFunction());
		Functions.put(licenseManager, new ServerLogFunction());

		Functions.put(licenseManager, new GrantFunction());
		Functions.put(licenseManager, new RevokeFunction());
		Functions.put(licenseManager, new IsAllowedFunction());
		Functions.put(licenseManager, new AddToGroupFunction());
		Functions.put(licenseManager, new RemoveFromGroupFunction());
		Functions.put(licenseManager, new IsInGroupFunction());

		Functions.put(licenseManager, new LocalizeFunction());

		Functions.put(licenseManager, new CallFunction());
		Functions.put(licenseManager, new CallPrivilegedFunction());
		Functions.put(licenseManager, new ExecFunction());
		Functions.put(licenseManager, new ExecBinaryFunction());

		Functions.put(licenseManager, new UnlockReadonlyPropertiesFunction());
		Functions.put(licenseManager, new UnlockSystemPropertiesFunction());
		Functions.put(licenseManager, new SetPrivilegedFunction());
		Functions.put(licenseManager, new PrivilegedFindFunction());

		Functions.put(licenseManager, new ReadFunction());
		Functions.put(licenseManager, new WriteFunction());
		Functions.put(licenseManager, new AppendFunction());
		Functions.put(licenseManager, new XmlFunction());
		Functions.put(licenseManager, new XPathFunction());
		Functions.put(licenseManager, new GeocodeFunction());

		Functions.put(licenseManager, new InstantiateFunction());

		Functions.put(licenseManager, new PropertyInfoFunction());
		Functions.put(licenseManager, new TypeInfoFunction());
		Functions.put(licenseManager, new EnumInfoFunction());
		Functions.put(licenseManager, new DisableCascadingDeleteFunction());
		Functions.put(licenseManager, new EnableCascadingDeleteFunction());
		Functions.put(licenseManager, new DisableNotificationsFunction());
		Functions.put(licenseManager, new DisablePreventDuplicateRelationshipsFunction());
		Functions.put(licenseManager, new EnableNotificationsFunction());
		Functions.put(licenseManager, new RInterpreterFunction());
		Functions.put(licenseManager, new EvaluateScriptFunction());
		Functions.put(licenseManager, new AncestorTypesFunction());
		Functions.put(licenseManager, new InheritingTypesFunction());

		Functions.put(licenseManager, new TemplateFunction());
		Functions.put(licenseManager, new JdbcFunction());

		Functions.put(licenseManager, new GetRelationshipTypesFunction());

		Functions.put(licenseManager, new SetEncryptionKeyFunction());
		Functions.put(licenseManager, new EncryptFunction());
		Functions.put(licenseManager, new DecryptFunction());

		Functions.put(licenseManager, new RangeFunction());
		Functions.put(licenseManager, new FindWithinDistanceFunction());
		Functions.put(licenseManager, new FindEmptyFunction());
		Functions.put(licenseManager, new FindEqualsFunction());
		Functions.put(licenseManager, new FindContainsFunction());
		Functions.put(licenseManager, new FindAndFunction());
		Functions.put(licenseManager, new FindOrFunction());
		Functions.put(licenseManager, new FindNotFunction());
		Functions.put(licenseManager, new FindSortFunction());
		Functions.put(licenseManager, new FindPageFunction());
	}

	@Override
	public String getName() {
		return "advanced-scripting";
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
