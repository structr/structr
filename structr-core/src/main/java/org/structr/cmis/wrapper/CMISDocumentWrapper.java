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
package org.structr.cmis.wrapper;

import java.math.BigInteger;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 *
 */
public class CMISDocumentWrapper extends CMISObjectWrapper<CMISDocumentInfo> {

	private boolean isImmutable = false;
	private String changeToken  = null;
	private String contentType  = null;
	private BigInteger fileSize = null;

	public CMISDocumentWrapper(final String propertyFilter, final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_DOCUMENT, propertyFilter, includeAllowableActions);
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final FilteredPropertyList properties) {

		properties.add(factory.createPropertyStringData(PropertyIds.CONTENT_STREAM_MIME_TYPE, contentType));
		properties.add(factory.createPropertyStringData(PropertyIds.CONTENT_STREAM_FILE_NAME, getName()));
		properties.add(factory.createPropertyIntegerData(PropertyIds.CONTENT_STREAM_LENGTH,    fileSize));
		properties.add(factory.createPropertyIdData(PropertyIds.CONTENT_STREAM_ID, getName()));

		properties.add(factory.createPropertyStringData(PropertyIds.CHANGE_TOKEN, changeToken));
		properties.add(factory.createPropertyBooleanData(PropertyIds.IS_IMMUTABLE, isImmutable));

		// added for specification compliance
		properties.add(factory.createPropertyIdData(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, (String)null));
		properties.add(factory.createPropertyBooleanData(PropertyIds.IS_LATEST_VERSION, true));
		properties.add(factory.createPropertyBooleanData(PropertyIds.IS_MAJOR_VERSION, true));
		properties.add(factory.createPropertyBooleanData(PropertyIds.IS_LATEST_MAJOR_VERSION, true));
		properties.add(factory.createPropertyBooleanData(PropertyIds.IS_PRIVATE_WORKING_COPY, false));
		properties.add(factory.createPropertyStringData(PropertyIds.VERSION_LABEL, ""));
		properties.add(factory.createPropertyIdData(PropertyIds.VERSION_SERIES_ID, ""));
		properties.add(factory.createPropertyBooleanData(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false));

		properties.add(factory.createPropertyStringData(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, (String)null));
		properties.add(factory.createPropertyIdData(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, (String)null));
		properties.add(factory.createPropertyStringData(PropertyIds.CHECKIN_COMMENT, (String)null));
	}

	@Override
	public void initializeFrom(final CMISDocumentInfo info) throws FrameworkException {

		super.initializeFrom(info);

		this.contentType = info.getContentType();
		this.changeToken = info.getChangeToken();

		final Long size = info.getSize();
		if (size != null) {

			this.fileSize = BigInteger.valueOf(size);
		}

		this.isImmutable = info.isImmutable();
	}
}
