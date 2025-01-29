/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.function;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.structr.api.config.Settings;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.VersionHelper;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.property.*;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.schema.action.ActionContext;
import org.structr.web.maintenance.DeployCommand;

public class StructrEnvFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_STRUCTR_ENV    = "Usage: ${structr_env()}. Example ${structr_env()}";
	public static final String ERROR_MESSAGE_STRUCTR_ENV_JS = "Usage: ${Structr.structr_env()}. Example ${Structr.structr_env()}";

	@Override
	public String getName() {
		return "structr_env";
	}

	@Override
	public String getSignature() {
		return "";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {
		return getStructrEnv();
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_STRUCTR_ENV : ERROR_MESSAGE_STRUCTR_ENV_JS);
	}

	@Override
	public String shortDescription() {
		return "Returns Structr runtime env information.";
	}

	public static GraphObjectMap getStructrEnv() throws FrameworkException {

		final GraphObjectMap info = new GraphObjectMap();

		info.setProperty(new GenericProperty("modules"),                        VersionHelper.getModules());
		info.setProperty(new GenericProperty("components"),                     VersionHelper.getComponents());
		info.setProperty(new StringProperty("classPath"),                       VersionHelper.getClassPath());

		info.setProperty(new StringProperty("instanceName"),                    VersionHelper.getInstanceName());
		info.setProperty(new StringProperty("instanceStage"),                   VersionHelper.getInstanceStage());

		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager != null) {

			info.setProperty(new StringProperty("edition"),  licenseManager.getEdition());
			info.setProperty(new StringProperty("licensee"), licenseManager.getLicensee());
			info.setProperty(new StringProperty("hostId"),   licenseManager.getHardwareFingerprint());
			info.setProperty(new DateProperty("startDate"),  licenseManager.getStartDate());
			info.setProperty(new DateProperty("endDate"),    licenseManager.getEndDate());

		} else {

			info.setProperty(new StringProperty("edition"),  "Community");
			info.setProperty(new StringProperty("licensee"), "Unlicensed");
		}

		info.setProperty(new GenericProperty("databaseService"),        Services.getInstance().getDatabaseService().getClass().getSimpleName());
		info.setProperty(new GenericProperty("resultCountSoftLimit"),   Settings.ResultCountSoftLimit.getValue());

		info.setProperty(new StringProperty("maintenanceModeActive"),   Settings.MaintenanceModeEnabled.getValue());
		info.setProperty(new StringProperty("validUUIDv4Regex"),        Settings.getValidUUIDRegexString());
		info.setProperty(new StringProperty("legacyRequestParameters"), Settings.RequestParameterLegacyMode.getValue());
		info.setProperty(new StringProperty("isDeploymentActive"),      DeployCommand.isDeploymentActive());

		info.setProperty(new StringProperty("debuggerEnabled"),          Settings.ScriptingDebugger.getValue());

		if (Settings.ScriptingDebugger.getValue()) {

			info.setProperty(new StringProperty("debuggerPath"), ContextFactory.getDebuggerPath());
		}

		info.setProperty(new StringProperty("availableReleasesUrl"),    Settings.ReleasesIndexUrl.getValue());
		info.setProperty(new StringProperty("availableSnapshotsUrl"),   Settings.SnapshotsIndexUrl.getValue());

		info.setProperty(new GenericProperty("dashboardInfo"),   getStructrDashboardInfo());

		return info;
	}

	public static GraphObjectMap getStructrDashboardInfo() throws FrameworkException {

		final GraphObjectMap dashboardInfo  = new GraphObjectMap();
		final GraphObjectMap configFileInfo = new GraphObjectMap();
		final GraphObjectMap runtimeInfo    = new GraphObjectMap();

		final PropertiesConfiguration conf = Settings.getDefaultPropertiesConfiguration();
		configFileInfo.setProperty(new StringProperty("actualPermissions"),    Settings.getActualConfigurationFilePermissionsAsString(conf));
		configFileInfo.setProperty(new StringProperty("expectedPermissions"),  Settings.getExpectedConfigurationFilePermissionsAsString());
		configFileInfo.setProperty(new BooleanProperty("permissionsOk"), Settings.checkConfigurationFilePermissions(conf, false));

		dashboardInfo.setProperty(new GenericProperty("configFileInfo"), configFileInfo);

		final Runtime runtime = Runtime.getRuntime();

		runtimeInfo.setProperty(new LongProperty("maxMemory"), runtime.maxMemory());
		runtimeInfo.setProperty(new LongProperty("freeMemory"), runtime.freeMemory());
		runtimeInfo.setProperty(new LongProperty("totalMemory"), runtime.totalMemory());
		runtimeInfo.setProperty(new IntProperty("availableProcessors"), runtime.availableProcessors());

		dashboardInfo.setProperty(new GenericProperty("runtimeInfo"), runtimeInfo);

		dashboardInfo.setProperty(new GenericProperty("allowedUUIDFormat"), Settings.UUIDv4AllowedFormats.getValue());
		dashboardInfo.setProperty(new GenericProperty("createCompactUUIDs"), Settings.UUIDv4CreateCompact.getValue());

		return dashboardInfo;
	}
}
