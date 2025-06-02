/*
 * Copyright (C) 2010-2025 Structr GmbH
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
class SigmaLayouter {

	constructor(container) {

		this.colors = {
			core: '#81ce25',
			html: '#81ce25',
			ui: '#81ce25',
			'': '#666666'
		};

		this.settings = {
			immutable: true,
			minNodeSize: 1,
			maxNodeSize: 10,
			borderSize: 4,
			defaultNodeBorderColor: '#a5a5a5',
			singleHover: true,
			doubleClickEnabled: true,
			minEdgeSize: 1,
			maxEdgeSize: 2,
			enableEdgeHovering: false,
			edgeHoverColor: 'default',
			edgeHoverSizeRatio: 1.3,
			edgeHoverExtremities: true,
			edgesPowRatio: 0.01,
			defaultEdgeType: 'arrow',
			edgeColor: '#ddd',
			defaultEdgeColor: '#ddd',
			defaultEdgeHoverColor: '#ddd',
			minArrowSize: 1,
			maxArrowSize: 1,
			labelSize: 'fixed',
			labelSizeRatio: 2,
			labelThreshold: 3,
			sideMargin: 1,
			drawEdgeLabels: false,
			zoomMax: 3
		};

		this.graphBrowser = new GraphBrowser({
			graphContainer: container,
			sigmaSettings: this.settings,
			moduleSettings: {
				nodeExpander: false,
				currentNodeTypes: {
					container: 'graph-legend',
					classes: 'btn btn-block'
				}
			}
		});

		/*
		this.graphBrowser.bindEvent('dragend', handleDragEndNodeEvent);
		this.graphBrowser.bindEvent('drag',      handleDragNodeEvent);
		this.graphBrowser.bindEvent('startdrag', handleStartDragNodeEvent);
		this.graphBrowser.bindEvent('clickNode', handleClickNodeEvent);
		this.graphBrowser.bindEvent('clickEdge', handleClickEdgeEvent);
		 */

		this.seed = 42;
	}

	addNode(node, path) {

		let id    = node.id;
		let name  = node.name;
		let type  = node.type;
		let size  = 2;
		let color = this.colors[node.category];

		this.graphBrowser.addNode({
			id: id,
			size: size,
			x: this.random() * 1000.0,
			y: this.random() * 1000.0,
			color: color,
			label: name,
			nodeType: type,
			path: path,
			builtIn: node.isBuiltinType
		});

		return id;
	}

	addEdge(id, name, source, target, active) {

		try {
			this.graphBrowser.addEdge({
				id: id,
				type: 'curvedArrow',
				source: source,
				target: target,
				color: 'rgba(192, 192, 192, 0.3)',
				label: name
			});

		} catch (e) {}

		return id;
	}

	layout() {

		this.graphBrowser.doLayout('fruchtermanReingold', {
			autoArea: false,
			area: 100000,
			gravity: 0.9,
			speed: 0.9,
			iterations: 100,
			easing: 'cubicInOut',
			duration: 10
		});
		this.graphBrowser.refresh();
	}

	refresh() {
		this.graphBrowser.dataChanged();
		sigma.canvas.edges.autoCurve(this.graphBrowser.getSigma());
	}

	update(key, value) {

	}

	on(event, func) {
		this.graphBrowser.bindEvent(event, func);
	}

	focus(id) {
	}

	enableEdgeCreationMode() {
	}

	enableEditMode() {
	}

	handleAddNode(e) {

	}

	handleAddEdge(data, callback) {
	}

	disableEditMode() {
	}

	getNodes() {
		return this.graphBrowser.getNodes();
	}

	getEdges() {
		return this.graphBrowser.getEdges();
	}

	disableInteraction() {
	}

	enableInteraction() {
	}

	random() {
	    var x = Math.sin(this.seed++) * 10000;
	    return x - Math.floor(x);
	}
}