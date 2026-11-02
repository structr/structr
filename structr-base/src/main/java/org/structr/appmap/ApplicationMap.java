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

import org.apache.commons.lang3.StringUtils;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * action mappings, a group, a REST endpoint; pages that belong together form a section of the
 * user interface instead (see sections); a user-defined function, like a method of a schema
 * relationship, belongs to the type it writes most, else it stands alone, and so does one that
 * CronService runs. The area says which of Structr's mechanisms the grouping is: "ui" (pages),
 * "components" (shared components), "integration" (REST endpoints and external services),
 * "security" (groups and the service classes that manage users and rights), "builtin" (the
 * system types the application extends), "logic" (user-defined functions and service classes),
 * "scheduled" (what CronService runs) and "data" (the application's own types).
 *
 * The result has six parts: "nodes" (id, k for the kind, n for the name, p for public, g for
 * the grouping, area, and depending on the kind d for the HTML outline (nested [tag, flags,
 * children, ids of the repeaters and action mappings on the element, template expressions its
 * text shows, data key of a repeater]), tpl for the template a page or shared component consists of alone, crud, svc, t for the
 * type, m for the method, how for the way the type was found, a and ev for the action and
 * the event of an action mapping, use for how a repeater or an action mapping uses its type
 * (see MapNode.setUse), cron for the schedule, rest for
 * the resource access grants on a type or endpoint: n rules, pub and auth for whom they open it,
 * pubw and authw whether those may write, acc for who may see a page or shared component:
 * pub and auth for its visibility, own for its owner, g for the grants as [principal,
 * permissions], s for its size: the properties of a type, the lines of code of a method, the
 * HTML elements of a page or shared component, dv for whom every object of a type is visible:
 * pub and auth, r for the URL routes of a page: p the path, cur the type of its current
 * object, w the number of warnings, pos for its position, err for the error codes it is shown
 * for, ct for a content type other than HTML, hits for the requests to a page, endpoint or
 * type: n their number, avg and max their average and longest time in milliseconds, see
 * HttpActivity, errs for the scripting errors since the last start: n their number, t the time,
 * msg the message and row the line in the node's own code of the latest, see ScriptErrors),
 * "edges"
 * ([source, target, weight, kind, inferred, evidence, details?]; the details of a schema
 * relationship: m for its multiplicity, cd for cascading delete, pp for the direction in which
 * it passes on rights, r, w, d and a for what it does with read, write, delete and access
 * control rights), "groups" (id, n, m for the number of members, t for the types they have
 * grants on, p for the rights per type as letters r, w and d, in for the groups they are
 * members of, g and area), "sites" (id, n for the host name and port, pages), "helpers" (the methods that
 * many call and that write nothing) and "sections" (the groupings of the user interface that
 * are no artifact: id, n for the name, how for what it rests on, members).
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
	public static final String AREA_COMPONENTS  = "components";
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

	// what a page does with a type, for the section it belongs to: changing counts twice, showing once
	private static final Map<String, Double> PAGE_WEIGHT = Map.of("C", 2.0, "U", 2.0, "D", 2.0, "R", 1.0);

	// navigation from an element that leads to more pages than this is the application's menu, it says nothing about where a page belongs
	private static final int MENU_LINKS = 8;

	private final Map<String, MapNode> nodes          = new LinkedHashMap<>();
	private final Map<String, MapEdge> edges          = new LinkedHashMap<>();
	private final Map<String, Integer> references     = new LinkedHashMap<>();
	private final Set<List<String>> connections       = new LinkedHashSet<>();
	private final Map<String, MapGroup> groups        = new LinkedHashMap<>();
	private final List<Map<String, Object>> sites     = new LinkedList<>();
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

		HttpActivity.apply(map);
		ScriptErrors.apply(map, app);

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

		addEdge(source, target, kind, weight, evidence, null);
	}

	/**
	 * Adds an edge with details of its own, e.g. what a schema relationship does on delete and
	 * which rights it passes on; they become the seventh part of the edge.
	 */
	public void addEdge(final String source, final String target, final String kind, final double weight, final String evidence, final Map<String, Object> details) {

		if (source == null || target == null || source.equals(target)) {
			return;
		}

		edges.putIfAbsent(edgeKey(source, target, kind), new MapEdge(source, target, kind, weight, evidence, details));
	}

	/**
	 * Adds a site: the host name (and port) it answers to and the pages it serves.
	 */
	public void addSite(final String id, final String name, final List<String> pages) {

		sites.add(Map.of("id", id, "n", name, "pages", pages));
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
	 * Only the requests and the scripting errors, per id of the nodes that have either, as hits and errs like in the nodes:
	 * a client refreshes them without loading the whole map, which needs no analysis for them. Nodes the map leaves out
	 * may be among them; a client ignores what it does not show.
	 */
	public GraphObjectMap toActivityObject() {

		final Map<String, Object> activity = new LinkedHashMap<>();

		for (final MapNode node : nodes.values()) {

			if (node.hasActivity()) {

				final Map<String, Object> values = new LinkedHashMap<>();

				node.putActivity(values);
				activity.put(node.id, values);
			}
		}

		final GraphObjectMap result = new GraphObjectMap();

		result.put(new GenericProperty<>("nodes"), activity);

		return result;
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

		final Map<String, Map<String, Object>> sections = sections(kept, analysis);
		final Map<String, String> groupings             = groupings(kept, analysis, sections);
		final Map<String, String> areas                 = new HashMap<>();
		final List<Object> sectionList                  = new LinkedList<>(sections.values());

		for (final String grouping : new HashSet<>(groupings.values())) {
			areas.put(grouping, sections.containsKey(grouping) ? AREA_UI : areaOf(nodes.get(grouping), analysis));
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
		result.put(new GenericProperty<>("sections"), sectionList);
		result.put(new GenericProperty<>("sites"),    sites.stream().map(site -> Map.of("id", site.get("id"), "n", site.get("n"), "pages", ((List<?>) site.get("pages")).stream().filter(kept::contains).toList())).toList());

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
	private Map<String, String> groupings(final Set<String> kept, final CrudAnalysis analysis, final Map<String, Map<String, Object>> sections) {

		final Map<String, String> sectionOf = new HashMap<>();

		for (final Map<String, Object> section : sections.values()) {

			for (final Object member : (List<?>) section.get("members")) {
				sectionOf.put((String) member, (String) section.get("id"));
			}
		}

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

			} else if (sectionOf.containsKey(id)) {

				grouping = sectionOf.get(id);
			}

			result.put(id, grouping);
		}

		return result;
	}

	/**
	 * The sections of the user interface: pages that belong together, each a grouping that is no artifact of its own, as
	 * { id, n for the name, how, members }. A page belongs to the category the developer gave it; else to the type it
	 * works with most (see PAGE_WEIGHT), through its repeaters and action mappings; else to the section of the pages that
	 * lead to it, by links and by the success and failure URLs of action mappings, apart from the menu (see MENU_LINKS),
	 * only when more than half of them lie in that section. A section of one page is none: it stays a grouping of its own.
	 * Shared components belong to no section, they form an area of their own.
	 */
	private Map<String, Map<String, Object>> sections(final Set<String> kept, final CrudAnalysis analysis) {

		final Map<String, String> sectionOf          = new LinkedHashMap<>();
		final Map<String, String> names              = new LinkedHashMap<>();
		final Map<String, String> hows               = new LinkedHashMap<>();
		final Map<String, Map<String, Double>> score = new LinkedHashMap<>();
		final Map<String, List<String>> leaders      = new LinkedHashMap<>();
		final Map<String, Set<String>> leadsTo       = new LinkedHashMap<>();
		final List<String> pages                     = new LinkedList<>();

		for (final String id : kept) {

			final MapNode node = nodes.get(id);

			if (PAGE.equals(node.kind)) {

				pages.add(id);

			} else if ((VIEW.equals(node.kind) || ACTION.equals(node.kind)) && node.home != null) {

				for (final String entry : analysis.crudOf(id) != null ? analysis.crudOf(id) : Set.<String>of()) {

					final String[] parts = entry.split(":", 2);
					final String typeId  = schema.getTypeIds().get(parts[1]);

					if (typeId != null && kept.contains(typeId)) {
						score.computeIfAbsent(node.home, k -> new LinkedHashMap<>()).merge(typeId, PAGE_WEIGHT.getOrDefault(parts[0], 0.0), Double::sum);
					}
				}
			}
		}

		for (final MapEdge edge : edges.values()) {

			if (!kept.contains(edge.source) || !kept.contains(edge.target) || edge.source.equals(edge.target)) {
				continue;
			}

			if ("navigates".equals(edge.kind)) {

				leaders.computeIfAbsent(edge.target, k -> new LinkedList<>()).add(edge.source);
				leadsTo.computeIfAbsent(edge.source, k -> new LinkedHashSet<>()).add(edge.target);
			}
		}

		// what the developer said, then what the page works with
		for (final String id : pages) {

			final MapNode node     = nodes.get(id);
			final String typeId    = mostOf(score.get(id));
			final boolean declared = StringUtils.isNotBlank(node.category);

			if (declared || typeId != null) {

				final String section = declared ? "section:category:" + node.category.trim() : "section:type:" + typeId;

				sectionOf.put(id, section);
				names.putIfAbsent(section, declared ? node.category.trim() : nodes.get(typeId).name + " pages");
				hows.putIfAbsent(section, declared ? "category" : "type");
			}
		}

		// then where it is reached from; twice, so a page reached from a page placed in the first round finds its place
		for (int round = 0; round < 2; round++) {

			for (final String id : pages) {

				if (sectionOf.containsKey(id)) {
					continue;
				}

				final List<String> from         = leaders.getOrDefault(id, List.of());
				final List<String> counted      = from.stream().filter(source -> leadsTo.getOrDefault(source, Set.of()).size() <= MENU_LINKS).toList();
				final Map<String, Double> votes = new LinkedHashMap<>();

				for (final String source : counted) {

					if (sectionOf.containsKey(source)) {
						votes.merge(sectionOf.get(source), 1.0, Double::sum);
					}
				}

				final String section = mostOf(votes);

				// only where most of them agree: what pages of every section hold, like the frame of the application, belongs to none
				if (section != null && votes.get(section) * 2 > counted.size()) {
					sectionOf.put(id, section);
				}
			}
		}

		final Map<String, List<String>> members         = new LinkedHashMap<>();
		final Map<String, Map<String, Object>> sections = new LinkedHashMap<>();

		sectionOf.forEach((id, section) -> members.computeIfAbsent(section, k -> new LinkedList<>()).add(id));

		members.forEach((id, list) -> {

			if (list.size() > 1) {
				sections.put(id, new LinkedHashMap<>(Map.of("id", id, "n", names.get(id), "how", hows.get(id), "members", list)));
			}
		});

		return sections;
	}

	// the key of the highest score, the first of equal ones, or null
	private static String mostOf(final Map<String, Double> score) {

		return score == null ? null : score.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
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

		return mostOf(score);
	}

	/**
	 * The area of a grouping, see the class comment.
	 */
	private String areaOf(final MapNode anchor, final CrudAnalysis analysis) {

		switch (anchor.kind) {

			case PAGE, VIEW, ACTION:
				return AREA_UI;

			case COMPONENT:
				return AREA_COMPONENTS;

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
		private String template;
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
		private String category;
		private Map<String, Object> defaultVisibility;
		private List<Object> routes;
		private Integer position;
		private String errorCodes;
		private String contentType;
		private Map<String, Object> use;
		private long requests;
		private long requestTime;
		private long slowest;
		private int errors;
		private String lastError;
		private Integer errorRow;
		private long lastErrorTime;

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
		 * The HTML outline of a page or shared component: nested [tag, flags, children, ids,
		 * expressions, data key] with the flags 1 (the element does something), 2 (it triggers
		 * an action) and 4 (it repeats), the ids of the repeaters and action mappings that live
		 * on the element, the template expressions its text shows (empty if none) and the data
		 * key under which a repeater names each object it shows. Trailing parts that are empty
		 * are left out.
		 */
		public MapNode setOutline(final List<Object> outline) {

			this.outline = outline;

			return this;
		}

		/**
		 * The template a page or shared component consists of alone, whose text a client can
		 * show in place of the outline.
		 */
		public MapNode setTemplate(final String template) {

			this.template = template;

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
		 * How a repeater or an action mapping uses its type, in the terms of the page: q for the
		 * data expression of a repeater, role for what its copies are (choice for the options of
		 * a select or datalist, row for table rows, item for anything else), f for the property
		 * a choice sets, as Type.property where the type is known; el for the element that
		 * triggers an action mapping and in for the names of the user input it sends.
		 */
		public MapNode setUse(final Map<String, Object> use) {

			this.use = use != null && !use.isEmpty() ? use : null;

			return this;
		}

		/**
		 * Requests to an entry point, as counted by the HTTP service: their number, their average
		 * and their longest time in milliseconds; several sources, like the views of a type, add up.
		 */
		public MapNode addActivity(final long count, final long average, final long max) {

			this.requests    += count;
			this.requestTime += count * average;
			this.slowest      = Math.max(slowest, max);

			return this;
		}

		/**
		 * Scripting errors that happened in the node: how many, and the message, the line in the
		 * node's own code (null for code elsewhere, like an HTML element of a page) and the time
		 * of the latest; several sources, like the elements of a page, add up.
		 */
		public MapNode addErrors(final int count, final String message, final Integer row, final long time) {

			this.errors += count;

			if (time >= lastErrorTime) {

				this.lastError     = message;
				this.errorRow      = row;
				this.lastErrorTime = time;
			}

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
		// which users see every object of a type: pub and auth, see the class comment
		public MapNode setDefaultVisibility(final boolean pub, final boolean auth) {

			this.defaultVisibility = pub || auth ? Map.of("pub", pub ? 1 : 0, "auth", auth ? 1 : 0) : null;

			return this;
		}

		/**
		 * How a page is reached: its URL routes as { p: the path, cur: the type of its current object, w: the number of
		 * warnings }, its position (the start page has the lowest), the error codes it is shown for and a content type other
		 * than HTML.
		 */
		public MapNode setRouting(final List<Object> routes, final Integer position, final String errorCodes, final String contentType) {

			this.routes      = routes != null && !routes.isEmpty() ? routes : null;
			this.position    = position;
			this.errorCodes  = StringUtils.isNotBlank(errorCodes) ? errorCodes : null;
			this.contentType = StringUtils.isNotBlank(contentType) && !"text/html".equals(contentType) ? contentType : null;

			return this;
		}

		// the category a developer gave a page, see sections
		public MapNode setCategory(final String category) {

			this.category = category;

			return this;
		}

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

			if (template != null) {
				map.put("tpl", template);
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

			if (defaultVisibility != null) {
				map.put("dv", defaultVisibility);
			}

			if (routes != null) {
				map.put("r", routes);
			}

			if (position != null) {
				map.put("pos", position);
			}

			if (errorCodes != null) {
				map.put("err", errorCodes);
			}

			if (contentType != null) {
				map.put("ct", contentType);
			}

			if (use != null) {
				map.put("use", use);
			}

			putActivity(map);

			if (restRules > 0) {
				map.put("rest", Map.of("n", restRules, "pub", restPublic ? 1 : 0, "auth", restAuthenticated ? 1 : 0, "pubw", restPublicWrite ? 1 : 0, "authw", restAuthenticatedWrite ? 1 : 0));
			}

			return map;
		}

		// the requests and the scripting errors of the node, as hits and errs, see the class comment
		private void putActivity(final Map<String, Object> map) {

			if (requests > 0) {
				map.put("hits", Map.of("n", requests, "avg", requestTime / requests, "max", slowest));
			}

			if (errors > 0) {

				final Map<String, Object> errs = new LinkedHashMap<>();

				errs.put("n", errors);
				errs.put("t", lastErrorTime);

				if (lastError != null) {
					errs.put("msg", lastError);
				}

				if (errorRow != null) {
					errs.put("row", errorRow);
				}

				map.put("errs", errs);
			}
		}

		private boolean hasActivity() {

			return requests > 0 || errors > 0;
		}
	}

	static class MapEdge {

		private final String source;
		private final String target;
		private final String kind;
		private final double weight;
		private final String evidence;
		private final Map<String, Object> details;

		MapEdge(final String source, final String target, final String kind, final double weight, final String evidence) {

			this(source, target, kind, weight, evidence, null);
		}

		MapEdge(final String source, final String target, final String kind, final double weight, final String evidence, final Map<String, Object> details) {

			this.source   = source;
			this.target   = target;
			this.kind     = kind;
			this.weight   = weight;
			this.evidence = evidence;
			this.details  = details;
		}

		private List<Object> toList() {

			final List<Object> list = new ArrayList<>(7);

			list.add(source);
			list.add(target);
			list.add(Math.round(weight * 100) / 100.0);
			list.add(kind);

			// inferred: every edge rests on a match in a script or a page, none is a guess
			list.add(0);
			list.add(evidence);

			if (details != null && !details.isEmpty()) {
				list.add(details);
			}

			return list;
		}
	}

	public static class MapGroup {

		private final Map<String, String> grants = new LinkedHashMap<>();
		private final Set<String> parents        = new LinkedHashSet<>();
		private final String id;
		private final String name;
		private final int members;

		private MapGroup(final String id, final String name, final int members) {

			this.id      = id;
			this.name    = name;
			this.members = members;
		}

		// what the group may do with every object of a type, as the letters r (read), w (write) and d (delete)
		public void addGrant(final String typeId, final String rights) {

			grants.merge(typeId, rights, (a, b) -> Stream.of("r", "w", "d").filter(right -> a.contains(right) || b.contains(right)).collect(Collectors.joining()));
		}

		// a group the group is a member of: its members have the rights of that group too
		public void addParent(final String groupId) {

			parents.add(groupId);
		}

		private Map<String, Object> toMap(final Set<String> kept) {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("id",   id);
			map.put("n",    name);
			map.put("m",    members);
			map.put("t",    grants.keySet().stream().filter(kept::contains).toList());
			map.put("p",    grants.entrySet().stream().filter(e -> kept.contains(e.getKey())).collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll));
			map.put("in",   new LinkedList<>(parents));
			map.put("g",    id);
			map.put("area", AREA_SECURITY);

			return map;
		}
	}
}
