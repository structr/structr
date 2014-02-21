/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

var hoverMass = null;
var clickedMass = null;
var springs = new Array();
var masses = new Array();
var xCenters = new Array();
var yCenters = new Array();
var ctx = null;
var colors = new Array();
var types = new Array();
var inc = 128;
var count = 0;
var running = null;
var dt = 0.05;
var drag = 50;
var springLength = 20;
var springForce = 500;
var springDamping = 100;
var enableSprings = true;
var centerForce = 10;
var maxMagneticDistance = 1000000;
var magneticForce = 50000;
var magneticFactor = 0.5;
var numMasses = 100;
var keys = new Array();
var mX = 0;
var mY = 0;
var mouseBtn = 0;
var scale = 1;
var lastX = 0;
var lastY = 0;
var backgroundColor = "rgb(255, 255, 255)";
var alpha = 0.8;
var textAlpha = 0.8;
var headerWidth = 0;
var headerHeight = 0;
var canvas;
var settings;
var relBox;
var relationshipTypes = {"OWNS": 0, "SECURITY": 0, "PAGE": 0, "undefined": 0};
var originX;
var originY;
var maxDepth = 2;

function Engine(parent) {

	this.parent = parent;
	canvas = $("<canvas style='cursor:default;'>")[0];
	$(parent).append(canvas);

	parent.append('<div id="settings"><table>'
		+ '<tr><td><label for="maxDepth">Max. depth</label></td><td><input id="maxDepth" name="maxDepth" size="3" value="' + maxDepth + '"></td></tr>'
		+ '<tr><td><label for="maxRels">Max. rels</label></td><td><input id="maxRels" name="maxRels" size="3" value="' + maxRels + '"></td></tr>'
		+ '<tr><td><label for="noOfNodes">Nodes</label></td><td><input id="noOfNodes" name="noOfNodes" size="3"></td></tr>'
		+ '<tr><td><label for="noOfRels">Rels</label></td><td><input id="noOfRels" name="noOfRels" size="3"></td></tr>'
		+ '<tr><td><label for="noOfRels">Enable Relationships</label></td><td><input type="checkbox" id="enableSprings" name="enableSprings" checked="checked" /></td></tr>'
		+ '<tr><td colspan="2" class="sliderLabel"><label for="relLength">Rel Length</label></td></tr>'
		+ '<tr><td colspan="2"><div id="relLength"></div></td></tr>'
		+ '<tr><td colspan="2" class="sliderLabel"><label for="centerForce">Center Force</label></td></tr>'
		+ '<tr><td colspan="2"><div id="centerForce"></div></td></tr>'
		+ '<tr><td colspan="2" class="sliderLabel"><label for="centerForce">Repellant Force</label></td></tr>'
		+ '<tr><td colspan="2"><div id="repellantForce"></div></td></tr>'
		+ '<tr><td colspan="2" class="sliderLabel"><label for="drag">Drag</label></td></tr>'
		+ '<tr><td colspan="2"><div id="drag"></div></td></tr>'
		+ '</table>'
		+ '<h3>Relationship Types</h3>'
		+ '<ul class="relBox"></ul>'
		+ '</div>');

	this.initialize = function() {
		
		colors[ 0] = "#cc0000";	// red
		colors[ 1] = "#00cc00";	// green
		colors[ 2] = "#0000cc";	// blue
		colors[ 3] = "#cc00cc";	// pink
		colors[ 4] = "#ff8800";	// orange
		colors[ 5] = "#00dddd";	// cyan
		colors[ 6] = "#ffff00";	// yellow
		
		colors[ 7] = "#880000";	// red
		colors[ 8] = "#008800";	// green
		colors[ 9] = "#000088";	// blue
		colors[10] = "#880088";	// pink
		colors[11] = "#884400";	// orange
		colors[12] = "#008888";	// cyan
		colors[13] = "#888800";	// yellow
		
		colors[14] = "#440000";	// red
		colors[15] = "#004400";	// green
		colors[16] = "#000044";	// blue
		colors[17] = "#440044";	// pink
		colors[18] = "#442200";	// orange
		colors[19] = "#004444";	// cyan
		colors[20] = "#444400";	// yellow

		for (i=21; i<255; i++) {
			colors[i] = "#999999";	// default gray
		}

		relBox = $('.relBox');

		relBox.append('<li><input type="checkbox" id="toggle_undefined">undefined</li>');
		relBox.append('<li><input type="checkbox" id="toggle_OWNS">OWNS</li>');
		relBox.append('<li><input type="checkbox" id="toggle_SECURITY">SECURITY</li>');
		relBox.append('<li><input type="checkbox" id="toggle_PAGE">PAGE</li>');

		$('input#maxDepth').on('change', function() {
			var inp = $(this);
			_Dashboard.maxDepth = inp.val();
		});

		$('input#relLength').on('change', function() {
			var inp = $(this);
			springLength = inp.val();
		});

		$('input#enableSprings').on('click', function() {
			var inp = $(this);
			enableSprings = inp.is(":checked");

			if (enableSprings === true) {

				$("#centerForce").slider('value', 5);
				$("#repellantForce").slider('value', 1);

			} else {

				$("#centerForce").slider('value', 20);
				$("#repellantForce").slider('value', 0.05);
			}
		});

		$(function() {
			$("#enableSprings").checked = enableSprings;
		});

		$(function() {
			$("#relLength").slider({
				min: -500,
				max: 500,
				value: springLength,
				slide: function(event, ui) {
					springLength = ui.value;
				},
				change: function(event, ui) {
					springLength = ui.value;
				}
			});
		});

		$(function() {
			$("#repellantForce").slider({
				min: 0.05,
				max: 2,
				step: 0.05,
				value: magneticFactor,
				slide: function(event, ui) {
					magneticFactor = ui.value;
				},
				change: function(event, ui) {
					magneticFactor = ui.value;
				}
			});
		});

		$(function() {
			$("#centerForce").slider({
				min: 0,
				max: 100,
				value: centerForce,
				slide: function(event, ui) {
					centerForce = ui.value;
				},
				change: function(event, ui) {
					centerForce = ui.value;
				}
			});
		});

		$(function() {
			$("#drag").slider({
				min: 50,
				max: 500,
				value: drag,
				slide: function(event, ui) {
					drag = ui.value;
				},
				change: function(event, ui) {
					drag = ui.value;
				}
			});
		});

		this.resizeCanvas();

		$(window).resize(function() {
			this.resizeCanvas();
		});

		originX = -canvas.width / 2 - headerWidth;
		originY = -canvas.height / 2 - headerHeight;

		ctx = canvas.getContext("2d");

		canvas.onmousemove = function(e) {

			lastX = mX;
			lastY = mY;

			mX = (e.x / scale);
			mY = (e.y / scale);

			// left button
			if (mouseBtn === 1) {

				if (clickedMass === null) {

					var offsetX = ((mX - lastX));
					var offsetY = ((mY - lastY));

					ctx.translate(offsetX, offsetY);

					originX -= offsetX;
					originY -= offsetY;

				} else {

					clickedMass.x = mX + originX;
					clickedMass.y = mY + originY;

				}
			}
		}

		canvas.onmousedown = function(e) {
			mouseBtn = e.button + 1;

			lastX = mX;
			lastY = mY;
		}

		canvas.onmouseup = function(e) {
			mouseBtn = 0;
			clickedMass = null;

			lastX = mX;
			lastY = mY;
		}

		canvas.onmousewheel = function(e) {

			var zoom = 1 + (e.wheelDelta / 1000);

			ctx.translate(originX, originY);
			ctx.scale(zoom, zoom);

			originX = (e.x / scale + originX - e.x / (scale * zoom));
			originY = (e.y / scale + originY - e.y / (scale * zoom));
			ctx.translate(-originX, -originY);

			scale *= zoom;

			mX = (e.x / scale);
			mY = (e.y / scale);

			$('#noOfRels').val(scale);

		}

		canvas.onblur = function() {
			clickedMass = null;
			mouseBtn = 0;
		}

		$(canvas).on('dblclick', function() {
			
			if (hoverMass) {
				
				_Dashboard.loadRelationships(hoverMass.nodeId, 0);
			}
		});

		document.onkeydown = function(e) {
			keys[e.keyCode] = 1;
		}

		document.onkeyup = function(e) {
			keys[e.keyCode] = 0;
		}

		// center canvas on 0:0
		ctx.translate(canvas.width / 2, canvas.height / 2);

		// initialize centers
		this.setCenters(canvas.width / 2, canvas.height / 2);
	}

	this.update = function() {

		var springNum = springs.length;
		var massNum = masses.length;
		var i = 0;
		var j = 0;

		ctx.save();
		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.fillStyle = backgroundColor;
		ctx.globalAlpha = 1;
		ctx.fillRect(0, 0, canvas.width, canvas.height);
		ctx.restore();

		// apply magnetic force
		for (i = 0; i < massNum; i++) {

			for (j = i + 1; j < massNum; j++) {

				var m1 = masses[i];
				var m2 = masses[j];

				var f = magneticForce;

				if (m1.type === m2.type) {
					f *= magneticFactor;
				}

				if (m1.selected === true && m2.selected === true) {
					f *= magneticFactor * magneticFactor;
				}

				var dx = (m1.x - m2.x);
				var dy = (m1.y - m2.y);

				var l2 = (dx * dx + dy * dy);

				if (l2 < maxMagneticDistance) {

					var l = Math.sqrt(l2);

					// magnetic force
					if (l > dt) {

						var dFx = dx * (1 / (l * l)) * f;
						var dFy = dy * (1 / (l * l)) * f;

						m1.Fx += dFx;
						m1.Fy += dFy;

						m2.Fx -= dFx;
						m2.Fy -= dFy;
					}
				}
			}
		}

		// update springs
		for (i = 0; i < springNum; i++) {
			var obj = springs[i];
			obj.update();
			obj.draw();
		}

		// update position & reset forces
		for (i = 0; i < massNum; i++) {
			var obj = masses[i];
			obj.update();
			obj.draw();
			obj.Fx = 0;
			obj.Fy = 0;
		}

		window.setTimeout("engine.update()", dt);
	}

	this.addNode = function(node) {

		if (!isIn(node.type, types)) {

			types.push(node.type);
			this.setCenters(100, 100);

		}

		new Mass(node, 0, 0);
		
		$('#noOfNodes').val(masses.length);
	}

	this.addRelationship = function(type, startNodeId, endNodeId) {

		if (relationshipTypes[type] === undefined) {

			relationshipTypes[type] = 1;

			relBox.append('<li><input type="checkbox" id="toggle_' + type + '" ' + (relationshipTypes[type] === 1 ? 'checked="checked"' : '') + '>' + type + '</li>');
		}

		$('#toggle_' + type).change(function() {

			relationshipTypes[type] = $(this).is(":checked") ? 1 : 0;
		});

		new Spring(type, startNodeId, endNodeId);
	}

	this.toggleCenterForce = function() {
		if (centerForce > 0) {
			centerForce = 0;
		} else {
			centerForce = 20;
		}
	}

	this.setCenter = function(type, x, y) {
		xCenters[type] = x;
		yCenters[type] = y;
	}

	this.setCenters = function(rx, ry) {

		var i = 0.0;
		var rad = (2 * Math.PI) / Math.max(1, types.length);
		for (i = 0.0; i < types.length; i += 1.0) {

			var cx = (Math.cos(i * rad) * rx);
			var cy = (Math.sin(i * rad) * ry);

			this.setCenter(types[i], cx, cy);
		}
	}

	this.resizeCanvas = function resizeCanvas() {

		canvas.width = $(window).width() - 26;
		canvas.height = $(window).height() - $('#header').height() - 48;
		headerWidth = 12;
		headerHeight = $('#header').height() + 12;
	}

	this.drawText = function(text, x, y, font) {

		if (text && x && y) {

			ctx.font = font;
			ctx.textAlign = "center";
			ctx.textBaseline = "bottom";
			ctx.globalAlpha = textAlpha;
			ctx.fillStyle = "#000000";
			ctx.strokeStyle = "#000000";
			ctx.fillText(text, x, y);
		}
	}
}

/******************************************************************************
 * A mass object.
 */
function Mass(node, startX, startY) {

	masses[masses.length] = this;
	this.nodeId = node.id;
	this.node = node;
	this.color = colors[types.indexOf(node.type)];
	this.typeString = node.type;
	this.nameString = node.name;
	this.x = startX;
	this.y = startY;
	this.x0 = this.x;
	this.y0 = this.y;
	this.Fx = 0;
	this.Fy = 0;
	this.r = 10;

	this.draw = function() {

		var fillStyle = this.color;
		var localAlpha = alpha;

		// highlight mass element under cursor
		if (Math.abs(this.x - (mX + originX)) <= this.r && Math.abs(this.y - (mY + originY)) <= this.r) {

			if (mouseBtn > 0) {

				clickedMass = this;
			}

			hoverMass = this;

			// fillStyle = "#ff0000";
			this.r = 20;
			localAlpha = 1;

		} else {

			if (!(clickedMass === this)) {
				this.r = 10;
			}
		}

		if (scale < 1) {
			this.r /= (Math.sqrt(scale));
			ctx.lineWidth = 1 / (Math.sqrt(scale));
		}

		ctx.globalAlpha = localAlpha;
		ctx.fillStyle = fillStyle;
		ctx.strokeStyle = '#000000';

		this.roundRect(this.x - this.r, this.y - this.r, this.r * 2, this.r * 2, 3 / Math.sqrt(scale));

		var name = this.nameString !== null ? this.nameString + ":" + this.typeString : this.typeString;
		engine.drawText(name, this.x, this.y - 10, "4px sans-serif");

		ctx.lineWidth = 1;
	}

	this.update = function() {

		var tmpX = this.x;
		var tmpY = this.y;

		// pull towards the center of the screen
		if (this !== clickedMass) {

			this.Fx += (xCenters[this.typeString] - this.x) * centerForce;
			this.Fy += (yCenters[this.typeString] - this.y) * centerForce;

			var dt2 = (dt * dt);

			// drag
			this.Fx -= (this.x - this.x0) * drag;
			this.Fy -= (this.y - this.y0) * drag;

			this.x += (this.x - this.x0) + (this.Fx * dt2);
			this.y += (this.y - this.y0) + (this.Fy * dt2);
		}

		this.x0 = tmpX;
		this.y0 = tmpY;
	}

	this.isInside = function(x, y, w, h) {

		var inside = true;
		var d = 5;

		if (w > 0) {
			inside &= (this.x + d >= x && this.x + d <= x + w);
		} else {
			inside &= (this.x + d <= x && this.x + d >= x + w);
		}

		if (h > 0) {
			inside &= (this.y + d >= y && this.y + d <= y + h);
		} else {
			inside &= (this.y + d <= y && this.y + d >= y + h);
		}

		return inside;
	}

	this.roundRect = function(x, y, w, h, r) {

		if (w < 2 * r)
			r = w / 2;
		if (h < 2 * r)
			r = h / 2;

		ctx.beginPath();
		ctx.moveTo(x + r, y);
		ctx.arcTo(x + w, y, x + w, y + h, r);
		ctx.arcTo(x + w, y + h, x, y + h, r);
		ctx.arcTo(x, y + h, x, y, r);
		ctx.arcTo(x, y, x + w, y, r);
		ctx.closePath();
		ctx.stroke();
		ctx.fill();

		return this;
	}
}

function Spring(typeString, startNodeId, endNodeId) {

	springs[springs.length] = this;
	this.typeString = typeString;
	this.startNodeId = startNodeId;
	this.endNodeId = endNodeId;
	this.m1 = null;
	this.m2 = null;

	this.update = function() {

		if (this.m1 && this.m2) {

			if (enableSprings && relationshipTypes[this.typeString] === 1) {

				var dx = (this.m1.x - this.m2.x);
				var dy = (this.m1.y - this.m2.y);

				var l = (Math.sqrt(dx * dx + dy * dy));

				// spring force
				var dl = springLength - l;

				var dF1x = dx * dl * springForce * (1 / springDamping) * dt * dt;
				var dF1y = dy * dl * springForce * (1 / springDamping) * dt * dt;

				if (dF1x > 100000 || dF1y > 100000) {
					dF1x *= 0.001;
					dF1y *= 0.001;
				}

				if (dF1x > 10000 || dF1y > 10000) {
					dF1x *= 0.01;
					dF1y *= 0.01;
				}

				if (dF1x > 1000 || dF1y > 1000) {
					dF1x *= 0.1;
					dF1y *= 0.1;
				}

				// apply forces
				this.m1.Fx += dF1x;
				this.m1.Fy += dF1y;

				this.m2.Fx -= dF1x;
				this.m2.Fy -= dF1y;
			}

		} else {

			for (i = 0; i < masses.length; i++) {
				if (masses[i].nodeId === startNodeId) {
					this.m1 = masses[i];
					continue;
				}

				if (masses[i].nodeId === endNodeId) {
					this.m2 = masses[i];
					continue;
				}
			}
		}
	}

	this.draw = function() {

		if (this.m1 && this.m2 && enableSprings && relationshipTypes[this.typeString] === 1) {

			this.drawRel(typeString, this.m1.x, this.m1.y, this.m2.x, this.m2.y, (this.m1.r * 2) + 2, (this.m1.r * 2) + 2, (this.m2.r * 2) + 2, (this.m2.r * 2) + 2);
		}
	}

	this.drawRel = function(type, x1, y1, x2, y2, w1, h1, w2, h2) {

		var ax, ay, bx, by;

		var cx = (h1 / 2) * (x2 - x1) / (y2 - y1);
		var cy = (w1 / 2) * (y2 - y1) / (x2 - x1);
		var dx = (h2 / 2) * (x2 - x1) / (y2 - y1);
		var dy = (w2 / 2) * (y2 - y1) / (x2 - x1);

		if (y2 >= y1) {

			ax = Math.min(Math.max(x1 + cx, x1 - (w1 / 2)), x1 + (w1 / 2));
			bx = Math.min(Math.max(x2 - dx, x2 - (w2 / 2)), x2 + (w2 / 2));

		} else if (y1 > y2) {

			ax = Math.min(Math.max(x1 - cx, x1 - (w1 / 2)), x1 + (w1 / 2));
			bx = Math.min(Math.max(x2 + dx, x2 - (w2 / 2)), x2 + (w2 / 2));

		}

		if (x2 >= x1) {

			ay = Math.min(Math.max(y1 + cy, y1 - (h1 / 2)), y1 + (h1 / 2));
			by = Math.min(Math.max(y2 - dy, y2 - (h2 / 2)), y2 + (h2 / 2));

		} else if (x1 > x2) {

			ay = Math.min(Math.max(y1 - cy, y1 - (h1 / 2)), y1 + (h1 / 2));
			by = Math.min(Math.max(y2 + dy, y2 - (h2 / 2)), y2 + (h2 / 2));

		}

		var style = "#777777";

		if (scale < 1) {
			style = "#333333";
		}

		if (scale < 0.5) {
			style = "#000000";
		}

		ctx.globalAlpha = 1;
		ctx.fillStyle = style;
		ctx.strokeStyle = style;
		this.drawArrow(ax, ay, bx, by);

		engine.drawText(type, bx - (bx - ax) / 2, by - (by - ay) / 2, "10px sans-serif");
	}

	this.drawArrow = function(x1, y1, x2, y2) {

		var size = 6;
		var angle = Math.atan2(y2 - y1, x2 - x1);
		ctx.beginPath();
		ctx.moveTo(x1, y1);
		ctx.lineTo(x2, y2);
		ctx.stroke();
		ctx.closePath();
		ctx.beginPath();
		var x3 = x2 - size * Math.cos(angle - Math.PI / 6);
		var y3 = y2 - size * Math.sin(angle - Math.PI / 6);
		ctx.lineTo(x3, y3);
		ctx.moveTo(x2, y2);
		var x4 = x2 - size * Math.cos(angle + Math.PI / 6);
		var y4 = y2 - size * Math.sin(angle + Math.PI / 6);
		ctx.lineTo(x4, y4);
		ctx.lineTo(x3, y3);
		ctx.lineTo(x2, y2);
		ctx.fill();
		ctx.stroke();
		ctx.closePath();
	}
}
