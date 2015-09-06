package org.structr.cmis.info;

import java.util.GregorianCalendar;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

/**
 * Optional interface for CMIS support in Structr core.
 *
 * @author Christian Morgner
 */
public interface CMISObjectInfo {

	public BaseTypeId getBaseTypeId();
	public String getUuid();
	public String getName();
	public String getDescription();
	public String getType();
	public String getCreatedBy();
	public String getLastModifiedBy();
	public GregorianCalendar getCreationDate();
	public GregorianCalendar getLastModificationDate();
}
