package org.structr.cmis.info;

/**
 * Optional interface for CMIS support in Structr core.
 *
 * @author Christian Morgner
 */
public interface CMISDocumentInfo extends CMISObjectInfo {

	public String getContentType();
	public String getParentId();
	public String getPath();

	public Long getSize();

}
