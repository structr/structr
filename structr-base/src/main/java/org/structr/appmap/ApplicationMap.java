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

import org.structr.common.AccessControllable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.GenericProperty;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * The application as one flat graph: its artifacts (types, methods, pages, shared components,
 * repeaters, event action mappings, REST endpoints, external services) and what they do to
 * each other, for the application map in the Admin UI. It carries no geometry; layout and
 * rendering are the client's business.
 *
 * The contributors of the loaded modules collect the artifacts and two kinds of references:
 * edges that the map shows (a page contains an action, a method belongs to its type) and
 * plain connections that only decide which artifacts take part (a page script names a type).
 * An artifact without any connection is no part of the application's web and is left out;
 * a repeater or an action mapping takes part when its page does. Last, the CRUD analysis
 * works out what every operation creates, reads, updates and deletes, and turns that into the
 * business edges.
 *
 * Every artifact also belongs to a grouping and to an area, so that any client can lay the
 * application out the way Structr structures it. A grouping is an artifact as the developer
 * declared it: a type with its methods, a page or shared component with its repeaters and event
 * action mappings, a group, a REST endpoint; a user-defined function, like a method of a schema
 * relationship, belongs to the type it writes most, else it stands alone, and so does one that
 * CronService runs. The area says which of Structr's mechanisms the grouping is: "ui" (pages
 * and shared components), "integration" (REST endpoints and external services),
 * "security" (groups and the service classes that manage users and rights), "builtin" (the
 * system types the application extends), "logic" (user-defined functions and service classes),
 * "scheduled" (what CronService runs) and "data" (the application's own types).
 *
 * The result has four parts: "nodes" (id, k for the kind, n for the name, p for public, g for
 * the grouping, area, and depending on the kind d for the HTML outline (nested [tag, flags,
 * children, ids of the repeaters and action mappings on the element]), crud, svc, t for the
 * type, m for the method, how for the way the type was found, a and ev for the action and
 * the event of an action mapping, cron for the schedule, rest for
 * the resource access grants on a type or endpoint: n rules, pub and auth for whom they open it,
 * pubw and authw whether those may write, acc for who may see a page or shared component:
 * pub and auth for its visibility, own for its owner, g for the grants as [principal,
 * permissions], s for its size: the properties of a type, the lines of code of a method, the
 * HTML elements of a page or shared component), "edges"
 * ([source, target, weight, kind, inferred, evidence]), "groups" (id, n, m for the number of
 * members, t for the types they have grants on, g and area) and "helpers" (the methods that
 * many call and that write nothing).
 */
public class ApplicationMap {

	public static final String TYPE      = "type";
	public static final String METHOD    = "method";
	public static final String PAGE      = "page";
	public static final String COMPONENT = "component";
	public static final String VIEW      = "view";
	public static final String ACTION    = "action";
	public static final String API       = "api";
	public static final String EXTERNAL  = "external";

	public static final String AREA_UI          = "ui";
	public static final String AREA_INTEGRATION = "integration";
	public static final String AREA_SECURITY    = "security";
	public static final String AREA_BUILTIN     = "builtin";
	public static final String AREA_LOGIC       = "logic";
	public static final String AREA_SCHEDULED   = "scheduled";
	public static final String AREA_DATA        = "data";

	private static final List<ApplicationMapContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

	// Structr's own types that an application extends in its schema; a type that merely inherits from one of them is the application's own
	private static final Set<String> BUILTIN_TYPES = Set.of(
		"AbstractFile", "File", "Folder", "Image", "FSObject", "VideoFile", "User", "Group", "Principal", "Page", "DOMNode",
		"DOMElement", "Content", "Template", "ShadowDocument", "Site", "Widget", "MailTemplate", "Localization", "Location"
	);

	private static final Set<String> SECURITY_TYPES = Set.of("User", "Group", "Principal");
	private static final Pattern SECURITY_NAME      = Pattern.compile("rights|permission|access|role|auth", Pattern.CASE_INSENSITIVE);
	// a function belongs to the type it writes most; reading only decides between types it does not write (see CrudAnalysis.WEIGHT)
	private static final Map<String, Double> WRITE_WEIGHT = Map.of("C", 3.0, "U", 2.5, "D", 2.5, "R", 0.01);

	private final Map<String, MapNode> nodes          = new LinkedHashMap<>();
	private final Map<String, MapEdge> edges          = new LinkedHashMap<>();
	private final Map<String, Integer> references     = new LinkedHashMap<>();
	private final Set<List<String>> connections       = new LinkedHashSet<>();
	private final Map<String, MapGroup> groups        = new LinkedHashMap<>();
	private final SchemaIndex schema;

	private ApplicationMap(final SchemaIndex schema) {

		this.schema = schema;
	}

	// ----- public static methods -----
	public static void register(final ApplicationMapContributor contributor) {

		CONTRIBUTORS.add(contributor);
	}

	/**
	 * Where the given objects lie on the map: per id the ids of the map elements it may be shown as, the object itself
	 * first, see ApplicationMapContributor.locate. Ids of unknown objects are left out.
	 */
	public static Map<String, Object> locate(final App app, final Collection<String> ids) throws FrameworkException {

		final Map<String, Object> located = new LinkedHashMap<>();

		for (final String id : ids) {

			final NodeInterface node = app.getNodeById(id);

			if (node == null) {
				continue;
			}

			final Set<String> chain = new LinkedHashSet<>(List.of(id));

			for (final ApplicationMapContributor contributor : CONTRIBUTORS) {

				chain.addAll(contributor.locate(node));
			}

			located.put(id, new LinkedList<>(chain));
		}

		return located;
	}

	public static ApplicationMap build(final App app) throws FrameworkException {

		final ApplicationMap map = new ApplicationMap(new SchemaIndex(app));

		for (final ApplicationMapContributor contributor : CONTRIBUTORS) {

			contributor.contribute(map, app);
		}

		return map;
	}

	// ----- public methods -----
	public SchemaIndex getSchema() {

		return schema;
	}

	/**
	 * Adds a node, or returns the existing node with that id.
	 */
	public MapNode addNode(final String id, final String kind, final String name) {

		return nodes.computeIfAbsent(id, k -> new MapNode(id, kind, name));
	}

	public MapNode getNode(final String id) {

		return nodes.get(id);
	}

	public boolean hasNode(final String id) {

		return nodes.containsKey(id);
	}

	/**
	 * Adds an edge the map shows. The first edge of a kind between two nodes wins.
	 */
	public void addEdge(final String source, final String target, final String kind, final double weight, final String evidence) {

		if (source == null || target == null || source.equals(target)) {
			return;
		}

		edges.putIfAbsent(edgeKey(source, target, kind), new MapEdge(source, target, kind, weight, evidence));
	}

	/**
	 * Adds a reference that is shown as an edge and connects both ends. Every further reference
	 * of the same kind between the same nodes counts logarithmically, so a component included
	 * forty times does not outweigh a method call.
	 */
	public void addReference(final String source, final String target, final String kind, final double baseWeight) {

		if (source == null || target == null || source.equals(target)) {
			return;
		}

		final String key = edgeKey(source, target, kind);
		final int count  = references.merge(key, 1, Integer::sum);

		edges.put(key, new MapEdge(source, target, kind, baseWeight * (1 + Math.log(count)), null));

		connect(source, target);
	}

	/**
	 * Records that two artifacts are connected, without an edge: a script that names a type,
	 * calls a method or an external service.
	 */
	public void connect(final String a, final String b) {

		if (a != null && b != null && !a.equals(b)) {

			connections.add(List.of(a, b));
		}
	}

	public MapGroup addGroup(final String id, final String name, final int members) {

		return groups.computeIfAbsent(id, k -> new MapGroup(id, name, members));
	}

	/**
	 * The finished map as one object, see the class comment for its parts.
	 */
	public GraphObjectMap toGraphObject() {

		final Set<String> kept       = keptNodes();
		final CrudAnalysis analysis  = new CrudAnalysis(this, kept);
		final GraphObjectMap result  = new GraphObjectMap();
		final List<Object> nodeList  = new LinkedList<>();
		final List<Object> edgeList  = new LinkedList<>();
		final List<Object> groupList = new LinkedList<>();
		final Set<String> seen       = new HashSet<>();

		analysis.run();

		final Map<String, String> groupings = groupings(kept, analysis);
		final Map<String, String> areas     = new HashMap<>();

		for (final String grouping : new HashSet<>(groupings.values())) {
			areas.put(grouping, areaOf(nodes.get(grouping), analysis));
		}

		for (final MapNode node : nodes.values()) {

			if (kept.contains(node.id)) {

				final String grouping = groupings.get(node.id);

				nodeList.add(node.toMap(kept, analysis.crudOf(node.id), grouping, areas.get(grouping)));
			}
		}

		// the business edges first, then what the contributors declared
		final List<MapEdge> all = new LinkedList<>(analysis.getEdges());

		all.addAll(edges.values());

		for (final MapEdge edge : all) {

			if (kept.contains(edge.source) && kept.contains(edge.target) && seen.add(edgeKey(edge.source, edge.target, edge.kind))) {
				edgeList.add(edge.toList());
			}
		}

		for (final MapGroup group : groups.values()) {
			groupList.add(group.toMap(kept));
		}

		result.put(new GenericProperty<>("nodes"),   nodeList);
		result.put(new GenericProperty<>("edges"),   edgeList);
		result.put(new GenericProperty<>("groups"),  groupList);
		result.put(new GenericProperty<>("helpers"), new LinkedList<>(analysis.getHelpers()));

		return result;
	}

	// ----- package-private methods -----
	Collection<MapNode> getNodes() {

		return nodes.values();
	}

	// ----- private methods -----
	/**
	 * The grouping of every artifact, see the class comment.
	 */
	private Map<String, String> groupings(final Set<String> kept, final CrudAnalysis analysis) {

		final Map<String, String> ownerOf = new HashMap<>();
		final Map<String, String> result  = new HashMap<>();

		for (final MapEdge edge : edges.values()) {

			final MapNode source = nodes.get(edge.source);
			final MapNode target = nodes.get(edge.target);

			if ("member".equals(edge.kind) && source != null && target != null && METHOD.equals(source.kind) && TYPE.equals(target.kind)) {
				ownerOf.put(edge.source, edge.target);
			}
		}

		for (final String id : kept) {

			final MapNode node = nodes.get(id);
			String grouping    = id;

			if (METHOD.equals(node.kind)) {

				// a scheduled user-defined function runs by itself, it joins no type
				grouping = ownerOf.containsKey(id) ? ownerOf.get(id) : (node.cronExpression != null ? id : writtenMost(analysis.crudOf(id), kept));

				if (grouping == null) {
					grouping = id;
				}

			} else if ((VIEW.equals(node.kind) || ACTION.equals(node.kind)) && node.home != null && kept.contains(node.home)) {

				grouping = node.home;
			}

			result.put(id, grouping);
		}

		return result;
	}

	/**
	 * The type an operation writes most (create counts most, a read barely), or null.
	 */
	private String writtenMost(final Set<String> crud, final Set<String> kept) {

		final Map<String, Double> score = new LinkedHashMap<>();

		if (crud != null) {

			for (final String entry : crud) {

				final String[] parts = entry.split(":", 2);
				final String typeId  = schema.getTypeIds().get(parts[1]);

				if (typeId != null && kept.contains(typeId)) {
					score.merge(typeId, WRITE_WEIGHT.getOrDefault(parts[0], 0.0), Double::sum);
				}
			}
		}

		return score.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
	}

	/**
	 * The area of a grouping, see the class comment.
	 */
	private String areaOf(final MapNode anchor, final CrudAnalysis analysis) {

		switch (anchor.kind) {

			case PAGE, COMPONENT, VIEW, ACTION:
				return AREA_UI;

			case API, EXTERNAL:
				return AREA_INTEGRATION;

			case METHOD:
				return anchor.cronExpression != null ? AREA_SCHEDULED : AREA_LOGIC;
		}

		if (BUILTIN_TYPES.contains(anchor.name)) {
			return AREA_BUILTIN;
		}

		if (!anchor.isService) {
			return AREA_DATA;
		}

		// a service class whose methods mostly write users and groups, or whose name says it is about rights, belongs to the
		// security; one whose methods are mostly scheduled runs by itself; any other holds logic
		final List<MapNode> own = new LinkedList<>();

		for (final MapEdge edge : edges.values()) {

			if ("member".equals(edge.kind) && anchor.id.equals(edge.target) && nodes.containsKey(edge.source)) {
				own.add(nodes.get(edge.source));
			}
		}

		final List<String> writes = new LinkedList<>();

		for (final MapNode method : own) {

			for (final String entry : analysis.crudOf(method.id) != null ? analysis.crudOf(method.id) : Set.<String>of()) {

				if (!entry.startsWith("R:")) {
					writes.add(entry.substring(2));
				}
			}
		}

		if ((!writes.isEmpty() && writes.stream().filter(SECURITY_TYPES::contains).count() * 2 > writes.size()) || SECURITY_NAME.matcher(anchor.name).find()) {
			return AREA_SECURITY;
		}

		final long scheduled = own.stream().filter(method -> method.cronExpression != null).count();

		return !own.isEmpty() && scheduled * 2 >= own.size() ? AREA_SCHEDULED : AREA_LOGIC;
	}

	/**
	 * The artifacts that take part: everything with a connection to another artifact, and the
	 * repeaters and action mappings of the pages and components that take part.
	 */
	private Set<String> keptNodes() {

		final Set<String> connected = new HashSet<>();
		final Set<String> kept      = new LinkedHashSet<>();

		for (final List<String> pair : connections) {

			final String a = pair.get(0);
			final String b = pair.get(1);

			if (nodes.containsKey(a) && nodes.containsKey(b)) {

				connected.add(a);
				connected.add(b);
			}
		}

		for (final MapNode node : nodes.values()) {

			if (node.home != null ? connected.contains(node.home) : connected.contains(node.id)) {
				kept.add(node.id);
			}
		}

		return kept;
	}

	private static String edgeKey(final String source, final String target, final String kind) {

		return source + "|" + target + "|" + kind;
	}

	// ----- nested classes -----
	public static class MapNode {

		private final String id;
		private final String kind;
		private final String name;
		private boolean isPublic;
		private boolean isService;
		private List<Object> outline;
		private String home;
		private String type;
		private String method;
		private String action;
		private String event;
		private String how;
		private String cronExpression;
		private int restRules;
		private boolean restPublic;
		private boolean restAuthenticated;
		private boolean restPublicWrite;
		private boolean restAuthenticatedWrite;
		private Map<String, Object> access;
		private int size;

		private MapNode(final String id, final String kind, final String name) {

			this.id   = id;
			this.kind = kind;
			this.name = name;
		}

		public String getId() {

			return id;
		}

		public String getKind() {

			return kind;
		}

		public String getName() {

			return name;
		}

		public String getType() {

			return type;
		}

		public String getMethod() {

			return method;
		}

		public String getAction() {

			return action;
		}

		public MapNode setPublic(final boolean isPublic) {

			this.isPublic = this.isPublic || isPublic;

			return this;
		}

		public MapNode setService(final boolean isService) {

			this.isService = isService;

			return this;
		}

		/**
		 * The HTML outline of a page or shared component: nested [tag, flags, children, ids] with
		 * the flags 1 (the element does something), 2 (it triggers an action) and 4 (it repeats),
		 * and the ids of the repeaters and action mappings that live on the element.
		 */
		public MapNode setOutline(final List<Object> outline) {

			this.outline = outline;

			return this;
		}

		/**
		 * The page or shared component a repeater or action mapping lives in; the node takes
		 * part in the map when its home does.
		 */
		public MapNode setHome(final String home) {

			this.home = home;

			return this;
		}

		public MapNode setType(final String type, final String how) {

			this.type = type;
			this.how  = how;

			return this;
		}

		public MapNode setMethod(final String method) {

			this.method = method;

			return this;
		}

		public MapNode setAction(final String action) {

			this.action = action;

			return this;
		}

		/**
		 * The event that triggers an action mapping, such as click or change.
		 */
		public MapNode setEvent(final String event) {

			this.event = event;

			return this;
		}

		/**
		 * The schedule of a method that CronService runs.
		 */
		public MapNode setCronExpression(final String cronExpression) {

			this.cronExpression = cronExpression;

			return this;
		}

		/**
		 * A resource access grant that opens this type or endpoint to REST: it is a property of the
		 * type, not an element of its own; the flags of the grant say whether it opens it to anonymous
		 * or to authenticated users, and whether they may write.
		 */
		public MapNode addRestAccess(final long flags) {

			this.restRules++;
			this.restPublic             = this.restPublic || (flags & EndpointMapContributor.PUBLIC_FLAGS) != 0;
			this.restAuthenticated      = this.restAuthenticated || (flags & EndpointMapContributor.AUTHENTICATED_FLAGS) != 0;
			this.restPublicWrite        = this.restPublicWrite || (flags & EndpointMapContributor.PUBLIC_WRITE_FLAGS) != 0;
			this.restAuthenticatedWrite = this.restAuthenticatedWrite || (flags & EndpointMapContributor.AUTHENTICATED_WRITE_FLAGS) != 0;

			return this;
		}

		/**
		 * How large the node is in its own terms: the properties of a type, the lines of code of
		 * a method, the HTML elements of a page or shared component.
		 */
		public MapNode setSize(final int size) {

			this.size = size;

			return this;
		}

		/**
		 * Who may see the node: its visibility, its owner and the principals with grants on it,
		 * each with the permissions it holds; for the pages and shared components, whose rights
		 * decide who reaches the application.
		 */
		public MapNode setAccess(final NodeInterface node) {

			final AccessControllable controllable = node.as(AccessControllable.class);
			final Principal owner                 = controllable.getOwnerNode();
			final List<List<String>> grants       = new LinkedList<>();

			for (final Security security : controllable.getSecurityRelationships()) {

				final Principal principal = security != null ? security.getSourceNode() : null;

				if (principal != null) {
					grants.add(List.of(String.valueOf(principal.getName()), String.join(",", security.getPermissions())));
				}
			}

			this.access = new LinkedHashMap<>();

			access.put("pub",  node.isVisibleToPublicUsers() ? 1 : 0);
			access.put("auth", node.isVisibleToAuthenticatedUsers() ? 1 : 0);

			if (owner != null) {
				access.put("own", owner.getName());
			}

			if (!grants.isEmpty()) {
				access.put("g", grants);
			}

			return this;
		}

		private Map<String, Object> toMap(final Set<String> kept, final Set<String> crud, final String grouping, final String area) {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("id",   id);
			map.put("k",    kind);
			map.put("n",    name);
			map.put("p",    isPublic ? 1 : 0);
			map.put("g",    grouping);
			map.put("area", area);

			if (outline != null) {
				map.put("d", outline);
			}

			if (crud != null && !crud.isEmpty()) {
				map.put("crud", new LinkedList<>(crud));
			}

			if (isService) {
				map.put("svc", true);
			}

			if (type != null) {
				map.put("t", type);
			}

			if (method != null && kept.contains(method)) {
				map.put("m", method);
			}

			if (how != null) {
				map.put("how", how);
			}

			if (action != null) {
				map.put("a", action);
			}

			if (event != null) {
				map.put("ev", event);
			}

			if (cronExpression != null) {
				map.put("cron", cronExpression);
			}

			if (access != null) {
				map.put("acc", access);
			}

			if (size > 0) {
				map.put("s", size);
			}

			if (restRules > 0) {
				map.put("rest", Map.of("n", restRules, "pub", restPublic ? 1 : 0, "auth", restAuthenticated ? 1 : 0, "pubw", restPublicWrite ? 1 : 0, "authw", restAuthenticatedWrite ? 1 : 0));
			}

			return map;
		}
	}

	static class MapEdge {

		private final String source;
		private final String target;
		private final String kind;
		private final double weight;
		private final String evidence;

		MapEdge(final String source, final String target, final String kind, final double weight, final String evidence) {

			this.source   = source;
			this.target   = target;
			this.kind     = kind;
			this.weight   = weight;
			this.evidence = evidence;
		}

		private List<Object> toList() {

			final List<Object> list = new ArrayList<>(6);

			list.add(source);
			list.add(target);
			list.add(Math.round(weight * 100) / 100.0);
			list.add(kind);

			// inferred: every edge rests on a match in a script or a page, none is a guess
			list.add(0);
			list.add(evidence);

			return list;
		}
	}

	public static class MapGroup {

		private final Set<String> grants = new LinkedHashSet<>();
		private final String id;
		private final String name;
		private final int members;

		private MapGroup(final String id, final String name, final int members) {

			this.id      = id;
			this.name    = name;
			this.members = members;
		}

		public void addGrant(final String typeId) {

			grants.add(typeId);
		}

		private Map<String, Object> toMap(final Set<String> kept) {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("id",   id);
			map.put("n",    name);
			map.put("m",    members);
			map.put("t",    grants.stream().filter(kept::contains).toList());
			map.put("g",    id);
			map.put("area", AREA_SECURITY);

			return map;
		}
	}
}
