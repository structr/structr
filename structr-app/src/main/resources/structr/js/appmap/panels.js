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

	// the panel tree: one column per depth, one row per leaf, boxes this high, in units of the map
	static COLUMN = 11;
	static ROW    = 2.6;
	static BOX    = 1.8;

	// an HTML element that triggers an event action mapping is marked in this colour
	static ACTION_COLOR = '#e8590c';

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
	static ELEMENT_BAR    = 0.06;

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

	// the advance of a character of code type as a share of its size
	static CODE_ADVANCE = 0.6;

	// an open page is a card with a header (see header): its padding as a share of its shorter side, the line of its kind
	// and its name as shares of its height
	static CARD_PAD  = 0.05;
	static CARD_KIND = 0.022;
	static CARD_NAME = 0.045;

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
			shows:    item[4] || null,
			key:      item[5] ?? null,
			id:       item[6] ?? null,
			text:     !!(item[1] & 8),
			when:     !!(item[1] & 16)
		});

		for (const page of this.view.nodes.filter(n => (n.k === 'page' || n.k === 'component') && n.d?.length && !n.tpl)) {

			const root = d3.hierarchy({ tag: page.n, children: page.d.map(toTree) });

			// a template takes the rows its height needs
			const rows = d => Math.max(1, (AppMapPanels.boxHeight(d) + AppMapPanels.ROW - AppMapPanels.BOX) / AppMapPanels.ROW);

			d3.tree().nodeSize([AppMapPanels.ROW, AppMapPanels.COLUMN]).separation((a, b) => (a.parent === b.parent ? 1 : 1.25) + (rows(a) + rows(b)) / 2 - 1)(root);

			const all    = root.descendants();
			const top    = d3.min(all, d => d.x - Math.max(AppMapPanels.ROW, AppMapPanels.boxHeight(d) / 2 + 0.8));
			const bottom = d3.max(all, d => d.x + Math.max(AppMapPanels.ROW, AppMapPanels.boxHeight(d) / 2 + 0.8));

			for (const d of all) {

				d.lx   = d.y;
				d.ly   = d.x;
				d.page = page;
			}

			// how far an element without children may show its expressions: up to the next box to its right at its height,
			// else to the edge
			for (const d of all) {

				if (d.data.shows && !d.children) {
					d.room = d3.min(all, o => o.lx > d.lx && Math.abs(o.ly - d.ly) < (AppMapPanels.BOX + AppMapPanels.boxHeight(o)) / 2 ? o.lx - d.lx : Infinity);
				}
			}

			const boxOf = new Map(all.flatMap(d => (d.data.inner ?? []).map(n => [n, d])));

			this.trees.set(page, { root, list: all.slice(1), boxOf, open: false, frame: null, panel: { width: d3.max(all, d => d.y) + AppMapPanels.COLUMN, top, height: bottom - top } });
		}
	}

	// the place of the tree in the sheet of a page, with the height of a row on screen: the sheet becomes a card with a
	// header for the kind and the name, and the tree fills the room below it
	pagePanel(page, tree, k, radius = this.view.shownRadius(page)) {

		// the sheet exactly as the element is drawn, at the current zoom or, for a glide, at the one it heads for
		const r      = Math.max(radius, 2 / k);
		const hw     = r * AppMapRenderer.PAGE_BOX[0];
		const hh     = r * AppMapRenderer.PAGE_BOX[1];
		const head   = AppMapPanels.header(page.x - hw, page.y - hh, 2 * hw, 2 * hh);
		const room   = [2 * hw - 2 * head.pad, page.y + hh - head.bottom - head.pad];
		const scale  = Math.min(room[0] / tree.panel.width, room[1] / tree.panel.height);
		const width  = tree.panel.width * scale;
		const height = tree.panel.height * scale;

		return { x0: page.x - width / 2, y0: head.bottom + (room[1] - height) / 2, width, height, scale, top: tree.panel.top, hw, hh, head, row: scale * AppMapPanels.ROW * k };
	}

	// the header of a card at the given place and size: a small line for the kind, the name below it and a divider, with
	// the padding of the card; the name gets its size from the height of the card and shrinks to fit its width (see
	// drawHeader). Bottom is where the content below the divider begins
	static header(x0, y0, width, height) {

		const pad  = Math.min(width, height) * AppMapPanels.CARD_PAD;
		const kind = height * AppMapPanels.CARD_KIND;
		const name = height * AppMapPanels.CARD_NAME;
		const line = y0 + pad + kind * 1.6 + name * 1.4;

		return { x0, y0, width, height, pad, kind, name, kindY: y0 + pad + kind * 0.5, nameY: y0 + pad + kind * 1.6 + name * 0.7, line, bottom: line + pad * 0.8 };
	}

	// the frame is the element itself: its sheet, open, as a card on the map, white with a soft shadow, rounded, and a fine
	// border in its colour; the card covers what lies beneath by the given share
	drawFrame(ctx, page, panel, colour, k, cover) {

		this.drawCard(ctx, page.x - panel.hw, page.y - panel.hh, 2 * panel.hw, 2 * panel.hh, colour, k, cover);
	}

	drawCard(ctx, x, y, width, height, colour, k, cover, radius = Math.min(width, height) * 0.035) {

		const look   = this.view.look;

		ctx.save();
		ctx.beginPath();
		ctx.roundRect(x, y, width, height, radius);

		// the shadow is given in pixels on the screen, whatever the zoom
		ctx.globalAlpha   = cover;
		ctx.shadowColor   = look.dark ? 'rgba(0, 0, 0, 0.55)' : 'rgba(16, 24, 40, 0.14)';
		ctx.shadowBlur    = 28;
		ctx.shadowOffsetY = 6;
		ctx.fillStyle     = look.card;
		ctx.fill();
		ctx.restore();

		ctx.save();
		ctx.beginPath();
		ctx.roundRect(x, y, width, height, radius);
		ctx.globalAlpha = Math.min(1, cover * 1.2);
		ctx.strokeStyle = d3.color(colour).copy({ opacity: 0.45 }).formatRgb();
		ctx.lineWidth   = 1.2 / k;
		ctx.stroke();
		ctx.restore();
	}

	// the header of a card: the kind in small capitals with a dot in the colour of the element, the name below it as large
	// as the header allows and no wider than the card, cut with an ellipsis only where even half that size is too wide
	drawHeader(renderer, head, kind, name, colour, k) {

		const ctx   = renderer.ctx;
		const look  = this.view.look;
		const left  = head.x0 + head.pad;
		const room  = head.width - 2 * head.pad;
		const fit   = AppMapPanels.fitText(renderer, name, 600, head.name, room);
		const caps  = kind.toUpperCase();

		ctx.fillStyle = colour;
		ctx.beginPath();
		ctx.arc(left + head.kind * 0.35, head.kindY, head.kind * 0.35, 0, Math.PI * 2);
		ctx.fill();

		renderer.screenText(caps, left + head.kind * 1.1, head.kindY, 600, head.kind * 0.85, false, 'left');

		ctx.fillStyle = look.labelFocus;
		renderer.screenText(fit.text, left, head.nameY, 600, fit.size, false, 'left');

		if (head.line == null) {
			return;
		}

		ctx.strokeStyle = look.dark ? 'rgba(255, 255, 255, 0.10)' : 'rgba(16, 24, 40, 0.08)';
		ctx.lineWidth   = 1 / k;
		ctx.beginPath();
		ctx.moveTo(head.x0, head.line);
		ctx.lineTo(head.x0 + head.width, head.line);
		ctx.stroke();
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

	// branches from the right end of a parent to the left end of each child, as the tidy trees of d3 draw them: each an
	// S-curve that leaves the parent and reaches the child level, so the children of one parent fan out from it; all in
	// one path
	static branches(tree, panel) {

		const s    = panel.scale;
		const path = new Path2D();

		for (const d of tree.list) {

			const [px, py] = AppMapPanels.box(d.parent, panel);
			const [cx, cy] = AppMapPanels.box(d, panel);
			const start    = px + (d.parent === tree.root ? 4 * s : AppMapPanels.boxWidth(d.parent, s));
			const middle   = (start + cx) / 2;

			path.moveTo(start, py);
			path.bezierCurveTo(middle, py, middle, cy, cx, cy);
		}

		return path;
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

	// whether an HTML element stands upright as a sheet: a template, or a script or a style with code in it; the document
	// at the root never is
	static isSheet(d) {

		return !!d.parent && (d.data.tag === 'template' || (!!AppMapPanels.SCRIPTS[d.data.tag] && d.data.text));
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

		return !!(data.repeater || data.action || data.text || data.when || data.tag === 'template');
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
		const kind   = unit * 0.05;
		const name   = unit * 0.085;
		const bottom = top + pad + kind * 1.6 + name * 1.4 + pad * 0.6;
		// the controls keep this far from the sides of the box, clear of the bar on its left
		const inset  = unit * 0.14;
		const room   = [width - 2 * inset, top + height - pad - bottom];
		const [W, H] = sheet ? AppMapPanels.SHEET_SIZE : AppMapPanels.ELEMENT_SIZE;
		const fit    = Math.min(room[0] * k / W, room[1] * k / H);
		const area   = { x: x + inset, y: bottom, width: room[0], height: room[1] };
		const head   = { x0: x + inset - pad, y0: top, width: width - 2 * (inset - pad), height, pad, kind, name, kindY: top + pad + kind * 0.5, nameY: top + pad + kind * 1.6 + name * 0.7, line: null, bottom };

		return { x, y: top, width, height, head, area, fit, px: unit * k, scale: s };
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
		const title    = h * 0.2;
		const lines    = this.code.get(n)?.lines ?? [];
		const columns  = Math.min(AppMapPanels.CODE_COLUMNS, Math.max(40, d3.max(lines, l => l.length) ?? 0));
		const inner    = [2 * w - 2 * pad, 2 * h - 2 * pad - title];

		// a box higher than wide, as the sheet of a page, holds more than CODE_LINES at the font its width allows
		const fit      = Math.floor(inner[1] / (inner[0] / (columns * 0.6) * 1.3));
		const rows     = Math.min(Math.max(AppMapPanels.CODE_LINES[1], fit), Math.max(AppMapPanels.CODE_LINES[0], lines.length));
		const font     = Math.min(inner[0] / (columns * 0.6), inner[1] / (rows * 1.3));

		return { x0: n.x - w, y0: n.y - h, w, h, pad, title, font, line: font * 1.3, rows, px: font * 1.3 * k };
	}

	// the field of a code panel the code stands on, below the header, as x, y, width and height on the map
	static codeField(panel) {

		const top = panel.y0 + panel.pad + panel.title;

		return [panel.x0 + panel.pad * 0.6, top - panel.pad * 0.3, 2 * panel.w - panel.pad * 1.2, panel.y0 + 2 * panel.h - top - panel.pad * 0.3];
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
			ctx.roundRect(panel.x0, panel.y0, 2 * panel.w, 2 * panel.h, Math.min(panel.w, panel.h) * 0.07);
			ctx.clip();

			if (panel.px >= AppMapPanels.CODE_READ) {

				// the header of a page card in the title band, and the code on a field of its own below it
				const head = { x0: panel.x0, y0: panel.y0, width: 2 * panel.w, height: 2 * panel.h, pad: panel.pad, kind: panel.title * 0.22, name: panel.title * 0.4, kindY: panel.y0 + panel.pad + panel.title * 0.11, nameY: panel.y0 + panel.pad + panel.title * 0.58, line: null };

				this.reading.add(n);
				this.drawHeader(renderer, head, AppMapModel.KIND_LABEL[n.k], n.n, colour, k);

				const [fx, fy, fw, fh] = AppMapPanels.codeField(panel);

				ctx.beginPath();
				ctx.roundRect(fx, fy, fw, fh, panel.pad * 0.3);
				ctx.fillStyle   = look.dark ? 'rgba(255, 255, 255, 0.05)' : '#f4f6f8';
				ctx.strokeStyle = look.dark ? 'rgba(255, 255, 255, 0.12)' : '#e3e7ec';
				ctx.lineWidth   = 1 / k;
				ctx.fill();
				ctx.stroke();

				// the code being edited stands in the editor above the field
				if (this.view.editing === n) {

					ctx.restore();
					continue;
				}

				ctx.fillStyle = look.labelFocus;

				for (let i = 0; i < shown; i++) {

					const text = lines[i].length > AppMapPanels.CODE_COLUMNS ? lines[i].slice(0, AppMapPanels.CODE_COLUMNS - 1) + '…' : lines[i];

					renderer.screenText(text, left, top + (i + 0.5) * panel.line, 400, panel.font, true, 'left');
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

		const codePanels = this.codePanels(renderer, k);
		const panels     = [...this.trees].map(([page, tree]) => [page, tree, this.onScreen(renderer, page, k, 2000) ? this.pagePanel(page, tree, k) : null]);
		const near       = Math.max(
			d3.max(panels, ([, , panel]) => panel ? Math.min(1, Math.max(0, (panel.row - AppMapPanels.SKETCH_ROW) / (AppMapPanels.OPEN_ROW - AppMapPanels.SKETCH_ROW))) : 0) ?? 0,
			d3.max(codePanels, c => c.strong) ?? 0
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
		this.drawHeader(renderer, panel.head, AppMapModel.KIND_LABEL[page.k], page.n, colour, k);

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
				continue;
			}

			// what an element without children shows, in the free space behind it, as long as the elements are too small to
			// open; closer up the text would grow far beyond the screen, also from elements beside it
			if (d.data.shows && !d.children && boxH * k < AppMapPanels.ELEMENT_SKETCH) {
				const end = Math.min(x + d.room * s - 1.6 * s, page.x + panel.hw - 1.6 * s);

				this.drawShows(renderer, d.data.shows, x + w + 0.8 * s, y, end - x - w - 0.8 * s, s);
			}
		}

		tree.frame = [page.x - panel.hw, page.y - panel.hh, 2 * panel.hw, 2 * panel.hh];
		ctx.restore();
	}

	// the box of an HTML element: a plain element a white box with a fine grey edge, one that does something with an edge and
	// a bar on its left in the colour of what it does, filled with it when it is pointed at or held; a repeater on a stack of
	// the copies it makes. Inside, the sign of what it does, its tag and a magnifier when it holds more; as the element opens
	// (change from 0 to 1), they give way to its header and controls, see drawElement
	drawPill(renderer, d, x, y, w, s, k, lit, change) {

		const ctx    = renderer.ctx;
		const look   = this.view.look;
		const tone   = this.toneOf(d);
		const plain  = !d.data.action && !d.data.repeater && !d.data.active;
		const boxH   = AppMapPanels.BOX * s;
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

		ctx.fillStyle = look.card;
		ctx.fill(shape);

		if (lit) {

			ctx.fillStyle = tone;
			ctx.fill(shape);

		} else if (plain) {

			ctx.fillStyle = d3.color(tone).copy({ opacity: look.dark ? 0.12 : 0.05 }).formatRgb();
			ctx.fill(shape);
		}

		// the bar on the left, inside the edge of the box
		if (!plain) {

			ctx.save();
			ctx.clip(shape);
			ctx.fillStyle = tone;
			ctx.fillRect(x, y - r, boxH * AppMapPanels.ELEMENT_BAR, height);
			ctx.restore();
		}

		ctx.strokeStyle = d3.color(tone).copy({ opacity: plain ? 0.35 : 0.7 }).formatRgb();
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

			this.drawSheetSketch(renderer, d, x, y - r, w, height, s, lit, 1 - 2 * change);

		} else if (change < 0.5) {

			const ink  = lit ? look.card : tone;
			const sign = d.data.action || d.data.repeater;
			const left = x + (sign ? 2 : 0.8) * s;
			const fit  = AppMapPanels.fitText(renderer, d.data.tag, plain ? 500 : 600, 1.05 * s, x + w - 0.8 * s - left);

			ctx.globalAlpha = 1 - 2 * change;

			if (d.data.action) {
				this.drawFlash(ctx, x + 1.1 * s, y, s, ink);
			} else if (d.data.repeater) {
				this.drawDatabase(ctx, x + 1.1 * s, y, s * 0.8, ink);
			}

			ctx.fillStyle = lit ? look.card : (plain ? look.labelNear : look.labelFocus);
			renderer.screenText(fit.text, left, y, plain ? 500 : 600, fit.size, false, 'left');

			// a magnifier says that it holds more, closer up
			if (AppMapPanels.hasInside(d)) {
				this.drawZoomSign(ctx, x + w - 1.05 * s, y, s, d3.color(lit ? look.card : look.trace).copy({ opacity: 0.85 }).formatRgb());
			}
		}

		ctx.restore();
	}

	// a closed template: its tag at the top as the sheet of a page has its name, lines of text below it, and the magnifier
	// at the bottom
	drawSheetSketch(renderer, d, x, top, w, height, s, lit, alpha) {

		const ctx  = renderer.ctx;
		const look = this.view.look;
		const left = x + 0.8 * s;
		const fit  = AppMapPanels.fitText(renderer, d.data.tag, 600, 1.05 * s, w - 1.6 * s - AppMapPanels.foldOf(w));
		const ink  = d3.color(lit ? look.card : look.trace).copy({ opacity: 0.5 }).formatRgb();

		ctx.save();
		ctx.globalAlpha = alpha;
		ctx.fillStyle   = lit ? look.card : look.labelFocus;
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
		this.drawZoomSign(ctx, x + w - 1.05 * s, top + height - 0.9 * s, s, d3.color(lit ? look.card : look.trace).copy({ opacity: 0.85 }).formatRgb());
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

		const kind = d.data.repeater ? 'Repeater' : (d.data.action ? 'Event action' : ({ template: 'Template', script: 'Script', style: 'Style' }[d.data.tag] ?? 'HTML element'));

		ctx.globalAlpha = Math.min(1, (change - 0.5) * 2);
		this.drawHeader(renderer, panel.head, kind, `<${d.data.tag}>${d.data.key ? ' ' + d.data.key : ''}`, colour, k);

		// the controls come with the header, growing and fading in with it, and go with it the same way
		d.opened = { ...panel, change };

		// a template shows its content as lines of text until it fills the screen, where its editor takes over
		if (AppMapPanels.isSheet(d) && !this.view.showsControls(d)) {

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
		}

		ctx.restore();
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

	// the template expressions of an element in code type, cut to the given width
	drawShows(renderer, text, x, y, width, s) {

		const size  = 0.9 * s;
		const fit   = Math.floor(width / (size * AppMapPanels.CODE_ADVANCE));

		if (fit < 4) {
			return;
		}

		renderer.ctx.fillStyle = this.view.look.labelNear;
		renderer.screenText(text.length > fit ? text.slice(0, fit - 1) + '…' : text, x, y, 400, size, true, 'left');
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

			// a relation of an element never runs back across the tree: where its way would leave the sheet behind its box,
			// it leaves on the right at the height of its box and turns outside; only those of the page as a whole leave toward any side
			const from = AppMapPanels.origin(d, panel);
			let   way  = AppMapPanels.crossing(page, panel, other);

			if (d !== tree.root && (way.out[0] < 0 || (way.out[1] && page.x + way.along < from.x))) {
				way = { out: [1, 0], along: from.y - page.y };
			}

			const key = way.out.join();

			if (!gates.has(key)) {
				gates.set(key, { out: way.out, along: [], ahead: -Infinity, lanes: new Map() });
			}

			const gate = gates.get(key);

			gate.along.push(way.along);
			gate.ahead = Math.max(gate.ahead, from.x - page.x);

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
			// a gate above or below lies no further left than the boxes whose relations leave through it
			const mean   = out[1] ? Math.max(d3.mean(gate.along), gate.ahead) : d3.mean(gate.along);
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

					// every line leaves its box to the right, as the tree reads, and turns where its gate lies elsewhere
					const ahead = Math.max(Math.abs(at.x - from.x) * 0.4, 6 * panel.scale);

					path.moveTo(from.x, from.y);
					path.bezierCurveTo(from.x + ahead, from.y, at.x - out[0] * back, at.y - out[1] * back, at.x, at.y);

					ctx.globalAlpha = alpha(lit);
					ctx.lineWidth   = width(lit, 1);
					ctx.stroke(path);
				}

				// beyond the edge one line to each element, the stronger the more it carries; one to an element behind the
				// right edge first goes on to the right and turns around the sheet
				for (const [other, line] of lane.lines) {

					ctx.globalAlpha = alpha(line.lit);
					ctx.lineWidth   = width(line.lit, line.count);
					ctx.stroke(out[0] > 0 && other.x < at.x ? AppMapPanels.turn(at, other, page, panel, k) : AppMapRenderer.arc(at, other, 0.12));
				}
			});
		}

		ctx.globalAlpha = 1;
	}

	// from a gate on the right edge of an open page to an element behind it: out to the right, turning up or down toward
	// the element, along the edge of the sheet, past its corner when the element lies beside the sheet, and on to the
	// element; the way keeps clear of the sheet by some pixels whatever the zoom
	static turn(at, other, page, panel, k) {

		const path  = new Path2D();
		const room  = Math.max(Math.min(panel.hw, panel.hh) * 0.08, 24 / k);
		const lane  = page.x + panel.hw + room;
		const up    = other.y < at.y;
		const sign  = up ? -1 : 1;
		const turn  = { x: lane, y: at.y + sign * room };
		// beside the sheet the way goes past its corner first
		const past  = up ? Math.min(other.y, page.y - panel.hh - room) : Math.max(other.y, page.y + panel.hh + room);

		path.moveTo(at.x, at.y);
		path.quadraticCurveTo(lane, at.y, turn.x, turn.y);
		path.lineTo(lane, past - sign * room);
		path.bezierCurveTo(lane, past + sign * room * 0.5, lane - room, past, Math.max(other.x, lane - 2 * room), past);
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

		return { x: x + (d.parent ? AppMapPanels.boxWidth(d, panel.scale) : 3.8 * panel.scale) / 2, y };
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
