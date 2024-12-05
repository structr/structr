package org.structr.core.traits.wrappers;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.AbstractTraitWrapper;
import org.structr.core.traits.Traits;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceAccessTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements ResourceAccess {

	private static final Map<String, List<ResourceAccess>> permissionsCache = new ConcurrentHashMap<>();

	// non-static members
	private String cachedResourceSignature = null;
	private Long cachedFlags               = null;

	public ResourceAccessTraitWrapper(final Traits traits, final NodeInterface nodeInterface) {
		super(traits, nodeInterface);
	}

	@Override
	public boolean hasFlag(long flag) {
		return (getFlags() & flag) == flag;
	}

	@Override
	public void setFlag(final long flag) throws FrameworkException {

		cachedFlags = null;

		wrappedObject.setProperty(traits.key("flags"), getFlags() | flag);
	}

	@Override
	public void clearFlag(final long flag) throws FrameworkException {

		cachedFlags = null;

		wrappedObject.setProperty(traits.key("flags"), getFlags() & ~flag);
	}

	@Override
	public long getFlags() {

		if (cachedFlags == null) {

			cachedFlags = wrappedObject.getProperty(traits.key("flags"));
		}

		if (cachedFlags != null) {
			return cachedFlags;
		}

		return 0;
	}

	@Override
	public String getResourceSignature() {

		if (cachedResourceSignature == null) {
			cachedResourceSignature = wrappedObject.getProperty(traits.key("signature"));
		}

		return cachedResourceSignature;
	}

	// ----- public static methods -----
	public static void clearCache() {
		permissionsCache.clear();
	}

	public static List<ResourceAccess> findPermissions(final SecurityContext securityContext, final String signature) throws FrameworkException {

		final Traits traits = Traits.of("ResourceAccess");
		List<ResourceAccess> permissions = permissionsCache.get(signature);
		if (permissions == null) {

			permissions = new LinkedList<>();

			// Ignore securityContext here (so we can cache all permissions for a signature independent of a user)
			final List<NodeInterface> nodes = StructrApp.getInstance().nodeQuery("ResourceAccess").and(traits.key("signature"), signature).getAsList();

			for (final NodeInterface node : nodes) {

				permissions.add(node.as(ResourceAccess.class));
			}

			permissionsCache.put(signature, permissions);
		}

		return permissions;
	}
}
