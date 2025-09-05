/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.function;

import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.util.CountResult;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.graph.NodeService;
import org.structr.schema.action.ActionContext;
import org.structr.web.maintenance.DeployCommand;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SystemInfoFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_SYSTEM_INFO = "Usage: ${system_info([key])}. When called without parameters all info will be returned, otherwise specify a key to request specific info.";

	@Override
	public String getName() {
		return "system_info";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final Map<String, Object> systemInfo = getSystemInfo();

		if (sources != null && sources.length == 1) {

			if (systemInfo.containsKey(sources[0])) {

				return systemInfo.get(sources[0]);
			} else {

				return usage(ctx.isJavaScriptContext());
			}
		} else if (sources.length > 1) {

			return usage(ctx.isJavaScriptContext());
		}

		return systemInfo;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SYSTEM_INFO;
	}

	@Override
	public String shortDescription() {
		return "Returns information about the system.";
	}

	public static Map getSystemInfo () {

		final Map<String, Object> info = new LinkedHashMap<>();

		info.put("now",     System.currentTimeMillis());
		info.put("uptime",  ManagementFactory.getRuntimeMXBean().getUptime());
		info.put("runtime", Runtime.version().toString());

		final String activeNodeServiceName = Settings.getOrCreateStringSetting("NodeService.active").getValue();

		if (activeNodeServiceName != null) {

			final NodeService nodeService = Services.getInstance().getService(NodeService.class, activeNodeServiceName);
			if (nodeService != null) {

				final Map<String, Number> counts                  = new LinkedHashMap<>();
				final Map<String, Map<String, Integer>> cacheInfo = new LinkedHashMap<>();

				final DatabaseService db = nodeService.getDatabaseService();

				final CountResult cr = db.getNodeAndRelationshipCount();

				counts.put("nodes", cr.getNodeCount());
				counts.put("relationships", cr.getRelationshipCount());

				cacheInfo.putAll(db.getCachesInfo());
				cacheInfo.put("localizations", LocalizeFunction.getCacheInfo());

				info.put("counts", counts);
				info.put("caches", cacheInfo);
			}
		}

		info.put("deployment_active", DeployCommand.isDeploymentActive());
		info.put("maintenance_active", Settings.MaintenanceModeEnabled.getValue());

		final Map<String, Map> memoryInfo = new LinkedHashMap<>();

		final Map<String, Long> memoryRuntimeInfo = new LinkedHashMap<>();
		memoryRuntimeInfo.put("free", Runtime.getRuntime().freeMemory());
		memoryRuntimeInfo.put("max", Runtime.getRuntime().maxMemory());
		memoryRuntimeInfo.put("total", Runtime.getRuntime().totalMemory());
		memoryInfo.put("runtime_info", memoryRuntimeInfo);

		final Map<String, Map> memoryBeansInfo = new LinkedHashMap<>();

		final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
		for (final MemoryPoolMXBean bean : beans) {

			final MemoryUsage usage = bean.getCollectionUsage();
			if (usage != null) {

				final Map<String, Long> beanInfo = new LinkedHashMap<>();
				beanInfo.put("init", usage.getInit());
				beanInfo.put("used", usage.getUsed());
				beanInfo.put("committed", usage.getCommitted());
				beanInfo.put("max", usage.getMax());

				memoryBeansInfo.put(bean.getName(), beanInfo);
			}
		}

		memoryInfo.put("mgmt_bean_info", memoryBeansInfo);

		info.put("memory", memoryInfo);

		return info;
	}
}
