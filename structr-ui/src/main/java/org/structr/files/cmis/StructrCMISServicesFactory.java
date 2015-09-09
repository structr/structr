package org.structr.files.cmis;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.commons.server.TempStoreOutputStream;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class StructrCMISServicesFactory implements CmisServiceFactory {

	private static final Logger logger = Logger.getLogger(CmisServiceFactory.class.getName());

	@Override
	public void init(final Map<String, String> config) {

		logger.log(Level.INFO, "Initialization map: {0}", config);
	}

	@Override
	public void destroy() {
	}

	@Override
	public CmisService getService(final CallContext cc) {
		return new StructrCMISService(checkAuthentication(cc));
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

	// ----- private methods -----
	private SecurityContext checkAuthentication(final CallContext callContext) {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final String username           = callContext.getUsername();
			final String password           = callContext.getPassword();
			final Principal principal       = AuthHelper.getPrincipalForPassword(Principal.name, username, password);
			SecurityContext securityContext = null;

			if (principal != null) {


				if (principal instanceof SuperUser) {

					securityContext = SecurityContext.getSuperUserInstance();

				} else {

					securityContext = SecurityContext.getInstance(principal, AccessMode.Backend);
				}
			}

			tx.success();

			if (securityContext != null) {
				return securityContext;
			}

		} catch (AuthenticationException aex) {

			throw new CmisUnauthorizedException(aex.getMessage());

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		throw new CmisUnauthorizedException();
	}
}
