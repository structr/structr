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

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class CMISAclService extends AbstractStructrCmisService implements AclService {

	private static final Logger logger = LoggerFactory.getLogger(CMISAclService.class.getName());

	public CMISAclService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	@Override
	public Acl getAcl(final String repositoryId, final String objectId, final Boolean onlyBasicPermissions, final ExtensionsData extension) {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final AbstractNode node = app.get(AbstractNode.class, objectId);
			if (node != null) {

				return CMISObjectWrapper.wrap(node, null, false);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	/**
	 * Applies the given Acl exclusively, i.e. removes all other permissions / grants first.
	 *
	 * @param repositoryId
	 * @param objectId
	 * @param acl
	 * @param aclPropagation
	 *
	 * @return the resulting Acl
	 */
	public Acl applyAcl(final String repositoryId, final String objectId, final Acl acl, final AclPropagation aclPropagation) {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final AbstractNode node = app.get(AbstractNode.class, objectId);
			if (node != null) {

				node.revokeAll();

				// process add ACL entries
				for (final Ace toAdd : acl.getAces()) {
					applyAce(node, toAdd, false);
				}

				tx.success();

				// return the wrapper which implements the Acl interface
				return CMISObjectWrapper.wrap(node, null, false);
			}

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public Acl applyAcl(final String repositoryId, final String objectId, final Acl addAces, final Acl removeAces, final AclPropagation aclPropagation, final ExtensionsData extension) {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.get(NodeInterface.class, objectId);
			if (node != null) {

				// process remove ACL entries first
				for (final Ace toRemove : removeAces.getAces()) {
					applyAce( node, toRemove, true);
				}

				// process add ACL entries
				for (final Ace toAdd : addAces.getAces()) {
					applyAce(node, toAdd, false);
				}

				tx.success();

				// return the wrapper which implements the Acl interface
				return CMISObjectWrapper.wrap(node, null, false);
			}

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	// ----- private methods -----
	private void applyAce(final AccessControllable node, final Ace toAdd, final boolean revoke) throws FrameworkException {

		final String principalId       = toAdd.getPrincipalId();
		final List<String> permissions = toAdd.getPermissions();

		final Principal principal = CMISObjectWrapper.translateUsernameToPrincipal(principalId);
		if (principal != null) {

			for (final String permissionString : permissions) {

				final Permission permission = Permissions.valueOf(permissionString);
				if (permission != null) {

					if (revoke) {

						node.revoke(permission, principal);

					} else {

						node.grant(permission, principal);
					}

				} else {

					throw new CmisInvalidArgumentException("Permission with ID " + permissionString + " does not exist");
				}
			}

		} else {

			throw new CmisObjectNotFoundException("Principal with ID " + principalId + " does not exist");
		}




	}
}
