/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.ArrayList;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.structr.cmis.common.CMISExtensionsData;
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
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

/**
 *
 *
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

				if(node instanceof AbstractNode) {

					AbstractNode aNode = (AbstractNode) node;

					//Gets directly the ACEs and puts it into an ACL.
					//The parameter "onlyBasicPermissions" must be
					//implemented to prevent failing TCK tests
					return new ACLEntry(aNode.getAccessControlEntries(onlyBasicPermissions));
				}
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

		checkMacro(acl);

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			Principal user = securityContext.getCachedUser();
			boolean isAdmin = user.getProperty(Principal.isAdmin);

			//Special limitations not for admins necessary
			if(!isAdmin) {
				checkPermission(acl, user);
			}

			final GraphObject graphNode = app.get(objectId);
			if (graphNode != null) {

				if (graphNode instanceof AbstractNode) {

					final AbstractNode node = (AbstractNode)graphNode;

					graphNode.setProperty(GraphObject.visibleToAuthenticatedUsers, false);
					graphNode.setProperty(GraphObject.visibleToPublicUsers, false);
					node.revokeAll();

					// process add ACL entries
					for (final Ace toAdd : acl.getAces()) {

						applyAce(graphNode, toAdd, false, aclPropagation);
					}

				} else {

					throw new CmisInvalidArgumentException("Object with ID " + objectId + " is not access controllable");
				}
			}

			tx.success();

			// return the wrapper which implements the Acl interface
			return CMISObjectWrapper.wrap(graphNode, null, false, true);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		throw new CmisObjectNotFoundException("You don't have permission to change ACE for the object" + objectId);
	}

	@Override
	public Acl applyAcl(final String repositoryId, final String objectId, final Acl addAces, final Acl removeAces, final AclPropagation aclPropagation, final ExtensionsData extension) {

		checkMacro(addAces);
		checkMacro(removeAces);

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			Principal user = securityContext.getCachedUser();
			boolean isAdmin = user.getProperty(Principal.isAdmin);

			//Special limitations not for admins necessary
			if(!isAdmin) {
				checkPermission(addAces, user);
				checkPermission(removeAces, user);
			}

			final GraphObject graphNode = app.get(objectId);

			if (graphNode != null) {

				if (graphNode instanceof AbstractNode) {

					// process remove ACL entries first
					for (final Ace toRemove : removeAces.getAces()) {
						applyAce(graphNode, toRemove, true, aclPropagation);
					}

					// process add ACL entries
					for (final Ace toAdd : addAces.getAces()) {
						applyAce(graphNode, toAdd, false, aclPropagation);
					}

				} else {

					throw new CmisInvalidArgumentException("Object with ID " + objectId + " is not access controllable");
				}

			}

			tx.success();

			// return the wrapper which implements the Acl interface
			return CMISObjectWrapper.wrap(graphNode, null, false, true);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	// ----- private methods -----
	private void applyAce(final GraphObject graphNode, final Ace toAdd, final boolean revoke, final AclPropagation aclPropagation) throws FrameworkException {

		final String principalId	        = toAdd.getPrincipalId();
		final List<String> permissions		= toAdd.getPermissions();

		//checks which flag sets the user in the acl editor
		//no further implementation yet
		/*boolean objectOnly;
		if(aclPropagation.value().equals(AclPropagation.REPOSITORYDETERMINED.value())) {
		objectOnly = false;
		} else if(aclPropagation.value().equals(AclPropagation.OBJECTONLY.value())) {
		objectOnly = true;
		} else {
		throw new CmisInvalidArgumentException("Only 'repository determined' and 'objectonly' available.");
		}*/
		//!

		switch (principalId) {

			case Principal.ANONYMOUS:

				//Only allow read-Permission here!!!
				//Don't accept other flags!!
				if(permissions.size() == 1 && permissions.get(0).equals(Permission.read.name())) {

					if(revoke) {

						graphNode.setProperty(GraphObject.visibleToPublicUsers, false);
					} else {

						graphNode.setProperty(GraphObject.visibleToPublicUsers, true);
					}

				} else {

					throw new CmisInvalidArgumentException("'anonymous' and 'anyone' accept only the read permission.");
				}

				break;

			case Principal.ANYONE:

				//Only allow read-Permission here!!!
				//Don't accept other flags!!
				if(permissions.size() == 1 && permissions.get(0).equals(Permission.read.name())) {

					if(revoke) {

						graphNode.setProperty(GraphObject.visibleToAuthenticatedUsers, false);
					} else {

						graphNode.setProperty(GraphObject.visibleToAuthenticatedUsers, true);
					}

				} else {

					throw new CmisInvalidArgumentException("'anonymous' and 'anyone' accept only the read permission.");
				}

				break;

			default:

				final AccessControllable node = (AccessControllable)graphNode;
				applyDefaultAce(principalId, permissions, revoke, node);
				break;
		}

	}

	private void applyDefaultAce(String principalId, List<String> permissions, boolean revoke, AccessControllable node) throws FrameworkException {

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

	/**
	* "The CMIS specification also defines the principal ID cmis:user . Repositories
	* that support this macro replace this principal ID with the principal ID
	* of the current user when an ACL is applied."
	* Didn't find a way to disable the macro yet. Implementing it complicates
	* things only. Implementation probably for later.
	* The function checks, if Macro was chosen by user and throw a exception.
	*/
	private void checkMacro(final Acl acl) {

		for (final Ace ace : acl.getAces()) {

			if(ace.getPrincipalId().equals("cmis:user")) {

				throw new CmisInvalidArgumentException("This macro is not supported in Structr. Use your login name as principal instead");
			}
		}
	}


	/**
	 * Users can only grant or revoke permissions to themself, their owned groups and anyone/anonymous.
	 * Granting or revoking other principals permissions results into a exception.
	*/
	private void checkPermission(Acl acl, Principal user) {

		if(user != null) {

			//Could use filter to get only ownedGroups, but dont know how exactly
			//Get all ownedNodes by the user and extract owned Groups out of it
			List<NodeInterface> ownedNodes = user.getProperty(Principal.ownedNodes);
			List<Group> ownedGroups = new ArrayList<>();

			for(NodeInterface node : ownedNodes) {

				if(node instanceof Group) {

					Group group = (Group)node;
					ownedGroups.add(group);
				}
			}

			for(Ace ace : acl.getAces()) {

				boolean principalAllowed = false;
				String principalToGrant = ace.getPrincipalId();

				for(Group group : ownedGroups) {

					//principal is in owned Groups
					if(principalToGrant.equals(group.getName())) {

						principalAllowed = true;
						break;
					}

					//Anyone and anonymous can be also always manipulated
					if(principalToGrant.equals(Principal.ANONYMOUS) || principalToGrant.equals(Principal.ANYONE)) {

						principalAllowed = true;
						break;
					}

					//Can also manipulate himself
					if(principalToGrant.equals(user.getName())) {

						principalAllowed = true;
						break;
					}
				}

				if(!principalAllowed) {

					//Found a new added principal, which the user cant grant access
					throw new CmisInvalidArgumentException("You have no permission to manipulate this user.");
				}
			}
		}
	}

	// ----- nested classes -----
	/**
	 * Used for delivering an ACL in getAcl()
	 */
	private class ACLEntry extends CMISExtensionsData implements Acl{

		private List<Ace> aces;

		public ACLEntry(List<Ace> aces) {

			this.aces = aces;
		}

		@Override
		public List<Ace> getAces() {
			return aces;
		}

		@Override
		public Boolean isExact() {
			return true;
		}
	}


}
