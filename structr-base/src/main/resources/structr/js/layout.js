/*
 * Copyright (C) 2010-2024 Structr GmbH
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
let _Layout = {

	timeout: -1,
	leftOffset: 25,
	topOffset: 131,

	doLayout: function(nodes, rels) {

		_Layout.timeout = window.setInterval(function() {

			_Layout.doForceLayout(nodes, rels, 0.01);
			_Schema.ui.jsPlumbInstance.repaintEverything();

		}, 10);
	},
	stopLayout: function() {
		window.clearInterval(_Layout.timeout);
	},
	doForceLayout: function(nodes, rels, dt) {

		var positions     = new Array();
		var springForce   = 200.0;
		var springDamping = 200.0;
		var count         = nodes.length;
		var i, j, stop    = true;

		$.each(nodes, function(i, node) {
			positions[i] = _Layout.getPosition(node);
		});

		for (i=0; i<count; i++) {

			for(j=0; j<count; j++) {

				var pos1 = positions[i];
				var pos2 = positions[j];

				var dx  = pos1.x - pos2.x;
				var dy  = pos1.y - pos2.y;
				var dl  = Math.sqrt(dx*dx + dy*dy);
				var len = 300.0 - dl;
				var dFx = 0.0;
				var dFy = 0.0;

				// "pulling" spring force (linear)
				if (_Layout.hasRelationship(pos1, pos2, rels)) {

					dFx = dx * len * springForce * (1.0 / springDamping);
					dFy = dy * len * springForce * (1.0 / springDamping);
				}

				// "pushing" magnetic force
				if (dl > 0.0) {

					dFx += dx * (1.0 / (dl * dl)) * 1000000.0;
					dFy += dy * (1.0 / (dl * dl)) * 1000000.0;
				}

				if (dFx > 1000000) {
					dFx = 1000000;
				}
				if (dFy > 1000000) {
					dFy = 1000000;
				}

				if (dFx < 12000 && dFy < 12000) {
					stop &= true;
				} else {
					stop = false;
				}

				// apply forces
				pos1.Fx += dFx;
				pos1.Fy += dFy;

				pos2.Fx -= dFx;
				pos2.Fy -= dFy;
			}
		}

		for (i=0; i<count; i++) {
			_Layout.update(positions[i], dt, 0.01);
		}

		$.each(nodes, function(i, node) {
			_Layout.setPosition(node, positions[i]);
		});

		if (stop) {
			_Layout.stopLayout();
		}
	},
	update: function(pos, dt, drag) {

		var tmpX = pos.x;
		var tmpY = pos.y;

		var dt2 = (dt * dt);

		// drag
		pos.Fx -= (pos.x - pos.x0) * drag;
		pos.Fy -= (pos.y - pos.y0) * drag;

		pos.x += (pos.x - pos.x0) + (pos.Fx * dt2);
		pos.y += (pos.y - pos.y0) + (pos.Fy * dt2);

		if (pos.x < _Layout.leftOffset) {
			pos.x = _Layout.leftOffset;
		}

		if (pos.y < _Layout.topOffset) {
			pos.y = _Layout.topOffset;
		}

		pos.x0 = tmpX;
		pos.y0 = tmpY;

	},
	setPosition: function(node, pos) {

		node.offset({
			left: pos.x,
			top: pos.y
		});

		pos.Fx = 0.0;
		pos.Fy = 0.0;
	},
	getPosition: function(node) {

		var offset = node.offset();
		return {
			x: offset.left,
			y: offset.top,
			x0: offset.left,
			y0: offset.top,
			Fx: 0.0,
			Fy: 0.0,
			id: node[0].id
		};
	},
	sub: function(pos1, pos2) {

		return {
			x: pos1.x - pos2.x,
			y: pos1.y - pos2.y
		};
	},
	add: function(pos1, pos2) {

		return {
			x: pos1.x + pos2.x,
			y: pos1.y + pos2.y
		};
	},
	mult: function(pos, f) {

		return {
			x: pos.x * f,
			y: pos.y * f
		};
	},
	hasRelationship: function(pos1, pos2, rels) {

		var ret = false;

		$.each(rels, function(i, rel) {

			if (rel.source.id === pos1.id && rel.target.id === pos2.id) {
				ret |= true;
			}
		});

		return ret;
	}
};
