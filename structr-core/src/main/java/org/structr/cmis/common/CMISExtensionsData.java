package org.structr.cmis.common;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;

/**
 *
 * @author Christian Morgner
 */
public class CMISExtensionsData implements ExtensionsData {

	private static final Logger logger = Logger.getLogger(CMISExtensionsData.class.getName());

	protected List<CmisExtensionElement> extensions = new LinkedList<>();

	@Override
	public List<CmisExtensionElement> getExtensions() {
		return extensions;
	}

	@Override
	public void setExtensions(final List<CmisExtensionElement> extensions) {
		logger.log(Level.INFO, "{0}", extensions);
		this.extensions.addAll(extensions);
	}
}
