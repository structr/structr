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
package org.structr.rest.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.api.config.Settings;
import org.structr.api.search.SortOrder;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.VersionHelper;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.DateProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;

/**
 *
 *
 */
public class EnvResource extends Resource {

	public enum UriPart {
		_env
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		return (UriPart._env.name().equals(part));
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		final List<GraphObjectMap> resultList             = new LinkedList<>();
		final GraphObjectMap info                         = new GraphObjectMap();

		info.setProperty(new GenericProperty("modules"),              VersionHelper.getModules());
		info.setProperty(new GenericProperty("components"),           VersionHelper.getComponents());
		info.setProperty(new StringProperty("classPath"),             VersionHelper.getClassPath());
		info.setProperty(new StringProperty("instanceName"),          VersionHelper.getInstanceName());
		info.setProperty(new StringProperty("instanceStage"),         VersionHelper.getInstanceStage());
		info.setProperty(new ArrayProperty("mainMenu", String.class), VersionHelper.getMenuEntries());

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

		info.setProperty(new GenericProperty("databaseService"), Services.getInstance().getDatabaseService().getClass().getSimpleName());
		info.setProperty(new GenericProperty("resultCountSoftLimit"), Settings.ResultCountSoftLimit.getValue());
		info.setProperty(new StringProperty("availableReleasesUrl"), Settings.ReleasesIndexUrl.getValue());
		info.setProperty(new StringProperty("availableSnapshotsUrl"), Settings.SnapshotsIndexUrl.getValue());

		resultList.add(info);

		return new PagingIterable(resultList);
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("POST not allowed on " + getResourceSignature());
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		throw new IllegalPathException(getResourceSignature() + " has no subresources");
	}

	@Override
	public String getUriPart() {
		return getResourceSignature();
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	@Override
	public String getResourceSignature() {
		return UriPart._env.name();
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return false;
	}

}
