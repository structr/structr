package org.structr.core.traits.wrappers;

import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.AbstractTraitWrapper;
import org.structr.core.traits.Traits;

import java.util.Set;

public class SecurityTraitWrapper extends AbstractTraitWrapper implements Security {

	public SecurityTraitWrapper(final Traits traits, final NodeInterface nodeInterface) {

		super(traits, nodeInterface);
	}

	public boolean isAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission permission) {
		return getPermissions(graphObject, key).contains(permission.name());
	}

	public void setAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Set<String> allowed) {

		String[] permissions = (String[]) allowed.toArray(new String[allowed.size()]);
		setAllowed(graphObject, key, permissions);
	}

	public void setAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission... permissions) {

		Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		setAllowed(graphObject, key, permissionSet);
	}

	private void setAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final String[] allowed) {

		if (allowed.length == 0) {

			StructrApp.getInstance().delete((RelationshipInterface)graphObject);

		} else {

			final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
			propertyContainer.setProperty(key.dbName(), allowed);

		}
	}

	public Set<String> getPermissions(final RelationshipInterface graphObject, final PropertyKey<String[]> key) {

		final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
		return getPermissionSet(propertyContainer, key);
	}

	public void addPermission(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission permission) {

		addPermissions(graphObject, key, Collections.singleton(permission));
	}

	public void addPermissions(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Set<Permission> permissions) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		boolean change = false;

		for (final Permission p : permissions) {

			if (!permissionSet.contains(p.name())) {

				change = true;
				permissionSet.add(p.name());
			}
		};

		if (change) {
			setAllowed(graphObject, key, permissionSet);
		}
	}

	public void removePermission(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission permission) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		if (!permissionSet.contains(permission.name())) {

			return;
		}

		permissionSet.remove(permission.name());
		setAllowed(graphObject, key, permissionSet);
	}

	public void removePermissions(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Set<Permission> permissions) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		boolean change = false;

		for (final Permission p : permissions) {

			if (permissionSet.contains(p.name())) {

				change = true;
				permissionSet.remove(p.name());
			}
		};

		if (change) {
			setAllowed(graphObject, key, permissionSet);
		}
	}

	public Set<String> getPermissionSet(final PropertyContainer propertyContainer, final PropertyKey<String[]> key) {

		final Set<String> permissionSet = new HashSet<>();

		if (propertyContainer.hasProperty(key.dbName())) {

			final String[] permissions = (String[])propertyContainer.getProperty(key.dbName());
			if (permissions != null) {

				permissionSet.addAll(Arrays.asList(permissions));
			}
		}

		return permissionSet;
	}

	@Override
	public boolean isAllowed(final Permission permission) {
		return SecurityDelegate.isAllowed(this, SecurityRelationship.allowed, permission);
	}

	@Override
	public void setAllowed(final Set<String> allowed) {
		SecurityDelegate.setAllowed(this, SecurityRelationship.allowed, allowed);
	}

	@Override
	public void setAllowed(final Permission... allowed) {
		SecurityDelegate.setAllowed(this, SecurityRelationship.allowed, allowed);
	}

	@Override
	public Set<String> getPermissions() {
		return SecurityDelegate.getPermissions(this, SecurityRelationship.allowed);
	}

	@Override
	public void addPermission(final Permission permission) {
		SecurityDelegate.addPermission(this, SecurityRelationship.allowed, permission);
	}

	@Override
	public void addPermissions(final Set<Permission> permissions) {
		SecurityDelegate.addPermissions(this, SecurityRelationship.allowed, permissions);
	}

	@Override
	public void removePermission(final Permission permission) {
		SecurityDelegate.removePermission(this, SecurityRelationship.allowed, permission);
	}

	@Override
	public void removePermissions(final Set<Permission> permissions) {
		SecurityDelegate.removePermissions(this, SecurityRelationship.allowed, permissions);
	}
	*/
}
