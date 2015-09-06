package org.structr.cmis.wrapper;

import java.math.BigInteger;
import java.util.List;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class CMISDocumentWrapper extends CMISObjectWrapper<CMISDocumentInfo> {

	private String contentType  = null;
	private BigInteger fileSize = null;

	public CMISDocumentWrapper() {
		super(BaseTypeId.CMIS_DOCUMENT);
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final List<PropertyData<?>> properties) {

		properties.add(factory.createPropertyStringData(PropertyIds.CONTENT_STREAM_MIME_TYPE, contentType));
		properties.add(factory.createPropertyStringData(PropertyIds.CONTENT_STREAM_FILE_NAME, getName()));

		properties.add(factory.createPropertyIntegerData(PropertyIds.CONTENT_STREAM_LENGTH,    fileSize));
	}

	@Override
	public void initializeFrom(final CMISDocumentInfo info) throws FrameworkException {

		super.initializeFrom(info);

		this.contentType = info.getContentType();

		final Long size = info.getSize();
		if (size != null) {

			this.fileSize = BigInteger.valueOf(size);
		}
	}
}
