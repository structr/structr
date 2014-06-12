package org.structr.cloud;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.common.Syncable;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.web.entity.File;

/**
 *
 * @author Christian Morgner
 */
public class ExportSet {

	private final Set<NodeInterface> nodes                 = new LinkedHashSet<>();
	private final Set<RelationshipInterface> relationships = new LinkedHashSet<>();
	private int size                                       = 0;

	private ExportSet() {}

	public boolean add(final Syncable data) {

		if (data.isNode()) {

			if (nodes.add(data.getSyncNode())) {

				size++;

				if (data.getSyncNode() instanceof File) {

					size += (((File)data.getSyncNode()).getSize().intValue() / CloudService.CHUNK_SIZE) + 2;
				}

				// node was new (added), return true
				return true;
			}

		} else {

			if (relationships.add(data.getSyncRelationship())) {

				size++;

				// rel was new (added), return true
				return true;
			}
		}

		// arriving here means node was not added, so we return false
		return false;
	}

	public Set<NodeInterface> getNodes() {
		return nodes;
	}

	public Set<RelationshipInterface> getRelationships() {
		return relationships;
	}

	public int getTotalSize() {
		return size;
	}

	// ----- public static methods -----
	public static ExportSet getInstance() {
		return new ExportSet();
	}

	public static ExportSet getInstance(final Syncable start, final boolean recursive) {

		final ExportSet exportSet = new ExportSet();

		exportSet.collectSyncables(start, recursive);

		return exportSet;
	}

	private void collectSyncables(final Syncable start, boolean recursive) {

		add(start);

		if (recursive) {

			// collect children
			for (final Syncable child : start.getSyncData()) {

				if (child != null && add(child)) {

					collectSyncables(child, recursive);
				}
			}
		}
	}
}
