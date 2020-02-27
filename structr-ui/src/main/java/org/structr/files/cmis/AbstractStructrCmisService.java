/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.files.cmis;

import java.util.Map;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.slf4j.Logger;
import org.structr.cmis.CMISInfo;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;

/**
 *
 *
 */
public abstract class AbstractStructrCmisService {

	public StructrCMISService parentService = null;
	public SecurityContext securityContext  = null;

	public AbstractStructrCmisService(final StructrCMISService parentService, final SecurityContext securityContext) {

		this.parentService   = parentService;
		this.securityContext = securityContext;
	}

	public void log(final Logger logger, final Object... objects) {

		final StringBuilder buf = new StringBuilder();

		for (int i=0; i<objects.length; i++) {
			buf.append("\n{").append(i).append("}");
		}

		logger.info(buf.toString(), objects);
	}

	public Object getValue(final Properties properties, final String key) {

		final Map<String, PropertyData<?>> data = properties.getProperties();
		if (data != null) {

			final PropertyData<?> value = data.get(key);
			if (value != null) {

				return value.getFirstValue();
			}
		}

		return null;
	}

	public String getStringValue(final Properties properties, final String key) {

		final Object value = getValue(properties, key);
		if (value != null && value instanceof String) {

			return (String)value;
		}

		return null;
	}

	/**
	 * Returns the CMIS info that is defined in the given Structr type, or null.
	 *
	 * @param type
	 * @return
	 */
	public CMISInfo getCMISInfo(final Class<? extends GraphObject> type) {

		try { return type.newInstance().getCMISInfo(); } catch (Throwable t) {}

		return null;
	}

	/**
	 * Returns the baseTypeId that is defined in the given Structr type, or null.
	 * @param type
	 * @return
	 */
	public BaseTypeId getBaseTypeId(final Class<? extends GraphObject> type) {

		final CMISInfo info = getCMISInfo(type);
		if (info != null) {

			return info.getBaseTypeId();
		}

		return null;
	}

	/**
	 * Returns the enum value for the given typeId, or null if no such value exists.
	 *
	 * @param typeId
	 * @return
	 */
	public BaseTypeId getBaseTypeId(final String typeId) {

		try { return BaseTypeId.fromValue(typeId); } catch (IllegalArgumentException iex) {}

		return null;
	}

	/**
	 * Returns the Structr type for the given objectTypeId, or the defaultClass of the objectTypeId
	 * matches the given baseTypeId.
	 *
	 * @param objectTypeId
	 * @param defaultType
	 * @param defaultClass
	 *
	 * @return a Structr type
	 */
	public Class typeFromObjectTypeId(final String objectTypeId, final BaseTypeId defaultType, final Class defaultClass) {

		// default for cmuis:folder
		if (defaultType.value().equals(objectTypeId)) {
			return defaultClass;
		}

		return StructrApp.getConfiguration().getNodeEntityClass(objectTypeId);
	}
}
