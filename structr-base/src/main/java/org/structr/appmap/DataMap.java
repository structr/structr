/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.appmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.entity.Relation;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The data of the application as a map in the form of the application map (see AppMapView): the types of the
 * application in families, each family a sector; the objects of every type in clusters, each cluster an element as
 * large as it has objects; the relationships between the objects of two clusters as one relation between them.
 *
 * A family is the first word of the name of a type: the part before an underscore, else the leading capitals or the
 * first capitalised word (ContentHub_Image, CMSContent and CavokImage belong to ContentHub, CMS and Cavok); the
 * FAMILIES largest families of at least two types by objects are sectors, the others share one. A cluster is the part of a type that lies in
 * one community of the data: the objects that are related to each other far more than to the rest, found by the Louvain
 * method over all objects; the CLUSTERS largest parts of a type that hold at least CLUSTER_SHARE of it or CLUSTER_MIN
 * objects are clusters, the rest of the type is one more. A cluster is named after the object with the most
 * relationships of its community that has a name.
 *
 * The data is read and grouped once and kept for KEEP_MS.
 */
public class DataMap {

	private static final Logger logger = LoggerFactory.getLogger(DataMap.class);

	// a group is named after the first of its HUBS objects with the most relationships that has a name; a leaf holds at
	// most LEAF objects, a field at most FIELD; communities smaller than SMALL objects or the SMALL_SHARE-th part of
	// their group are bundled by type; groups deeper than DEPTH are split into leaves in the order of their objects
	public static final int LEAF        = 400;
	public static final int SMALL       = 12;
	public static final int SMALL_SHARE = 300;
	public static final int DEPTH       = 6;
	public static final int FIELD       = 60_000;
	public static final int HUBS        = 8;

	// the sectors and clusters of the map, see the class comment; the data is kept this long
	public static final int FAMILIES        = 7;
	public static final int CLUSTERS        = 12;
	public static final double CLUSTER_SHARE = 0.02;
	public static final int CLUSTER_MIN     = 30;
	public static final long KEEP_MS        = 10 * 60 * 1000L;

	private static DataMap current = null;

	private final List<String> typeList = new ArrayList<>();
	private final List<String> rels     = new ArrayList<>();
	private final List<Group> groups    = new ArrayList<>();
	private final long built;

	// the objects, by their index: node id, kind (index of the type); the relationships as adjacency lists (both
	// directions, rel type of each), and the leaf of every object
	private long[] nodeIds = new long[0];
	private int[] kinds    = new int[0];
	private int[] start    = new int[1];
	private int[] adjacent = new int[0];
	private short[] relOf  = new short[0];
	private boolean[] out  = new boolean[0];
	private int[] leafOf   = new int[0];
	private long[] counts  = new long[0];

	private static class Group {

		int id;
		int parent;
		int[] nodes;
		boolean bundle;
		boolean field;
		int[] islands;
		List<List<Integer>> parts;
		final List<Integer> children = new ArrayList<>();
	}

	private DataMap() {

		this.built = System.currentTimeMillis();

		// the types of the application: the node types of its schema, without those Structr brings along
		typeList.addAll(new TreeSet<>(Traits.getAllTypes(t -> t.isNodeType() && !t.isBuiltinType() && !t.isServiceClass() && !t.isAbstract() && !t.isInterface())));

		// a type without objects counts none, also where the data cannot be read (a database without Cypher)
		counts = new long[typeList.size()];

		// the relationship types between them, from their relation properties
		for (final String type : typeList) {

			for (final PropertyKey key : Traits.of(type).getAllPropertyKeys()) {

				if (key instanceof RelationProperty property) {

					final Relation relation = property.getRelation();
					final String source     = relation.getSourceType();
					final String target     = relation.getTargetType();

					if (StructrTraits.NODE_INTERFACE.equals(source) || StructrTraits.NODE_INTERFACE.equals(target)) {
						continue;
					}

					if (!rels.contains(relation.name())) {
						rels.add(relation.name());
					}
				}
			}
		}
	}

	/**
	 * The data map, read and grouped anew when it is older than KEEP_MS or a fresh one is asked for.
	 */
	public static synchronized DataMap get(final App app, final boolean fresh) {

		if (fresh || current == null || System.currentTimeMillis() - current.built > KEEP_MS) {

			final DataMap map = new DataMap();
			final long t0     = System.currentTimeMillis();

			if (Services.getInstance().getDatabaseService().supportsCypher()) {

				map.read(app);
				map.group();
			}

			logger.info("Data map: {} objects, {} relationships in {} groups, built in {} ms", map.nodeIds.length, map.adjacent.length / 2, map.groups.size(), System.currentTimeMillis() - t0);

			current = map;
		}

		return current;
	}

	/**
	 * The data map in the form of the application map: { areas, nodes, edges, objects }, an area as { key, label }, a
	 * cluster as a node { id, k: 'type', n: its name, t: its type, cn: its community, g: its type as its grouping, area:
	 * its family, s: its number of objects }, the relationships between two clusters as an edge [source, target, number,
	 * 'links', 0, 'data'].
	 */
	public GraphObjectMap toModel(final App app) {

		final GraphObjectMap result = new GraphObjectMap();

		// the families by objects, the largest FAMILIES of those with at least two types with objects sectors of their own
		final Map<String, Long> byFamily    = new LinkedHashMap<>();
		final Map<String, Integer> members = new HashMap<>();

		for (int k = 0; k < typeList.size(); k++) {

			if (counts[k] > 0) {

				byFamily.merge(familyOf(typeList.get(k)), counts[k], Long::sum);
				members.merge(familyOf(typeList.get(k)), 1, Integer::sum);
			}
		}

		final List<String> families = byFamily.entrySet().stream().filter(e -> members.get(e.getKey()) > 1).sorted(Map.Entry.<String, Long>comparingByValue().reversed()).map(Map.Entry::getKey).limit(FAMILIES).toList();
		final boolean others        = byFamily.keySet().stream().anyMatch(family -> !families.contains(family));
		final List<Object> areas    = new ArrayList<>();

		for (final String family : families) {
			areas.add(Map.of("key", areaOf(family), "label", family));
		}

		if (others) {
			areas.add(Map.of("key", "other", "label", "Other"));
		}

		// the community of every object: the child of the whole data its leaf lies in
		final Group root          = groups.isEmpty() ? null : groups.get(0);
		final int[] communityOf   = new int[nodeIds.length];
		final Map<Integer, Integer> indexOfChild = new HashMap<>();

		if (root != null) {

			for (int c = 0; c < root.children.size(); c++) {
				indexOfChild.put(root.children.get(c), c);
			}
		}

		for (int i = 0; i < nodeIds.length; i++) {

			int g = leafOf[i];

			while (g > 0 && groups.get(g).parent != 0) {
				g = groups.get(g).parent;
			}

			communityOf[i] = indexOfChild.getOrDefault(g, -1);
		}

		final List<String> communityNames = root != null ? namesOf(app, root) : List.of();

		// the clusters: per type the largest parts in one community, the rest one more
		final Map<Long, Integer> clusterOf = new HashMap<>();
		final List<Object> nodes           = new ArrayList<>();
		final Map<Integer, Map<Integer, Integer>> parts = new HashMap<>();

		for (int i = 0; i < nodeIds.length; i++) {

			if (kinds[i] >= 0) {
				parts.computeIfAbsent(kinds[i], k -> new HashMap<>()).merge(communityOf[i], 1, Integer::sum);
			}
		}

		for (final Map.Entry<Integer, Map<Integer, Integer>> entry : parts.entrySet()) {

			final int kind          = entry.getKey();
			final String type       = typeList.get(kind);
			final String family     = familyOf(type);
			final String area       = families.contains(family) ? areaOf(family) : "other";
			final int total         = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
			final List<Integer> own = entry.getValue().entrySet().stream()
				.filter(e -> e.getKey() >= 0 && e.getValue() >= Math.max(CLUSTER_MIN, CLUSTER_SHARE * total))
				.sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
				.limit(CLUSTERS)
				.map(Map.Entry::getKey)
				.toList();

			int rest = total;

			for (final int community : own) {

				final int size = entry.getValue().get(community);
				final String name = community < communityNames.size() && communityNames.get(community) != null ? communityNames.get(community) : "loosely related";

				clusterOf.put(kind * 1_000_000L + community, nodes.size());
				nodes.add(cluster(nodes.size(), type, name, area, size));

				rest -= size;
			}

			if (rest > 0) {

				final int index = nodes.size();

				nodes.add(cluster(index, type, own.isEmpty() ? null : "other", area, rest));

				for (final int community : entry.getValue().keySet()) {
					clusterOf.putIfAbsent(kind * 1_000_000L + community, index);
				}
			}
		}

		// the relationships between the clusters, each pair once in the direction of its relationships
		final Map<Long, Integer> pairs = new HashMap<>();

		for (int i = 0; i < nodeIds.length; i++) {

			final Integer from = clusterOf.get(kinds[i] * 1_000_000L + communityOf[i]);

			for (int k = start[i]; k < start[i + 1]; k++) {

				final int j       = adjacent[k];
				final Integer to  = clusterOf.get(kinds[j] * 1_000_000L + communityOf[j]);

				if (out[k] && from != null && to != null && !from.equals(to)) {
					pairs.merge(from * 1_000_000L + to, 1, Integer::sum);
				}
			}
		}

		final List<Object> edges = new ArrayList<>();

		for (final Map.Entry<Long, Integer> pair : pairs.entrySet()) {
			edges.add(Map.of("source", "d" + pair.getKey() / 1_000_000L, "target", "d" + pair.getKey() % 1_000_000L, "weight", pair.getValue(), "kind", "links", "evidence", "data"));
		}

		result.put(new GenericProperty<>("areas"),   areas);
		result.put(new GenericProperty<>("nodes"),   nodes);
		result.put(new GenericProperty<>("edges"),   edges);
		result.put(new GenericProperty<>("objects"), nodeIds.length);

		return result;
	}

	// ----- private methods -----
	private Map<String, Object> cluster(final int index, final String type, final String community, final String area, final int size) {

		final Map<String, Object> node = new LinkedHashMap<>();

		node.put("id",        "d" + index);
		node.put("kind",      "type");
		// the family stands over the sector, so a name with an underscore leaves it out
		final String shown = type.indexOf('_') > 0 ? type.substring(type.indexOf('_') + 1) : type;

		node.put("name",      community != null ? shown + " · " + community : shown);
		node.put("type",      type);
		node.put("community", community);
		node.put("grouping",  "type:" + type);
		node.put("area",      area);
		node.put("size",      size);

		return node;
	}

	/**
	 * The family of a type, the first word of its name: the part before an underscore, else the leading capitals before
	 * the next word (CMSContent is CMS) or the first capitalised word (CavokImage is Cavok).
	 */
	static String familyOf(final String type) {

		final int underscore = type.indexOf('_');

		if (underscore > 0) {
			return type.substring(0, underscore);
		}

		int end = 1;

		while (end < type.length() && Character.isUpperCase(type.charAt(end))) {
			end++;
		}

		// leading capitals: the last of them begins the next word
		if (end > 2) {
			return end < type.length() ? type.substring(0, end - 1) : type;
		}

		while (end < type.length() && !Character.isUpperCase(type.charAt(end))) {
			end++;
		}

		return type.substring(0, end);
	}

	// the key of the area of a family, as the map uses it in keys of its layout
	private static String areaOf(final String family) {

		return "family-" + family.toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	// the names of the children of a group: each the name of its first of HUBS objects with the most relationships that
	// has one, null where none has
	private List<String> namesOf(final App app, final Group group) {

		final List<long[]> hubs = new ArrayList<>();

		for (final int id : group.children) {

			final int[] nodes = groups.get(id).nodes;
			final int[] top   = Arrays.stream(nodes).boxed().sorted(Comparator.comparingInt(this::degree).reversed()).limit(HUBS).mapToInt(Integer::intValue).toArray();

			hubs.add(Arrays.stream(top).mapToLong(node -> nodeIds[node]).toArray());
		}

		final Map<Long, String> names = new HashMap<>();
		final List<Long> wanted       = hubs.stream().flatMapToLong(Arrays::stream).boxed().toList();

		for (final Map<String, Object> row : rows(app, "MATCH (n) WHERE ID(n) IN $nodes RETURN ID(n) AS node, n.name AS name", Map.of("nodes", wanted))) {

			if (row.get("node") instanceof Number node && row.get("name") instanceof String name && !name.isBlank()) {
				names.put(node.longValue(), name);
			}
		}

		return hubs.stream().map(ids -> Arrays.stream(ids).mapToObj(names::get).filter(n -> n != null).findFirst().orElse(null)).toList();
	}

	/**
	 * All objects and relationships, each as a few long lists in one row. The lists are returned in a map: a column that
	 * holds a list loses its name on the way through Structr (see NativeQueryCommand), the keys of a map do not.
	 */
	private void read(final App app) {

		final Map<String, Object> types = Map.of("types", typeList);
		final List<Long> ids             = new ArrayList<>();
		final List<Integer> kindList     = new ArrayList<>();

		for (final Map<String, Object> row : rows(app, "MATCH (n" + nodeLabels() + ") WHERE n.type IN $types RETURN { nodes: collect(ID(n)), types: collect(n.type) } AS net", types)) {

			final List<?> nodes = list(row.get("nodes"));
			final List<?> names = list(row.get("types"));

			for (int i = 0; i < nodes.size(); i++) {

				if (nodes.get(i) instanceof Number node) {

					ids.add(node.longValue());
					kindList.add(typeList.indexOf((String) names.get(i)));
				}
			}
		}

		nodeIds = ids.stream().mapToLong(Long::longValue).toArray();
		kinds   = kindList.stream().mapToInt(Integer::intValue).toArray();
		counts  = new long[typeList.size()];

		for (final int kind : kinds) {

			if (kind >= 0) {
				counts[kind]++;
			}
		}

		final Map<Long, Integer> index = indexOfNodes();
		final int n                    = nodeIds.length;
		int[] src                      = new int[0];
		int[] dst                      = new int[0];
		short[] rel                    = new short[0];

		if (!rels.isEmpty()) {

			final String pattern = rels.stream().map(r -> "`" + r.replace("`", "``") + "`").collect(Collectors.joining("|"));

			for (final Map<String, Object> row : rows(app, "MATCH (a" + nodeLabels() + ")-[r:" + pattern + "]->(b" + nodeLabels() + ") WHERE a.type IN $types AND b.type IN $types RETURN { a: collect(ID(a)), b: collect(ID(b)), r: collect(type(r)) } AS net", types)) {

				final List<?> a = list(row.get("a"));
				final List<?> b = list(row.get("b"));
				final List<?> r = list(row.get("r"));

				src = new int[a.size()];
				dst = new int[a.size()];
				rel = new short[a.size()];

				int m = 0;

				for (int i = 0; i < a.size(); i++) {

					final Integer from = index.get(((Number) a.get(i)).longValue());
					final Integer to   = index.get(((Number) b.get(i)).longValue());

					if (from != null && to != null && !from.equals(to)) {

						src[m] = from;
						dst[m] = to;
						rel[m] = (short) rels.indexOf((String) r.get(i));
						m++;
					}
				}

				src = Arrays.copyOf(src, m);
				dst = Arrays.copyOf(dst, m);
				rel = Arrays.copyOf(rel, m);
			}
		}

		// adjacency in both directions
		start = new int[n + 1];

		for (int i = 0; i < src.length; i++) {

			start[src[i] + 1]++;
			start[dst[i] + 1]++;
		}

		for (int i = 0; i < n; i++) {
			start[i + 1] += start[i];
		}

		final int[] fill = Arrays.copyOf(start, n);

		adjacent = new int[src.length * 2];
		relOf    = new short[src.length * 2];
		out      = new boolean[src.length * 2];

		for (int i = 0; i < src.length; i++) {

			int k = fill[src[i]]++;

			adjacent[k] = dst[i];
			relOf[k]    = rel[i];
			out[k]      = true;

			k = fill[dst[i]]++;

			adjacent[k] = src[i];
			relOf[k]    = rel[i];
			out[k]      = false;
		}
	}


	// the groups: 0 is the whole net, its children the communities of the net and the bundles of small ones
	private void group() {

		final Group root = new Group();
		final int[] all  = new int[nodeIds.length];

		for (int i = 0; i < all.length; i++) {
			all[i] = i;
		}

		root.id     = 0;
		root.parent = -1;
		root.nodes  = all;

		groups.add(root);

		leafOf = new int[nodeIds.length];

		split(root, 0);
	}

	/**
	 * Splits a group into its communities and splits every child further. The communities that are small for the group
	 * are bundled by the type most of their objects have; a bundle is split into the communities it was made of, those
	 * still small for it put together into fields (see field).
	 */
	private void split(final Group group, final int depth) {

		// the whole net is never a leaf itself, so that the explorer always starts from groups
		if ((group.nodes.length <= LEAF || group.field) && group.id != 0) {

			for (final int node : group.nodes) {
				leafOf[node] = group.id;
			}

			return;
		}

		final List<List<Integer>> members = group.parts != null ? group.parts : communities(group, depth);
		final int small                   = Math.max(SMALL, group.nodes.length / SMALL_SHARE);
		final List<Integer> rest          = new ArrayList<>();

		members.sort((a, b) -> b.size() - a.size());

		// no split found: leaves of LEAF objects in the order of the group, which follows the communities found before
		if (members.size() == 1) {

			chunk(group, members.get(0), depth);

			return;
		}

		if (group.parts != null) {

			final List<List<Integer>> islands = new ArrayList<>();

			for (final List<Integer> list : members) {

				if (list.size() >= small) {
					islands.add(null);
				} else {
					islands.add(list);
				}
			}

			// a bundle of islands only is a field itself
			if (islands.stream().allMatch(list -> list != null) && group.nodes.length <= FIELD) {

				fieldOf(group, members);
				split(group, depth);

				return;
			}

			for (int i = 0; i < members.size(); i++) {

				if (islands.get(i) == null) {
					child(group, members.get(i), false, null, depth);
				}
			}

			field(group, islands.stream().filter(list -> list != null).toList(), depth);

			return;
		}

		final Map<Integer, List<List<Integer>>> bundles = new LinkedHashMap<>();

		for (final List<Integer> list : members) {

			if (list.size() >= small) {

				child(group, list, false, null, depth);

			} else {

				final Map<Integer, Long> byKind = list.stream().collect(Collectors.groupingBy(node -> kinds[node], Collectors.counting()));
				final int kind                  = byKind.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(-1);

				bundles.computeIfAbsent(kind, k -> new ArrayList<>()).add(list);
			}
		}

		for (final List<List<Integer>> bundle : bundles.values()) {

			if (bundle.size() == 1) {

				child(group, bundle.get(0), false, null, depth);

			} else {

				child(group, bundle.stream().flatMap(List::stream).toList(), true, bundle, depth);
			}
		}
	}

	// the communities of a group, as lists of its objects; one list where none are found
	private List<List<Integer>> communities(final Group group, final int depth) {

		final List<List<Integer>> members = new ArrayList<>();

		if (depth >= DEPTH) {

			members.add(Arrays.stream(group.nodes).boxed().collect(Collectors.toCollection(ArrayList::new)));

			return members;
		}

		final int[] community = louvain(group.nodes);
		final int count       = Arrays.stream(community).max().orElse(0) + 1;

		for (int c = 0; c < count; c++) {
			members.add(new ArrayList<>());
		}

		for (int i = 0; i < group.nodes.length; i++) {
			members.get(community[i]).add(group.nodes[i]);
		}

		return members;
	}

	// leaves of at most LEAF of the given objects, in their order
	private void chunk(final Group group, final List<Integer> nodes, final int depth) {

		for (int from = 0; from < nodes.size(); from += LEAF) {
			child(group, nodes.subList(from, Math.min(nodes.size(), from + LEAF)), false, null, depth);
		}
	}

	// fields of at most FIELD objects of the given islands, each island whole and in its order
	private void field(final Group group, final List<List<Integer>> islands, final int depth) {

		int from = 0;

		while (from < islands.size()) {

			final List<List<Integer>> part = new ArrayList<>();
			int size                       = 0;

			while (from < islands.size() && (part.isEmpty() || size + islands.get(from).size() <= FIELD)) {

				size += islands.get(from).size();
				part.add(islands.get(from++));
			}

			final Group child = new Group();

			child.id     = groups.size();
			child.parent = group.id;
			child.bundle = true;

			fieldOf(child, part);
			groups.add(child);
			group.children.add(child.id);

			split(child, depth + 1);
		}
	}

	// makes a group a field of the given islands, whose objects it holds in their order
	private void fieldOf(final Group group, final List<List<Integer>> islands) {

		group.nodes   = islands.stream().flatMap(List::stream).mapToInt(Integer::intValue).toArray();
		group.field   = true;
		group.parts   = null;
		group.islands = islands.stream().mapToInt(List::size).toArray();
	}

	private void child(final Group parent, final List<Integer> nodes, final boolean bundle, final List<List<Integer>> parts, final int depth) {

		final Group group = new Group();

		group.id     = groups.size();
		group.parent = parent.id;
		group.nodes  = nodes.stream().mapToInt(Integer::intValue).toArray();
		group.bundle = bundle;
		group.parts  = parts;

		groups.add(group);
		parent.children.add(group.id);

		split(group, depth + 1);

		group.parts = null;
	}

	/**
	 * The communities of the subgraph of the given objects by the Louvain method: every object moves to the community of
	 * its neighbours that gains the most modularity, as long as one does; then the communities become the objects of a
	 * smaller graph, and so on. Returns the community of every object, numbered from 0.
	 */
	private int[] louvain(final int[] nodes) {

		final int n = nodes.length;
		final int[] local = new int[nodeIds.length];

		Arrays.fill(local, -1);

		for (int i = 0; i < n; i++) {
			local[nodes[i]] = i;
		}

		// the subgraph as weighted adjacency lists, every relationship of weight 1 in both directions
		int[] gStart = new int[n + 1];

		for (int i = 0; i < n; i++) {

			for (int k = start[nodes[i]]; k < start[nodes[i] + 1]; k++) {

				if (local[adjacent[k]] >= 0) {
					gStart[i + 1]++;
				}
			}
		}

		for (int i = 0; i < n; i++) {
			gStart[i + 1] += gStart[i];
		}

		int[] gAdj    = new int[gStart[n]];
		double[] gW   = new double[gStart[n]];
		double[] self = new double[n];

		for (int i = 0, k2 = 0; i < n; i++) {

			for (int k = start[nodes[i]]; k < start[nodes[i] + 1]; k++) {

				if (local[adjacent[k]] >= 0) {

					gAdj[k2] = local[adjacent[k]];
					gW[k2]   = 1;
					k2++;
				}
			}
		}

		int[] mapping = new int[n];
		int size      = n;

		for (int i = 0; i < n; i++) {
			mapping[i] = i;
		}

		for (int level = 0; level < 10; level++) {

			final int[] comm = moveNodes(size, gStart, gAdj, gW, self);
			final int count  = Arrays.stream(comm).max().orElse(-1) + 1;

			for (int i = 0; i < n; i++) {
				mapping[i] = comm[mapping[i]];
			}

			if (count == size) {
				break;
			}

			// the communities as the objects of the next level, the weights between them summed
			final List<Map<Integer, Double>> edges = new ArrayList<>();
			final double[] nextSelf                = new double[count];

			for (int c = 0; c < count; c++) {
				edges.add(new HashMap<>());
			}

			for (int i = 0; i < size; i++) {

				nextSelf[comm[i]] += self[i];

				for (int k = gStart[i]; k < gStart[i + 1]; k++) {

					final int a = comm[i];
					final int b = comm[gAdj[k]];

					if (a == b) {
						nextSelf[a] += gW[k];
					} else {
						edges.get(a).merge(b, gW[k], Double::sum);
					}
				}
			}

			gStart = new int[count + 1];

			for (int c = 0; c < count; c++) {
				gStart[c + 1] = gStart[c] + edges.get(c).size();
			}

			gAdj = new int[gStart[count]];
			gW   = new double[gStart[count]];

			for (int c = 0; c < count; c++) {

				int k = gStart[c];

				for (final Map.Entry<Integer, Double> e : edges.get(c).entrySet()) {

					gAdj[k] = e.getKey();
					gW[k]   = e.getValue();
					k++;
				}
			}

			self = nextSelf;
			size = count;
		}

		return mapping;
	}

	// one level of Louvain: moves the objects between communities as long as the modularity grows; returns the
	// communities, numbered from 0
	private int[] moveNodes(final int n, final int[] gStart, final int[] gAdj, final double[] gW, final double[] self) {

		final double[] degree = new double[n];
		double total          = 0;

		for (int i = 0; i < n; i++) {

			degree[i] = self[i];

			for (int k = gStart[i]; k < gStart[i + 1]; k++) {
				degree[i] += gW[k];
			}

			total += degree[i];
		}

		final int[] comm    = new int[n];
		final double[] tot  = new double[n];
		final double[] link = new double[n];
		final int[] touched = new int[n];

		Arrays.fill(link, -1);

		for (int i = 0; i < n; i++) {

			comm[i] = i;
			tot[i]  = degree[i];
		}

		if (total == 0) {
			return renumber(comm);
		}

		boolean moved = true;

		for (int pass = 0; pass < 20 && moved; pass++) {

			moved = false;

			for (int i = 0; i < n; i++) {

				int count = 0;

				for (int k = gStart[i]; k < gStart[i + 1]; k++) {

					final int c = comm[gAdj[k]];

					if (link[c] < 0) {

						link[c] = 0;
						touched[count++] = c;
					}

					link[c] += gW[k];
				}

				final int own = comm[i];

				tot[own] -= degree[i];

				int best       = own;
				double gain    = (link[own] < 0 ? 0 : link[own]) - tot[own] * degree[i] / total;

				for (int t = 0; t < count; t++) {

					final int c    = touched[t];
					final double g = link[c] - tot[c] * degree[i] / total;

					if (g > gain + 1e-12) {

						gain = g;
						best = c;
					}
				}

				tot[best] += degree[i];
				comm[i]    = best;

				if (best != own) {
					moved = true;
				}

				for (int t = 0; t < count; t++) {
					link[touched[t]] = -1;
				}
			}
		}

		return renumber(comm);
	}

	private int[] renumber(final int[] comm) {

		final Map<Integer, Integer> numbers = new HashMap<>();
		final int[] result                  = new int[comm.length];

		for (int i = 0; i < comm.length; i++) {
			result[i] = numbers.computeIfAbsent(comm[i], c -> numbers.size());
		}

		return result;
	}


	private int degree(final int node) {

		return start[node + 1] - start[node];
	}

	private Map<Long, Integer> indexOfNodes() {

		final Map<Long, Integer> index = new HashMap<>(nodeIds.length * 2);

		for (int i = 0; i < nodeIds.length; i++) {
			index.put(nodeIds[i], i);
		}

		return index;
	}


	// a list as an ArrayList: Structr hands lists of a query over as linked lists, read by index they would take forever
	private static List<?> list(final Object value) {

		if (value instanceof Iterable<?> iterable) {

			final List<Object> list = new ArrayList<>();

			iterable.forEach(list::add);

			return list;
		}

		if (value instanceof Object[] array) {
			return Arrays.asList(array);
		}

		return List.of();
	}

	// the labels of the objects of this application: a database shared by several tenants holds the objects of the others as well
	private static String nodeLabels() {

		final String tenant = Services.getInstance().getDatabaseService().getTenantIdentifier();

		return tenant != null ? ":NodeInterface:`" + tenant.replace("`", "``") + "`" : ":NodeInterface";
	}

	// the rows of a Cypher query as maps; a query that fails is logged and has none
	private static List<Map<String, Object>> rows(final App app, final String query, final Map<String, Object> parameters) {

		final List<Map<String, Object>> rows = new ArrayList<>();

		try {

			for (final Object row : app.query(query, parameters)) {

				final Map<String, Object> map = toRow(row);

				if (map != null) {
					rows.add(map);
				}
			}

		} catch (FrameworkException fex) {

			logger.warn("Unable to read the data for the data map with {}: {}", query, fex.getMessage());
		}

		return rows;
	}

	// a row of a Cypher query as a map: a GraphObjectMap itself, or the columns of a row put together
	private static Map<String, Object> toRow(final Object row) {

		if (row instanceof GraphObjectMap map) {
			return map.toMap();
		}

		if (row instanceof Iterable<?> columns) {

			final Map<String, Object> map = new LinkedHashMap<>();

			for (final Object column : columns) {

				if (column instanceof GraphObjectMap part) {
					map.putAll(part.toMap());
				}
			}

			return map;
		}

		return null;
	}
}
