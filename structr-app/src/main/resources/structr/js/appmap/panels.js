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

/**
 * The inner structure of the elements on the application map, shown inside the element itself as it comes close: the
 * code of a method in its rectangle, and the DOM of a page or shared component. Close enough, the page
 * opens inside its own sheet as a panel with its tree from left to right, every HTML element a labelled box as in the
 * page tree of the Pages area. The repeaters and event action mappings live on their HTML elements, so in an open page
 * their relations leave from the box of that element instead of from the page. Closer still, an HTML element opens in
 * the same way inside its own box: a card with its kind and name, and below them the controls for its settings, its
 * repeater, conditions, action mappings and text (see AppMapView.followElement).
 */
class AppMapPanels {

	// the panel tree from top to bottom, in units of the map: boxes BOX high, ROW the measure of how large the tree is on
	// screen (see OPEN_ROW); GAP across between the boxes of siblings, twice that between cousins, LEVEL down between the
	// levels of the tree, each as high as its highest box; the document at the root is ROOT wide and high
	static ROW    = 2.6;
	static BOX    = 1.8;
	static GAP    = 1.2;
	static LEVEL  = 2.6;
	static ROOT   = [3.8, 4.8];

	// siblings without children stand one below the other from this many in a row on, see layoutOf
	static STACK  = 3;

	// an HTML element that triggers an event action mapping is marked in this colour
	static ACTION_COLOR = '#e06a40';

	// an HTML element opens inside its box: from ELEMENT_SKETCH pixels of box height on as a sketch, open when its
	// controls, laid out for a height of ELEMENT_SIZE[1] pixels and at least ELEMENT_SIZE[0] wide, fit in at ELEMENT_OPEN
	// of that size; they fill the box, so the open element keeps its shape. An element with settings or a text is
	// ELEMENT_SIGN wider for a small magnifier that says that there is more inside
	static ELEMENT_SKETCH = 40;
	static ELEMENT_SIZE   = [300, 190];
	static ELEMENT_OPEN   = 0.8;
	static ELEMENT_SIGN   = 1.3;
	// the corners of a box and the width of the bar on its left in the colour of what it does, in parts of its height
	static ELEMENT_CORNER = 0.08;
	// a template, and a script or a style with code, stands upright like the sheet of a page, so that more of its content
	// fits when it opens: width and height
	static TEMPLATE_BOX   = [5.4, 7.4];
	// the size the controls of a sheet are made for, see ELEMENT_SIZE: smaller than for other elements, so that it opens
	// about as early
	static SHEET_SIZE     = [165, 220];
	// the elements whose text is code, with the content type it has unless it names one
	static SCRIPTS        = { script: 'text/javascript', style: 'text/css' };

	// the sources of a repeater, as the Pages area names them
	static SOURCES = [['functionQuery', 'Function'], ['cypherQuery', 'Cypher'], ['flow', 'Flow']];

	// a page opens when a row of its tree is this many pixels high; from SKETCH_ROW on, its tree already shows as a sketch
	// without names that grows stronger as the page comes closer, so the reader sees what will open
	static OPEN_ROW   = 9;
	static SKETCH_ROW = 1.5;

	// the code of a method in its rectangle: at least and at most this many lines fill its height, each line cut after
	// CODE_COLUMNS characters; from CODE_SKETCH pixels per line it shows as grey bars, from CODE_READ on as text
	static CODE_LINES   = [16, 40];
	static CODE_COLUMNS = 100;
	static CODE_SKETCH  = 1.5;
	static CODE_READ    = 7;

	// a type opens to a card in place of its circle, its rows this many radii across from its middle, and as far down as
	// they need (see insidePanel); the second, how far down a card is looked for on the screen, as a long one reaches
	// further: its properties as rows, TILE in units of the size of their text across and down, TILE_GAP apart across and
	// down, TILE_WIDE of them across, below a heading per group TILE_HEADING high and TILE_PAD from the
	// sides. The box of a row, the square before its name, opens in itself from TILE_SKETCH to TILE_OPEN pixels across,
	// see drawBox
	static INSIDE_CARD     = [1.15, 6];

	// the methods of HTTP a REST endpoint answers, as the bits of the flags of a resource access permission for signed in
	// and for public users, and their colours as the documentation of APIs shows them, [light, dark], taken from the colours
	// of status of the design system and the purple of its Screens for PATCH
	static VERBS       = [['GET', 1, 16], ['POST', 4, 64], ['PUT', 2, 32], ['PATCH', 4096, 8192], ['DELETE', 8, 128]];
	static VERB_COLOR  = { GET: ['#23763f', '#5fb87e'], POST: ['#2b6cb8', '#7fb0ff'], PUT: ['#b5480f', '#f28a4c'], PATCH: ['#6f66d4', '#a9a3f0'], DELETE: ['#c22a2a', '#f4746e'] };

	// the rights of a group on a type as the badges of its grant (see groupRows): the badge, a letter so that the names of
	// the types keep their room, the letter the map data has for it and its name, in the colours of the methods that do the
	// same over REST
	static RIGHTS      = [['R', 'r', 'Read'], ['W', 'w', 'Write'], ['D', 'd', 'Delete'], ['A', 'a', 'Access control']];
	static RIGHT_COLOR = { R: AppMapPanels.VERB_COLOR.GET, W: AppMapPanels.VERB_COLOR.PUT, D: AppMapPanels.VERB_COLOR.DELETE, A: AppMapPanels.VERB_COLOR.PATCH };

	// the name of a row on a card is this large, in units of the size of its text (em), see drawTile and
	// AppMapView.fillZoom
	static ROW_NAME = 0.9;

	// every box stands open this many times the size of its text wide, whatever it shows (see openWidth), so that all boxes
	// open to the same width and the same text, and only their height follows what they hold
	static BOX_TEXT_WIDTH = 34;

	// the lines of text a box holds, in the pitch all boxes share (see drawBox): those of a permission, a relationship, a
	// field or a summary as they count them, else its settings
	static boxLines(tile) {

		return tile.access?.lines ?? tile.relation?.lines ?? tile.field?.lines ?? (tile.summary ? AppMapPanels.summaryLines(tile) : (tile.settings?.length ?? 0));
	}

	// how high a box stands open on the screen, in pixels: its header, its lines at TILE_TEXT and its foot, at least a
	// little more than TILE_OPEN (see AppMapView.tileZoom)
	static openHeight(tile) {

		return Math.max(AppMapPanels.HEAD.band + (AppMapPanels.boxLines(tile) + 0.6) * AppMapPanels.TILE_TEXT * 2.1 + AppMapPanels.HEAD.foot, AppMapPanels.TILE_OPEN * 1.1);
	}

	// how wide a box stands open, on the map: BOX_TEXT_WIDTH times TILE_TEXT on the screen where it stands fully open, which
	// is where it is openHeight high; never narrower than it is high
	static openWidth(item, full) {

		return Math.max(full, full * AppMapPanels.BOX_TEXT_WIDTH * AppMapPanels.TILE_TEXT / AppMapPanels.openHeight(item.tile));
	}

	// where a box stands open across, on the map: from its left edge, or for one at the end of its row, a permission, from
	// its right edge to the left, so that it stays on its card
	static openLeft(item, x, square, width) {

		return item.tile?.access ? x + square - width : x;
	}

	// the lines of the matrix of a permission stand this much closer than the settings of a box, see drawMatrix
	static ACCESS_LINE = 0.72;

	// the chart of the requests per day in the box of a permission is this many of its lines high, see drawMatrix
	static CHART_LINES = 4;

	// the states of a cell of the matrix of a permission (see matrixOf) as their meaning and their colours [light, dark], the
	// colours of status of the design system: yes allowed, some allowed only for the users and groups with rights on the
	// permission, none set but without effect, implied
	// allowed for a user or group as for every signed-in user, no not allowed
	static ACCESS_CELL = {
		yes:     ['Allowed', '#23763f', '#5fb87e'],
		some:    ['Only the users and groups below', '#2b6cb8', '#7fb0ff'],
		none:    ['Set, but without effect – click to fix', '#8a6500', '#e6c13f'],
		implied: ['Allowed for every signed-in user', '#8a8678', '#8b887d'],
		no:      ['Not allowed', '#d8d5ca', '#3a3832']
	};

	// the kinds of elements that open to such a card
	static INSIDE_KINDS = new Set(['type', 'api', 'group', 'external']);
	static TILE          = [12, 1.9];
	static TILE_GAP      = [1.6, 0.12];
	static TILE_HEADING  = 1.8;
	static TILE_PAD      = 1.2;

	// the box of a row carries the sign that it opens (see drawOpenSign) once it is this many pixels high on the screen
	static OPEN_SIGN     = 14;

	// the columns of a section of a card, unless it says otherwise, and the least width of the rows of a card in units of
	// TILE, see insidePanel
	static TILE_ACROSS   = 2;
	static TILE_WIDE     = 3;

	// the badge of the method of a request, and the box of a permission, is this many times as wide as the square of other rows
	static BADGE         = 2.4;
	static TILE_SKETCH   = 110;
	static TILE_OPEN     = 200;

	// the signs of the types of fields, see drawTypeSign
	static TYPE_SIGNS = {
		String: 'Aa', Encrypted: 'Aa', Thumbnail: 'Aa', Join: 'Aa',
		Integer: '#', Long: '#', Double: '#', Count: '#',
		Boolean: '✓',
		Date: 'date', ZonedDateTime: 'date',
		// a function property is a script, as its curly braces in the Code area say
		Function: '{}', Cypher: 'ƒ', Custom: 'ƒ',
		Enum: '≡'
	};

	// the text in an open tile is no larger than this many pixels on the screen, a little smaller than the name in its
	// header; the headings of the groups no larger than the kind in a header
	static TILE_TEXT     = 15;

	// the content of an open HTML element is this many pixels high when its box has the size of ELEMENT_SIZE, and the
	// advance of a character of code type as a share of its size
	static CONTENT_TEXT  = 13;
	static CODE_ADVANCE  = 0.6;

	// the code on an open sheet is as small as this many characters fill its width
	static SHEET_COLUMNS = 44;

	// an open page is a card with a header (see header): its padding as a share of its shorter side
	static CARD_PAD  = 0.05;

	// the header of every card has the same size on the screen, in pixels: the height of its band, the size of the name
	// and of the kind, and how far they stand from the sides; the foot with the way into the Code area is as high as foot; a card smaller on the screen than HEAD_FULL (across, down)
	// has a header that much smaller, so it never fills the card
	static HEAD      = { band: 46, name: 17, kind: 10.5, inset: 18, foot: 46 };

	// what the kind on the right of the header of a card is, for the tooltip over it (see kindAt), by the text it shows
	static KIND_ABOUT = {
		'Page':                 'A page of the application, which Structr renders for its URL from its HTML elements.',
		'Shared Component':     'A piece of HTML that several pages include. A change to it shows on every page that uses it.',
		'Type':                 'A type of the data model, defined in the schema. Its objects are stored in the database with its properties and relationships.',
		'Service class':        'A type of the schema that has no objects and only holds methods.',
		'Method':               'A method of a type or a user-defined function. Pages, other methods, REST requests or the scheduler call it.',
		'REST Endpoint':        'A path of the REST API. Resource access permissions decide which HTTP methods may be sent to it, and by whom.',
		'External Service':     'A service outside of Structr that the application uses: it sends requests or mail to it, or queries it.',
		'Group':                'A group of users. Its rights on types and its members decide what the users in it may do.',
		'User':                 'A user who is a member of the group.',
		'Repeater':             'An HTML element that is repeated for every object of a collection, from a function query, a Cypher query or a flow.',
		'Event Action Mapping': 'Connects an event on an HTML element, such as a click, to an action: creating, changing or deleting an object, or calling a method.',
		'Template':             'An HTML element whose content is written as text with template expressions, instead of as a tree of HTML elements.',
		'Script':               'A script or a text of a page.',
		'Style':                'A style element of a page, with CSS.',
		'HTML element':         'An element of the HTML structure of a page or shared component.',
		'Property':             'A property of the type, defined in the schema.',
		'Relationship':         'A relationship of the type to another type, defined in the schema.',
		'Request':              'A path of the REST endpoint, with the permission that decides which HTTP methods may be sent to it, and by whom.',
		'Permission':           'A resource access permission: which HTTP methods it allows on a path of the REST API, for public and for signed-in users.',
		'Grant':                'The rights of the group on a type: read, write, delete or control access.'
	};
	static HEAD_FULL = [240, 160];

	// the corners of every card are rounded by this many pixels on the screen, or less on a small card; the colour of the
	// card runs along its top edge BAND pixels deep
	static CORNER = 14;
	static BAND   = 3;

	constructor(view) {

		this.view  = view;
		this.trees = new Map();
		this.code  = new Map();
		this.insides = new Map();

		// the texts of HTML elements by their id, read when they open, see requestContent
		this.contents = new Map();

		// the methods whose code was readable in the last frame: they carry their name inside, not beside them
		this.reading = new Set();

		// the types whose rows were readable in the last frame
		this.inspecting = new Set();

		// the properties shown as tiles in the last frame, and the cards open in it that lead into the Code area, see
		// AppMapView.placeOpenButton
		this.tiles = [];
		this.cards = [];

		// the boxes of settings that can be switched in the last frame, see toggleAt
		this.toggles = [];

		this.buildTrees();
	}

	get domCount() {

		return d3.sum([...this.trees.values()], t => t.list.length);
	}

	buildTrees() {

		const inner  = this.view.model.inner;
		const byId   = this.view.byId;
		const toTree = item => ({
			tag:       item[0],
			active:    !!(item[1] & 1),
			action:    !!(item[1] & 2),
			repeater:  !!(item[1] & 4),
			children:  (item[2] ?? []).map(toTree),
			inner:     (item[3] ?? []).map(id => byId.get(id)).filter(n => n && inner.has(n)),
			shows:     item[4] || null,
			key:       item[5] ?? null,
			id:        item[6] ?? null,
			text:      !!(item[1] & 8),
			when:      !!(item[1] & 16),
			component: !!(item[1] & 32),
			sources:   item[7] ?? []
		});

		for (const page of this.view.nodes.filter(n => (n.k === 'page' || n.k === 'component') && n.d?.length && !n.tpl)) {

			const root = d3.hierarchy({ tag: page.n, children: page.d.map(toTree) });

			// the boxes of one level stand side by side, as far apart as their widths need; a run of leaves stands as a stack
			// (see layoutOf), which the layout takes for a chain, one leaf below the other
			const all    = root.descendants();
			const layout = AppMapPanels.layoutOf(root);
			const width  = d => d.stack ? d3.max(d.stack, AppMapPanels.width) : AppMapPanels.width(d);

			d3.tree().nodeSize([1, 1]).separation((a, b) => (width(a.data) + width(b.data)) / 2 + AppMapPanels.GAP * (a.parent === b.parent ? 1 : 2))(layout);

			const levels = [];

			for (const l of layout.descendants()) {

				l.data.x     = l.x;
				l.data.level = l.depth;
				levels[l.depth] = Math.max(levels[l.depth] ?? 0, AppMapPanels.sizeOf(l.data)[1]);
			}

			// the middle of each level, below the one above it
			const middle = [];

			levels.reduce((y, height, depth) => (middle[depth] = y + height / 2, y + height + AppMapPanels.LEVEL), 0);

			const left = d3.min(all, d => d.x - width(d) / 2);

			// the place of a box is its left end and its middle, as box gives it; the leaves of a stack align on the left
			for (const d of all) {

				d.lx   = d.x - width(d) / 2 - left + AppMapPanels.GAP;
				d.ly   = middle[d.level];
				d.page = page;
			}

			const top    = -AppMapPanels.GAP;
			const bottom = d3.sum(levels) + AppMapPanels.LEVEL * (levels.length - 1) + AppMapPanels.GAP;

			// a repeater that cannot work, judged as the page tree of the Pages area judges it (outside the admin UI, as in
			// the preview, there is no judge); the page itself (the root) is no element, and a shared component is no page,
			// so its context is not known
			for (const d of all.slice(1)) {

				const ancestors = d.ancestors().slice(1, -1).map(a => ({ dataKey: a.data.key, component: a.data.component }));

				d.warnings = typeof _Pages === 'undefined' ? [] : _Pages.problems.checkRepeater({ dataKey: d.data.key, component: d.data.component, sources: d.data.sources }, { ancestors, inPage: page.k === 'page' });
			}

			const boxOf = new Map(all.flatMap(d => (d.data.inner ?? []).map(n => [n, d])));

			this.trees.set(page, { root, list: all.slice(1), boxOf, open: false, frame: null, panel: { width: d3.max(all, d => d.lx + AppMapPanels.sizeOf(d)[0]) + AppMapPanels.GAP, top, height: bottom - top } });
		}
	}

	// the place of the tree in the sheet of a page, with the height of a row on screen: the sheet becomes a card with a
	// header for the kind and the name, and the tree fills the room below it, at its top and in the middle across
	pagePanel(page, tree, k, radius = this.view.shownRadius(page)) {

		// the sheet exactly as the element is drawn, at the current zoom or, for a glide, at the one it heads for
		const r      = Math.max(radius, 2 / k);
		const hw     = r * AppMapRenderer.PAGE_BOX[0];
		const hh     = r * AppMapRenderer.PAGE_BOX[1];
		const head   = AppMapPanels.header(page.x - hw, page.y - hh, 2 * hw, 2 * hh, k);
		// below the header the strip of what the page is (see drawPageStrip), the tree right below it: both spaced by the
		// header, which has its size on the screen, so that a large sheet has no wide gaps
		const strip  = head.depth * AppMapPanels.PAGE_STRIP;
		const top    = head.line + strip;
		const room   = [2 * hw - 2 * head.pad, page.y + hh - top - head.pad];
		const scale  = Math.min(room[0] / tree.panel.width, room[1] / tree.panel.height);
		const width  = tree.panel.width * scale;
		const height = tree.panel.height * scale;

		return { x0: page.x - width / 2, y0: top, width, height, scale, top: tree.panel.top, hw, hh, head, strip, row: scale * AppMapPanels.ROW * k };
	}

	// the sizes of the header of a card of the given size at a zoom, in map units, see HEAD
	static headSize(width, height, k) {

		const unit = Math.min(1, width * k / AppMapPanels.HEAD_FULL[0], height * k / AppMapPanels.HEAD_FULL[1]) / k;

		return { depth: AppMapPanels.HEAD.band * unit, name: AppMapPanels.HEAD.name * unit, kind: AppMapPanels.HEAD.kind * unit, inset: AppMapPanels.HEAD.inset * unit, foot: AppMapPanels.HEAD.foot * unit };
	}

	// the header of a card at the given place and size at a zoom: the band of drawHeader with the name and the kind in one
	// line, with the padding of the card. Line is where the band ends, bottom where the content below it begins
	static header(x0, y0, width, height, k) {

		const pad  = Math.min(width, height) * AppMapPanels.CARD_PAD;
		const size = AppMapPanels.headSize(width, height, k);
		const line = y0 + size.depth;

		return { x0, y0, width, height, pad, ...size, line, bottom: line + pad * 0.8 };
	}

	// the frame is the element itself: its sheet, open, as a card on the map, as the node window of the design system has it:
	// white, rounded, with a fine border and a soft shadow, its colour only in the edge along its top (see drawBand); the
	// card covers what lies beneath by the given share
	drawFrame(ctx, page, panel, colour, k, cover) {

		this.drawCard(ctx, page.x - panel.hw, page.y - panel.hh, 2 * panel.hw, 2 * panel.hh, colour, k, cover);
	}

	drawCard(ctx, x, y, width, height, colour, k, cover, radius = AppMapPanels.corner(width, height, k)) {

		const look = this.view.look;

		ctx.save();
		ctx.beginPath();
		ctx.roundRect(x, y, width, height, radius);

		// the shadow is given in pixels on the screen, whatever the zoom
		ctx.globalAlpha   = cover;
		ctx.shadowColor   = look.dark ? 'rgba(0, 0, 0, 0.55)' : 'rgba(40, 38, 30, 0.10)';
		ctx.shadowBlur    = look.dark ? 44 : 34;
		ctx.shadowOffsetY = look.dark ? 16 : 12;
		ctx.fillStyle     = look.card;
		ctx.fill();
		ctx.restore();

		ctx.save();
		ctx.beginPath();
		ctx.roundRect(x, y, width, height, radius);
		ctx.globalAlpha = Math.min(1, cover * 1.2);
		ctx.strokeStyle = look.border;
		ctx.lineWidth   = 1 / k;
		ctx.stroke();
		ctx.restore();
	}

	// the header of a card, the same on every card, as the node window of the design system has it: on the card itself, its
	// name on the left in the size of HEAD and no wider than the header leaves beside the kind, which stands on the right as
	// an outlined chip in the colour of the element; the colour itself runs along the top edge of the card (see drawBand),
	// and a fine line closes the header off at head.line. The header reaches across the card (head.band: x, width and the
	// outline of the card it is cut to, else rounded at the top as cards are); on a sheet the kind keeps clear of its folded
	// corner (head.band.fold)
	drawHeader(renderer, head, kind, name, colour, k) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const band   = head.band ?? { x: head.x0, width: head.width, outline: null };
		const bottom = head.line;
		const left   = band.x + head.inset;
		const right  = band.x + band.width - head.inset - (band.fold ?? 0);
		const small  = head.kind;
		const beside = kind ? AppMapPanels.labelWidth(renderer, kind, small, 500) + head.inset : 0;
		const fit    = AppMapPanels.fitText(renderer, name, 600, head.name, right - left - beside);
		const middle = (head.y0 + bottom) / 2;

		ctx.save();

		if (band.outline) {
			ctx.clip(band.outline);
		}

		this.drawBand(ctx, { ...head, x0: band.x, width: band.width }, colour, !!band.outline);

		ctx.fillStyle = look.text;
		renderer.screenText(fit.text, left, middle, 600, fit.size, false, 'left');

		if (kind) {

			const wide = AppMapPanels.labelWidth(renderer, kind, small, 500);
			const tall = small * AppMapPanels.PILL_HEIGHT;

			this.drawLabel(renderer, kind, right, middle, small, 'chip', colour, k, 'right');
			this.kinds.push({ kind, box: [right - wide, middle - tall / 2, right, middle + tall / 2] });
		}

		this.drawHairline(ctx, band.x, band.x + band.width, bottom, k);
		ctx.restore();
	}

	// the radius of the corners of a card of the given size at a zoom, see CORNER
	static corner(width, height, k) {

		return Math.min(AppMapPanels.CORNER / k, Math.min(width, height) * 0.035);
	}

	// the colour of a card along its top edge, BAND pixels on the screen, or straight where the caller has cut it to the
	// outline of the card already; a card that is still a sketch shows it alone. Rounded, it runs on into the corners as a
	// CSS border-top does beside a border of one pixel: between the outline of the card and an inner outline whose corners
	// are smaller by the widths of both borders, thinning along the curve and ending on the diagonal from the outer to the
	// inner corner, as on structr.org
	drawBand(ctx, head, colour, cut = false) {

		const k      = this.view.transform.k;
		const radius = AppMapPanels.corner(head.width, head.height, k);
		const depth  = Math.min(AppMapPanels.BAND / k, (head.line - head.y0) / 3);
		const side   = 1 / k;
		const { x0, y0, width } = head;

		if (depth <= 0) {
			return;
		}

		ctx.save();
		ctx.fillStyle = colour;

		if (cut) {

			ctx.fillRect(x0, y0, width, depth);

		} else {

			const bottom = y0 + radius + depth;
			const slant  = side * (radius + depth) / depth;
			const inner  = { x: Math.max(0, radius - side), y: Math.max(0, radius - depth) };

			ctx.beginPath();
			ctx.moveTo(x0, y0);
			ctx.lineTo(x0 + width, y0);
			ctx.lineTo(x0 + width - slant, bottom);
			ctx.lineTo(x0 + slant, bottom);
			ctx.closePath();
			ctx.clip();

			ctx.beginPath();
			ctx.roundRect(x0, y0, width, radius * 2 + depth, [radius, radius, 0, 0]);
			ctx.roundRect(x0 + side, y0 + depth, width - 2 * side, radius * 2 + depth, [inner, inner, 0, 0]);
			ctx.fill('evenodd');
		}

		ctx.restore();
	}

	// a text at the given size, made smaller to fit the width, down to half of it, and cut with an ellipsis below that
	static fitText(renderer, text, weight, size, width) {

		const per = renderer.textWidth(text, weight) / AppMapRenderer.LABEL_SIZE;

		if (per * size <= width) {
			return { text, size };
		}

		if (per * size / 2 <= width) {
			return { text, size: width / per };
		}

		const small = size / 2;
		let cut     = text;

		while (cut.length > 1 && renderer.textWidth(cut + '…', weight) / AppMapRenderer.LABEL_SIZE * small > width) {
			cut = cut.slice(0, -1);
		}

		return { text: cut + '…', size: small };
	}

	// a text at the given size, cut with an ellipsis where it is wider than the width
	static cutText(renderer, text, weight, size, width, mono = false) {

		const per = t => renderer.textWidth(t, weight, mono) / AppMapRenderer.LABEL_SIZE * size;
		let cut   = text;

		if (per(cut) <= width) {
			return text;
		}

		while (cut.length > 1 && per(cut + '…') > width) {
			cut = cut.slice(0, -1);
		}

		return cut + '…';
	}

	// branches from the bottom of a parent to the top of each child, as the tidy trees of d3 draw them: each an S-curve
	// that leaves the parent and reaches the level of the child, so the children of one parent fan out from it; all in one
	// path
	static branches(tree, panel) {

		const s    = panel.scale;
		const path = new Path2D();

		for (const d of tree.list) {

			const [px, py] = AppMapPanels.box(d.parent, panel);
			const [cx, cy] = AppMapPanels.box(d, panel);
			const [pw, ph] = AppMapPanels.sizeOf(d.parent);
			const [cw, ch] = AppMapPanels.sizeOf(d);
			const start    = [px + pw * s / 2, py + ph * s / 2];

			// a stack hangs from a rail on its left, which the branch reaches above its first leaf; each leaf on a short
			// line from the rail
			if (d.stack) {

				if (d === d.stack[0]) {

					const rail = cx - AppMapPanels.GAP * s * 0.6;
					const last = AppMapPanels.box(d.stack[d.stack.length - 1], panel)[1];
					const top  = cy - ch * s / 2;
					const bend = (start[1] + top) / 2;

					path.moveTo(start[0], start[1]);
					path.bezierCurveTo(start[0], bend, rail, bend, rail, top);
					path.lineTo(rail, last);

					for (const leaf of d.stack) {

						const [lx, ly] = AppMapPanels.box(leaf, panel);

						path.moveTo(rail, ly);
						path.lineTo(lx, ly);
					}
				}

				continue;
			}

			const end      = [cx + cw * s / 2, cy - ch * s / 2];
			const middle   = (start[1] + end[1]) / 2;

			path.moveTo(start[0], start[1]);
			path.bezierCurveTo(start[0], middle, end[0], middle, end[0], end[1]);
		}

		return path;
	}

	// the tree as the layout takes it, as a hierarchy of the nodes of the tree: a run of at least STACK siblings without
	// children, and no sheets, stands as a stack, each of them the only child of the one before it, so that the layout puts
	// them one below the other at the place of the run; they keep the stack they belong to in stack
	static layoutOf(root) {

		const children = d => {

			const list = [];
			let   run  = [];

			const close = () => {

				if (run.length >= AppMapPanels.STACK) {

					run.forEach(leaf => leaf.stack = run);
					list.push(run);

				} else {

					list.push(...run);
				}

				run = [];
			};

			for (const child of d.children ?? []) {

				child.stack = null;

				if (!child.children && !AppMapPanels.isSheet(child)) {

					run.push(child);

				} else {

					close();
					list.push(child);
				}
			}

			close();

			// a stack is a chain: its first leaf with the next as its only child, and so on
			return list.map(item => Array.isArray(item) ? item.reduceRight((below, leaf) => ({ node: leaf, children: below ? [below] : [] }), null) : { node: item, children: children(item) });
		};

		return d3.hierarchy({ node: root, children: children(root) }).each(l => l.data = l.data.node);
	}

	static width(d) {

		return AppMapPanels.sizeOf(d)[0];
	}

	// the width and the height of the box of an HTML element in the tree, before the scale of the panel; the document at
	// the root has its own size
	static sizeOf(d) {

		return d.parent ? [AppMapPanels.boxWidth(d, 1), AppMapPanels.boxHeight(d)] : AppMapPanels.ROOT;
	}

	// the width of the box of an HTML element: its tag and the sign of what it does, at the scale of the panel; the data key
	// of a repeater and everything else shows only when the element opens
	static boxWidth(d, s) {

		if (AppMapPanels.isSheet(d)) {
			return AppMapPanels.TEMPLATE_BOX[0] * s;
		}

		return (d.data.tag.length * 0.6 + (d.data.action || d.data.repeater ? 2.8 : 1.6) + (AppMapPanels.hasInside(d) ? AppMapPanels.ELEMENT_SIGN : 0)) * s;
	}

	// the height of the box of an HTML element, before the scale of the panel
	static boxHeight(d) {

		return AppMapPanels.isSheet(d) ? AppMapPanels.TEMPLATE_BOX[1] : AppMapPanels.BOX;
	}

	// whether an HTML element stands upright as a sheet: a template, a text node with a script block (the outline calls it
	// content), or a script or a style with code in it; the document at the root never is
	static isSheet(d) {

		return !!d.parent && (d.data.tag === 'template' || d.data.tag === 'content' || (!!AppMapPanels.SCRIPTS[d.data.tag] && d.data.text));
	}

	// the name of an HTML element as the map writes it: its tag in angle brackets, a text node with a script block as the
	// Pages area calls it
	static nameOf(d) {

		return d.data.tag === 'content' ? 'Content' : `<${d.data.tag}>`;
	}

	// the outline of the box of an HTML element: a rectangle with small corners, a template a sheet with a folded corner
	static outline(d, x, top, width, height, s, grow = 0) {

		const path   = new Path2D();
		const corner = AppMapPanels.BOX * s * AppMapPanels.ELEMENT_CORNER + grow;

		if (!AppMapPanels.isSheet(d)) {

			path.roundRect(x - grow, top - grow, width + 2 * grow, height + 2 * grow, corner);

			return path;
		}

		const fold = AppMapPanels.foldOf(width);

		path.moveTo(x - grow, top - grow);
		path.lineTo(x + width - fold, top - grow);
		path.lineTo(x + width + grow, top + fold);
		path.lineTo(x + width + grow, top + height + grow);
		path.lineTo(x - grow, top + height + grow);
		path.closePath();

		return path;
	}

	static foldOf(width) {

		return width * 0.18;
	}

	// whether an HTML element has settings or a text to show when it opens; every element opens, but only these are marked
	static hasInside(d) {

		const data = d.data;

		return !!(data.repeater || data.action || data.text || data.when || data.tag === 'template' || data.tag === 'content');
	}

	// how an HTML element opens inside its box at the given zoom: the box as a card, its header, and the room below the
	// header in which its controls stand, at the share of ELEMENT_SIZE that fits; px is the height of the box on screen
	elementPanel(d, k) {

		const page   = d.page;
		const panel  = this.pagePanel(page, this.trees.get(page), k, this.view.drawnRadiusAt(page, k));
		const s      = panel.scale;
		const [x, y] = AppMapPanels.box(d, panel);
		const width  = AppMapPanels.boxWidth(d, s);
		const height = AppMapPanels.boxHeight(d) * s;
		// the header and the margins follow the height of a box, on the upright sheet of a template its width
		const sheet  = AppMapPanels.isSheet(d);
		const unit   = sheet ? width * 0.6 : AppMapPanels.BOX * s;
		const top    = y - height / 2;
		const pad    = unit * 0.08;
		const size   = AppMapPanels.headSize(width, height, k);
		// below the header the strip of what the element is (see drawElementStrip), sized by the header like that of a page
		const strip  = size.depth * AppMapPanels.ELEMENT_STRIP;
		const bottom = top + size.depth + strip + pad * 0.6;
		// the controls keep this far from the sides of the box, clear of the bar on its left
		const inset  = unit * 0.14;
		const room   = [width - 2 * inset, top + height - pad - bottom];
		const [W, H] = sheet ? AppMapPanels.SHEET_SIZE : AppMapPanels.ELEMENT_SIZE;
		const fit    = Math.min(room[0] * k / W, room[1] * k / H);
		const area   = { x: x + inset, y: bottom, width: room[0], height: room[1] };
		const head   = { x0: x, y0: top, width, height, pad, ...size, line: top + size.depth, bottom, band: { x, width, outline: AppMapPanels.outline(d, x, top, width, height, s), fold: sheet ? AppMapPanels.foldOf(width) : 0 } };

		return { x, y: top, width, height, head, area, fit, strip, inset, px: unit * k, scale: s };
	}

	// the strip of an HTML element is this many times as high as its header
	static ELEMENT_STRIP = 0.95;

	// the kind of an open HTML element, for its header and its strip
	static elementKind(d) {

		return d.data.repeater ? AppMapModel.KIND_LABEL.view : (d.data.action ? AppMapModel.KIND_LABEL.action : ({ template: 'Template', content: 'Script', script: 'Script', style: 'Style' }[d.data.tag] ?? 'HTML element'));
	}

	// the strip of an open HTML element below its header, in the style of that of a page (see drawPageStrip): on the left
	// its kind and its tag, on the right what it does as pills: the data key of a repeater, the type it shows and where its
	// data comes from, that it runs an event action, that it is shown or hidden on a condition, that it is a shared
	// component, and what is wrong with it; a hairline below
	drawElementStrip(renderer, d, panel, colour, k) {

		const look   = this.view.look;
		const head   = panel.head;
		const left   = panel.x + panel.inset;
		const right  = panel.x + panel.width - panel.inset;
		const middle = head.line + panel.strip * 0.5;
		const font   = head.name * 0.62;
		const data   = d.data;
		const type   = data.shows ? this.view.nodes.find(m => m.k === 'type' && m.n === data.shows) : null;
		const source = { functionQuery: 'function query', cypherQuery: 'Cypher query', flow: 'flow' }[(data.sources ?? [])[0]];
		const issues = d.warnings?.length ?? 0;

		// its kind and its tag
		let x = left;

		x += this.drawLabel(renderer, AppMapPanels.elementKind(d), x, middle, font, 'chip', colour, k);

		if (data.tag && !['template', 'content'].includes(data.tag)) {
			x += this.drawLabel(renderer, `<${data.tag}>`, x, middle, font, 'chip', look.label, k);
		}

		// what it does, from the right, as far as there is room
		const marks = [
			data.key && [`key ${data.key}`, 'chip', look.label],
			type && [`shows ${type.n}`, 'chip', renderer.colorOf(type)],
			!type && data.shows && [`shows ${data.shows}`, 'chip', look.label],
			source && [source, 'chip', look.label],
			// an event action of an element that is something else first, a repeater; the kind says it of any other
			data.action && data.repeater && ['event action', 'on', AppMapPanels.ACTION_COLOR],
			data.when && ['conditional', 'chip', look.label],
			data.component && ['shared component', 'chip', look.kinds.component],
			issues && [`${issues} ${issues === 1 ? 'problem' : 'problems'}`, 'on', look.warning]
		].filter(Boolean);

		let r = right;

		for (const [text, style, ink] of [...marks].reverse()) {

			if (r - AppMapPanels.labelWidth(renderer, text, font) - font * AppMapPanels.PILL_GAP < x) {
				break;
			}

			r -= this.drawLabel(renderer, text, r, middle, font, style, ink, k, 'right');
		}

		this.drawHairline(renderer.ctx, left, right, head.line + panel.strip, k);
	}

	// the HTML elements that are open on the screen, the one nearest its middle first
	openElements() {

		const view = this.view;
		const list = [];

		for (const tree of this.trees.values()) {

			for (const d of tree.open ? tree.list : []) {

				if (d.opened) {
					list.push(d);
				}
			}
		}

		return list.sort((a, b) => this.fromCentre(a) - this.fromCentre(b));
	}

	// how far the middle of an open HTML element lies from the middle of the screen, in pixels
	fromCentre(d) {

		const view = this.view;
		const t    = view.transform;

		return Math.hypot(t.applyX(d.opened.x + d.opened.width / 2) - view.width / 2, t.applyY(d.opened.y + d.opened.height / 2) - view.height / 2);
	}

	static box(d, panel) {

		return [panel.x0 + d.lx * panel.scale, panel.y0 + (d.ly - panel.top) * panel.scale];
	}

	// the open page or readable method a point lies in: there only its boxes answer, the elements beneath it are covered
	openAt(x, y) {

		for (const [page, tree] of this.trees) {

			const f = tree.open && tree.frame;

			if (f && x >= f[0] && x <= f[0] + f[2] && y >= f[1] && y <= f[1] + f[3]) {
				return page;
			}
		}

		for (const n of this.reading) {

			const r      = this.view.shownRadius(n);
			const [w, h] = AppMapRenderer.boxOf(n);

			if (Math.abs(x - n.x) <= r * w && Math.abs(y - n.y) <= r * h) {
				return n;
			}
		}

		for (const n of this.inspecting) {

			const p = this.insidePanel(n, this.view.transform.k);

			if (x >= p.x0 && x <= p.x0 + p.width && y >= p.y0 && y <= p.y0 + p.height) {
				return n;
			}
		}

		return null;
	}

	// the kind on the header of a card under a point of the map, with what it is (see KIND_ABOUT); a card drawn later lies
	// above, so it answers first
	kindAt(x, y) {

		const pad = 2 / this.view.transform.k;

		for (let i = (this.kinds ?? []).length - 1; i >= 0; i--) {

			const { kind, box } = this.kinds[i];

			if (x >= box[0] - pad && x <= box[2] + pad && y >= box[1] - pad && y <= box[3] + pad) {

				const error = /^Error page (\d+)$/.exec(kind);
				const about = error ? `A page that Structr shows when a request ends with the status code ${error[1]}.` : AppMapPanels.KIND_ABOUT[kind];

				return about ? { kind, about } : null;
			}
		}

		return null;
	}

	covers(x, y) {

		return !!this.openAt(x, y);
	}

	// whether an element shows its inner structure: an open page or shared component, a method whose code can be read, a
	// type whose properties can be read
	isOpen(n) {

		return !!this.trees.get(n)?.open || this.reading.has(n) || this.inspecting.has(n);
	}

	// the HTML element under the pointer: only an open panel shows them, and there the whole box answers
	findDom(x, y) {

		for (const tree of this.trees.values()) {

			if (!tree.open) {
				continue;
			}

			for (const d of tree.list) {

				const box = d.panelHit;

				if (box && x >= box[0] && x <= box[0] + box[2] && y >= box[1] && y <= box[1] + box[3]) {
					return d;
				}
			}
		}

		return null;
	}

	// ----- the code of the methods -----
	// whether an element shows its code close up: a method, and a page or shared component that consists of a single
	// template, which shows the text of the template in place of its tree
	static hasCode(n) {

		return n.k === 'method' || !!n.tpl;
	}

	// reads the source of a method or the text of a template once, when its box first comes close enough to show it
	requestCode(n) {

		if (this.code.has(n) || !this.view.loadSource) {
			return;
		}

		this.code.set(n, { loading: true });

		Promise.resolve(this.view.loadSource(n)).then(loaded => {

			// a method comes with what its card shows of it, a template as its text alone
			const source = loaded && typeof loaded === 'object' ? loaded.source : loaded;
			const meta   = loaded && typeof loaded === 'object' ? loaded.meta : null;
			const lines  = String(source ?? '').replace(/\t/g, '  ').replace(/\s+$/, '').split('\n');

			this.code.set(n, source == null ? { failed: true } : { lines, meta, tokens: AppMapPanels.tokenize(lines) });
			this.view.requestDraw();

		}).catch(() => {

			this.code.set(n, { failed: true });
			this.view.requestDraw();
		});
	}

	// the colours of the kinds of words in code (see tokenize), [light, dark], in those of the methods of HTTP
	static CODE_COLOR = {
		keyword: AppMapPanels.VERB_COLOR.PATCH,
		string:  AppMapPanels.VERB_COLOR.GET,
		number:  AppMapPanels.VERB_COLOR.PUT,
		call:    AppMapPanels.VERB_COLOR.POST,
		comment: ['#8a8678', '#8b887d']
	};

	static CODE_KEYWORDS = new Set(['async', 'await', 'break', 'case', 'catch', 'class', 'const', 'continue', 'default', 'delete', 'do', 'else', 'export', 'false', 'finally', 'for', 'function', 'if', 'import', 'in', 'instanceof', 'let', 'new', 'null', 'of', 'return', 'switch', 'this', 'throw', 'true', 'try', 'typeof', 'undefined', 'var', 'void', 'while', 'yield']);

	// the words of the lines of a method by their kind, as a list of [text, kind] per line: comments (also across lines),
	// strings, numbers, the calls of the Structr API ($.find and the like), the keywords of JavaScript, and plain text
	static tokenize(lines) {

		let inComment = false;

		return lines.map(line => {

			const out  = [];
			const push = (text, kind) => {

				// plain text in one piece
				if (kind === 'plain' && out.length && out[out.length - 1][1] === 'plain') {
					out[out.length - 1][0] += text;
				} else if (text) {
					out.push([text, kind]);
				}
			};

			let i = 0;

			while (i < line.length) {

				const rest = line.slice(i);
				let m;

				if (inComment) {

					const end = rest.indexOf('*/');
					const len = end < 0 ? rest.length : end + 2;

					push(rest.slice(0, len), 'comment');
					inComment = end < 0;
					i += len;

				} else if (rest.startsWith('//')) {

					push(rest, 'comment');
					i = line.length;

				} else if (rest.startsWith('/*')) {

					inComment = true;
					push('/*', 'comment');
					i += 2;

				} else if ((m = rest.match(/^(["'`])(?:\\.|(?!\1).)*\1?/))) {

					push(m[0], 'string');
					i += m[0].length;

				} else if ((m = rest.match(/^\$\.[A-Za-z_]\w*/))) {

					push(m[0], 'call');
					i += m[0].length;

				} else if ((m = rest.match(/^[A-Za-z_$][\w$]*/))) {

					push(m[0], AppMapPanels.CODE_KEYWORDS.has(m[0]) ? 'keyword' : 'plain');
					i += m[0].length;

				} else if ((m = rest.match(/^\d+(\.\d+)?/))) {

					push(m[0], 'number');
					i += m[0].length;

				} else {

					push(line[i], 'plain');
					i++;
				}
			}

			return out;
		});
	}

	// below the header of a method, what it is, in the style of the box of a field (see drawField): on the left the kind of
	// method as a chip, on the right how it can be called, the HTTP verb as a badge in its colour or that it is private, and
	// whether it returns its raw result and is in the OpenAPI description; below them its summary, where it has one; a
	// hairline below
	drawMethodStrip(renderer, n, panel, colour, k) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const meta   = panel.meta;
		const left   = panel.x0 + panel.pad;
		const right  = panel.x0 + 2 * panel.w - panel.pad;
		const top    = panel.y0 + panel.head.depth;
		const font   = panel.head.name * 0.62;
		const unit   = panel.head.depth;
		const middle = top + unit * 0.52;
		const name   = n.n.split('.').pop();
		const cycle  = meta.onType && /^(on|after)[A-Z]/.test(name);
		const kind   = n.cron ? `Scheduled · ${n.cron}` : (!meta.onType ? 'Function' : (cycle ? 'Lifecycle method' : (meta.isStatic ? 'Static method' : 'Method')));

		// the kind as a chip in the colour of the method
		this.drawLabel(renderer, AppMapPanels.cutText(renderer, kind, 600, font, (right - left) * 0.5), left, middle, font, 'chip', colour, k);

		// how it can be called, from the right: its verb as a badge, or that it is private; a lifecycle method is called by
		// Structr alone. Then what is set of it
		const verb  = meta.httpVerb ?? 'POST';
		const marks = [
			!cycle && (meta.isPrivate ? ['private', 'chip', look.label] : [verb, 'badge', (AppMapPanels.VERB_COLOR[verb] ?? AppMapPanels.VERB_COLOR.POST)[look.dark ? 1 : 0]]),
			meta.returnRawResult && ['raw result', 'on', colour],
			meta.includeInOpenAPI && ['OpenAPI', 'on', colour]
		].filter(Boolean);

		let x = right;

		for (const [text, style, ink] of [...marks].reverse()) {
			x -= this.drawLabel(renderer, text, x, middle, font, style, ink, k, 'right');
		}

		if (meta.summary) {

			ctx.fillStyle = look.label;
			renderer.screenText(AppMapPanels.cutText(renderer, meta.summary, 400, font * 1.1, right - left), left, top + unit * 1.1, 400, font * 1.1, false, 'left');
		}

		this.drawCallers(renderer, this.callersOf(n), left, right, top + unit * ((meta.summary ? 1.5 : 1) + AppMapPanels.CALLERS_LINE * 0.45), font, k);
	}

	// the line for the callers of a method below its strip, as a share of the height of the header
	static CALLERS_LINE = 0.7;

	// what calls a method or reads a function property, as the map knows it (see callersOf and readersOf), as a list of
	// { node, label }: a label in small capitals, then a chip per caller in its colour, as many as the line holds, and how
	// many more there are. Where the map knows of none, it says so in grey, and that it only knows of calls in the code
	drawCallers(renderer, callers, left, right, y, font, k, caption = 'Called by', none = 'No callers found in the code of the application') {

		const ctx = renderer.ctx;
		let   x   = left + this.drawCaption(renderer, caption, left, y, font);

		if (!callers.length) {

			ctx.fillStyle = this.view.look.label;
			renderer.screenText(none, x, y, 400, font * 1.1, false, 'left');

			return;
		}

		const more  = n => `+${n} more`;
		const room  = right - AppMapPanels.labelWidth(renderer, more(callers.length), font) - font * AppMapPanels.PILL_GAP;
		let   shown = 0;

		for (const caller of callers) {

			const label = AppMapPanels.cutText(renderer, caller.label, 600, font, (right - left) * 0.45);

			// the last chip may reach the edge, the others leave room for how many more there are
			if (x + AppMapPanels.labelWidth(renderer, label, font) > (shown === callers.length - 1 ? right : room)) {
				break;
			}

			x += this.drawLabel(renderer, label, x, y, font, 'chip', this.tintOf(renderer, caller.node) ?? this.view.look.label, k);
			shown++;
		}

		if (shown < callers.length) {
			this.drawLabel(renderer, more(callers.length - shown), x, y, font, 'off', null, k);
		}
	}

	// the readers of a function property, as the map data has them on the type that declares it (fr): the methods whose
	// code reads it, by their name, and the pages and shared components that show it, with the element that shows it, from
	// the evidence ("Total shows item.total"). As a list of { node, label }
	readersOf(owner, property) {

		const readers = [];

		for (const [id, evidence] of owner?.fr?.[property] ?? []) {

			const node = this.view.byId.get(id);

			if (!node) {
				continue;
			}

			const element = node.k === 'method' ? null : String(evidence ?? '').match(/^(.*?) shows /)?.[1];

			readers.push({ node, label: element ? `${node.n} › ${element}` : node.n });
		}

		return readers.sort((a, b) => a.label.localeCompare(b.label));
	}

	// the callers of a method, as the relations of the map have them: the elements of pages whose event action invokes it,
	// named after the page and the element, as the evidence of the relation gives it; the methods and pages whose code
	// calls it; the REST endpoint it answers, with its verb and the requests of the last days (see AppMapView.usageOf). A
	// caller with several relations to it counts once. As a list of { node, label }
	callersOf(n) {

		const meta = this.code.get(n)?.meta ?? null;
		const seen = new Map();

		for (const l of this.view.links) {

			if (l.target !== n || l.source === n || !['invokes', 'calls', 'helper', 'endpoint'].includes(l.kind)) {
				continue;
			}

			let label = l.source.n;

			if (l.kind === 'invokes') {

				// "invoices: Save → saveHighestInvoiceNo() calls …": the element is what stands before the arrow
				const element = String(l.evidence ?? '').match(/^[^:]*:\s*(.*?)\s*→/)?.[1];

				label = element ? `${l.source.n} › ${element}` : l.source.n;

			} else if (l.kind === 'endpoint') {

				const signature = l.source.n.replace(/^\//, '');
				const usage     = this.view.usage.grants?.data?.usage;
				const requests  = usage ? d3.sum(Object.entries(usage).filter(([source]) => source.startsWith(signature + ' ')), ([, u]) => u.total) : null;

				// the requests of the last days, read once they are needed
				this.view.usageOf('grants');

				label = `${meta?.httpVerb ?? 'POST'} ${l.source.n}` + (requests != null ? ` · ${AppMapPanels.shortCount(requests)} ${requests === 1 ? 'request' : 'requests'}` : '');
			}

			seen.set(`${l.source.id}|${label}`, { node: l.source, label });
		}

		return [...seen.values()].sort((a, b) => a.label.localeCompare(b.label));
	}

	// the place of the code in the rectangle of a method, with the size of a line on screen; the font follows the number
	// of lines and the longest of them, within CODE_LINES and CODE_COLUMNS
	codePanel(n, k, radius = this.view.shownRadius(n)) {

		const [bw, bh] = AppMapRenderer.boxOf(n);
		const w        = radius * bw;
		const h        = radius * bh;
		const pad      = h * 0.08;
		const head     = AppMapPanels.headSize(2 * w, 2 * h, k);
		const meta     = this.code.get(n)?.meta ?? null;
		// below the header of a method, the strip of what it is (see drawMethodStrip), as high as the header, half again
		// with a summary, and a line for its callers
		const strip    = meta ? head.depth * ((meta.summary ? 1.5 : 1) + AppMapPanels.CALLERS_LINE) : 0;
		const title    = head.depth - pad * 0.5 + strip;
		const lines    = this.code.get(n)?.lines ?? [];
		const columns  = Math.min(AppMapPanels.CODE_COLUMNS, Math.max(40, d3.max(lines, l => l.length) ?? 0));
		const inner    = [2 * w - 2 * pad, 2 * h - 2 * pad - title - head.foot];

		// a box higher than wide, as the sheet of a page, holds more than CODE_LINES at the font its width allows
		const fit      = Math.floor(inner[1] / (inner[0] / (columns * 0.6) * 1.3));
		const rows     = Math.min(Math.max(AppMapPanels.CODE_LINES[1], fit), Math.max(AppMapPanels.CODE_LINES[0], lines.length));
		const font     = Math.min(inner[0] / (columns * 0.6), inner[1] / (rows * 1.3));

		return { x0: n.x - w, y0: n.y - h, w, h, pad, title, head, strip, meta, font, line: font * 1.3, rows, px: font * 1.3 * k };
	}

	// the field of a code panel the code stands on, between the header and the foot, as x, y, width and height on the map
	static codeField(panel) {

		const top = panel.y0 + panel.pad + panel.title;

		return [panel.x0 + panel.pad * 0.6, top - panel.pad * 0.3, 2 * panel.w - panel.pad * 1.2, panel.y0 + 2 * panel.h - top - panel.pad * 0.3 - panel.head.foot];
	}

	// the property tile a point on the map lies on, or null
	tileAt(x, y) {

		return this.tiles.find(t => x >= t.x && x <= t.x + t.width && y >= t.y && y <= t.y + t.height) ?? null;
	}

	// the box of a setting that can be switched a point on the map lies on, with the row and the setting, or null
	toggleAt(x, y) {

		return this.toggles.find(({ rect: [tx, ty, tw, th] }) => x >= tx && x <= tx + tw && y >= ty && y <= ty + th) ?? null;
	}

	// the readable code a point on the map lies on, or null
	codeAt(x, y, k) {

		for (const n of this.reading) {

			const [fx, fy, fw, fh] = AppMapPanels.codeField(this.codePanel(n, k));

			if (x >= fx && x <= fx + fw && y >= fy && y <= fy + fh) {
				return n;
			}
		}

		return null;
	}

	// whether any of the box of an element is on the screen, or within the margin around it; close up the box of a page or
	// method is far larger than the screen, and its middle may lie far beyond it
	onScreen(renderer, n, k, margin) {

		return renderer.onScreen(n, margin + Math.max(...AppMapRenderer.boxOf(n)) * this.view.shownRadius(n) * k);
	}

	// the elements on screen that are close enough to show their code, with their panel and how far they have opened
	codePanels(renderer, k) {

		return this.view.nodes
			.filter(n => AppMapPanels.hasCode(n) && this.onScreen(renderer, n, k, 200))
			.map(n => ({ n, panel: this.codePanel(n, k) }))
			.filter(({ panel }) => panel.px >= AppMapPanels.CODE_SKETCH)
			.map(c => ({ ...c, strong: Math.min(1, (c.panel.px - AppMapPanels.CODE_SKETCH) / (AppMapPanels.CODE_READ - AppMapPanels.CODE_SKETCH)) }));
	}

	drawCode(renderer, k, codePanels) {

		const ctx  = renderer.ctx;
		const look = this.view.look;

		this.reading.clear();

		for (const { n, panel, strong } of codePanels) {

			this.requestCode(n);

			const code   = this.code.get(n);
			const colour = renderer.colorOf(n);
			const lines  = code?.lines ?? [];
			const shown  = lines.length > panel.rows ? panel.rows - 1 : lines.length;
			const left   = panel.x0 + panel.pad;
			const top    = panel.y0 + panel.pad + panel.title;

			ctx.save();

			// the rectangle opens as a card like an open page: its name on top, the code below
			this.drawCard(ctx, panel.x0, panel.y0, 2 * panel.w, 2 * panel.h, colour, k, 0.35 + 0.65 * strong);

			ctx.beginPath();
			ctx.roundRect(panel.x0, panel.y0, 2 * panel.w, 2 * panel.h, AppMapPanels.corner(2 * panel.w, 2 * panel.h, k));
			ctx.clip();

			if (panel.px >= AppMapPanels.CODE_READ) {

				// the header of every card in the title band, and the code on a field of its own below it
				const head = { x0: panel.x0, y0: panel.y0, width: 2 * panel.w, height: 2 * panel.h, ...panel.head, line: panel.y0 + panel.head.depth };

				this.reading.add(n);

				// a method has its way into the Code area at its foot, see AppMapView.placeOpenButton
				if (n.k === 'method') {
					this.cards.push({ n, open: { area: 'code', kind: 'method', node: n }, rect: [panel.x0, panel.y0, 2 * panel.w, 2 * panel.h], foot: panel.head.foot, opacity: 1 });
				}
				this.drawHeader(renderer, head, AppMapModel.KIND_LABEL[n.k], n.n, colour, k);

				if (panel.meta) {
					this.drawMethodStrip(renderer, n, panel, colour, k);
				}

				const [fx, fy, fw, fh] = AppMapPanels.codeField(panel);

				ctx.beginPath();
				ctx.roundRect(fx, fy, fw, fh, panel.pad * 0.3);
				ctx.fillStyle   = look.bg;
				ctx.strokeStyle = look.hair;
				ctx.lineWidth   = 1 / k;
				ctx.fill();
				ctx.stroke();

				// the code being edited stands in the editor above the field
				if (this.view.editing === n) {

					ctx.restore();
					continue;
				}

				// the code, its words in the colours of their kind (see tokenize), cut where a line is longer than CODE_COLUMNS
				const char = renderer.textWidth('M', 400, true) / AppMapRenderer.LABEL_SIZE * panel.font;
				const dark = look.dark ? 1 : 0;

				for (let i = 0; i < shown; i++) {

					const y    = top + (i + 0.5) * panel.line;
					let   x    = left;
					let   room = AppMapPanels.CODE_COLUMNS;

					for (const [text, kind] of code?.tokens?.[i] ?? [[lines[i], 'plain']]) {

						const cut = text.length > room ? text.slice(0, Math.max(0, room - 1)) + '…' : text;

						ctx.fillStyle = kind === 'plain' ? look.labelFocus : AppMapPanels.CODE_COLOR[kind][dark];
						renderer.screenText(cut, x, y, 400, panel.font, true, 'left');

						x    += cut.length * char;
						room -= cut.length;

						if (room <= 0) {
							break;
						}
					}
				}

				ctx.fillStyle = look.labelNear;

				if (lines.length > shown) {
					renderer.screenText(`… ${lines.length - shown} more lines`, left, top + (shown + 0.5) * panel.line, 400, panel.font, true, 'left');
				} else if (!code || code.loading) {
					renderer.screenText('loading the code …', left, top + 0.5 * panel.line, 400, panel.font, true, 'left');
				} else if (code.failed) {
					renderer.screenText('the code could not be read', left, top + 0.5 * panel.line, 400, panel.font, true, 'left');
				}

			} else {

				// from afar the code is a sketch: the band of the header without its text, a grey bar per line, as long and as far
				// indented as the line
				ctx.globalAlpha = 0.35 + 0.65 * strong;
				this.drawBand(ctx, { x0: panel.x0, y0: panel.y0, width: 2 * panel.w, height: 2 * panel.h, line: panel.y0 + panel.head.depth }, colour);

				ctx.fillStyle   = look.trace;
				ctx.globalAlpha = 0.25 + 0.35 * strong;
				ctx.beginPath();

				for (let i = 0; i < shown; i++) {

					const indent = lines[i].length - lines[i].trimStart().length;
					const length = Math.min(lines[i].trim().length, AppMapPanels.CODE_COLUMNS - indent);

					if (length) {
						ctx.rect(left + indent * panel.font * 0.6, top + i * panel.line + panel.line * 0.25, length * panel.font * 0.6, panel.line * 0.5);
					}
				}

				ctx.fill();
			}

			ctx.restore();
		}
	}

	// ----- the inside of a type, a REST endpoint, a group and an external service -----
	// reads what an element holds once, when it first comes close enough to show it (see insideOf); the promise it returns
	// settles once it is there
	requestInside(n) {

		if (this.insides.has(n)) {
			return this.insides.get(n).loaded;
		}

		const loaded = Promise.resolve(this.insideOf(n)).then(data => {

			this.insides.set(n, { ...(data ?? { failed: true }), layouts: new Map(), loaded });
			this.view.requestDraw();

		}).catch(error => {

			console.warn(`Application map: what ${n.n} holds could not be read`, error);

			this.insides.set(n, { failed: true, layouts: new Map(), loaded });
			this.view.requestDraw();
		});

		this.insides.set(n, { loading: true, loaded });

		return loaded;
	}

	// what the card of an element shows, as sections of rows: the properties of a type from its schema, the grants of a
	// REST endpoint, the grants, members and parents of a group, the elements that use an external service; with the way
	// out of the map at the foot of the card (open), and what the card calls the element, where that is not its kind
	async insideOf(n) {

		if (n.k === 'type') {

			const schema = this.view.loadType ? await this.view.loadType(n) : null;

			// the schema relationships of the type and of the types it inherits relationships from, for their boxes (see
			// relationshipSettings)
			const types   = new Map(this.view.nodes.filter(m => m.k === 'type').map(m => [m.n, m]));
			const owners  = [...new Set(Object.values(schema?.views?.all ?? {}).filter(p => p.relatedType).map(p => types.get(p.declaringClass)?.id).filter(Boolean))];
			const related = this.view.loadRelationships && owners.length ? await this.view.loadRelationships(owners).catch(() => []) : [];

			// the code of its function properties, for their boxes
			const functions = new Map((this.view.loadFunctions ? await this.view.loadFunctions(n).catch(() => []) : []).map(f => [f.name, f]));

			return schema ? { sections: this.typeRows(n, schema, related, functions), strip: this.typeStripOf(n, schema), kindLabel: schema.isServiceClass ? 'Service class' : null, open: { area: 'code', kind: 'type', node: n } } : null;
		}

		if (n.k === 'api') {

			const [grants, usage] = await Promise.all([this.view.loadGrants ? this.view.loadGrants() : null, this.view.usageOf('grants')]);

			// what the endpoint gives access to, for its strip (see drawEndpointStrip)
			const target = this.view.links.find(l => l.kind === 'endpoint' && l.source === n)?.target ?? null;

			return grants ? { sections: this.endpointRows(n, grants, usage), usage, strip: { endpoint: true, target }, open: { area: 'security', tab: 'resource-access', filter: n.n.replace(/^\//, '') } } : null;
		}

		if (n.k === 'group') {

			const members = this.view.loadMembers ? await this.view.loadMembers(n) : null;

			// the rights are changed in the properties of the group, see _UsersAndGroups.typePermissions
			return { sections: this.groupRows(n, members ?? []), strip: this.groupStripOf(n, members ?? []), open: { area: 'security', tab: 'users-and-groups', group: true, filter: n.n, id: n.id, dialog: 'type-permissions' } };
		}

		// what the service is to its code says the strip, see drawExternalStrip
		return { sections: this.externalRows(n), strip: this.externalStripOf(n) };
	}

	// the settings of the box of a row that leads to another element of the map: what it is, and the way to it
	static leadTo(node) {

		return node.k === 'method' ? { area: 'code', kind: 'method', node } : (node.k === 'type' ? { area: 'code', kind: 'type', node } : null);
	}

	// the properties of a type, from its schema (see SchemaHelper.getSchemaTypeInfo): its relationships, its fields and, per
	// type of the application it extends, those it inherits; the properties of the framework every type has, like id and
	// createdDate, are left out, and so are its methods, which stand beside it on the map
	typeRows(n, schema, related = [], functions = new Map()) {

		const types  = new Map(this.view.nodes.filter(m => m.k === 'type').map(m => [m.n, m]));
		const all    = Object.values(schema.views?.all ?? {}).filter(p => types.has(p.declaringClass));
		const byName = (a, b) => a.name.localeCompare(b.name);

		const item = p => {

			const target = p.relatedType ? types.get(p.relatedType) ?? null : null;
			const type   = p.relatedType ?? p.type ?? '';
			const owner  = types.get(p.declaringClass) ?? n;
			// the settings of a field can be switched where its schema property is, not those of a relationship, and not of a
			// field that cannot be changed
			const editable = !target && !p.readOnly;
			// a function property is known by its class: as its type the schema names the type of its value, Object
			// without a type hint
			const script   = !target && AppMapPanels.isFunctionProperty(p);
			const valued   = p.uiType ?? (type.endsWith('[]') || !p.isCollection ? type : type + '[]');
			const shown    = script ? (type && type !== 'Object' && type !== 'Function' ? `Function · ${valued}` : 'Function') : valued;
			const field    = target ? null : AppMapPanels.fieldOf(p, owner, n, shown, editable, owner === n ? functions.get(p.jsonName) : null, script, script ? this.readersOf(owner, p.jsonName) : null);

			return {
				name:     p.jsonName,
				kind:     target ? 'Relationship' : 'Property',
				sign:     script ? 'Function' : type,
				arrow:    !!target,
				tint:     target,
				chip:     target ? type : null,
				many:     !!p.isCollection,
				dot:      !!p.notNull,
				owner,
				target,
				relation: target ? AppMapPanels.relationOf(p, owner, n, target, related) : null,
				field,
				// the marks of a field, which AppMapView.flip switches; a relationship has none
				settings: field?.settings ?? [],
				open:     { area: 'code', kind: 'property', node: owner, name: p.jsonName },
				edit:     editable ? { kind: 'property', owner, name: p.jsonName } : null
			};
		};

		const own       = all.filter(p => p.declaringClass === n.n).map(item);
		const relations = own.filter(i => i.target).sort(byName);
		const fields    = own.filter(i => !i.target).sort(byName);
		const inherited = d3.groups(all.filter(p => p.declaringClass !== n.n), p => p.declaringClass).map(([parent, list]) => ({ heading: `From ${parent} · ${list.length}`, items: list.map(item).sort(byName), faded: true }));

		// a service class holds methods, not data: its card lists them, each leading to the method beside it on the map;
		// the methods of any other type stand beside it and are not listed again
		const methods   = schema.isServiceClass ? this.view.links.filter(l => l.kind === 'member' && l.target === n && l.source.k === 'method').map(l => l.source) : [];
		// named without the class, which the card is
		const rows      = methods.map(m => ({ ...this.leadRow(m, m.cron ? 'Scheduled' : 'Method', m.cron ? `runs at ${m.cron}:` : 'a method of the service class:'), name: m.n.split('.').pop() })).sort((a, b) => a.name.localeCompare(b.name));
		const empty     = !relations.length && !fields.length && !inherited.length && !rows.length;

		return [
			{ heading: `Relationships · ${relations.length}`, items: relations },
			{ heading: `Fields · ${fields.length}`, items: fields },
			...inherited,
			{ heading: `Methods · ${rows.length}`, items: rows },
			// what the card of a type without properties says, in place of a list
			empty && { heading: null, items: [], empty: schema.isServiceClass ? 'No methods yet' : 'No properties of its own – its methods stand beside it on the map' }
		].filter(Boolean);
	}

	// the requests a REST endpoint answers: a row per path, as there is a resource access permission per signature, with the
	// methods it allows as coloured badges and who may send them; its box opens to the whole permission (see drawMatrix).
	// From the permissions whose signature the endpoint stands for, that of a type with those of its views (Item, Item/_Ui);
	// without any, only admins may use it. Below them, what it gives access to, a type or a method. With the usage of the
	// grants (see AppMapView.usageOf), each row also says how often its permission decided a request
	endpointRows(n, grants, usage) {

		const base   = n.n.replace(/^\//, '');
		const own    = new Set(this.view.nodes.filter(m => m.k === 'api').map(m => m.n.replace(/^\//, '')));
		const mine   = grants.filter(g => g.signature === base || (g.signature?.startsWith(base + '/') && !own.has(g.signature)));
		const opens  = this.view.links.filter(l => l.kind === 'endpoint' && l.source === n).map(l => l.target);
		AppMapPanels.annotateUsage(mine, usage);

		const rows   = mine.sort((a, b) => a.signature.localeCompare(b.signature)).map(AppMapPanels.requestRow);

		return [
			{ heading: `Requests · ${rows.length}`, items: rows, columns: 1, empty: 'No grants – only admins may use it' },
			{ heading: 'Gives access to', items: opens.map(node => this.leadRow(node, null, node.k === 'method' ? 'requests to it run' : 'requests to it read and change the objects of')), columns: 1 }
		];
	}

	// how often a permission decided a request in the last days, from the usage of the grants (see UsageStatistics), as
	// { days, flags, allowed, denied, last }, flags the requests per flag of the permission (AUTH_USER_GET and so on) as
	// { allowed, denied }; null without the usage. The usage is counted per signature, so permissions for the same
	// signature share it
	static usageOfGrant(g, data) {

		if (!data?.usage) {
			return null;
		}

		const day    = 24 * 60 * 60 * 1000;
		const first  = Date.now() - (Date.now() % day) - (data.days - 1) * day;
		const use    = { days: data.days, flags: {}, allowed: 0, denied: 0, last: 0, peak: 0 };

		// the requests per day, the oldest first, the days start at midnight UTC as the server counts them
		use.series = Array.from({ length: data.days }, (_, i) => ({ day: first + i * day, allowed: 0, denied: 0 }));

		// the sources are "<signature> <flag> <outcome>", and the signature itself may contain spaces
		for (const [source, { total, lastUsed, days }] of Object.entries(data.usage)) {

			const parts   = source.split(' ');
			const outcome = parts.pop();
			const flag    = parts.pop();

			if (parts.join(' ') !== g.signature || (outcome !== 'allowed' && outcome !== 'denied')) {
				continue;
			}

			use.flags[flag]          = use.flags[flag] ?? { allowed: 0, denied: 0 };
			use.flags[flag][outcome] += total;
			use[outcome]             += total;
			use.last                  = Math.max(use.last, lastUsed);

			for (const [start, count] of Object.entries(days ?? {})) {

				const i = Math.round((Number(start) - first) / day);

				if (i >= 0 && i < use.series.length) {
					use.series[i][outcome] += count;
				}
			}
		}

		return use;
	}

	// the usage of the permissions on the card of a REST endpoint (see usageOfGrant), on one scale: the busiest day of any
	// of them is the full height of the bars, so the rows can be compared
	static annotateUsage(grants, data) {

		grants.forEach(g => g.usage = AppMapPanels.usageOfGrant(g, data));

		const peak = d3.max(grants, g => d3.max(g.usage?.series ?? [], d => d.allowed + d.denied)) ?? 0;

		grants.forEach(g => {

			if (g.usage) {
				g.usage.peak = peak;
			}
		});
	}

	// the requests per day as bars from left to right, the oldest day first: the allowed ones in the ink, the denied ones
	// in red on top, a thin line along the bottom so that a quiet time still shows as such
	static drawSeries(ctx, use, x, y, width, height, ink, red, k) {

		const across = width / use.series.length;

		ctx.save();
		ctx.fillStyle = d3.color(ink).copy({ opacity: 0.35 }).formatRgb();
		ctx.fillRect(x, y + height - 1 / k, width, 1 / k);

		use.series.forEach((d, i) => {

			const scale   = use.peak ? height / use.peak : 0;
			const allowed = d.allowed ? Math.max(1.5 / k, d.allowed * scale) : 0;
			const denied  = d.denied ? Math.max(1.5 / k, d.denied * scale) : 0;
			const left    = x + i * across + across * 0.12;
			const wide    = across * 0.76;

			ctx.fillStyle = d3.color(ink).copy({ opacity: 0.6 }).formatRgb();
			ctx.fillRect(left, y + height - allowed, wide, allowed);
			ctx.fillStyle = red;
			ctx.fillRect(left, y + height - allowed - denied, wide, denied);
		});

		ctx.restore();
	}

	// the usage on the card of a REST endpoint stays current while the card shows: AppMapView.usageOf reads it again once it
	// is ACTIVITY_REFRESH old, and where it is newer than that of the card, its rows take it over
	refreshGrantUsage(data) {

		this.view.usageOf('grants');

		const usage = this.view.usage.grants?.data;

		if (!usage || usage === data.usage) {
			return;
		}

		data.usage = usage;

		const rows = data.sections.flatMap(s => s.items).filter(row => row.grant);

		AppMapPanels.annotateUsage(rows.map(row => row.grant), usage);
		rows.forEach(AppMapPanels.updateRequestRow);
	}

	// the columns the rows of a card share, in units of the size of their text: the requests on that of a REST endpoint and
	// the grants on that of a group. On the right the widest chip among them, before it the widest number of requests and
	// the methods or rights any of them has, each in a column of its own, so that they stand below each other whatever a
	// row says; as { rows, chip, count, verbs }, rows the items that keep to them, or null on a card without such rows
	static rowSlots(renderer, items) {

		const rows = items.filter(item => item.tile?.access || item.tile?.kind === 'Grant');

		if (!rows.length) {
			return null;
		}

		const order = rows.some(item => item.tile.access) ? AppMapPanels.VERBS.map(([verb]) => verb) : AppMapPanels.RIGHTS.map(([verb]) => verb);

		return {
			rows:  new Set(rows),
			chip:  d3.max(rows, item => item.tile.chip ? AppMapPanels.labelWidth(renderer, item.tile.chip, AppMapPanels.ROW_PILL) + (item.tile.many ? 0.25 : 0) : 0),
			count: d3.max(rows, item => item.tile.count != null ? renderer.textWidth(item.tile.count, 600) / AppMapRenderer.LABEL_SIZE * AppMapPanels.ROW_PILL : 0),
			// a column for each method that a row of the card allows, in the order of VERBS
			verbs: order.filter(verb => rows.some(item => item.tile.verbs?.some(v => v.verb === verb)))
		};
	}

	// a number of requests in short, as on the row of a request and in the cells of its matrix
	static shortCount(count) {

		return new Intl.NumberFormat(undefined, { notation: 'compact' }).format(count);
	}

	// the row of a path on the card of a REST endpoint: the path, the methods its permission allows, who may send them (see
	// matrixOf), how often it was requested, and the permission, so that its box can switch it (see AppMapView.flipAccess)
	static requestRow(g) {

		const row = {
			name:   '/' + g.signature,
			kind:   'Request',
			mono:   true,
			grant:  g,
			open:   { area: 'security', tab: 'resource-access', filter: g.signature }
		};

		return AppMapPanels.updateRequestRow(row);
	}

	// who may send the requests of a row, after its permission changed: the matrix of the permission for its box, the methods
	// it allows, faded where they are set without effect, and a chip with everyone who may send at least one of them
	static updateRequestRow(row) {

		const access = AppMapPanels.matrixOf(row.grant);
		const [pub, auth] = access.rows;
		const some   = access.rows.length - 2;
		const any    = (cells, state) => cells.some(cell => cell === state);
		const chips  = [any(pub.cells, 'yes') && 'public', any(auth.cells, 'yes') && 'signed in', any(auth.cells, 'some') && `${some} ${some > 1 ? 'grantees' : 'grantee'}`].filter(Boolean);
		const set    = AppMapPanels.VERBS.map(([verb], i) => ({ verb, cells: access.rows.map(r => r.cells[i]) })).filter(v => v.cells.some(cell => cell !== 'no'));

		const use    = row.grant.usage;

		return Object.assign(row, {
			access,
			count:    use ? AppMapPanels.shortCount(use.allowed + use.denied) : null,
			series:   use,
			verbs:    set.map(({ verb, cells }) => ({ verb, live: cells.some(cell => cell === 'yes' || cell === 'some') })),
			chip:     set.length ? (chips.length ? chips.join(' · ') : 'no effect') : 'admins only',
			chipTint: { access: any(pub.cells, 'yes') ? 'pub' : (chips.length ? 'auth' : 'none') }
		});
	}

	// who may send which request a resource access permission allows, as a matrix of audiences and methods (see VERBS): the
	// flags of a permission only apply to those who can see it, so a method for public users needs the permission to be
	// visible to public users, and one for signed-in users needs it to be visible to them, or to public users, or to a user
	// or group with rights on the permission (a grantee), which then applies to them alone. Public and signed-in users come
	// first, then the grantees; a cell is one of the states of ACCESS_CELL
	static matrixOf(g) {

		const flags    = g.flags ?? 0;
		const visPub   = !!g.visibleToPublicUsers;
		const visAuth  = !!g.visibleToAuthenticatedUsers;
		const grantees = (g.grantees ?? []).map(p => ({ name: p.name ?? p.id, group: !!p.isGroup || /Group$/.test(p.type ?? '') }));
		const allAuth  = visAuth || visPub;
		const cells    = state => AppMapPanels.VERBS.map(([, a, p]) => state((flags & a) !== 0, (flags & p) !== 0));

		const rows = [
			{ who: 'Public users', about: 'Visitors who are not signed in', icon: 'public', kind: 'pub', cells: cells((auth, pub) => !pub ? 'no' : (visPub ? 'yes' : 'none')) },
			{ who: 'Signed-in users', about: 'Every user who is signed in', icon: 'auth', kind: 'auth', cells: cells(auth => !auth ? 'no' : (allAuth ? 'yes' : (grantees.length ? 'some' : 'none'))) },
			...grantees.map(p => ({ who: p.name, about: `${p.group ? 'Group' : 'User'} with rights on the permission`, icon: p.group ? 'group' : 'user', kind: 'grantee', cells: cells(auth => !auth ? 'no' : (allAuth ? 'implied' : 'yes')) }))
		];

		const legend = [...new Set(rows.flatMap(r => r.cells))].filter(state => state !== 'no' && state !== 'implied');

		// with its usage, public and signed-in users have the number of their requests below their cells, which takes
		// higher rows, and the sum takes a line at the foot
		const use    = g.usage ?? null;
		const rowH   = use ? 2.4 : 1.9;

		// the usage below a hairline: a heading with the sums, the requests per day CHART_LINES high, the dates below them
		const chart  = use ? 1.7 + 0.8 + AppMapPanels.CHART_LINES + 1.2 : 0;

		return {
			rows,
			legend,
			use,
			rowH,
			// the rows the box needs, as AppMapView.tileZoom counts them, in the lines of drawMatrix, which stand ACCESS_LINE
			// of them apart: the strip and its hairline, the badges of the methods, the audiences, the hairline below them,
			// the legend in one line, the line about admins, the usage
			lines: AppMapPanels.ACCESS_LINE * (3.8 + rows.length * rowH + 0.9 + (legend.length ? 1.1 : 0) + 0.6 + chart) - 0.6
		};
	}

	// how a click on a cell of the matrix of a permission changes it: on, the method is allowed for the audience, with the
	// visibility it needs; off, it is not. Where a change opens more than the cell to anyone, the question to ask first. As
	// { flags, visibleToPublicUsers, visibleToAuthenticatedUsers, question }
	static switchAccess(g, kind, column, on) {

		const [verb, a, p] = AppMapPanels.VERBS[column];
		const allAuth      = AppMapPanels.VERBS.reduce((bits, [, auth]) => bits | auth, 0);
		const allPub       = AppMapPanels.VERBS.reduce((bits, [, , pub]) => bits | pub, 0);
		const grantees     = (g.grantees ?? []).length > 0;
		const questions    = [];
		let flags          = g.flags ?? 0;
		let visPub         = !!g.visibleToPublicUsers;
		let visAuth        = !!g.visibleToAuthenticatedUsers;

		if (kind === 'pub') {

			if (on) {

				flags |= p;
				questions.push(`Public users may then send ${verb} without signing in.`);

				// visible to public users, the permission is visible to every signed-in user, too
				if (!visPub && !visAuth && grantees && (flags & allAuth)) {
					questions.push('Every signed-in user may then also send what only the users and groups with rights on it may send now.');
				}

				visPub = true;

			} else {

				flags &= ~p;

				// with no method left for public users the permission is no longer visible to them; signed-in users keep what
				// they were allowed through that
				if (visPub && !(flags & allPub)) {

					visPub  = false;
					visAuth = visAuth || (flags & allAuth) !== 0;
				}
			}

		} else if (on) {

			flags |= a;

			// a method for every signed-in user makes the permission visible to them, which opens the other methods for the
			// users and groups with rights on it to them as well
			if (kind === 'auth' && !visPub && !visAuth) {

				if (grantees && (flags & allAuth & ~a)) {
					questions.push('Every signed-in user may then also send what only the users and groups with rights on it may send now.');
				}

				visAuth = true;
			}

		} else {

			flags &= ~a;
		}

		return { flags, visibleToPublicUsers: visPub, visibleToAuthenticatedUsers: visAuth, question: questions.length ? questions.join(' ') + ' Continue?' : null };
	}

	// a row that leads to another element of the map: an arrow and a chip in its colour, with the kind of the relation in
	// short, or the kind of the element; opened, the relation in full and what the map knows it from
	leadRow(node, chip, does, evidence) {

		return {
			name:     node.n,
			kind:     AppMapModel.KIND_LABEL[node.k],
			arrow:    true,
			tint:     node,
			chip:     chip ?? AppMapModel.KIND_LABEL[node.k],
			// its box: what it leads to, as a chip in its colour, and in words how it relates, with what the map knows it
			// from in grey (see drawSummary); the way there is the button at its foot
			summary:  {
				chip:      AppMapModel.KIND_LABEL[node.k],
				chipTint:  node,
				marks:     [],
				sentences: () => [
					does ? `${does[0].toUpperCase()}${does.slice(1)} ${node.n}` : node.n,
					evidence && { text: String(evidence), muted: true }
				]
			},
			open:     AppMapPanels.leadTo(node)
		};
	}

	// the strip of a type above its rows (see drawTypeStrip): what kind of type it is and the types it extends; who may
	// request it over REST, from its resource access permissions, and how many groups have rights on it; and how many
	// pages, shared components and methods read, create, change and delete its objects, as the relations of the map have
	// them, each counted once
	typeStripOf(n, schema) {

		const kind    = schema.isServiceClass ? 'Service class' : (schema.isInterface ? 'Interface' : (schema.isAbstract ? 'Abstract type' : (schema.isBuiltin ? 'Built-in type' : 'Type')));
		const parents = [...new Set(this.view.links.filter(l => l.source === n && l.kind === 'inherits').map(l => l.target))];
		const users   = kind => new Set(this.view.links.filter(l => l.target === n && l.kind === kind && l.source !== n).map(l => l.source)).size;
		const groups  = new Set(this.view.links.filter(l => l.target === n && l.kind === 'grant').map(l => l.source)).size;
		const rest    = n.rest ?? null;

		return {
			kind,
			parents,
			access: !rest || !rest.n ? 'admins only' : [rest.pub && 'public', rest.auth && 'signed in'].filter(Boolean).join(' · ') || 'no effect',
			accessTint: { access: rest?.pub ? 'pub' : (rest?.auth ? 'auth' : 'none') },
			groups,
			uses: AppMapPanels.TYPE_USES.map(([key, label, verb]) => ({ label, verb, count: users(key) }))
		};
	}

	// how the map relates the code and the pages to a type, with what the strip calls it and the method of HTTP whose colour
	// it has
	static TYPE_USES = [['reads', 'read by', 'GET'], ['creates', 'created by', 'POST'], ['updates', 'changed by', 'PUT'], ['deletes', 'deleted by', 'DELETE']];

	// the strip is this many rows of text high
	static TYPE_STRIP = 5;

	// the frame of the strip of a card (see STRIP_LINES): its left and right edge, the size of its text, the middle of its
	// first and second line and where its hairline stands
	static stripFrame(panel) {

		const em = panel.em;

		return {
			left:   panel.left,
			right:  panel.x0 + panel.width - (panel.left - panel.x0),
			font:   em * 0.62,
			first:  panel.stripTop + em * AppMapPanels.STRIP_LINES[0],
			second: panel.stripTop + em * AppMapPanels.STRIP_LINES[1],
			line:   panel.stripTop + em * AppMapPanels.STRIP_LINES[2]
		};
	}

	// where the lines of the strip of a card stand below its header, in units of the size of its text (em): the middle of
	// its first and of its second line, and its hairline. Its pills are 1.15 high, so the first keeps the padding of the
	// sides (TILE_PAD) from the header, the second half a line from the first, the hairline as much from the second
	static STRIP_LINES = [1.77, 3.42, 4.6];

	// the strip of a type below the header of its card, in the style of the boxes (see drawField): on the left the kind of
	// type as a chip in its colour and the types it extends as chips in theirs; on the right who may request it over REST,
	// as the chip of a request, and how many groups have rights on it; below them, after USED BY, how many pages, shared
	// components and methods read, create, change and delete its objects, as pills in the colours of the methods of HTTP,
	// faint where there are none; a hairline below
	drawTypeStrip(renderer, strip, panel, colour, k) {

		const look = this.view.look;
		const { left, right, font, first, second, line } = AppMapPanels.stripFrame(panel);

		// the kind and the types it extends
		let x = left;

		x += this.drawLabel(renderer, strip.kind, x, first, font, 'chip', colour, k);

		for (const parent of strip.parents) {
			x += this.drawLabel(renderer, `extends ${parent.n}`, x, first, font, 'chip', this.tintOf(renderer, parent) ?? look.label, k);
		}

		// who may request it over REST, and the groups with rights on it
		let r = right;

		if (strip.groups) {
			r -= this.drawLabel(renderer, `${strip.groups} ${strip.groups === 1 ? 'group' : 'groups'} with rights`, r, first, font, 'chip', look.label, k, 'right');
		}

		this.drawLabel(renderer, `REST: ${strip.access}`, r, first, font, 'chip', this.tintOf(renderer, strip.accessTint) ?? look.label, k, 'right');

		// what uses it, in the colours of the methods of HTTP
		x = left + this.drawCaption(renderer, 'Used by', left, second, font);

		for (const { label, verb, count } of strip.uses) {
			x += this.drawLabel(renderer, `${label} ${count}`, x, second, font, count ? 'on' : 'off', AppMapPanels.VERB_COLOR[verb][look.dark ? 1 : 0], k);
		}

		this.drawHairline(renderer.ctx, left, right, line, k);
	}

	// what the strip of an external service shows (see drawExternalStrip): what kind of service it is and where it is, from
	// its relations and its id; how many methods and pages use it; the functions they call it with, from the evidence of
	// the relations (see ScriptScanner.callOf)
	externalStripOf(n) {

		const links = this.view.links.filter(l => l.target === n && l.source !== n);
		const what  = n.id.replace(/^ext:/, '');
		const where = what.includes(':') ? what.slice(what.indexOf(':') + 1) : null;
		const kind  = { requests: what === 'http' ? 'HTTP requests' : (what === 'geocoding' ? 'Geocoding' : (what.startsWith('translation') ? 'Translation' : 'Web service')), mails: 'Mail server', executes: 'Script', queries: 'Database' }[links[0]?.kind] ?? 'External service';
		const count = k => new Set(links.filter(l => l.source.k === k).map(l => l.source)).size;

		return {
			external: true,
			kind,
			// where it is, unless the header names it already or it has no place of its own
			where:    [where ?? what].filter(w => w !== n.n && !['http', 'geocoding', 'exec', 'mail'].includes(w))[0] ?? null,
			users:    [['method', 'method', 'methods'], ['page', 'page', 'pages'], ['component', 'shared component', 'shared components']].map(([k, one, many]) => count(k) && `${count(k)} ${count(k) === 1 ? one : many}`).filter(Boolean),
			calls:    [...new Set(links.map(l => l.x?.f).filter(Boolean))].sort(),
			lead:     AppMapPanels.leadOf(n, links)
		};
	}

	// the strip of an external service below the header of its card, in the style of that of a type (see drawTypeStrip):
	// on the left what kind of service it is and where, on the right what uses it; below, after CALLS, the functions it
	// is called with, those of the methods of HTTP in their colours, and in grey what the service is to its code (see
	// leadOf), alone where the functions are not known; a hairline below
	drawExternalStrip(renderer, strip, panel, colour, k) {

		const ctx  = renderer.ctx;
		const look = this.view.look;
		const { left, right, font, first, second, line } = AppMapPanels.stripFrame(panel);

		// what and where
		let x = left;

		x += this.drawLabel(renderer, strip.kind, x, first, font, 'chip', colour, k);

		if (strip.where) {
			x += this.drawLabel(renderer, AppMapPanels.cutText(renderer, strip.where, 600, font, (right - left) * 0.4), x, first, font, 'chip', look.label, k);
		}

		// what uses it
		let r = right;

		for (const text of [...strip.users].reverse()) {
			r -= this.drawLabel(renderer, text, r, first, font, 'chip', look.label, k, 'right');
		}

		// the functions it is called with, those of the methods of HTTP as their badges
		x = left;

		if (strip.calls.length) {

			x += this.drawCaption(renderer, 'Calls', left, second, font);

			for (const call of strip.calls) {

				const verb = AppMapPanels.VERB_COLOR[call.toUpperCase()] ? call.toUpperCase() : null;

				x += this.drawLabel(renderer, `${call}()`, x, second, font, verb ? 'badge' : 'chip', verb ? AppMapPanels.VERB_COLOR[verb][look.dark ? 1 : 0] : look.label, k);
			}
		}

		// what the service is to its code, in the room that is left
		if (strip.lead && right - x > font * 6) {

			ctx.fillStyle = look.label;
			renderer.screenText(AppMapPanels.cutText(renderer, strip.lead, 400, font * 1.1, right - x), x, second, 400, font * 1.1, false, 'left');
		}

		this.drawHairline(ctx, left, right, line, k);
	}

	// what the strip of a group shows (see drawGroupStrip): the groups it is a member of, how many users and groups are its
	// members, and on how many types it has each right, from the map data of the group
	groupStripOf(n, members) {

		const group   = (this.view.model.groups ?? []).find(g => g.id === n.id);
		const letters = Object.values(group?.p ?? {}).map(String);

		return {
			group:   true,
			parents: (group?.in ?? []).map(id => this.view.byId.get(id)).filter(Boolean),
			users:   members.filter(m => !m.isGroup).length,
			groups:  members.filter(m => m.isGroup).length,
			rights:  AppMapPanels.RIGHTS.map(([badge, letter, label], i) => ({ badge, count: letters.filter(l => l.includes(letter)).length, label: ['reads', 'writes', 'deletes', 'controls access to'][i] }))
		};
	}

	// the strip of a group below the header of its card, in the style of that of a type (see drawTypeStrip): on the left
	// that it is a group and the groups it is a member of, as chips in their colours; on the right its members; below,
	// after RIGHTS ON, on how many types it may read, write, delete and control access, in the colours of its badges,
	// faint where there are none; a hairline below
	drawGroupStrip(renderer, strip, panel, colour, k) {

		const look = this.view.look;
		const { left, right, font, first, second, line } = AppMapPanels.stripFrame(panel);

		// that it is a group, and the groups it is a member of
		let x = left;

		x += this.drawLabel(renderer, 'Group', x, first, font, 'chip', colour, k);

		for (const parent of strip.parents) {
			x += this.drawLabel(renderer, `member of ${parent.n}`, x, first, font, 'chip', this.tintOf(renderer, parent) ?? look.label, k);
		}

		// its members
		const members = [
			strip.users && `${strip.users} ${strip.users === 1 ? 'user' : 'users'}`,
			strip.groups && `${strip.groups} ${strip.groups === 1 ? 'group' : 'groups'}`
		].filter(Boolean);

		let r = right;

		for (const text of (members.length ? members : ['no members']).reverse()) {
			r -= this.drawLabel(renderer, text, r, first, font, members.length ? 'chip' : 'off', look.label, k, 'right');
		}

		// its rights, in the colours of their badges
		x = left + this.drawCaption(renderer, 'Rights on', left, second, font);

		for (const { badge, count, label } of strip.rights) {
			x += this.drawLabel(renderer, `${label} ${count} ${count === 1 ? 'type' : 'types'}`, x, second, font, count ? 'on' : 'off', AppMapPanels.RIGHT_COLOR[badge][look.dark ? 1 : 0], k);
		}

		this.drawHairline(renderer.ctx, left, right, line, k);
	}

	// the strip of a REST endpoint below the header of its card, in the style of that of a type (see drawTypeStrip): on the
	// left what kind of endpoint it is, with what it gives access to as a chip in its colour; on the right the requests of
	// the last days over all its permissions, as bars and in sum; below them, after ALLOWS, the methods any permission
	// allows as badges, faint where none does, and on the right who may send any of them. Read from the rows of its
	// permissions when it is drawn, so that it follows a switched cell and fresh usage at once
	drawEndpointStrip(renderer, data, panel, colour, k) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const dark   = look.dark ? 1 : 0;
		const rows   = data.sections.flatMap(s => s.items).filter(row => row.grant);
		const target = data.strip.target;
		const { left, right, font, first, second, line } = AppMapPanels.stripFrame(panel);

		// what kind of endpoint, and what it gives access to
		let x = left;

		x += this.drawLabel(renderer, target?.k === 'method' ? 'Method endpoint' : (target?.k === 'type' ? 'Type endpoint' : 'REST endpoint'), x, first, font, 'chip', colour, k);

		if (target) {
			x += this.drawLabel(renderer, target.n, x, first, font, 'chip', this.tintOf(renderer, target) ?? look.label, k);
		}

		// the requests of the last days over all its permissions: the sums, the denied ones in red, and before them the bars
		const uses = rows.map(row => row.grant.usage).filter(Boolean);

		if (uses.length) {

			const days    = uses[0].series.length;
			const series  = Array.from({ length: days }, (_, i) => ({ day: uses[0].series[i].day, allowed: d3.sum(uses, u => u.series[i]?.allowed ?? 0), denied: d3.sum(uses, u => u.series[i]?.denied ?? 0) }));
			const allowed = d3.sum(uses, u => u.allowed);
			const denied  = d3.sum(uses, u => u.denied);
			const red     = AppMapPanels.VERB_COLOR.DELETE[dark];
			const sums    = [[`${AppMapPanels.shortCount(allowed + denied)} requests in ${uses[0].days} days`, look.label], denied && [`${AppMapPanels.shortCount(denied)} denied`, red]].filter(Boolean);
			let   r       = right;

			for (const [text, ink] of [...sums].reverse()) {

				ctx.fillStyle = ink;
				renderer.screenText(text, r, first, 600, font, false, 'right');
				r -= renderer.textWidth(text, 600) / AppMapRenderer.LABEL_SIZE * font + font * 0.8;
			}

			const tall = font * AppMapPanels.PILL_HEIGHT;

			AppMapPanels.drawSeries(ctx, { series, peak: d3.max(series, d => d.allowed + d.denied) ?? 0 }, r - font * 9, first - tall / 2, font * 9, tall, look.label, red, k);
		}

		// the methods any permission allows as their badges, and who may send any of them
		const index = verb => AppMapPanels.VERBS.findIndex(([v]) => v === verb);
		const live  = (verb, kinds) => rows.some(row => row.access.rows.some(a => (kinds.includes(a.kind) && a.cells[index(verb)] === 'yes') || (kinds.includes('auth') && a.kind === 'auth' && a.cells[index(verb)] === 'some')));

		x = left + this.drawCaption(renderer, 'Allows', left, second, font);

		for (const [verb] of AppMapPanels.VERBS) {
			x += this.drawLabel(renderer, verb, x, second, font, live(verb, ['pub', 'auth', 'grantee']) ? 'badge' : 'badge-off', AppMapPanels.VERB_COLOR[verb][dark], k);
		}

		let r = right;

		for (const [text, kind] of [['signed in', 'auth'], ['public', 'pub']]) {
			r -= this.drawLabel(renderer, text, r, second, font, AppMapPanels.VERBS.some(([verb]) => live(verb, [kind])) ? 'on' : 'off', AppMapPanels.ACCESS_CELL.some[1 + dark], k, 'right');
		}

		this.drawHairline(ctx, left, right, line, k);
	}

	// what the box of a field shows (see drawField), in the style of that of a relationship: its type as a chip, whether
	// it must be set, is unique and is indexed as marks that can be switched where the field allows it (they are its
	// settings, see AppMapView.flip), and below them only what applies: its default, the values of an enum as chips, the
	// format of anything else, whether it is read-only, and where it is inherited from. A function property computes its
	// value, so it has no marks; it shows the first line of the code that reads and writes it, the whole is in the Code
	// area
	static fieldOf(p, owner, n, shown, editable, code, script, readers) {

		const values   = String(p.format ?? '').split(',').map(v => v.trim()).filter(Boolean);
		const computed = script || !!code;
		const isEnum   = !computed && /^Enum/.test(p.type ?? '') && values.length > 0;
		const most     = AppMapPanels.ENUM_VALUES;

		// the first line of code that says something, not a brace, with an ellipsis where more follows
		const first = text => {

			const lines = String(text ?? '').trim().split('\n').map(l => l.trim()).filter(l => l && l !== '{' && l !== '}');

			return (lines[0] ?? '') + (lines.length > 1 ? ' …' : '');
		};

		const facts = (computed ? [
			['Reads', code?.readFunction ? first(code.readFunction) : 'no code', !!code?.readFunction],
			['Writes', code?.writeFunction ? first(code.writeFunction) : 'cannot be written', !!code?.writeFunction]
		] : [
			p.defaultValue != null && p.defaultValue !== '' && ['Default', String(p.defaultValue)],
			!isEnum && p.format && ['Format', String(p.format)],
			p.readOnly && ['Read-only', 'cannot be changed']
		]).concat([owner !== n && ['From', owner.n]]).filter(Boolean);

		const shownValues = isEnum ? values.slice(0, most).concat(values.length > most ? [`+${values.length - most}`] : []) : [];
		const valueLines  = Math.ceil(shownValues.length / AppMapPanels.ENUM_COLUMNS);

		return {
			type:     shown,
			computed,
			settings: computed ? [] : [['Required', !!p.notNull, null, editable && 'notNull'], ['Unique', !!p.unique, null, editable && 'unique'], ['Indexed', !!p.indexed, null, editable && 'indexed']],
			facts,
			values:   shownValues,
			// who reads a function property, as the methods have their callers (see drawCallers)
			readers:  computed ? (readers ?? []) : null,
			// the lines the box needs, as AppMapView.tileZoom counts them: the type and the marks take two, the values of an
			// enum one per row of chips, everything else one each, or one that there is nothing more, and one for the readers
			lines:    2.4 + valueLines + Math.max(facts.length, valueLines ? 0 : 1) + (computed ? 1.2 : 0)
		};
	}

	// an enum shows at most this many of its values in the box of its field
	static ENUM_VALUES  = 8;
	static ENUM_COLUMNS = 3;

	// what the box of a relationship shows, the same as the relationship dialog in the Code area, in the words of the map
	// (see drawRelation): the two types with the relationship type between them, an arrow in its direction and how many
	// objects there are at either end, below each type the property that leads to the other; below them what the
	// relationship does beyond connecting, as sentences, only what is set. The schema relationship is the one between the
	// owner and the target with this relationship type that starts at the owner, unless the property is a StartNode or
	// StartNodes, and whose multiplicity at the end of the property agrees with it holding one or many objects
	static relationOf(p, owner, n, target, relationships) {

		const incoming   = /StartNodes?$/.test(p.className ?? '');
		const type       = p.relationshipType ?? '';
		const [from, to] = incoming ? [target, owner] : [owner, target];
		// a type of the map is its schema node, and one built into Structr is known by its name
		const is         = (node, m) => !!node && (node.id === m.id || node.name === m.n);
		const far        = r => incoming ? r.sourceMultiplicity : r.targetMultiplicity;
		const rel        = relationships.find(r => r.relationshipType === type && is(r.sourceNode, from) && is(r.targetNode, to) && (far(r) === '*') === !!p.isCollection) ?? null;
		const count      = multiplicity => multiplicity === '1' ? '1' : 'n';
		const facts      = [];

		if (p.notNull) {
			facts.push('Must be set');
		}

		if (rel) {

			const pp     = rel.permissionPropagation ?? 'None';
			const passes = AppMapPanels.PROPAGATED.filter(([key]) => (rel[key] ?? 'Remove') !== 'Remove').map(([, label]) => label);
			const list   = items => items.length > 1 ? items.slice(0, -1).join(', ') + ' and ' + items[items.length - 1] : items[0];

			facts.push(...[
				[null, `Deleting ${from.n} deletes its ${to.n}`, `Deleting ${to.n} deletes its ${from.n}`, 'Deleting either side deletes the other', 'Deletes the other side where a constraint requires it'][rel.cascadingDeleteFlag ?? 0],
				[null, `${from.n} can create its ${to.n} on the fly`, `${to.n} can create its ${from.n} on the fly`, 'Either side can create the other on the fly'][rel.autocreationFlag ?? 0],
				pp === 'None' ? null : (passes.length ? `Passes ${list(passes)} rights ` + { Out: `on to ${to.n}`, In: `on to ${from.n}`, Both: 'both ways' }[pp] : `Removes all rights ` + { Out: `on the way to ${to.n}`, In: `on the way to ${from.n}`, Both: 'both ways' }[pp]),
				rel.propertyMask ? `Hides ${rel.propertyMask}` : null
			].filter(Boolean));
		}

		if (owner !== n) {
			facts.push(`Inherited from ${owner.n}`);
		}

		const remote = rel ? (incoming ? rel.targetJsonName : rel.sourceJsonName) : null;

		// a relationship that does nothing beyond connecting says so in a line of its own
		const quiet  = !!rel && facts.length === (p.notNull ? 1 : 0) + (owner !== n ? 1 : 0);

		return {
			here:     { node: owner, name: p.jsonName, count: rel ? count(incoming ? rel.targetMultiplicity : rel.sourceMultiplicity) : null },
			there:    { node: target, name: remote, count: p.isCollection ? 'n' : '1' },
			type,
			incoming,
			facts,
			quiet,
			// without its schema relationship, the box knows neither the other side nor what the relationship does
			known:    !!rel,
			// the lines the box needs, as AppMapView.tileZoom counts them: the drawing takes three, the sentences one each
			lines:    3.4 + Math.max(1, facts.length + (quiet ? 1 : 0))
		};
	}

	// whether a property of the schema computes its value with a script: its class is FunctionProperty, or, where the
	// schema leaves the class out, its type is Function
	static isFunctionProperty(p) {

		return /FunctionProperty$/.test(p.className ?? '') || p.type === 'Function';
	}

	// the rights a schema relationship can pass on, with what the map calls them
	static PROPAGATED = [['readPropagation', 'read'], ['writePropagation', 'write'], ['deletePropagation', 'delete'], ['accessControlPropagation', 'access control']];

	// a group: the types it has grants on with the rights it has there, its members, users and groups, and the groups it is
	// a member of itself. A grant shows its rights as badges in columns, as a request its methods (see rowSlots), and
	// leads to its type; the rights are changed in the properties of the group, where the card leads
	groupRows(n, members) {

		const group  = (this.view.model.groups ?? []).find(g => g.id === n.id);
		const byId   = this.view.byId;
		const byName = (a, b) => a.name.localeCompare(b.name);

		const grants = Object.entries(group?.p ?? {}).map(([id, letters]) => [byId.get(id), String(letters)]).filter(([node]) => node).map(([node, letters]) => ({
			name:     node.n,
			kind:     'Grant',
			arrow:    true,
			tint:     node,
			verbs:    AppMapPanels.RIGHTS.filter(([, letter]) => letters.includes(letter)).map(([verb]) => ({ verb, live: true })),
			// its box: the rights as marks in their colours, and in words what the members may do; the type is its name
			summary:  {
				chip:      null,
				marks:     AppMapPanels.RIGHTS.map(([verb, letter, label]) => ({ text: label.toLowerCase(), on: letters.includes(letter), colour: AppMapPanels.RIGHT_COLOR[verb] })),
				sentences: () => [
					AppMapPanels.describeRights(node.n, letters),
					{ text: 'Changed under Type Permissions of the group', muted: true }
				]
			},
			open:     AppMapPanels.leadTo(node)
		})).sort(byName);

		// a member: whether it is an admin and whether it is blocked, where the map knows it, as marks that can be switched
		// (they are its settings, see AppMapView.flip), and in words what that means
		const people = members.map(m => {

			const settings = [m.isAdmin != null && ['Admin', !!m.isAdmin, null, 'isAdmin'], m.blocked != null && ['Blocked', !!m.blocked, null, 'blocked']].filter(Boolean);

			return {
				name:     m.name ?? m.id,
				kind:     m.isGroup ? 'Group' : 'User',
				glyph:    m.isGroup ? '◎' : '●',
				tint:     m.isGroup ? byId.get(m.id) ?? null : null,
				settings,
				summary:  {
					chip:      m.isGroup ? 'Group' : 'User',
					chipTint:  m.isGroup ? byId.get(m.id) ?? null : null,
					marks:     settings.map(([label], index) => ({ text: label.toLowerCase(), index })),
					sentences: tile => {

						const on = label => tile.settings.find(([l]) => l === label)?.[1];

						return [
							m.isGroup ? `A member of ${n.n}, so its members are as well` : `A member of ${n.n}`,
							on('Admin') && { text: 'An admin may do everything, whatever the groups allow', muted: true },
							on('Blocked') && { text: 'Blocked, cannot sign in', muted: true }
						];
					}
				},
				edit:     { kind: 'principal', id: m.id, name: m.name ?? m.id },
				open:     { area: 'security', tab: 'users-and-groups', group: !!m.isGroup, filter: m.name ?? '' }
			};

		}).sort(byName);

		const parents = (group?.in ?? []).map(id => byId.get(id)).filter(Boolean).map(node => ({
			...this.leadRow(node, null, 'member of'),
			summary: {
				chip:      'Group',
				chipTint:  node,
				marks:     [],
				sentences: () => [`${n.n} is a member of ${node.n}`, { text: `So the members of ${n.n} have the rights of ${node.n} as well`, muted: true }]
			},
			open:    { area: 'security', tab: 'users-and-groups', group: true, filter: node.n }
		}));

		return [
			// what the letters of the badges stand for, once above them
			{ heading: `Grants · ${grants.length}` + (grants.length ? ' · ' + AppMapPanels.RIGHTS.map(([badge, , label]) => `${badge} ${label}`).join(', ') : ''), items: grants },
			{ heading: `Members · ${people.length}`, items: people },
			{ heading: `Member of · ${parents.length}`, items: parents }
		];
	}

	// what the members of a group may do with every object of a type, as a sentence on its card: read, write and delete,
	// with access control where they may change the rights of others
	static describeRights(type, letters) {

		const may  = AppMapPanels.RIGHTS.filter(([, letter]) => letter !== 'a' && letters.includes(letter)).map(([, , label]) => label.toLowerCase());
		const list = may.length > 1 ? may.slice(0, -1).join(', ') + ' and ' + may[may.length - 1] : may[0];
		const ac   = letters.includes('a');

		if (!may.length) {
			return ac ? `May change who may access every ${type}` : `May do nothing with ${type}`;
		}

		return `May ${list} every ${type}` + (ac ? ', and control access' : '');
	}

	// an external service: the methods and pages that use it, and how
	externalRows(n) {

		const links = this.view.links.filter(l => l.target === n && l.source !== n);
		const row   = l => {

			const call = l.x?.f ? l.x : null;

			const lead = this.leadRow(l.source);

			return {
				...lead,
				chip:     call ? `${call.f}()` : AppMapModel.KIND_LABEL[l.source.k],
				// its box: where the service is called, and the line of code that calls it
				summary:  {
					...lead.summary,
					sentences: () => [
						call ? `Calls ${call.f}() in line ${call.l}` : 'Uses this service',
						call?.c && { text: String(call.c), mono: true }
					]
				}
			};
		};

		// the code that uses the service, by its kind, methods first; each row with the call that was found
		return d3.groups(links, l => l.source.k).sort(([a], [b]) => (a === 'method' ? 0 : 1) - (b === 'method' ? 0 : 1)).map(([kind, list]) => ({
			heading: `${AppMapModel.KIND_LABEL[kind]}s · ${list.length}`,
			items:   list.map(row).sort((a, b) => a.name.localeCompare(b.name))
		}));
	}

	// what an external service is to the code that uses it, in one sentence above its rows
	static leadOf(n, links) {

		const kind = links[0]?.kind;
		const what = n.id.replace(/^ext:/, '');

		switch (kind) {

			case 'requests':
				return what === 'http' ? 'The code below sends requests to URLs it computes when it runs.'
					: what === 'geocoding' ? 'The code below looks up addresses with this provider.'
					: what.startsWith('translation') ? 'The code below translates texts with this provider.'
					: `The code below sends requests to ${what}.`;
			case 'mails':
				return 'The code below sends e-mails through the SMTP server in structr.conf.';
			case 'executes':
				return what === 'exec' ? 'The code below runs scripts it chooses when it runs.' : 'The code below runs this script, as structr.conf names it.';
			case 'queries':
				return 'The code below queries this database.';
			default:
				return null;
		}
	}

	// where the rows of an element stand on its card in the given number of columns, in units of the size of their text: a
	// heading per section that has rows, the rows of a section filling its columns from top to bottom, one after the other,
	// so they read as lists; while the element loads, a few empty rows
	static insideLayout(data, columns) {

		const [tw, th] = AppMapPanels.TILE;
		const [gx, gy] = AppMapPanels.TILE_GAP;
		const items    = [];
		let y          = 0;

		const full = columns * tw + (columns - 1) * gx;

		// a section has two columns, or as many as it says, which share the whole width of the card; a card with many rows is
		// wider for its height (more columns of TILE in the loop of insidePanel), so its rows are smaller, and longer
		const group = (heading, tiles, faded, own, empty) => {

			if (!tiles.length && !empty) {
				return;
			}

			if (heading) {

				items.push({ heading, x: 0, y: y + AppMapPanels.TILE_HEADING * 0.6 });
				y += AppMapPanels.TILE_HEADING;
			}

			// a section that says so when it has no rows
			if (!tiles.length) {

				items.push({ note: empty, x: 0, y: y + th / 2 });
				y += th + gy;

				return;
			}

			const across = own ?? AppMapPanels.TILE_ACROSS;
			const width  = (full - (across - 1) * gx) / across;
			const rows   = Math.ceil(tiles.length / across);

			tiles.forEach((tile, i) => items.push({ tile, faded, col: Math.floor(i / rows), x: Math.floor(i / rows) * (width + gx), y: y + (i % rows) * (th + gy), width }));

			y += rows * (th + gy) + gy;
		};

		if (data?.failed) {

			items.push({ note: 'This could not be read', x: 0, y: AppMapPanels.TILE_HEADING * 0.6 });
			y = AppMapPanels.TILE_HEADING;

		} else if (!data?.sections) {

			group(null, [{}, {}, {}, {}, {}, {}, {}, {}], false);

		} else {

			data.sections.forEach(s => group(s.heading, s.items, !!s.faded, s.columns, s.empty));

			// unless a section says itself what is missing
			if (!data.sections.some(s => s.items.length || s.empty)) {
				items.push({ note: 'Nothing inside', x: 0, y: AppMapPanels.TILE_HEADING * 0.6 });
				y = AppMapPanels.TILE_HEADING;
			}
		}

		return { items, width: full, height: Math.max(0, y - gy) };
	}

	// the card a type, a REST endpoint, a group or an external service opens to in place of its shape: what it holds as
	// rows, as a page shows its elements as boxes, with the header above them and the foot with the way out of the map
	// below. The rows have their place on the map whatever the zoom, as the boxes of a page do: INSIDE_CARD[0] radii wide to
	// each side, TILE_WIDE rows across, and as long as they need, so that every card has the same width for its text and
	// only its length differs. Header and foot have their size on the screen (see HEAD) and grow out of that area. Em is
	// the size of the text of a tile
	insidePanel(n, k, radius = this.view.shownRadius(n)) {

		const data     = this.insides.get(n);
		const width    = 2 * radius * AppMapPanels.INSIDE_CARD[0];
		const pad      = AppMapPanels.TILE_PAD;
		const layouts  = data?.layouts ?? (this.pending ??= new Map());

		// every card is as wide as TILE_WIDE rows of TILE side by side and as long as its rows need, so that at the size of
		// their text (see AppMapView.fillZoom) all cards are equally wide and only their length differs
		const columns = AppMapPanels.TILE_WIDE;
		const key     = `${columns}`;
		const layout  = layouts.get(key) ?? layouts.set(key, AppMapPanels.insideLayout(data, columns)).get(key);
		const em      = width / (layout.width + 2 * pad);
		let   area    = (layout.height + 2 * pad) * em;

		// a type has the strip of what it is above its rows (see drawTypeStrip), TYPE_STRIP rows of text high
		const strip = data?.strip ? AppMapPanels.TYPE_STRIP * em : 0;

		area += strip;

		const size = AppMapPanels.headSize(width, area, k);
		const top  = n.y - area / 2;
		const y0   = top - size.depth;

		// the rows begin a little below the header, closer than the padding at the sides
		return { x0: n.x - width / 2, y0, width, height: size.depth + area + size.foot, em, columns, layout, head: size.depth, size, strip, stripTop: top, left: n.x - width / 2 + pad * em, top: top + strip + pad * em * 0.3, px: em * 1.3 * k };
	}

	// whether an element is close enough to show its card, even if only as a sketch
	hasInsideCard(n, k) {

		return AppMapPanels.INSIDE_KINDS.has(n.k) && this.insidePanel(n, k).px >= AppMapPanels.CODE_SKETCH;
	}

	// how far the card of an element has taken the place of its shape, from 0 to 1, see AppMapRenderer.drawElements
	insideCover(n, k) {

		const px = AppMapPanels.INSIDE_KINDS.has(n.k) ? this.insidePanel(n, k).px : 0;

		return Math.min(1, Math.max(0, (px - AppMapPanels.CODE_SKETCH) / (AppMapPanels.CODE_READ - AppMapPanels.CODE_SKETCH)));
	}

	// the types on screen that are close enough to show what they hold, with their panel and how far they have opened
	insidePanels(renderer, k) {

		return this.view.nodes
			.filter(n => AppMapPanels.INSIDE_KINDS.has(n.k) && renderer.onScreen(n, 200 + Math.max(...AppMapPanels.INSIDE_CARD) * this.view.shownRadius(n) * k))
			.map(n => ({ n, panel: this.insidePanel(n, k) }))
			.filter(({ panel }) => panel.px >= AppMapPanels.CODE_SKETCH)
			.map(c => ({ ...c, strong: Math.min(1, (c.panel.px - AppMapPanels.CODE_SKETCH) / (AppMapPanels.CODE_READ - AppMapPanels.CODE_SKETCH)) }));
	}

	// a type opens as a card like a page: from afar a sketch of its header and its tiles, close up its name in the header
	// and its properties as tiles, a relationship with a bar in the colour of the type it leads to and a dot at the edge of
	// the card where its line leaves (see drawGates). Closer still each tile opens in itself, see drawTile
	drawInsides(renderer, k, insidePanels) {

		const ctx   = renderer.ctx;
		const look  = this.view.look;
		const gates    = new Map();
		const [tw, th] = AppMapPanels.TILE;

		this.inspecting.clear();

		for (const { n, panel, strong } of insidePanels) {

			this.requestInside(n);

			const data   = this.insides.get(n);
			const colour = renderer.colorOf(n);

			if (n.k === 'api' && data?.sections) {
				this.refreshGrantUsage(data);
			}
			const em     = panel.em;
			const radius = AppMapPanels.corner(panel.width, panel.height, k);
			const read   = panel.px >= AppMapPanels.CODE_READ;

			ctx.save();
			this.drawCard(ctx, panel.x0, panel.y0, panel.width, panel.height, colour, k, 0.35 + 0.65 * strong, radius);

			ctx.beginPath();
			ctx.roundRect(panel.x0, panel.y0, panel.width, panel.height, radius);
			ctx.clip();

			const head = { x0: panel.x0, y0: panel.y0, width: panel.width, height: panel.height, ...panel.size, line: panel.y0 + panel.head };

			if (read) {

				this.inspecting.add(n);

				if (data?.open) {
					this.cards.push({ n, open: data.open, rect: [panel.x0, panel.y0, panel.width, panel.height], foot: panel.size.foot, opacity: 1 });
				}

				this.drawHeader(renderer, head, data?.kindLabel ?? AppMapModel.KIND_LABEL[n.k], n.n, colour, k);

				if (data?.strip?.endpoint) {
					this.drawEndpointStrip(renderer, data, panel, colour, k);
				} else if (data?.strip?.group) {
					this.drawGroupStrip(renderer, data.strip, panel, colour, k);
				} else if (data?.strip?.external) {
					this.drawExternalStrip(renderer, data.strip, panel, colour, k);
				} else if (data?.strip) {
					this.drawTypeStrip(renderer, data.strip, panel, colour, k);
				}

			} else {

				ctx.globalAlpha = 0.35 + 0.65 * strong;
				this.drawBand(ctx, head, colour);
			}

			const list    = [];
			const slots   = AppMapPanels.rowSlots(renderer, panel.layout.items);

			for (const item of panel.layout.items) {

				// the rows of a card share their columns, see rowSlots
				if (slots?.rows.has(item)) {
					item.slots = slots;
				}

				const x = panel.left + item.x * em;
				const y = panel.top + item.y * em;

				if (item.heading || item.note) {

					if (read) {

						ctx.globalAlpha = 1;
						// a heading is a caption as large as the chips in the rows, a note in the size of the rows
						if (item.heading) {
							this.drawCaption(renderer, item.heading, x, y, em * AppMapPanels.ROW_PILL / AppMapPanels.CAPTION);
						} else {
							ctx.fillStyle = look.label;
							renderer.screenText(item.note, x, y, 400, em * 0.8, false, 'left');
						}
					}

					continue;
				}

				// the card the tile lies in, for a glide into it, see AppMapView.glideIntoTile
				item.card = n;

				this.drawTile(renderer, item, x, y, (item.width ?? tw) * em, th * em, em, k, read ? 1 : 0.2 + 0.3 * strong, read, item.tile.owner ?? n);

				// the dot of a relationship stands at the side of the card, at the height of its tile, a little apart from
				// those of the other tiles in its row
				if (read && item.tile.target && !item.faded) {
					list.push({ row: item.tile, y: y + th * em / 2 + (item.col - (panel.columns - 1) / 2) * th * em * 0.18, panel: { x0: panel.x0, width: panel.width } });
				}
			}

			if (read) {

				gates.set(n, list);
			}

			ctx.restore();
		}

		this.drawGates(renderer, k, gates);
	}

	// the colour of a row: that of the element of the map it leads to, of who may send a request (see ACCESS_COLOR) or of its
	// method (see VERB_COLOR)
	tintOf(renderer, tint) {

		if (!tint) {
			return null;
		}

		const dark = this.view.look.dark ? 1 : 0;

		return tint.verb ? AppMapPanels.VERB_COLOR[tint.verb][dark] : (tint.access ? AppMapRenderer.ACCESS_COLOR[tint.access][dark] : renderer.colorOf(tint));
	}

	// a row of a card, as the fields of a database in Notion or Airtable: a small square with a sign (that of the type of a
	// field, an arrow to another element, the sign of a grant or a member), then its name and a dot when it must be set; on
	// the right a chip, in the colour of the element it leads to, stacked when it leads to many, as a repeater stacks its
	// copies; a faded row (one a type inherits) is paler. The square is the box of the row, framed in its colour: closer
	// up it opens in itself, as an HTML element does in its page, see drawBox
	drawTile(renderer, item, x, y, width, height, em, k, alpha, read, n) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const tile   = item.tile;
		const tint   = this.tintOf(renderer, tile.tint);
		const middle = y + height / 2;
		const sign   = height * 0.62;
		const size   = em * AppMapPanels.ROW_NAME;
		const wide   = tile.badge || tile.access ? sign * AppMapPanels.BADGE : sign;
		const faded  = item.faded ? 0.65 : 1;

		// the box of a permission stands at the end of its row, after what it allows, with the path in bold in front; every
		// other box at the start
		const end    = !!tile.access;
		const box    = [end ? x + width - wide : x, middle - sign / 2, wide, sign];
		const start  = end ? x : x + wide + em * 0.5;

		// names of fields and relationships in the monospaced font, as the design system writes them, as the methods a request
		// allows; a path in bold
		const mono   = !!(tile.field || tile.relation || tile.badge || tile.mono);
		const weight = end ? 700 : (mono ? 400 : 500);

		// the name, the chip and the dot give way while the box opens, slowly, over the whole of it (see drawBox)
		const fade = 1 - Math.min(1, Math.max(0, (sign * k - AppMapPanels.TILE_SKETCH) / (AppMapPanels.TILE_OPEN - AppMapPanels.TILE_SKETCH)));

		ctx.save();
		ctx.globalAlpha = alpha * faded * (read && tile.name ? fade : 1);

		if (!read || !tile.name) {

			// from afar a grey square for the box and a bar for the name
			ctx.fillStyle   = look.trace;
			ctx.globalAlpha = alpha * 0.45;
			ctx.fillRect(...box);
			ctx.fillRect(start, middle - em * 0.2, Math.min(width * 0.55, (tile.name?.length ?? 8) * em * 0.5), em * 0.4);
			ctx.restore();

			return;
		}

		const limit = end ? box[0] - em * 0.5 : x + width;
		let   room  = limit - start;

		// the rows of a card stand in columns (see rowSlots): what lies before the chip begins where the widest chip of the
		// card begins, not where its own does
		const slots = item.slots ?? null;

		// on the right, the chip, stacked where it stands for many, as a repeater stacks its copies; on the colour of the card,
		// so that the stack behind it does not show through
		if (tile.chip) {

			const pill   = em * AppMapPanels.ROW_PILL;
			const ink    = this.tintOf(renderer, tile.chipTint) ?? tint ?? look.label;
			const label  = AppMapPanels.cutText(renderer, tile.chip, 500, pill, width * 0.45);
			const across = AppMapPanels.labelWidth(renderer, label, pill);
			const high   = pill * AppMapPanels.PILL_HEIGHT;
			const left   = limit - across - (tile.many ? em * 0.25 : 0);

			if (tile.many) {

				ctx.strokeStyle = d3.color(ink).copy({ opacity: 0.45 }).formatRgb();
				ctx.lineWidth   = 1 / k;
				ctx.beginPath();
				ctx.roundRect(left + em * 0.25, middle - high / 2 - em * 0.18, across, high, high * AppMapPanels.PILL_ROUND);
				ctx.stroke();
			}

			ctx.beginPath();
			ctx.roundRect(left, middle - high / 2, across, high, high * AppMapPanels.PILL_ROUND);
			ctx.fillStyle = look.card;
			ctx.fill();

			this.drawLabel(renderer, label, left, middle, pill, 'chip', ink, k);

			room = (slots ? limit - Math.min(slots.chip * em, width * 0.45 + pill * AppMapPanels.PILL_PAD) : left) - start - em * 0.5;
		}

		// before the chip, how often the request was sent in the last days: the number, in grey, and before it the requests
		// per day as bars (see drawSeries)
		if (tile.count != null) {

			const font   = em * AppMapPanels.ROW_PILL;
			const across = renderer.textWidth(tile.count, 600) / AppMapRenderer.LABEL_SIZE * font;

			ctx.fillStyle = look.label;
			renderer.screenText(tile.count, start + room - across, middle, 600, font, false, 'left');

			room -= (slots ? slots.count * em : across) + em * 0.4;
		}

		if (tile.series) {

			const wide = Math.min(em * 6, room * 0.3);
			const high = height * 0.56;

			AppMapPanels.drawSeries(ctx, tile.series, start + room - wide, middle - high / 2, wide, high, look.label, AppMapPanels.VERB_COLOR.DELETE[look.dark ? 1 : 0], k);

			room -= wide + em * 0.6;
		}

		// before it, the methods a request allows or the rights of a grant, each as a badge in its colour, faded where it is
		// set without effect
		if (tile.verbs?.length) {

			const badge = em * AppMapPanels.ROW_BADGE;
			let   right = start + room;

			// in columns each has its place, so that they stand below each other; else they follow each other
			const order = slots ? slots.verbs.map(verb => tile.verbs.find(v => v.verb === verb) ?? { verb, absent: true }) : tile.verbs;

			for (const { verb, live, absent } of [...order].reverse()) {

				if (absent) {

					right -= AppMapPanels.labelWidth(renderer, verb, badge, 600) + badge * AppMapPanels.PILL_GAP;
					continue;
				}

				ctx.save();
				ctx.globalAlpha *= live ? 1 : 0.35;
				right -= this.drawLabel(renderer, verb, right, middle, badge, 'badge', (AppMapPanels.VERB_COLOR[verb] ?? AppMapPanels.RIGHT_COLOR[verb])[look.dark ? 1 : 0], k, 'right');
				ctx.restore();
			}

			room = right - start - em * 0.5;
		}

		// the name, and a dot when it must be set
		const name = AppMapPanels.cutText(renderer, tile.name, weight, size, room, mono);

		ctx.fillStyle = item.faded ? look.label : look.labelFocus;
		renderer.screenText(name, start, middle, weight, size, mono, 'left');

		if (tile.dot) {

			ctx.fillStyle = renderer.colorOf(n);
			ctx.beginPath();
			ctx.arc(start + renderer.textWidth(name, weight, mono) / AppMapRenderer.LABEL_SIZE * size + em * 0.4, middle, em * 0.16, 0, 2 * Math.PI);
			ctx.fill();
		}

		ctx.restore();

		// the box, where it stands on the map, also for a double click and a glide into it (see AppMapView.glideIntoTile)
		item.box = box;
		this.tiles.push({ n, item, x: box[0], y: box[1], width: box[2], height: box[3] });

		this.drawBox(renderer, item, n, box, k, alpha * faded);
	}

	// the box of a row: closed, a light square framed in its colour (that of the element it leads to, else that of the
	// element of the card) with its sign; as it comes closer (from TILE_SKETCH to TILE_OPEN pixels high) it opens in itself
	// to a card like every other, with its name in the header, below it its settings as the controls of an HTML element show
	// them, and at its foot the way out of the map, where there is one (see AppMapView.placeOpenButton)
	drawBox(renderer, item, n, box, k, alpha) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const tile   = item.tile;
		const tint   = this.tintOf(renderer, tile.tint);
		const colour = tint ?? renderer.colorOf(n);
		const [bx, y, square, full] = box;
		const change = Math.min(1, Math.max(0, (full * k - AppMapPanels.TILE_SKETCH) / (AppMapPanels.TILE_OPEN - AppMapPanels.TILE_SKETCH)));

		// the box opens from its square to the width all boxes share (see openWidth), to the right, or to the left at the
		// end of its row (see openLeft)
		const width  = square + (AppMapPanels.openWidth(item, full) - square) * change;
		const x      = AppMapPanels.openLeft(item, bx, square, width);

		// the box opens from its square to the height of what it shows: its header, its lines at their size (see below) and
		// its foot, so a box with few settings has no empty space at the bottom
		const lines  = AppMapPanels.boxLines(tile);
		const sized  = AppMapPanels.headSize(width, full, k);
		const pitch  = Math.min((full - sized.depth - sized.foot) / (lines + 0.6), AppMapPanels.TILE_TEXT * 2.1 / k);
		const height = full - Math.max(0, full - (sized.depth + pitch * (lines + 0.6) + sized.foot)) * change;
		const radius = Math.min(height * 0.22, AppMapPanels.corner(width, height, k));
		const shape  = new Path2D();

		shape.roundRect(x, y, width, height, radius);

		// closed, a box is the icon tile of the design system: a faint tint of its colour, or of the paper where it has none,
		// without a frame; one under the pointer stands out with a frame in its colour, see AppMapView.onPointerMove
		const lit = this.view.hoveredTile === item && change < 0.5;

		ctx.save();
		ctx.globalAlpha = alpha;
		ctx.fillStyle   = tint ? d3.color(tint).copy({ opacity: look.dark ? 0.2 : 0.16 }).formatRgb() : look.tint;
		ctx.fill(shape);

		if (lit) {

			ctx.strokeStyle = colour;
			ctx.lineWidth   = 1.5 / k;
			ctx.stroke(shape);
		}

		// closed: the sign, which gives way to the card
		if (change < 0.5) {

			ctx.globalAlpha = alpha * (1 - 2 * change);

			if (tile.badge) {

				// the method in its colour, as a badge
				ctx.fillStyle = this.inkOf(tint);
				renderer.screenText(tile.badge, x + width / 2, y + height / 2, 700, height * 0.4, false, 'center');

			} else if (tile.access) {

				// a permission: the matrix it opens to in small, public and signed-in users by the methods (see drawMatrix)
				const across = width / AppMapPanels.VERBS.length;

				tile.access.rows.slice(0, 2).forEach((row, j) => row.cells.forEach((state, i) => {
					this.drawCell(ctx, x + across * (i + 0.5), y + height * (0.32 + j * 0.36), height * 0.12, state, look, k);
				}));

			} else if (tile.arrow) {

				// the icon of a relationship in the Code area, else an arrow
				const ink = tint ? this.inkOf(tint) : look.label;

				if (!this.drawIcon(ctx, typeof _Icons === 'undefined' ? null : _Icons.iconSchemaRelationship, x + width / 2, y + height / 2, width * 0.5, ink)) {

					ctx.fillStyle = ink;
					renderer.screenText('→', x + width / 2, y + height / 2, 700, height * 0.58, false, 'center');
				}

			} else if (tile.glyph) {

				ctx.fillStyle = tint ? this.inkOf(tint) : look.label;
				renderer.screenText(tile.glyph, x + width / 2, y + height / 2, 700, height * (tile.glyph.length > 1 ? 0.42 : 0.55), false, 'center');

			} else {

				this.drawTypeSign(renderer, tile.sign, x + width / 2, y + height / 2, width, look.label);
			}

			this.drawOpenSign(renderer, x + width, y + height, height, k, colour, lit);

			ctx.restore();
			return;
		}

		// open: the card of the box, its header, and what it holds, each kind of row in its own way (see drawRelation,
		// drawField, drawSummary and drawMatrix)
		const head   = sized;
		const top    = y + head.depth;
		const bottom = y + height - head.foot;
		const left   = x + head.inset;
		const right  = x + width - head.inset;

		ctx.globalAlpha = alpha * (2 * change - 1);
		ctx.fillStyle   = look.card;
		ctx.fill(shape);
		ctx.strokeStyle = look.border;
		ctx.lineWidth   = 1 / k;
		ctx.stroke(shape);

		this.drawHeader(renderer, { x0: x, y0: y, width, height, ...head, line: top, band: { x, width, outline: shape } }, tile.access ? 'Permission' : (tile.kind ?? ''), tile.badge ? `${tile.badge} ${tile.name}` : tile.name, colour, k);

		if (tile.relation) {
			this.drawRelation(renderer, item, left, right, top, bottom, k);
		}

		if (tile.field) {
			this.drawField(renderer, item, n, left, right, top, bottom, k, change, colour);
		}

		if (tile.summary) {
			this.drawSummary(renderer, item, n, left, right, top, bottom, k, change, colour);
		}

		if (tile.access) {
			this.drawMatrix(renderer, item, n, left, right, top, bottom, k, change);
		}

		if (tile.open) {
			this.cards.push({ n, item, open: tile.open, rect: [x, y, width, height], foot: head.foot, opacity: 2 * change - 1 });
		}

		ctx.restore();
	}

	// the permission of a request in its open box (see matrixOf), in the style of the boxes of fields and relationships:
	// a strip with who may send the requests as a chip and for whom the permission is visible as marks; below a hairline
	// the matrix, as the policy editors of Supabase or Firebase show roles and actions, a column per method headed by its
	// badge, a row per audience with its sign, its name and in small grey text who it is, and a cell per method, with the
	// requests of the last days below the cells of public and signed-in users. The cells of public and signed-in users,
	// and of a grantee where not every signed-in user is allowed anyway, answer a click once the box stands open (see
	// AppMapView.flipAccess). Below a hairline what the colours mean where they are not plain and that admins may always
	// send every request, and with its usage the requests per day as a chart
	drawMatrix(renderer, item, n, left, right, top, bottom, k, change) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const tile   = item.tile;
		const access = tile.access;
		const g      = tile.grant ?? {};
		const step   = Math.min((bottom - top) / (access.lines + 0.6), AppMapPanels.TILE_TEXT * 2.1 / k) * AppMapPanels.ACCESS_LINE;
		// the text has the size of that of every other box (see drawBox), though its lines stand closer
		const font   = step / AppMapPanels.ACCESS_LINE / 2.1;
		const dark   = look.dark ? 1 : 0;
		const across = right - left;
		const names  = across * 0.4;
		const column = (across - names) / AppMapPanels.VERBS.length;
		const centre = i => left + names + column * (i + 0.5);
		const live   = !!tile.grant?.id && change >= 0.9;
		const rowH   = step * access.rowH;
		const red    = AppMapPanels.VERB_COLOR.DELETE[dark];
		const pill   = font * AppMapPanels.BOX_PILL;
		const body   = font * AppMapPanels.BOX_BODY;

		// the strip: who may send the requests, as the row of the request says it, and for whom the permission is visible
		const strip = top + step * 1.25;

		this.drawLabel(renderer, tile.chip ?? '', left, strip, pill, 'chip', this.tintOf(renderer, tile.chipTint) ?? look.label, k);

		const grantees = (g.grantees ?? []).length;
		const marks    = [['public', !!g.visibleToPublicUsers], ['signed in', !!g.visibleToAuthenticatedUsers], grantees > 0 && [`${grantees} ${grantees > 1 ? 'grantees' : 'grantee'}`, true]].filter(Boolean);
		let   x        = right;

		for (const [text, on] of [...marks].reverse()) {
			x -= this.drawLabel(renderer, text, x, strip, pill, on ? 'on' : 'off', AppMapPanels.ACCESS_CELL.some[1 + dark], k, 'right');
		}

		this.drawCaption(renderer, 'Visible to', x - pill * 0.3, strip, font, 'right');
		this.drawHairline(ctx, left, right, top + step * 2.4, k);

		// the methods above their columns, as badges in their colours
		let line = top + step * 3.2;

		AppMapPanels.VERBS.forEach(([verb], i) => this.drawLabel(renderer, verb, centre(i), line, font * 0.62, 'badge', AppMapPanels.VERB_COLOR[verb][dark], k, 'center'));

		line += step * 0.6 + rowH / 2;

		for (const row of access.rows) {

			this.drawAudienceSign(ctx, row.icon, left + font * 0.6, line - step * 0.3, font * 0.6, look.label);

			ctx.fillStyle = look.labelFocus;
			renderer.screenText(AppMapPanels.cutText(renderer, row.who, 600, font, names - font * 2), left + font * 1.6, line - step * 0.3, 600, font, false, 'left');
			ctx.fillStyle = look.label;
			renderer.screenText(AppMapPanels.cutText(renderer, row.about, 400, font * 0.72, names - font * 2), left + font * 1.6, line + step * 0.45, 400, font * 0.72, false, 'left');

			row.cells.forEach((state, i) => {

				const r = font * 0.5;

				this.drawCell(ctx, centre(i), line, r, state, look, k);

				if (live && state !== 'implied') {
					this.toggles.push({ rect: [centre(i) - r * 1.8, line - r * 1.8, r * 3.6, r * 3.6], item, n, key: row.kind, column: i, state, label: `${AppMapPanels.VERBS[i][0]} for ${row.who}` });
				}
			});

			// below the cells of public and signed-in users, how often they sent the method in the last days: allowed in
			// grey, denied after it in red; the grantees are among the signed-in users, the usage does not tell them apart
			if (access.use && (row.kind === 'pub' || row.kind === 'auth')) {

				AppMapPanels.VERBS.forEach(([verb], i) => {

					const use = access.use.flags[(row.kind === 'pub' ? 'NON_AUTH_USER_' : 'AUTH_USER_') + verb];

					if (!use) {
						return;
					}

					const allowed = AppMapPanels.shortCount(use.allowed);
					const denied  = use.denied ? ' / ' + AppMapPanels.shortCount(use.denied) : '';
					const size    = font * 0.62;
					const width   = renderer.textWidth(allowed + denied, 600) / AppMapRenderer.LABEL_SIZE * size;
					const x0      = centre(i) - width / 2;
					const y0      = line + step * 0.95;

					ctx.fillStyle = look.label;
					renderer.screenText(allowed, x0, y0, 600, size, false, 'left');

					if (denied) {

						ctx.fillStyle = red;
						renderer.screenText(denied, x0 + renderer.textWidth(allowed, 600) / AppMapRenderer.LABEL_SIZE * size, y0, 600, size, false, 'left');
					}
				});
			}

			line += rowH;
		}

		// a hairline, then what the colours mean where they are not plain, in one line, and that admins may always send
		// every request
		line -= rowH / 2;
		this.drawHairline(ctx, left, right, line + step * 0.4, k);
		line += step * 1.3;

		if (access.legend.length) {

			let lx = left;

			for (const state of access.legend) {

				const text = AppMapPanels.ACCESS_CELL[state][0];

				this.drawCell(ctx, lx + font * 0.4, line, font * 0.36, state, look, k);
				ctx.fillStyle = look.label;
				renderer.screenText(text, lx + font * 1.1, line, 400, body, false, 'left');
				lx += font * 1.1 + renderer.textWidth(text, 400) / AppMapRenderer.LABEL_SIZE * body + font * 1.2;
			}

			line += step * 1.1;
		}

		ctx.fillStyle = look.label;
		renderer.screenText('Admins may always send every request.', left, line, 400, body, false, 'left');

		// the usage of the last days: a heading with the sums on its right, then the requests per day, on their own scale
		// here (the rows of the card share one, see annotateUsage), with the first and the last day below
		if (access.use) {

			const use    = { ...access.use, peak: d3.max(access.use.series, d => d.allowed + d.denied) ?? 0 };
			const date   = day => new Date(day).toLocaleDateString(undefined, { day: 'numeric', month: 'short', timeZone: 'UTC' });
			const small  = font * AppMapPanels.CAPTION;

			this.drawHairline(ctx, left, right, line + step * 0.8, k);
			line += step * 1.7;

			this.drawCaption(renderer, `Requests · last ${use.days} ${use.days === 1 ? 'day' : 'days'}`, left, line, font);

			// the sums, from the right: the last request, the denied ones in red, the allowed ones
			const parts = [
				[`${use.allowed.toLocaleString()} allowed`, look.labelFocus],
				use.denied ? [`${use.denied.toLocaleString()} denied`, red] : null,
				use.last ? [`last ${new Date(use.last).toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}`, look.label] : null
			].filter(Boolean);

			let sx = right;

			for (const [text, colour] of [...parts].reverse()) {

				ctx.fillStyle = colour;
				renderer.screenText(text, sx, line, 600, pill, false, 'right');
				sx -= renderer.textWidth(text, 600) / AppMapRenderer.LABEL_SIZE * pill + pill;
			}

			const chartY = line + step * 0.8;
			const chartH = step * AppMapPanels.CHART_LINES;

			AppMapPanels.drawSeries(ctx, use, left, chartY, across, chartH, look.label, red, k);

			ctx.fillStyle = look.label;
			renderer.screenText(date(use.series[0].day), left, chartY + chartH + step * 0.6, 400, small, false, 'left');
			renderer.screenText(date(use.series[use.series.length - 1].day), right, chartY + chartH + step * 0.6, 400, small, false, 'right');

			if (use.peak) {
				renderer.screenText(`at most ${use.peak.toLocaleString()} a day`, left + across / 2, chartY + chartH + step * 0.6, 400, small, false, 'center');
			}
		}
	}

	// the relationship in the open box of its property (see relationOf), as the relationship dialog in the Code area draws
	// it, simpler: this type on the left and the other on the right, each as a chip in its colour with the property that
	// leads to the other below it; between them a line with an arrow in the direction of the relationship, its type above
	// it and how many objects there are at either end; below a hairline what the relationship does, one sentence a line, or
	// in grey that it does nothing beyond connecting
	drawRelation(renderer, item, left, right, top, bottom, k) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const rel    = item.tile.relation;
		const step   = Math.min((bottom - top) / (rel.lines + 0.6), AppMapPanels.TILE_TEXT * 2.1 / k);
		const font   = step / 2.1;
		const across = right - left;
		const middle = top + step * 1.3;
		const pill   = font * AppMapPanels.BOX_PILL;
		const body   = font * AppMapPanels.BOX_BODY;

		// the two types as chips in their colours, each at most a third of the width
		const chip = (node, x, align) => this.drawLabel(renderer, AppMapPanels.cutText(renderer, node.n, 600, pill, across * 0.3), x, middle, pill, 'chip', this.tintOf(renderer, node) ?? look.label, k, align) - pill * AppMapPanels.PILL_GAP;

		const a = left + chip(rel.here.node, left, 'left');
		const b = right - chip(rel.there.node, right, 'right');
		const gap   = font * 0.8;
		const x1    = a + gap;
		const x2    = b - gap;
		const head  = font * 0.55;

		// the line with the arrow in the direction of the relationship
		ctx.strokeStyle = look.label;
		ctx.fillStyle   = look.label;
		ctx.lineWidth   = Math.max(1 / k, font * 0.08);
		ctx.beginPath();
		ctx.moveTo(x1, middle);
		ctx.lineTo(x2, middle);
		ctx.stroke();

		const tip = rel.incoming ? x1 : x2;
		const dir = rel.incoming ? 1 : -1;

		ctx.beginPath();
		ctx.moveTo(tip, middle);
		ctx.lineTo(tip + dir * head * 1.4, middle - head * 0.7);
		ctx.lineTo(tip + dir * head * 1.4, middle + head * 0.7);
		ctx.closePath();
		ctx.fill();

		// the relationship type above the line, how many at either end below it
		ctx.fillStyle = look.labelFocus;
		renderer.screenText(AppMapPanels.cutText(renderer, rel.type, 600, font * 0.8, x2 - x1 - font * 2), (x1 + x2) / 2, middle - font * 0.85, 600, font * 0.8, false, 'center');

		ctx.fillStyle = look.label;

		if (rel.here.count) {
			renderer.screenText(rel.here.count, x1 + font * 0.2, middle + font * 0.75, 600, font * 0.75, false, 'left');
		}

		renderer.screenText(rel.there.count, x2 - font * 0.2, middle + font * 0.75, 600, font * 0.75, false, 'right');

		// below each type the property that leads to the other
		const names = middle + step * 1.15;

		ctx.fillStyle = look.labelFocus;
		renderer.screenText(AppMapPanels.cutText(renderer, rel.here.name, 500, body, across * 0.45), left, names, 500, body, false, 'left');
		if (rel.known) {

			ctx.fillStyle = rel.there.name ? look.labelFocus : look.label;
			renderer.screenText(AppMapPanels.cutText(renderer, rel.there.name ?? 'no property', 500, body, across * 0.45), right, names, 500, body, false, 'right');
		}

		// a hairline, then what the relationship does
		let line = top + step * 3.4;

		this.drawHairline(ctx, left, right, line - step * 0.45, k);

		const sentences = rel.quiet ? [...rel.facts, null] : rel.facts;

		for (const fact of sentences.length ? sentences : [null]) {

			ctx.fillStyle = fact ? look.labelFocus : look.label;
			const none = rel.known ? 'No cascading delete, no auto-create, passes no rights' : 'Its settings are in the Code area';

			renderer.screenText(AppMapPanels.cutText(renderer, fact ?? none, 400, body, across), left, line + step * 0.2, 400, body, false, 'left');
			line += step;
		}
	}

	// a field in the open box of its property (see fieldOf), in the style of a relationship (see drawRelation): its type as
	// a chip in the colour of its type, on the right its marks, filled where they are set and faint where not, which can be
	// switched where the field allows it once the box stands open; below a hairline what else applies, a label in small
	// capitals and its value, the code of a function in a fixed-width font and the values of an enum as chips in columns
	drawField(renderer, item, n, left, right, top, bottom, k, change, colour) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const tile   = item.tile;
		const field  = tile.field;
		const step   = Math.min((bottom - top) / (field.lines + 0.6), AppMapPanels.TILE_TEXT * 2.1 / k);
		const font   = step / 2.1;
		const body   = font * AppMapPanels.BOX_BODY;
		const pill   = font * AppMapPanels.BOX_PILL;

		// the type as a chip, the marks of the field on the right, those that can be switched answering a click
		this.drawChipAndMarks(renderer, item, n, left, right, top + step * 1.1, pill, field.type, colour, field.settings.map(([label], index) => ({ text: label.toLowerCase(), index })), colour, k, change);

		// a hairline, then what else applies: a caption and its value per line, the values of an enum as chips in columns,
		// the code of a function in a fixed-width font
		let line = top + step * 2.4;

		this.drawHairline(ctx, left, right, line - step * 0.45, k);

		const valueX = left + (right - left) * 0.22;

		if (field.values.length) {

			const columns = AppMapPanels.ENUM_COLUMNS;
			const column  = (right - valueX) / columns;

			this.drawCaption(renderer, 'Values', left, line + step * 0.2, font);

			field.values.forEach((value, i) => {

				this.drawLabel(renderer, AppMapPanels.cutText(renderer, value, 600, pill, column - pill * 2), valueX + (i % columns) * column, line + step * 0.2, pill, 'chip', look.label, k);

				if (i % columns === columns - 1 || i === field.values.length - 1) {
					line += step;
				}
			});
		}

		for (const [name, value, code] of field.facts) {

			this.drawCaption(renderer, name, left, line + step * 0.2, font);

			ctx.fillStyle = look.labelFocus;
			renderer.screenText(AppMapPanels.cutText(renderer, value, 400, body, right - valueX, !!code), valueX, line + step * 0.2, 400, body, !!code, 'left');
			line += step;
		}

		if (!field.facts.length && !field.values.length) {

			ctx.fillStyle = look.label;
			renderer.screenText('No default, no format', left, line + step * 0.2, 400, body, false, 'left');
			line += step;
		}

		// who reads a function property, below a hairline
		if (field.readers) {

			this.drawHairline(ctx, left, right, line - step * 0.3, k);
			this.drawCallers(renderer, field.readers, left, right, line + step * 0.35, pill, k, 'Read by', 'No readers found in the code of the application');
		}
	}

	// the lines the box of a row with a summary needs (see drawSummary): the chip and the marks take two, a sentence one
	// each, with room for the sentences that switching a mark can add
	static summaryLines(tile) {

		const most = tile.summary.marks.filter(m => m.index != null).length;

		return 2.4 + Math.max(1, tile.summary.sentences(tile).filter(Boolean).length + most);
	}

	// the box of a row of the card of a group, in the style of the box of a field (see drawField): what it stands for as a
	// chip in its colour, on the right its marks, filled where they are set and faint where not, those of a setting
	// switchable once the box stands open; below a hairline what that means, in sentences, the remarks in grey
	drawSummary(renderer, item, n, left, right, top, bottom, k, change, colour) {

		const ctx     = renderer.ctx;
		const look    = this.view.look;
		const tile    = item.tile;
		const summary = tile.summary;
		const step    = Math.min((bottom - top) / (AppMapPanels.summaryLines(tile) + 0.6), AppMapPanels.TILE_TEXT * 2.1 / k);
		const font    = step / 2.1;
		const body    = font * AppMapPanels.BOX_BODY;

		// what the row stands for and its marks, those of a setting answering a click
		this.drawChipAndMarks(renderer, item, n, left, right, top + step * 1.1, font * AppMapPanels.BOX_PILL, summary.chip, this.tintOf(renderer, summary.chipTint) ?? colour, summary.marks, colour, k, change);

		// a hairline, then the sentences, the remarks in grey, code in a fixed-width font
		let line = top + step * 2.4;

		this.drawHairline(ctx, left, right, line - step * 0.45, k);

		for (const sentence of summary.sentences(tile).filter(Boolean)) {

			const text  = typeof sentence === 'string' ? sentence : sentence.text;
			const muted = typeof sentence === 'object' && sentence.muted;
			const mono  = typeof sentence === 'object' && !!sentence.mono;

			ctx.fillStyle = muted ? look.label : look.labelFocus;
			renderer.screenText(AppMapPanels.cutText(renderer, text, 400, body, right - left, mono), left, line + step * 0.2, 400, body, mono, 'left');
			line += step;
		}
	}

	// the head of a box: what it stands for as a chip on the left, in the room its marks leave, and its marks on the right,
	// or on the left where it has no chip; a mark is { text, on, colour } or, for a setting of the row, { text, index },
	// which is on as its setting is and answers a click once the box stands open (see AppMapView.flip)
	drawChipAndMarks(renderer, item, n, left, right, y, size, chip, chipInk, marks, colour, k, change) {

		const tile  = item.tile;
		const wide  = d3.sum(marks, mark => AppMapPanels.labelWidth(renderer, mark.text, size) + size * AppMapPanels.PILL_GAP);
		const room  = right - left - wide - size;
		let   x     = chip ? right : left + wide - size * AppMapPanels.PILL_GAP;

		if (chip && room > size * 4) {
			this.drawLabel(renderer, AppMapPanels.cutText(renderer, chip, 600, size, room - size * AppMapPanels.PILL_PAD), left, y, size, 'chip', chipInk, k);
		}

		for (const mark of [...marks].reverse()) {

			const setting = mark.index != null ? tile.settings[mark.index] : null;
			const on      = setting ? !!setting[1] : !!mark.on;
			const ink     = mark.colour ? mark.colour[this.view.look.dark ? 1 : 0] : colour;
			const width   = AppMapPanels.labelWidth(renderer, mark.text, size);
			const tall    = size * AppMapPanels.PILL_HEIGHT;

			x -= this.drawLabel(renderer, mark.text, x, y, size, on ? 'on' : 'off', ink, k, 'right') - size * AppMapPanels.PILL_GAP;

			if (setting?.[3] && tile.edit && change >= 0.9) {
				this.toggles.push({ rect: [x, y - tall / 2, width, tall], item, n, index: mark.index, key: setting[3] });
			}

			x -= size * AppMapPanels.PILL_GAP;
		}
	}

	// ----- the parts every box and card is made of -----
	// the labels are the chips of the design system of the new UI: this many times the size of their text high, wider than
	// their text by PILL_PAD of it, PILL_GAP of it apart from the next one, and rounded by PILL_ROUND of their height
	static PILL_HEIGHT = 1.7;
	static PILL_PAD    = 1.3;
	static PILL_GAP    = 0.5;
	static PILL_ROUND  = 0.28;

	// a caption is this share of the size of the text it stands before
	static CAPTION = 0.85;

	// in a row of a card, its chip and its number are this large, its badges this large, in units of the size of its
	// text (em, see drawTile)
	static ROW_PILL  = 0.66;
	static ROW_BADGE = 0.55;

	// in a box, its pills and its text are this share of the size of its text (font, see drawBox)
	static BOX_PILL = 0.85;
	static BOX_BODY = 0.9;

	// how wide a label is, on the map (see drawLabel)
	static labelWidth(renderer, text, size, weight = 500) {

		return renderer.textWidth(text, weight) / AppMapRenderer.LABEL_SIZE * size + size * AppMapPanels.PILL_PAD;
	}

	// a colour as the text of a label or a mark: on paper darker than the colours of the aspects are, which are made for
	// strokes and rings and are too light to be read as small text; on the dark look as it is
	inkOf(colour) {

		return this.view.look.dark ? colour : d3.color(colour).darker(0.8).formatHex();
	}

	// a label of a box or a card, a chip of the design system (not to be confused with drawPill, the box of an HTML element
	// in its page); its colour lives in its border and its text, at most in a faint tint behind it, never in a fill: chip,
	// outlined in its colour, for what something is; on, tinted besides, for what is set or there; off, outlined and faint,
	// for what is not; badge, tinted and bold, for a method of HTTP or a right, and badge-off for one that is not allowed.
	// Stands at x with its middle at y, or ends at x where it is aligned right, or has its middle there where it is centred;
	// returns how far the next one stands
	drawLabel(renderer, text, x, y, size, style, ink, k, align = 'left') {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const badge  = style === 'badge' || style === 'badge-off';
		const weight = badge ? 600 : 500;
		const wide   = AppMapPanels.labelWidth(renderer, text, size, weight);
		const tall   = size * AppMapPanels.PILL_HEIGHT;
		const x0     = align === 'right' ? x - wide : (align === 'center' ? x - wide / 2 : x);
		const off    = style === 'off' || style === 'badge-off';

		ctx.save();
		ctx.beginPath();
		ctx.roundRect(x0, y - tall / 2, wide, tall, tall * AppMapPanels.PILL_ROUND);

		if (!off && style !== 'chip') {

			ctx.fillStyle = d3.color(ink).copy({ opacity: look.dark ? 0.2 : 0.14 }).formatRgb();
			ctx.fill();
		}

		ctx.strokeStyle = off ? look.border : d3.color(ink).copy({ opacity: 0.5 }).formatRgb();
		ctx.lineWidth   = 1 / k;
		ctx.stroke();

		ctx.fillStyle = off ? look.faint : this.inkOf(ink);
		renderer.screenText(text, x0 + wide / 2, y, weight, size, false, 'center');
		ctx.restore();

		return wide + size * AppMapPanels.PILL_GAP;
	}

	// how much room a caption takes, with the space before what follows it
	static captionWidth(renderer, text, size) {

		return renderer.textWidth(text, 500) / AppMapRenderer.LABEL_SIZE * size * AppMapPanels.CAPTION + size * 0.8;
	}

	// a caption, quiet and grey as the meta lines of the design system, before a line of labels or a value; returns how far
	// what follows it stands
	drawCaption(renderer, text, x, y, size, align = 'left') {

		renderer.ctx.fillStyle = this.view.look.faint;
		renderer.screenText(text, x, y, 500, size * AppMapPanels.CAPTION, false, align);

		return AppMapPanels.captionWidth(renderer, text, size);
	}

	// the fine line below a header or a strip and between the parts of a box
	drawHairline(ctx, left, right, y, k) {

		ctx.fillStyle = this.view.look.hair;
		ctx.fillRect(left, y, right - left, 1 / k);
	}

	// a cell of the matrix of a permission: a disc in the colour of its state, with a tick where it is allowed, a ring with a
	// dot where only the grantees are, a ring where it is set without effect, a faint ring where it is not allowed
	drawCell(ctx, cx, cy, r, state, look, k) {

		const [, light, dark] = AppMapPanels.ACCESS_CELL[state];
		const tone            = look.dark ? dark : light;

		ctx.save();
		ctx.beginPath();
		ctx.arc(cx, cy, r, 0, 2 * Math.PI);

		if (state === 'yes' || state === 'implied') {

			ctx.fillStyle = state === 'implied' ? d3.color(tone).copy({ opacity: 0.55 }).formatRgb() : tone;
			ctx.fill();

			ctx.strokeStyle = look.card;
			ctx.lineWidth   = r * 0.28;
			ctx.lineCap     = 'round';
			ctx.lineJoin    = 'round';
			ctx.beginPath();
			ctx.moveTo(cx - r * 0.42, cy + r * 0.02);
			ctx.lineTo(cx - r * 0.1, cy + r * 0.32);
			ctx.lineTo(cx + r * 0.45, cy - r * 0.3);
			ctx.stroke();

		} else {

			ctx.strokeStyle = state === 'no' ? d3.color(tone).copy({ opacity: 0.7 }).formatRgb() : tone;
			ctx.lineWidth   = state === 'no' ? Math.max(1 / k, r * 0.12) : r * 0.24;
			ctx.stroke();

			if (state === 'some') {

				ctx.beginPath();
				ctx.arc(cx, cy, r * 0.38, 0, 2 * Math.PI);
				ctx.fillStyle = tone;
				ctx.fill();
			}
		}

		ctx.restore();
	}

	// the sign of an audience: the world for public users, a person for signed-in users and a user, two for a group
	drawAudienceSign(ctx, icon, cx, cy, r, colour) {

		const person = (px, py, size) => {

			ctx.beginPath();
			ctx.arc(px, py - size * 0.45, size * 0.38, 0, 2 * Math.PI);
			ctx.fill();
			ctx.beginPath();
			ctx.ellipse(px, py + size * 0.55, size * 0.68, size * 0.48, 0, Math.PI, 2 * Math.PI);
			ctx.fill();
		};

		ctx.save();
		ctx.fillStyle   = colour;
		ctx.strokeStyle = colour;
		ctx.lineWidth   = r * 0.16;

		if (icon === 'public') {

			ctx.beginPath();
			ctx.arc(cx, cy, r * 0.85, 0, 2 * Math.PI);
			ctx.moveTo(cx - r * 0.85, cy);
			ctx.lineTo(cx + r * 0.85, cy);
			ctx.stroke();
			ctx.beginPath();
			ctx.ellipse(cx, cy, r * 0.38, r * 0.85, 0, 0, 2 * Math.PI);
			ctx.stroke();

		} else if (icon === 'group') {

			ctx.globalAlpha *= 0.55;
			person(cx + r * 0.42, cy - r * 0.05, r * 0.75);
			ctx.globalAlpha /= 0.55;
			person(cx - r * 0.25, cy + r * 0.05, r * 0.85);

		} else {

			person(cx, cy, r * 0.95);
		}

		ctx.restore();
	}

	// the magnifier with a plus that says there is more inside, as on the boxes of HTML elements (see drawZoomSign): small
	// and faint at the lower right corner of a box, once the box is large enough on the screen to carry it; on a disc in the
	// colour of the box while the pointer is on it
	drawOpenSign(renderer, cx, cy, size, k, colour, lit) {

		const ctx    = renderer.ctx;
		const radius = size * (lit ? 0.3 : 0.2);

		if (size * k < AppMapPanels.OPEN_SIGN) {
			return;
		}

		ctx.save();

		if (lit) {

			ctx.beginPath();
			ctx.arc(cx, cy, radius, 0, 2 * Math.PI);
			ctx.fillStyle = colour;
			ctx.fill();

		} else {

			ctx.globalAlpha *= 0.6;
		}

		// drawZoomSign puts the glass a little up and to the left of the point it is given, as much as the handle reaches
		// down and to the right, so the sign as a whole stands in the middle of the disc; small enough to keep clear of its rim
		this.drawZoomSign(ctx, cx, cy, radius * 1.5, lit ? this.view.look.card : colour);
		ctx.restore();
	}

	// the sign of the type of a field, as databases show it: Aa for text, # for numbers, a tick for yes or no, a calendar
	// for dates, ƒ for what is computed, ≡ for a choice; a dot for anything else
	// the icon of the Code area for the type of a property, as the map names it (String[] for an array of strings), see
	// _Icons.getIconForSchemaPropertyType; null outside the Admin UI
	static propertyIcon(type) {

		if (typeof _Icons === 'undefined') {
			return null;
		}

		const name = String(type);

		return _Icons.getIconForSchemaPropertyType(name.endsWith('[]') ? name.slice(0, -2) + 'Array' : name);
	}

	// draws an icon of the Admin UI (a symbol of icon/sprites.svg, which the Admin UI has in its document) centred on a
	// point, as large as given and in the colour given; says whether it could. The symbol becomes an image once per icon
	// and colour, and the map is drawn again when it has loaded; while it loads the place stays empty, so that the sign it
	// stands for does not flash up
	drawIcon(ctx, id, cx, cy, size, ink) {

		const image = id ? AppMapPanels.iconImage(id, ink, () => this.view.requestDraw()) : null;

		if (image) {
			ctx.drawImage(image, cx - size / 2, cy - size / 2, size, size);
		}

		return image !== null;
	}

	static icons = new Map();

	// the image of a symbol in a colour, undefined while it loads, null where there is no such symbol
	static iconImage(id, ink, loaded) {

		const key = `${id} ${ink}`;
		let entry = AppMapPanels.icons.get(key);

		if (!entry) {

			const symbol = typeof document.getElementById === 'function' ? document.getElementById(id) : null;

			if (!symbol || typeof Image === 'undefined') {
				return null;
			}

			// the symbol draws with currentColor, which the colour of the image gives; it is drawn large, so that it stays
			// sharp on a tile close up
			const attributes = [...symbol.attributes].filter(a => a.name !== 'id' && a.name !== 'viewBox').map(a => `${a.name}="${a.value}"`).join(' ');
			const svg        = `<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="${symbol.getAttribute('viewBox') ?? '0 0 24 24'}" color="${ink}"><g ${attributes}>${symbol.innerHTML}</g></svg>`;
			const image      = new Image();

			entry = { image, ready: false };
			image.onload = () => {

				entry.ready = true;
				loaded();
			};
			image.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);

			AppMapPanels.icons.set(key, entry);
		}

		return entry.ready ? entry.image : undefined;
	}

	drawTypeSign(renderer, type, cx, cy, size, ink) {

		const ctx  = renderer.ctx;
		const sign = AppMapPanels.TYPE_SIGNS[String(type).replace(/\[\]$/, '')] ?? '•';

		// the icon the Code area shows for the type of the property, where the Admin UI has its icons
		if (this.drawIcon(ctx, AppMapPanels.propertyIcon(type), cx, cy, size * 0.5, ink)) {
			return;
		}

		ctx.save();
		ctx.fillStyle   = ink;
		ctx.strokeStyle = ink;

		if (sign === 'date') {

			// a calendar: a frame with a bar on top
			const w = size * 0.5;

			ctx.lineWidth = size * 0.07;
			ctx.beginPath();
			ctx.roundRect(cx - w / 2, cy - w / 2 + size * 0.03, w, w * 0.9, size * 0.06);
			ctx.stroke();
			ctx.fillRect(cx - w / 2, cy - w / 2 + size * 0.03, w, w * 0.25);

		} else {

			renderer.screenText(sign, cx, cy, 700, size * (sign.length > 1 ? 0.42 : 0.55), false, 'center');
		}

		ctx.restore();
	}

	// whether a relation is drawn from the row of an open type instead of between the middles of its elements: a schema
	// relationship of a type whose card can be read
	withinOpenType(l) {

		return l.kind === 'relates' && (this.inspecting.has(l.source) || this.inspecting.has(l.target));
	}

	// the schema relationships of the open types, each from the row of the property that holds it, at the side of the card
	// that faces the type on the other end, with a dot there; to that type's own row when it is open too, else to its
	// middle. Rows and relationships pair up by the type on the other end, in order, as the schema gives both
	drawGates(renderer, k, gates) {

		const ctx  = renderer.ctx;
		const look = this.view.look;
		const done = new Set();

		const gateOf = (n, other) => {

			const list  = gates.get(n);
			const found = list?.find(g => g.row.target === other && !g.used);

			if (!found) {
				return null;
			}

			found.used = true;

			const { panel } = found;
			const right     = other.x >= n.x;

			return { x: right ? panel.x0 + panel.width : panel.x0, y: found.y, out: right ? 1 : -1, colour: renderer.colorOf(other) };
		};

		ctx.save();
		ctx.lineWidth   = 1.2 / k;
		ctx.strokeStyle = look.trace;
		ctx.fillStyle   = look.trace;
		ctx.globalAlpha = 0.8;

		for (const n of gates.keys()) {

			for (const l of this.view.links) {

				if (l.kind !== 'relates' || done.has(l) || (l.source !== n && l.target !== n) || l.source === l.target) {
					continue;
				}

				done.add(l);

				const other = l.source === n ? l.target : l.source;
				const from  = gateOf(n, other);

				if (!from) {
					continue;
				}

				const to    = gates.has(other) ? gateOf(other, n) : null;
				const end   = to ?? { x: other.x, y: other.y, out: 0 };
				const reach = Math.max(Math.abs(end.x - from.x) * 0.5, 30 / k);

				ctx.beginPath();
				ctx.moveTo(from.x, from.y);
				ctx.bezierCurveTo(from.x + from.out * reach, from.y, end.x + end.out * reach, end.y, end.x, end.y);
				ctx.stroke();

				// a dot in the colour of the type on the other end, set off from the card by a rim
				for (const p of to ? [from, to] : [from]) {

					ctx.beginPath();
					ctx.arc(p.x, p.y, 3.2 / k, 0, 2 * Math.PI);
					ctx.fillStyle   = p.colour;
					ctx.strokeStyle = look.card;
					ctx.lineWidth   = 1.5 / k;
					ctx.fill();
					ctx.stroke();
					ctx.strokeStyle = look.trace;
					ctx.lineWidth   = 1.2 / k;
				}
			}
		}

		ctx.restore();
	}

	draw(renderer, k) {

		this.tiles   = [];
		this.cards   = [];
		this.toggles = [];
		this.kinds   = [];

		const codePanels = this.codePanels(renderer, k);
		const insidePanels = this.insidePanels(renderer, k);
		const panels     = [...this.trees].map(([page, tree]) => [page, tree, this.onScreen(renderer, page, k, 2000) ? this.pagePanel(page, tree, k) : null]);
		const near       = Math.max(
			d3.max(panels, ([, , panel]) => panel ? Math.min(1, Math.max(0, (panel.row - AppMapPanels.SKETCH_ROW) / (AppMapPanels.OPEN_ROW - AppMapPanels.SKETCH_ROW))) : 0) ?? 0,
			d3.max(codePanels, c => c.strong) ?? 0,
			d3.max(insidePanels, c => c.strong) ?? 0
		);

		// the closer a page comes, the further the map around it steps back behind a veil, so that its card stands apart
		// from the elements and lines around it
		if (near > 0) {

			const ctx = renderer.ctx;

			ctx.save();
			ctx.setTransform(1, 0, 0, 1, 0, 0);
			ctx.globalAlpha = 0.75 * near;
			ctx.fillStyle   = this.view.look.veil;
			ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
			ctx.restore();
		}

		this.drawCode(renderer, k, codePanels);
		this.drawInsides(renderer, k, insidePanels);

		for (const [page, tree, panel] of panels) {

			// open only when the boxes can be read
			tree.open = !!panel && panel.row >= AppMapPanels.OPEN_ROW;

			if (tree.open) {

				this.drawPanel(renderer, page, tree, panel, k);

			} else if (panel && panel.row >= AppMapPanels.SKETCH_ROW) {

				this.drawSketch(renderer, page, tree, panel, k, (panel.row - AppMapPanels.SKETCH_ROW) / (AppMapPanels.OPEN_ROW - AppMapPanels.SKETCH_ROW));
			}
		}
	}

	// the strip of a page is this many times as high as its header
	static PAGE_STRIP = 1.9;

	// what the strip of a page shows (see drawPageStrip), once per page: what it is, where it is reached, who may see it;
	// how many types it reads, creates, changes and deletes, how many methods its elements call, how many pages it links
	// to and how many link to it, as the relations of the map have them
	pageStripOf(page) {

		const out   = kind => new Set(this.view.links.filter(l => l.source === page && l.kind === kind).map(l => l.target)).size;
		const into  = kind => new Set(this.view.links.filter(l => l.target === page && l.kind === kind && l.source !== page).map(l => l.source)).size;
		const route = (page.r ?? []).map(r => r.p).filter(Boolean);
		const acc   = page.acc ?? null;

		return {
			kind:    page.k === 'component' ? AppMapModel.KIND_LABEL.component : (page.err ? `Error page ${page.err}` : AppMapModel.KIND_LABEL.page),
			route:   page.k === 'component' ? null : (route[0] ?? `/${page.n}`) + (route.length > 1 ? ` +${route.length - 1}` : ''),
			visible: page.k === 'component' ? [] : [['public', !!(acc?.pub ?? page.p)], ['signed in', !!(acc?.auth ?? page.p)]],
			data:    AppMapPanels.TYPE_USES.map(([key, , verb], i) => ({ label: `${['reads', 'creates', 'changes', 'deletes'][i]} ${out(key)} ${out(key) === 1 ? 'type' : 'types'}`, verb, count: out(key) })),
			more:    [
				out('invokes') && `calls ${out('invokes')} ${out('invokes') === 1 ? 'method' : 'methods'}`,
				out('navigates') && `links to ${out('navigates')}`,
				into('navigates') && `linked from ${into('navigates')}`
			].filter(Boolean),
			// a shared component says first where it is used, the pages and components that include it
			usedOn:  page.k === 'component' ? new Set(this.view.links.filter(l => l.target === page && (l.kind === 'uses' || l.kind === 'include') && l.source !== page).map(l => l.source)).size : null
		};
	}

	// the strip of a page below the header of its sheet, in the style of that of a type (see drawTypeStrip): on the left
	// what it is and its route, on the right who may see it and how often it was viewed in the last days (see
	// AppMapView.describePageViews); below, after USES, how many types it reads, creates, changes and deletes, as pills in
	// the colours of the methods of HTTP, faint where there are none, and in grey what else it reaches; a hairline below
	drawPageStrip(renderer, page, tree, panel, colour, k) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const head   = panel.head;
		const strip  = tree.strip ??= this.pageStripOf(page);
		const dark   = look.dark ? 1 : 0;
		// in line with the name in the header
		const left   = head.x0 + head.inset;
		const right  = head.x0 + head.width - head.inset;
		const first  = head.line + panel.strip * 0.3;
		const second = head.line + panel.strip * 0.64;
		const font   = head.name * 0.62;
		const width  = text => AppMapPanels.labelWidth(renderer, text, font) + font * AppMapPanels.PILL_GAP;
		const textW  = text => renderer.textWidth(text, 600) / AppMapRenderer.LABEL_SIZE * font + font;

		// what it is and its route
		let x = left;

		x += this.drawLabel(renderer, strip.kind, x, first, font, 'chip', colour, k);

		if (strip.route) {
			x += this.drawLabel(renderer, strip.route, x, first, font, 'chip', look.label, k);
		}

		// who may see it, and its views, as far as there is room beside what it is: on a narrow sheet the caption goes
		// first, then the views; a shared component is never viewed by itself
		const views   = page.k === 'component' ? null : this.view.usage.pages?.data;
		const total   = views ? (views.usage?.[page.id]?.total ?? 0) : null;
		const viewed  = views ? `${AppMapPanels.shortCount(total)} ${total === 1 ? 'view' : 'views'} in ${views.days} days` : null;
		const pills   = d3.sum(strip.visible, ([text]) => width(text));
		const caption = strip.visible.length ? AppMapPanels.captionWidth(renderer, 'Visible to', font) : 0;
		const room    = right - x - font;
		const showsViews   = viewed && pills + textW(viewed) <= room;
		const showsCaption = caption && pills + caption + (showsViews ? textW(viewed) : 0) <= room;
		// on a sheet wider than the screen they follow what it is, else they would stand far beyond the screen
		const inline       = (right - left) * k > this.view.width * 0.9;
		let   r            = inline ? Math.min(right, x + font + caption + pills + (showsViews ? textW(viewed) : 0)) : right;

		this.view.usageOf('pages');

		if (showsViews) {

			ctx.fillStyle = look.label;
			renderer.screenText(viewed, r, first, 600, font, false, 'right');
			r -= textW(viewed);
		}

		if (pills <= room) {

			for (const [text, on] of [...strip.visible].reverse()) {
				r -= this.drawLabel(renderer, text, r, first, font, on ? 'on' : 'off', AppMapPanels.ACCESS_CELL.some[1 + dark], k, 'right');
			}
		}

		if (showsCaption) {
			this.drawCaption(renderer, 'Visible to', r - font * 0.3, first, font, 'right');
		}

		// what it uses, as many pills as the line holds, and how many more there are; a shared component leads with where it
		// is used, and shows its data only where it has any
		const component = strip.usedOn != null;
		const data      = component ? strip.data.filter(({ count }) => count) : strip.data;
		const uses      = [
			...(component ? [[strip.usedOn ? `${strip.usedOn} ${strip.usedOn === 1 ? 'page' : 'pages'}` : 'no page', strip.usedOn ? 'on' : 'off', look.kinds.component]] : []),
			...data.map(({ label, verb, count }) => [label, count ? 'on' : 'off', AppMapPanels.VERB_COLOR[verb][dark]]),
			...strip.more.map(text => [text, 'chip', look.label])
		];

		x = left + this.drawCaption(renderer, component ? 'Used on' : 'Uses', left, second, font);

		let full = false;

		uses.forEach(([text, style, ink], i) => {

			const rest = uses.length - i;

			if (full || x + width(text) + (rest > 1 ? width(`+${rest - 1}`) : 0) > right) {

				if (!full) {
					this.drawLabel(renderer, `+${rest}`, x, second, font, 'off', null, k);
				}

				full = true;
				return;
			}

			x += this.drawLabel(renderer, text, x, second, font, style, ink, k);
		});

		this.drawHairline(ctx, left, right, head.line + panel.strip * 0.9, k);
	}

	// a page close enough opens inside itself: the tree from top to bottom, with curved branches, every element a
	// labelled box, repeaters with their data and stacked copies, actions in orange
	drawPanel(renderer, page, tree, panel, k) {

		const ctx     = renderer.ctx;
		const look    = this.view.look;
		const hovered = this.view.hoveredDom ?? this.view.pinnedDom;
		const s       = panel.scale;
		const boxH    = AppMapPanels.BOX * s;
		const colour  = renderer.colorOf(page);
		const widthOf = d => AppMapPanels.boxWidth(d, s);

		ctx.save();
		ctx.globalAlpha = 1;

		this.drawFrame(ctx, page, panel, colour, k, 1);
		this.drawHeader(renderer, panel.head, AppMapModel.KIND_LABEL[page.k], page.n, colour, k);
		this.drawPageStrip(renderer, page, tree, panel, colour, k);

		// lines grow with the tree, up to a few pixels, so close up they do not become bars; light, so that the relations
		// drawn over them stand out
		ctx.globalAlpha = 0.4;
		ctx.strokeStyle = look.trace;
		ctx.lineWidth   = Math.min(Math.max(0.7 / k, 0.1 * s), 2.5 / k);
		ctx.stroke(AppMapPanels.branches(tree, panel));
		ctx.globalAlpha = 1;

		// the relations leave from the middle of their elements, so they go first and the elements cover where they start
		this.drawRelations(renderer, page, tree, panel, k);

		// the page itself: a document at the root
		const [rx, ry] = AppMapPanels.box(tree.root, panel);
		const visible  = renderer.visibleRect();

		ctx.fillStyle   = look.card;
		ctx.strokeStyle = colour;
		ctx.lineWidth   = Math.min(Math.max(1 / k, 0.15 * s), 3 / k);
		ctx.beginPath();
		ctx.moveTo(rx, ry - 2.4 * s);
		ctx.lineTo(rx + 2.8 * s, ry - 2.4 * s);
		ctx.lineTo(rx + 3.8 * s, ry - 1.4 * s);
		ctx.lineTo(rx + 3.8 * s, ry + 2.4 * s);
		ctx.lineTo(rx, ry + 2.4 * s);
		ctx.closePath();
		ctx.fill();
		ctx.stroke();

		for (const d of tree.list) {

			const [x, y] = AppMapPanels.box(d, panel);
			const w      = widthOf(d);
			const lit    = d === hovered;
			const r      = AppMapPanels.boxHeight(d) * s / 2;

			// closer up the element opens inside its box, see drawElement: the box stays, its content changes
			const inView  = x <= visible[2] && x + w >= visible[0] && y + r >= visible[1] && y - r <= visible[3];
			const opening = inView && boxH * k >= AppMapPanels.ELEMENT_SKETCH;
			const panelOf = opening ? this.elementPanel(d, k) : null;
			const change  = panelOf ? Math.min(1, Math.max(0, panelOf.fit / AppMapPanels.ELEMENT_OPEN)) : 0;

			d.panelHit = [x, y - r, w, 2 * r];
			d.opened   = null;

			this.drawPill(renderer, d, x, y, w, s, k, lit && !opening, change);

			if (opening) {
				this.drawElement(renderer, d, k, panelOf, change);
			}

			// on top of everything, also of the folded corner of a sheet and of the header of an open element
			this.drawProblemMark(renderer.ctx, d, x, y - r, w, s, k);
		}

		tree.frame = [page.x - panel.hw, page.y - panel.hh, 2 * panel.hw, 2 * panel.hh];
		ctx.restore();
	}

	// the box of an HTML element: a plain element a white box with a fine grey edge, one that does something with an edge and
	// what is wrong with an HTML element: the scripting errors it caused while rendering, which the Pages area keeps (see
	// _Pages.problems), and what cannot work in its repeater (see buildTrees)
	static problemsOf(d) {

		const errors = (typeof _Pages === 'undefined' || !d.data.id) ? [] : _Pages.problems.getRuntimeProblems({ id: d.data.id });

		return { errors, warnings: d.warnings ?? [] };
	}

	/**
	 * How many HTML elements of a page or shared component have problems: { errors, warnings }. Those of its tree, and for
	 * a page also those below the depth of its outline, which the Pages area finds by their page (see
	 * _Pages.problems.erroneousNodes), and the page itself. Counted anew when the scripting errors were loaded anew.
	 */
	problemsOfPage(n) {

		const runtime = typeof _Pages === 'undefined' ? null : _Pages.problems.runtimeErrors;

		if (this.countedFor !== runtime) {

			this.counted    = new Map();
			this.countedFor = runtime;
		}

		let result = this.counted.get(n);

		if (!result) {

			const list   = this.trees.get(n)?.list ?? [];
			const errors = new Set(list.filter(d => d.data.id && runtime?.has(d.data.id)).map(d => d.data.id));

			if (runtime && n.k === 'page') {

				for (const [id, node] of _Pages.problems.erroneousNodes) {

					if (node.pageId === n.id) {
						errors.add(id);
					}
				}
			}

			result = { errors: errors.size, warnings: list.filter(d => d.warnings?.length).length };

			this.counted.set(n, result);
		}

		return result;
	}

	// a mark at the top right corner of the box of an HTML element that has problems: red for scripting errors, else amber
	drawProblemMark(ctx, d, x, top, w, s, k) {

		const { errors, warnings } = AppMapPanels.problemsOf(d);

		if (errors.length || warnings.length) {

			// it keeps the smallest size of the access badge, so that it stays visible from afar, and grows with the box only
			// up to a few pixels on screen, beyond that much more slowly, so that it does not cover the controls of an open
			// element
			const onScreen = Math.max(AppMapRenderer.ACCESS_SMALL / 2, 0.55 * s * k);
			const radius   = (onScreen > 10 ? 10 * Math.pow(onScreen / 10, 0.3) : onScreen) / k;

			this.drawProblemSign(ctx, x + w - 0.15 * s, top + 0.15 * s, radius, k, errors.length > 0);
		}
	}

	// a filled circle in red or amber with an exclamation mark, once it can be told apart, and a rim in the colour of cards
	drawProblemSign(ctx, cx, cy, radius, k, error) {

		const look = this.view.look;
		const s    = radius / 0.55;

		ctx.save();
		ctx.beginPath();
		ctx.arc(cx, cy, radius, 0, 2 * Math.PI);
		ctx.fillStyle   = error ? look.error : look.warning;
		ctx.fill();
		ctx.lineWidth   = Math.min(Math.max(0.8 / k, 0.12 * s), 2 / k);
		ctx.strokeStyle = look.card;
		ctx.stroke();

		// an exclamation mark, once it can be told apart
		if (radius * k > 4) {

			ctx.strokeStyle = look.card;
			ctx.lineWidth   = 0.14 * s;
			ctx.lineCap     = 'round';
			ctx.beginPath();
			ctx.moveTo(cx, cy - 0.28 * s);
			ctx.lineTo(cx, cy + 0.06 * s);
			ctx.stroke();

			ctx.fillStyle = look.card;
			ctx.beginPath();
			ctx.arc(cx, cy + 0.26 * s, 0.08 * s, 0, 2 * Math.PI);
			ctx.fill();
		}

		ctx.restore();
	}

	// as a chip of the design system: an element that does something (an action, a repeater, a toggle) tinted in the colour
	// of what it does and outlined in it, a plain one white with a fine border; tinted more strongly when it is pointed at or
	// held, and white as it opens to a card; a repeater on a stack of the copies it makes. Inside, the sign of what it does, its tag and a magnifier when it holds more; as the element opens
	// (change from 0 to 1), they give way to its header and controls, see drawElement
	drawPill(renderer, d, x, y, w, s, k, lit, change) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const tone   = this.toneOf(d);
		const plain  = !d.data.action && !d.data.repeater && !d.data.active;
		const height = AppMapPanels.boxHeight(d) * s;
		const r      = height / 2;
		const sheet  = AppMapPanels.isSheet(d);

		ctx.save();
		ctx.lineWidth = Math.min(Math.max(0.8 / k, 0.12 * s), 2 / k);

		if (d.data.repeater) {

			for (const o of [0.4, 0.2]) {

				const copy = AppMapPanels.outline(d, x + o * s, y - r + o * s, w, height, s);

				ctx.fillStyle   = look.card;
				ctx.fill(copy);
				ctx.strokeStyle = d3.color(tone).copy({ opacity: 0.35 }).formatRgb();
				ctx.stroke(copy);
			}
		}

		// what the global search found lies on a mark, as on the map
		if (this.view.found && (d.data.inner ?? []).some(m => this.view.found.inner.has(m))) {

			ctx.fillStyle = look.found;
			ctx.fill(AppMapPanels.outline(d, x, y - r, w, height, s, 0.6 * s));
		}

		const shape = AppMapPanels.outline(d, x, y - r, w, height, s);

		const closed = Math.max(0, 1 - 2 * change);
		const tint   = lit ? 0.28 : (plain ? 0 : 0.14);

		ctx.fillStyle = look.card;
		ctx.fill(shape);

		if (tint) {

			ctx.fillStyle = d3.color(tone).copy({ opacity: tint * closed }).formatRgb();
			ctx.fill(shape);
		}

		ctx.strokeStyle = (plain && !lit) || !closed ? look.border : tone;
		ctx.stroke(shape);

		// the folded corner of a sheet
		if (sheet) {

			const fold = AppMapPanels.foldOf(w);

			ctx.beginPath();
			ctx.moveTo(x + w - fold, y - r);
			ctx.lineTo(x + w - fold, y - r + fold);
			ctx.lineTo(x + w, y - r + fold);
			ctx.stroke();
		}

		if (change < 0.5 && sheet) {

			this.drawSheetSketch(renderer, d, x, y - r, w, height, s, 1 - 2 * change);

		} else if (change < 0.5) {

			const ink  = this.inkOf(tone);
			const sign = d.data.action || d.data.repeater;
			const left = x + (sign ? 2 : 0.8) * s;
			const fit  = AppMapPanels.fitText(renderer, d.data.tag, plain ? 500 : 600, 1.05 * s, x + w - 0.8 * s - left);

			ctx.globalAlpha = 1 - 2 * change;

			if (d.data.action) {
				this.drawFlash(ctx, x + 1.1 * s, y, s, ink);
			} else if (d.data.repeater) {
				this.drawDatabase(ctx, x + 1.1 * s, y, s * 0.8, ink);
			}

			ctx.fillStyle = plain ? look.labelNear : look.labelFocus;
			renderer.screenText(fit.text, left, y, plain ? 500 : 600, fit.size, false, 'left');

			// a magnifier says that it holds more, closer up
			if (AppMapPanels.hasInside(d)) {
				this.drawZoomSign(ctx, x + w - 1.05 * s, y, s, d3.color(look.trace).copy({ opacity: 0.85 }).formatRgb());
			}
		}

		ctx.restore();
	}

	// a closed template: its tag at the top as the sheet of a page has its name, lines of text below it, and the magnifier
	// at the bottom
	drawSheetSketch(renderer, d, x, top, w, height, s, alpha) {

		const ctx  = renderer.ctx;
		const look = this.view.look;
		const left = x + 0.8 * s;
		const fit  = AppMapPanels.fitText(renderer, d.data.tag, 600, 1.05 * s, w - 1.6 * s - AppMapPanels.foldOf(w));
		const ink  = d3.color(look.trace).copy({ opacity: 0.5 }).formatRgb();

		ctx.save();
		ctx.globalAlpha = alpha;
		ctx.fillStyle   = look.labelFocus;
		renderer.screenText(fit.text, left, top + 1.3 * s, 600, fit.size, false, 'left');

		ctx.strokeStyle = ink;
		ctx.lineWidth   = 0.22 * s;
		ctx.lineCap     = 'round';
		ctx.beginPath();

		const lines = [0.85, 0.6, 0.75, 0.45, 0.7, 0.55];

		for (let i = 0; i < lines.length; i++) {

			const y = top + 2.7 * s + i * 0.75 * s;

			if (y > top + height - 1.6 * s) {
				break;
			}

			ctx.moveTo(left, y);
			ctx.lineTo(left + (w - 1.6 * s) * lines[i], y);
		}

		ctx.stroke();
		this.drawZoomSign(ctx, x + w - 1.05 * s, top + height - 0.9 * s, s, d3.color(look.trace).copy({ opacity: 0.85 }).formatRgb());
		ctx.restore();
	}

	// an HTML element opened inside its box, as a page opens inside its sheet: its kind and its name, and below them the
	// room for its controls, which the view places there (see AppMapView.followElement); on the way there, a sketch of them
	drawElement(renderer, d, k, panel, change) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const tone   = this.toneOf(d);
		const colour = tone === look.trace ? look.kinds.dom : tone;
		const area   = panel.area;

		if (change < 0.5) {
			return;
		}

		ctx.save();

		const kind = AppMapPanels.elementKind(d);

		ctx.globalAlpha = Math.min(1, (change - 0.5) * 2);
		this.drawHeader(renderer, panel.head, kind, `${AppMapPanels.nameOf(d)}${d.data.key ? ' ' + d.data.key : ''}`, colour, k);
		this.drawElementStrip(renderer, d, panel, colour, k);

		// the controls come with the header, growing and fading in with it, and go with it the same way
		d.opened = { ...panel, change };

		// a sheet shows its code until it fills the screen, where its editor takes over; while the code loads, grey lines
		if (AppMapPanels.isSheet(d) && !this.view.showsControls(d)) {

			const content = d.data.id ? (this.requestContent(d.data.id), this.contents.get(d.data.id)) : null;

			if (content?.texts?.length) {

				ctx.globalAlpha = Math.max(0, change * 2 - 1);
				this.drawContent(renderer, AppMapPanels.contentOf(d, content.texts), area, area.width / (AppMapPanels.SHEET_COLUMNS * AppMapPanels.CODE_ADVANCE));
				ctx.restore();

				return;
			}

			const u = area.height / 40;

			ctx.globalAlpha = 0.35 * Math.max(0, change * 2 - 1);
			ctx.strokeStyle = look.trace;
			ctx.lineWidth   = u * 0.45;
			ctx.lineCap     = 'round';
			ctx.beginPath();

			for (let i = 0; i < 30; i++) {

				const y = area.y + u * (1 + i * 1.25);

				ctx.moveTo(area.x + u * (i % 5 ? 3 : 1), y);
				ctx.lineTo(area.x + area.width * [0.7, 0.45, 0.8, 0.6, 0.35][i % 5], y);
			}

			ctx.stroke();
			ctx.restore();

			return;
		}

		// while they load, grey bars where they will be: the main field on the left, the settings on the right; an open
		// element whose controls are in another one nearer the middle of the screen shows its header only
		if (this.view.editing === d && !this.view.showsControls(d)) {

			const u = area.height / 10;

			ctx.globalAlpha = 0.25 * Math.max(0, change * 2 - 1);
			ctx.fillStyle   = look.trace;
			ctx.beginPath();
			ctx.roundRect(area.x, area.y, area.width * 0.56, area.height * 0.78, u * 0.15);
			ctx.roundRect(area.x + area.width * 0.6, area.y, area.width * 0.4, u * 1.4, u * 0.15);
			ctx.roundRect(area.x + area.width * 0.6, area.y + u * 2, area.width * 0.4, u * 1.4, u * 0.15);
			ctx.roundRect(area.x + area.width * 0.6, area.y + u * 4, area.width * 0.4, u * 1.4, u * 0.15);
			ctx.fill();

		} else if (this.view.editing !== d && d.data.text && d.data.id) {

			// an element without its controls shows its text below the header
			this.requestContent(d.data.id);

			const content = this.contents.get(d.data.id);

			if (content?.texts?.length) {

				ctx.globalAlpha = Math.max(0, change * 2 - 1);
				this.drawContent(renderer, AppMapPanels.contentOf(d, content.texts), area, AppMapPanels.CONTENT_TEXT * panel.fit / k);
			}
		}

		ctx.restore();
	}

	// reads the texts of an HTML element once, when it first opens
	requestContent(id) {

		if (this.contents.has(id) || !this.view.loadContent) {
			return;
		}

		this.contents.set(id, { loading: true });

		Promise.resolve(this.view.loadContent(id)).then(texts => {

			this.contents.set(id, { texts: texts ?? [] });
			this.view.requestDraw();

		}).catch(error => {

			console.warn('Application map: the text of an element could not be read', error);

			this.contents.set(id, { texts: [] });
		});
	}

	// what an open HTML element shows of its texts: a sheet its code line by line, with no more than one empty line in a
	// row; any other element its text as words, with its white space collapsed
	static contentOf(d, texts) {

		if (AppMapPanels.isSheet(d)) {

			const lines  = texts.map(t => t.content).join('\n').replace(/\t/g, '    ').replace(/^\s*\n|\s+$/g, '').replace(/\n\s*\n(\s*\n)+/g, '\n\n').split('\n');
			const indent = d3.min(lines.filter(l => l.trim()), l => l.length - l.trimStart().length) ?? 0;

			// the lines of a block that is indented as a whole stand at the left edge
			return { code: true, lines: lines.map(l => l.slice(Math.min(indent, l.length - l.trimStart().length))) };
		}

		return { code: false, text: texts.map(t => t.content).join(' ').replace(/\s+/g, ' ').trim() };
	}

	// the content of an open HTML element in its area: code line by line, cut at the width; a text broken into lines at
	// its words; as many lines as fit the height
	drawContent(renderer, content, area, size) {

		const ctx   = renderer.ctx;
		const look  = this.view.look;
		const line  = size * 1.4;
		const width = Math.floor(area.width / (size * AppMapPanels.CODE_ADVANCE));
		const most  = Math.floor(area.height / line);

		if (width < 4 || most < 1) {
			return;
		}

		const cut   = text => text.length > width ? text.slice(0, width - 1) + '…' : text;
		let   lines = [];

		if (content.code) {

			lines = content.lines.map(cut);

		} else {

			for (const word of content.text.split(' ')) {

				const last = lines.length - 1;

				if (last >= 0 && lines[last].length + 1 + word.length <= width) {
					lines[last] += ' ' + word;
				} else {
					lines.push(cut(word));
				}
			}
		}

		if (lines.length > most) {
			lines = [...lines.slice(0, most - 1), content.code ? `… ${lines.length - most + 1} more lines` : cut(lines[most - 1] + ' …')];
		}

		ctx.fillStyle = content.code ? look.labelFocus : look.labelNear;
		lines.forEach((text, i) => renderer.screenText(text, area.x, area.y + line * (i + 0.6), 400, size, true, 'left'));
	}

	// a small magnifier with a plus: closer up there is more to see
	drawZoomSign(ctx, cx, y, s, colour) {

		const r = 0.26 * s;
		const c = [cx - 0.08 * s, y - 0.08 * s];

		ctx.save();
		ctx.strokeStyle = colour;
		ctx.lineWidth   = 0.09 * s;
		ctx.lineCap     = 'round';
		ctx.beginPath();
		ctx.arc(c[0], c[1], r, 0, Math.PI * 2);
		ctx.moveTo(c[0] + r * 0.72, c[1] + r * 0.72);
		ctx.lineTo(c[0] + r * 1.6, c[1] + r * 1.6);
		ctx.moveTo(c[0] - r * 0.5, c[1]);
		ctx.lineTo(c[0] + r * 0.5, c[1]);
		ctx.moveTo(c[0], c[1] - r * 0.5);
		ctx.lineTo(c[0], c[1] + r * 0.5);
		ctx.stroke();
		ctx.restore();
	}

	// the tree of a page that is not yet open: its branches and boxes without names, as strong as the page is close, on its
	// sheet that lightens toward the frame it will become
	drawSketch(renderer, page, tree, panel, k, strength) {

		const ctx     = renderer.ctx;
		const look    = this.view.look;
		const s       = panel.scale;
		const widthOf = d => AppMapPanels.boxWidth(d, s);

		ctx.save();

		// the sheet turns into the frame early, so the sketch can be seen on it
		this.drawFrame(ctx, page, panel, renderer.colorOf(page), k, Math.min(1, 0.55 + strength));

		ctx.globalAlpha = 0.35 + 0.65 * strength;
		this.drawBand(ctx, panel.head, renderer.colorOf(page));

		ctx.globalAlpha = 0.45 + 0.55 * strength;
		ctx.strokeStyle = look.trace;
		ctx.lineWidth   = Math.max(0.5 / k, 0.12 * s);
		ctx.stroke(AppMapPanels.branches(tree, panel));

		for (const d of tree.list) {

			const [x, y] = AppMapPanels.box(d, panel);

			const height = AppMapPanels.boxHeight(d) * s;

			ctx.strokeStyle = this.toneOf(d);
			ctx.stroke(AppMapPanels.outline(d, x, y - height / 2, widthOf(d), height, s));
		}

		ctx.restore();
	}

	// whether a relation belongs to a page that is open: the map does not draw it then. Those that leave from its boxes the
	// page draws itself (see drawRelations); those of the page as a whole, such as links to other pages, grants or the
	// shared components it uses, rest while its elements are in view
	withinOpenPage(l) {

		return !!(this.trees.get(l.source)?.open || this.trees.get(l.target)?.open);
	}

	// the relations that leave from the boxes of a page, as [box, the element on the other side, relation], for the current
	// relation set; kept until the set changes. Those of the page as a whole, which no box of it makes, are not among them,
	// see withinOpenPage
	relationsOf(page, tree) {

		const view = this.view;

		if (tree.relations?.generation !== view.generation) {

			const list = [];

			for (const l of view.links) {

				if (l.source !== page && l.target !== page) {
					continue;
				}

				const d = l.via && tree.boxOf.get(l.via);

				if (d && !AppMapModel.STRUCTURE.has(l.kind)) {
					list.push([d, l.source === page ? l.target : l.source, l]);
				}
			}

			tree.relations = { generation: view.generation, list };
		}

		return tree.relations.list;
	}

	// from the middle of a box to the element it relates to, in the colour of the effect, bundled: the relations leave the
	// sheet together through one gate on each side, the side the way from the middle of the sheet to their element crosses,
	// at the place along it where they cross on average. Inside, each relation swings from its box into the gate and arrives
	// there square to the edge, as all others of the gate do, so that they come together on their way; at the gate each
	// colour has its own lane. Outside, one line goes on to each element, the stronger the more it carries. With a box under
	// the pointer its relations come forward and the others step back
	drawRelations(renderer, page, tree, panel, k) {

		const ctx     = renderer.ctx;
		const hovered = this.view.hoveredDom ?? this.view.pinnedDom;
		const list    = this.relationsOf(page, tree);

		if (!list.length) {
			return;
		}

		const left   = page.x - panel.hw;
		const right  = page.x + panel.hw;
		const top    = page.y - panel.hh;
		const bottom = page.y + panel.hh;
		const alpha  = lit => hovered ? (lit ? 0.95 : 0.12) : 0.55;
		const width  = (lit, count) => ((lit ? 1.6 : 0.9) + 0.45 * Math.sqrt(count - 1)) / k;
		const gates  = new Map();
		const direct = [];

		for (const [d, other, l] of list) {

			const lit    = d === hovered;
			const colour = renderer.opColor(l) ?? renderer.colorOf(other);

			// an element on the sheet itself has nothing to bundle with
			if (other.x > left && other.x < right && other.y > top && other.y < bottom) {

				direct.push([d, other, colour, lit]);
				continue;
			}

			// a relation of an element never runs back up across the tree: where its way would leave the sheet above its box,
			// it leaves on the side of its target at the height of its box and turns outside; only those of the page as a whole
			// leave toward any side
			const from = AppMapPanels.origin(d, panel);
			let   way  = AppMapPanels.crossing(page, panel, other);

			if (d !== tree.root && (way.out[1] < 0 || (way.out[0] && page.y + way.along < from.y))) {
				way = { out: [Math.sign(other.x - page.x) || 1, 0], along: from.y - page.y };
			}

			const key = way.out.join();

			if (!gates.has(key)) {
				gates.set(key, { out: way.out, along: [], ahead: -Infinity, lanes: new Map() });
			}

			const gate = gates.get(key);

			gate.along.push(way.along);
			gate.ahead = Math.max(gate.ahead, from.y - page.y);

			if (!gate.lanes.has(colour)) {
				gate.lanes.set(colour, { colour, count: 0, lit: false, lines: new Map(), boxes: [] });
			}

			const lane = gate.lanes.get(colour);

			lane.count += 1;
			lane.lit  ||= lit;
			lane.boxes.push([d, lit]);

			if (!lane.lines.has(other)) {
				lane.lines.set(other, { count: 0, lit: false });
			}

			lane.lines.get(other).count += 1;
			lane.lines.get(other).lit  ||= lit;
		}

		ctx.lineCap = 'round';

		for (const [d, other, colour, lit] of direct) {

			ctx.globalAlpha = alpha(lit);
			ctx.strokeStyle = colour;
			ctx.lineWidth   = width(lit, 1);
			ctx.stroke(AppMapRenderer.arc(AppMapPanels.origin(d, panel), other, 0.12));
		}

		for (const gate of gates.values()) {

			const out    = gate.out;
			const half   = out[0] ? panel.hh : panel.hw;
			// a gate on a side lies no higher than the boxes whose relations leave through it
			const mean   = out[0] ? Math.max(d3.mean(gate.along), gate.ahead) : d3.mean(gate.along);
			const place  = Math.max(-0.85 * half, Math.min(0.85 * half, mean));
			const centre = out[0] ? { x: page.x + out[0] * panel.hw, y: page.y + place } : { x: page.x + place, y: page.y + out[1] * panel.hh };
			const across = [-out[1], out[0]];
			const lanes  = [...gate.lanes.values()].sort((a, b) => b.count - a.count);
			// how far before the gate the lines turn square to the edge, the same for all, so that they come together
			const bend   = Math.min(panel.hw, panel.hh) * 0.45;

			lanes.forEach((lane, i) => {

				// the lanes lie side by side in the gate, the strongest in the middle
				const shift = (i % 2 ? -1 : 1) * Math.ceil(i / 2) * 2.2 / k;
				const at    = { x: centre.x + across[0] * shift, y: centre.y + across[1] * shift };

				ctx.strokeStyle = lane.colour;

				for (const [d, lit] of lane.boxes) {

					const from = AppMapPanels.origin(d, panel);
					// a box close to the edge turns no further back than it stands
					const back = Math.max(0, Math.min(bend, (at.x - from.x) * out[0] + (at.y - from.y) * out[1]));
					const path = new Path2D();

					// every line leaves its box toward its gate, and turns square to the edge before it
					const ahead = Math.max(Math.abs(out[0] ? at.x - from.x : at.y - from.y) * 0.4, 6 * panel.scale);

					path.moveTo(from.x, from.y);
					path.bezierCurveTo(from.x + out[0] * ahead, from.y + out[1] * ahead, at.x - out[0] * back, at.y - out[1] * back, at.x, at.y);

					ctx.globalAlpha = alpha(lit);
					ctx.lineWidth   = width(lit, 1);
					ctx.stroke(path);
				}

				// beyond the edge one line to each element, the stronger the more it carries; one to an element behind a side
				// first goes on outward and turns around the sheet
				for (const [other, line] of lane.lines) {

					ctx.globalAlpha = alpha(line.lit);
					ctx.lineWidth   = width(line.lit, line.count);
					ctx.stroke(out[0] && (other.x - at.x) * out[0] < 0 ? AppMapPanels.turn(at, other, page, panel, k) : AppMapRenderer.arc(at, other, 0.12));
				}
			});
		}

		ctx.globalAlpha = 1;
	}

	// from a gate on a side of an open page to an element behind it: outward, turning up or down toward the element, along
	// the edge of the sheet, past its corner when the element lies beside the sheet, and on to the element; the way keeps
	// clear of the sheet by some pixels whatever the zoom
	static turn(at, other, page, panel, k) {

		const path  = new Path2D();
		const room  = Math.max(Math.min(panel.hw, panel.hh) * 0.08, 24 / k);
		const side  = Math.sign(at.x - page.x) || 1;
		const lane  = page.x + side * (panel.hw + room);
		const up    = other.y < at.y;
		const sign  = up ? -1 : 1;
		const turn  = { x: lane, y: at.y + sign * room };
		// beside the sheet the way goes past its corner first
		const past  = up ? Math.min(other.y, page.y - panel.hh - room) : Math.max(other.y, page.y + panel.hh + room);

		path.moveTo(at.x, at.y);
		path.quadraticCurveTo(lane, at.y, turn.x, turn.y);
		path.lineTo(lane, past - sign * room);
		path.bezierCurveTo(lane, past + sign * room * 0.5, lane - side * room, past, side > 0 ? Math.max(other.x, lane - 2 * room) : Math.min(other.x, lane + 2 * room), past);
		path.quadraticCurveTo(other.x + (lane - other.x) * 0.3, past, other.x, other.y);

		return path;
	}

	// where the way from the middle of the sheet of a page to an element beyond it crosses the edge: the direction out of the
	// sheet on that side, and how far along the side from its middle
	static crossing(page, panel, other) {

		const dx    = other.x - page.x;
		const dy    = other.y - page.y;
		const side  = panel.hw / Math.abs(dx || 1e-9) <= panel.hh / Math.abs(dy || 1e-9);
		const scale = Math.min(panel.hw / Math.abs(dx || 1e-9), panel.hh / Math.abs(dy || 1e-9));

		return side ? { out: [Math.sign(dx) || 1, 0], along: dy * scale } : { out: [0, Math.sign(dy) || 1], along: dx * scale };
	}

	// where the relations of an HTML element leave its box: its middle; for the page itself the middle of the document at the
	// root
	static origin(d, panel) {

		const [x, y] = AppMapPanels.box(d, panel);

		return { x: x + AppMapPanels.sizeOf(d)[0] * panel.scale / 2, y };
	}

	// ----- exploring from an HTML element: the elements its repeaters and action mappings reach -----
	// as [element, number of relations], the strongest first
	boxTargets(d) {

		const list = this.relationsOf(d.page, this.trees.get(d.page)).filter(([box]) => box === d);

		return d3.rollups(list, v => v.length, ([, other]) => other).sort((a, b) => b[1] - a[1]);
	}

	// where the relations of an HTML element leave its box at the current zoom, in map units
	boxOrigin(d) {

		return AppMapPanels.origin(d, this.pagePanel(d.page, this.trees.get(d.page), this.view.transform.k));
	}

	// the colour of what an HTML element does: an event action mapping orange, a repeater or an element with data in the
	// colour of the pages, anything else the quiet grey of the lines
	toneOf(d) {

		return d.data.action ? AppMapPanels.ACTION_COLOR : (d.data.repeater || d.data.active ? this.view.look.kinds.page : this.view.look.trace);
	}

	// a small flash: an event action mapping
	drawFlash(ctx, cx, y, s, colour) {

		ctx.fillStyle = colour;
		ctx.beginPath();
		ctx.moveTo(cx + 0.15 * s, y - 0.6 * s);
		ctx.lineTo(cx - 0.35 * s, y + 0.1 * s);
		ctx.lineTo(cx - 0.02 * s, y + 0.1 * s);
		ctx.lineTo(cx - 0.15 * s, y + 0.6 * s);
		ctx.lineTo(cx + 0.35 * s, y - 0.1 * s);
		ctx.lineTo(cx + 0.02 * s, y - 0.1 * s);
		ctx.closePath();
		ctx.fill();
	}

	// a small database: the data a repeater shows
	drawDatabase(ctx, cx, y, s, colour = this.view.look.trace) {

		ctx.fillStyle = colour;
		ctx.beginPath();
		ctx.ellipse(cx, y - 0.45 * s, 0.6 * s, 0.22 * s, 0, 0, Math.PI * 2);
		ctx.rect(cx - 0.6 * s, y - 0.45 * s, 1.2 * s, 0.9 * s);
		ctx.ellipse(cx, y + 0.45 * s, 0.6 * s, 0.22 * s, 0, 0, Math.PI * 2);
		ctx.fill();
	}
}
