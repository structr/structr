package org.structr.files.cmis;

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class CMISAclService extends AbstractStructrCmisService implements AclService {

	public CMISAclService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	@Override
	public Acl getAcl(final String repositoryId, final String objectId, final Boolean onlyBasicPermissions, final ExtensionsData extension) {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final GraphObject node = app.get(objectId);
			if (node != null) {

				return CMISObjectWrapper.wrap(node, false);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
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

			final GraphObject obj = app.get(objectId);
			if (obj != null) {

				if (obj instanceof AbstractNode) {

					final AbstractNode node = (AbstractNode)obj;

					node.revokeAll();

					// process add ACL entries
					for (final Ace toAdd : acl.getAces()) {
						applyAce(node, toAdd, false);
					}

				} else {

					throw new CmisInvalidArgumentException("Object with ID " + objectId + " is not access controllable");
				}
			}

			tx.success();

			// return the wrapper which implements the Acl interface
			return CMISObjectWrapper.wrap(obj, false);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public Acl applyAcl(final String repositoryId, final String objectId, final Acl addAces, final Acl removeAces, final AclPropagation aclPropagation, final ExtensionsData extension) {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final GraphObject obj = app.get(objectId);
			if (obj != null) {

				if (obj instanceof AccessControllable) {

					final AccessControllable node = (AccessControllable)obj;

					// process remove ACL entries first
					for (final Ace toRemove : removeAces.getAces()) {
						applyAce( node, toRemove, true);
					}

					// process add ACL entries
					for (final Ace toAdd : addAces.getAces()) {
						applyAce(node, toAdd, false);
					}

				} else {

					throw new CmisInvalidArgumentException("Object with ID " + objectId + " is not access controllable");
				}
			}

			tx.success();

			// return the wrapper which implements the Acl interface
			return CMISObjectWrapper.wrap(obj, false);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
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
