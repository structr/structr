package org.structr.files.cmis;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.commons.server.TempStoreOutputStream;

/**
 *
 * @author Christian Morgner
 */
public class StructrCmisServicesFactory implements CmisServiceFactory {

	private static final Logger logger = Logger.getLogger(CmisServiceFactory.class.getName());
	private StructrCmisService service = null;

	@Override
	public void init(final Map<String, String> config) {

		logger.log(Level.INFO, "Initialization map: {0}", config);
	}

	@Override
	public void destroy() {
	}

	@Override
	public CmisService getService(final CallContext cc) {

		if (service == null) {

			service = new StructrCmisService(cc);

			logger.log(Level.INFO, "Returning new CMIS service");

		} else {

			logger.log(Level.INFO, "Returning existing CMIS service");
		}

		return service;
	}

	@Override
	public File getTempDirectory() {
		return new File("/tmp");
	}

	@Override
	public boolean encryptTempFiles() {
		return false;
	}

	@Override
	public int getMemoryThreshold() {

		// 20 MB
		return 20*1024*1024;
	}

	@Override
	public long getMaxContentSize() {

		// 1 GB
		return 1024*1024*1024;
	}

	@Override
	public TempStoreOutputStream getTempFileOutputStream(final String string) {
		return null;
	}

}
