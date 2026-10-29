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
 * their relations leave from the box of that element instead of from the page.
 */
class AppMapPanels {

	// the panel tree: one column per depth, one row per leaf, boxes this high, in units of the map
	static COLUMN = 11;
	static ROW    = 2.6;
	static BOX    = 1.8;

	// an HTML element that triggers an event action mapping is marked in this colour
	static ACTION_COLOR = '#e8590c';

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

	// the advance of a character of code type as a share of its size
	static CODE_ADVANCE = 0.6;

	constructor(view) {

		this.view  = view;
		this.trees = new Map();
		this.code  = new Map();

		// the methods whose code was readable in the last frame: they carry their name inside, not beside them
		this.reading = new Set();

		this.buildTrees();
	}

	get domCount() {

		return d3.sum([...this.trees.values()], t => t.list.length);
	}

	buildTrees() {

		const inner  = this.view.model.inner;
		const byId   = this.view.byId;
		const toTree = item => ({
			tag:      item[0],
			active:   !!(item[1] & 1),
			action:   !!(item[1] & 2),
			repeater: !!(item[1] & 4),
			children: (item[2] ?? []).map(toTree),
			inner:    (item[3] ?? []).map(id => byId.get(id)).filter(n => n && inner.has(n)),
			shows:    item[4] ?? null
		});

		for (const page of this.view.nodes.filter(n => (n.k === 'page' || n.k === 'component') && n.d?.length && !n.tpl)) {

			const root = d3.hierarchy({ tag: page.n, children: page.d.map(toTree) });

			d3.tree().nodeSize([AppMapPanels.ROW, AppMapPanels.COLUMN]).separation((a, b) => a.parent === b.parent ? 1 : 1.25)(root);

			const all    = root.descendants();
			const top    = d3.min(all, d => d.x) - AppMapPanels.ROW;
			const bottom = d3.max(all, d => d.x) + AppMapPanels.ROW;

			for (const d of all) {

				d.lx   = d.y;
				d.ly   = d.x;
				d.page = page;
			}

			// how far an element without children may show its expressions: up to the next box to its right at its height,
			// else to the edge
			for (const d of all) {

				if (d.data.shows && !d.children) {
					d.room = d3.min(all, o => o.lx > d.lx && Math.abs(o.ly - d.ly) < AppMapPanels.BOX ? o.lx - d.lx : Infinity);
				}
			}

			const boxOf = new Map(all.flatMap(d => (d.data.inner ?? []).map(n => [n, d])));

			this.trees.set(page, { root, list: all.slice(1), boxOf, open: false, frame: null, panel: { width: d3.max(all, d => d.y) + AppMapPanels.COLUMN, top, height: bottom - top } });
		}
	}

	// the place of the tree in the sheet of a page, with the height of a row on screen: the sheet becomes the frame
	pagePanel(page, tree, k, radius = this.view.shownRadius(page)) {

		// the sheet exactly as the element is drawn, at the current zoom or, for a glide, at the one it heads for
		const r      = Math.max(radius, 2 / k);
		const hw     = r * AppMapRenderer.PAGE_BOX[0];
		const hh     = r * AppMapRenderer.PAGE_BOX[1];
		const title  = r * 2 * 0.08;
		const scale  = Math.min(hw * 2 * 0.88 / tree.panel.width, (hh * 2 * 0.88 - title) / tree.panel.height);
		const width  = tree.panel.width * scale;
		const height = tree.panel.height * scale;

		return { x0: page.x - width / 2, y0: page.y - height / 2 + title / 2, width, height, scale, top: tree.panel.top, hw, hh, title, row: scale * AppMapPanels.ROW * k };
	}

	// the frame is the element itself: its sheet, open, in a pale tint of its colour with its colour as border; the
	// background covers what lies beneath by the given share
	drawFrame(ctx, page, panel, colour, k, cover) {

		const { hw, hh } = panel;

		ctx.globalAlpha = cover;
		ctx.fillStyle   = this.view.look.bg;
		ctx.fillRect(page.x - hw, page.y - hh, 2 * hw, 2 * hh);
		ctx.globalAlpha = 1;
		ctx.fillStyle   = d3.color(colour).copy({ opacity: 0.06 }).formatRgb();
		ctx.fillRect(page.x - hw, page.y - hh, 2 * hw, 2 * hh);
		ctx.strokeStyle = colour;
		ctx.lineWidth   = 1.6 / k;
		ctx.strokeRect(page.x - hw, page.y - hh, 2 * hw, 2 * hh);
	}

	// branches from the right end of a parent to the left end of each child, with rounded bends, all in one path
	static branches(tree, panel) {

		const s    = panel.scale;
		const path = new Path2D();

		for (const d of tree.list) {

			const [px, py] = AppMapPanels.box(d.parent, panel);
			const [cx, cy] = AppMapPanels.box(d, panel);
			const start    = px + (d.parent === tree.root ? 4 * s : AppMapPanels.boxWidth(d.parent, s));
			const turn     = cx - 1.2 * s;

			const bend     = Math.min(0.6 * s, Math.abs(cy - py) / 2);

			path.moveTo(start, py);
			path.arcTo(turn, py, turn, cy, bend);
			path.arcTo(turn, cy, cx, cy, bend);
			path.lineTo(cx, cy);
		}

		return path;
	}

	// the width of the box of an HTML element: its tag, and the sign of what it does, at the scale of the panel
	static boxWidth(d, s) {

		return (d.data.tag.length * 0.6 + (d.data.action || d.data.repeater ? 2.8 : 1.6)) * s;
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

		return null;
	}

	covers(x, y) {

		return !!this.openAt(x, y);
	}

	// whether an element shows its inner structure: an open page or shared component, a method whose code can be read
	isOpen(n) {

		return !!this.trees.get(n)?.open || this.reading.has(n);
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

		Promise.resolve(this.view.loadSource(n)).then(source => {

			const lines = String(source ?? '').replace(/\t/g, '  ').replace(/\s+$/, '').split('\n');

			this.code.set(n, source == null ? { failed: true } : { lines });
			this.view.requestDraw();

		}).catch(() => {

			this.code.set(n, { failed: true });
			this.view.requestDraw();
		});
	}

	// the place of the code in the rectangle of a method, with the size of a line on screen; the font follows the number
	// of lines and the longest of them, within CODE_LINES and CODE_COLUMNS
	codePanel(n, k, radius = this.view.shownRadius(n)) {

		const [bw, bh] = AppMapRenderer.boxOf(n);
		const w        = radius * bw;
		const h        = radius * bh;
		const pad      = h * 0.08;
		const title    = h * 0.16;
		const lines    = this.code.get(n)?.lines ?? [];
		const columns  = Math.min(AppMapPanels.CODE_COLUMNS, Math.max(40, d3.max(lines, l => l.length) ?? 0));
		const inner    = [2 * w - 2 * pad, 2 * h - 2 * pad - title];

		// a box higher than wide, as the sheet of a page, holds more than CODE_LINES at the font its width allows
		const fit      = Math.floor(inner[1] / (inner[0] / (columns * 0.6) * 1.3));
		const rows     = Math.min(Math.max(AppMapPanels.CODE_LINES[1], fit), Math.max(AppMapPanels.CODE_LINES[0], lines.length));
		const font     = Math.min(inner[0] / (columns * 0.6), inner[1] / (rows * 1.3));

		return { x0: n.x - w, y0: n.y - h, w, h, pad, title, font, line: font * 1.3, rows, px: font * 1.3 * k };
	}

	drawCode(renderer, k) {

		const ctx  = renderer.ctx;
		const look = this.view.look;

		this.reading.clear();

		for (const n of this.view.nodes) {

			if (!AppMapPanels.hasCode(n) || !renderer.onScreen(n, 200)) {
				continue;
			}

			const panel = this.codePanel(n, k);

			if (panel.px < AppMapPanels.CODE_SKETCH) {
				continue;
			}

			this.requestCode(n);

			const code   = this.code.get(n);
			const colour = renderer.colorOf(n);
			const lines  = code?.lines ?? [];
			const shown  = lines.length > panel.rows ? panel.rows - 1 : lines.length;
			const left   = panel.x0 + panel.pad;
			const top    = panel.y0 + panel.pad + panel.title;
			const strong = Math.min(1, (panel.px - AppMapPanels.CODE_SKETCH) / (AppMapPanels.CODE_READ - AppMapPanels.CODE_SKETCH));

			ctx.save();

			// the rectangle opens: pale in the colour of the method, its name on top, the code below
			ctx.beginPath();
			ctx.roundRect(panel.x0, panel.y0, 2 * panel.w, 2 * panel.h, n.k === 'method' ? panel.h * 0.14 : 0);
			ctx.globalAlpha = 0.35 + 0.65 * strong;
			ctx.fillStyle   = look.bg;
			ctx.fill();
			ctx.fillStyle   = d3.color(colour).copy({ opacity: 0.08 }).formatRgb();
			ctx.fill();
			ctx.strokeStyle = colour;
			ctx.lineWidth   = 1.4 / k;
			ctx.stroke();
			ctx.clip();

			if (panel.px >= AppMapPanels.CODE_READ) {

				this.reading.add(n);

				ctx.fillStyle = colour;
				renderer.screenText(n.k === 'method' ? n.n : `${AppMapModel.KIND_LABEL[n.k]} ${n.n}`, left, panel.y0 + panel.pad + panel.title * 0.45, 600, panel.title * 0.6, false, 'left');
				ctx.fillStyle = look.labelFocus;

				for (let i = 0; i < shown; i++) {

					const text = lines[i].length > AppMapPanels.CODE_COLUMNS ? lines[i].slice(0, AppMapPanels.CODE_COLUMNS - 1) + '…' : lines[i];

					renderer.screenText(text, left, top + (i + 0.5) * panel.line, 400, panel.font, true, 'left');
				}

				ctx.fillStyle = look.label;

				if (lines.length > shown) {
					renderer.screenText(`… ${lines.length - shown} more lines`, left, top + (shown + 0.5) * panel.line, 400, panel.font, true, 'left');
				} else if (!code || code.loading) {
					renderer.screenText('loading the code …', left, top + 0.5 * panel.line, 400, panel.font, true, 'left');
				} else if (code.failed) {
					renderer.screenText('the code could not be read', left, top + 0.5 * panel.line, 400, panel.font, true, 'left');
				}

			} else {

				// from afar the code is a sketch: a grey bar per line, as long and as far indented as the line
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

	draw(renderer, k) {

		this.drawCode(renderer, k);

		for (const [page, tree] of this.trees) {

			const panel = renderer.onScreen(page, 2000) ? this.pagePanel(page, tree, k) : null;

			// open only when the boxes can be read
			tree.open = !!panel && panel.row >= AppMapPanels.OPEN_ROW;

			if (tree.open) {

				this.drawPanel(renderer, page, tree, panel, k);

			} else if (panel && panel.row >= AppMapPanels.SKETCH_ROW) {

				this.drawSketch(renderer, page, tree, panel, k, (panel.row - AppMapPanels.SKETCH_ROW) / (AppMapPanels.OPEN_ROW - AppMapPanels.SKETCH_ROW));
			}
		}
	}

	// a page close enough opens inside itself: the tree from left to right, with right-angled branches, every element a
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

		ctx.fillStyle = colour;
		renderer.screenText(`${AppMapModel.KIND_LABEL[page.k]} ${page.n}`, page.x - panel.hw + panel.title * 0.5, page.y - panel.hh + panel.title * 0.75, 600, panel.title * 0.7, false, 'left');

		ctx.strokeStyle = look.column;
		ctx.lineWidth   = Math.max(0.6 / k, 0.12 * s);
		ctx.stroke(AppMapPanels.branches(tree, panel));

		// the page itself: a document at the root
		const [rx, ry] = AppMapPanels.box(tree.root, panel);

		ctx.fillStyle   = look.bg;
		ctx.strokeStyle = colour;
		ctx.lineWidth   = Math.max(1 / k, 0.15 * s);
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
			const tone   = this.toneOf(d);
			const plain  = !d.data.action && !d.data.repeater && !d.data.active;
			const lit    = d === hovered;
			const r      = boxH / 2;

			ctx.lineWidth = Math.max(0.8 / k, 0.12 * s);

			// a repeater shows the copies it makes, stacked behind it
			if (d.data.repeater) {

				for (const o of [0.6, 0.3]) {

					ctx.beginPath();
					ctx.roundRect(x + o * s, y - r + o * s, w, boxH, r);
					ctx.fillStyle   = look.bg;
					ctx.fill();
					ctx.strokeStyle = d3.color(tone).copy({ opacity: 0.45 }).formatRgb();
					ctx.stroke();
				}
			}

			// what the global search found lies on a mark, as on the map
			if (this.view.found && (d.data.inner ?? []).some(m => this.view.found.inner.has(m))) {

				ctx.beginPath();
				ctx.roundRect(x - 0.6 * s, y - r - 0.6 * s, w + 1.2 * s, boxH + 1.2 * s, r + 0.6 * s);
				ctx.fillStyle = this.view.look.found;
				ctx.fill();
			}

			// a pill, tinted in the colour of what the element does, filled with it when it is pointed at or held
			ctx.beginPath();
			ctx.roundRect(x, y - r, w, boxH, r);
			ctx.fillStyle = look.bg;
			ctx.fill();
			ctx.fillStyle = lit ? tone : d3.color(tone).copy({ opacity: plain ? 0 : 0.12 }).formatRgb();
			ctx.fill();
			ctx.strokeStyle = plain ? d3.color(tone).copy({ opacity: 0.7 }).formatRgb() : tone;
			ctx.stroke();

			// what it does, as a small sign at its start: a flash for an event action mapping, the data a repeater shows
			const ink  = lit ? look.bg : tone;
			const sign = d.data.action || d.data.repeater;

			if (d.data.action) {
				this.drawFlash(ctx, x + 1.1 * s, y, s, ink);
			} else if (d.data.repeater) {
				this.drawDatabase(ctx, x + 1.1 * s, y, s * 0.8, ink);
			}

			ctx.fillStyle = lit ? look.bg : (plain ? look.label : look.labelFocus);
			renderer.screenText(d.data.tag, x + (sign ? 2 : 0.8) * s, y, plain ? 400 : 500, 1.05 * s, false, 'left');

			d.panelHit = [x, y - r, w, boxH];

			// what an element without children shows, in the free space behind it
			if (d.data.shows && !d.children) {
				const end = Math.min(x + d.room * s - 1.6 * s, page.x + panel.hw - 1.6 * s);

				this.drawShows(renderer, d.data.shows, x + w + 0.8 * s, y, end - x - w - 0.8 * s, s);
			}
		}

		this.drawRelations(renderer, page, tree, panel, k);

		tree.frame = [page.x - panel.hw, page.y - panel.hh, 2 * panel.hw, 2 * panel.hh];
		ctx.restore();
	}

	// the template expressions of an element in code type, cut to the given width
	drawShows(renderer, text, x, y, width, s) {

		const size  = 0.9 * s;
		const fit   = Math.floor(width / (size * AppMapPanels.CODE_ADVANCE));

		if (fit < 4) {
			return;
		}

		renderer.ctx.fillStyle = this.view.look.label;
		renderer.screenText(text.length > fit ? text.slice(0, fit - 1) + '…' : text, x, y, 400, size, true, 'left');
	}

	// the tree of a page that is not yet open: its branches and boxes without names, as strong as the page is close, on its
	// sheet that lightens toward the frame it will become
	drawSketch(renderer, page, tree, panel, k, strength) {

		const ctx     = renderer.ctx;
		const look    = this.view.look;
		const s       = panel.scale;
		const boxH    = AppMapPanels.BOX * s;
		const widthOf = d => AppMapPanels.boxWidth(d, s);

		ctx.save();

		// the sheet turns into the frame early, so the sketch can be seen on it
		this.drawFrame(ctx, page, panel, renderer.colorOf(page), k, Math.min(1, 0.55 + strength));

		ctx.globalAlpha = 0.45 + 0.55 * strength;
		ctx.strokeStyle = look.trace;
		ctx.lineWidth   = Math.max(0.5 / k, 0.12 * s);
		ctx.stroke(AppMapPanels.branches(tree, panel));

		for (const d of tree.list) {

			const [x, y] = AppMapPanels.box(d, panel);

			ctx.strokeStyle = this.toneOf(d);
			ctx.beginPath();
			ctx.roundRect(x, y - boxH / 2, widthOf(d), boxH, boxH / 2);
			ctx.stroke();
		}

		ctx.restore();
	}

	// whether a relation of the page leaves from one of its boxes while the page is open; then the map does not draw it from the page
	leavesFromBox(l) {

		const tree = l.via && this.trees.get(this.view.model.inner.get(l.via));

		return !!(tree?.open && tree.boxOf.has(l.via));
	}

	// the relations that leave from the boxes of a page, as [box, the element on the other side, relation], for the current
	// relation set; kept until the set changes
	relationsOf(page, tree) {

		const view = this.view;

		if (tree.relations?.generation !== view.generation) {

			const list = [];

			for (const l of view.links) {

				const d = l.via && tree.boxOf.get(l.via);

				if (d && (l.source === page || l.target === page) && !AppMapModel.STRUCTURE.has(l.kind)) {
					list.push([d, l.source === page ? l.target : l.source, l]);
				}
			}

			tree.relations = { generation: view.generation, list };
		}

		return tree.relations.list;
	}

	// from the right end of a box to the element it relates to, in the colour of the effect; with a box under the pointer its
	// relations come forward and the others step back
	drawRelations(renderer, page, tree, panel, k) {

		const ctx     = renderer.ctx;
		const hovered = this.view.hoveredDom ?? this.view.pinnedDom;
		const list    = this.relationsOf(page, tree);

		if (!list.length) {
			return;
		}

		ctx.lineCap = 'round';

		for (const [d, other, l] of list) {

			const lit    = d === hovered;
			const from   = AppMapPanels.origin(d, panel);

			ctx.globalAlpha = hovered ? (lit ? 0.95 : 0.12) : 0.55;
			ctx.strokeStyle = renderer.opColor(l) ?? renderer.colorOf(other);
			ctx.lineWidth   = (lit ? 1.6 : 0.9) / k;
			ctx.stroke(AppMapRenderer.arc(from, other, 0.12));
		}

		ctx.globalAlpha = 1;
	}

	// where the relations of an HTML element leave its box: its right end
	static origin(d, panel) {

		const [x, y] = AppMapPanels.box(d, panel);

		return { x: x + AppMapPanels.boxWidth(d, panel.scale) + 0.3 * panel.scale, y };
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
