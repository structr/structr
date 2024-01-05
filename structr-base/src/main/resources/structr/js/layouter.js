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
class VISLayouter {

	constructor(container) {

		this.container = container;
		this.data = {
			nodes: new vis.DataSet(),
			edges: new vis.DataSet()
		};
		this.cache = {};
		this.options = {

			nodes: {
				shape: 'box',
				widthConstraint: {
					minimum: 80
				},
				margin: {
					top: 2,
					bottom: 1,
					left: 20,
					right: 20
				},
				shapeProperties: {
					borderRadius: 10
				},
				font: {
					multi: 'html',
					color: '#999999',
					size: 10,
					bold: '10px roboto #333333'
				}
			},
			edges: {
				arrows: {
					to: {
						enabled: true,
						scaleFactor: 0.6,
						type: 'arrow'
					},
					middle: {
						enabled: false,
						scaleFactor: 1,
						type: 'arrow'
					},
					from: {
						enabled: true,
						scaleFactor: 0.05,
						type: 'circle'
					}
				},
				width: 2,
				arrowStrikethrough: false,
				font: {
					size: 8
				},
				smooth: {
					type: 'discrete',
					roundness: 0.9
				},
				color: {
					color: '#81ce25',
					opacity: 1.0
				},
			},
			interaction: {
				dragNodes: false,
				multiselect: true
			},
			physics: {
				enabled: false,
				forceAtlas2Based: {
					gravitationalConstant: -300,
					centralGravity: 0.02,
					springConstant: 0.5,
					springLength: 100,
					damping: 0.4,
					avoidOverlap: 1
				},
				maxVelocity: 50,
				minVelocity: 0.1,
				solver: 'forceAtlas2Based',
				stabilization: {
					enabled: true,
					iterations: 1000,
					updateInterval: 100,
					onlyDynamicEdges: false,
					fit: true
				},
				timestep: 0.5,
				adaptiveTimestep: true
			},
			layout: {
				improvedLayout: true,
				randomSeed: 1234,
				hierarchical: true
			},
			manipulation: {
				enabled: false,
				addEdge: (data, callback) => {
					this.handleAddEdge(data, callback);
				},
				editNode: (data, callback) => {
					callback(data);
				},
				initiallyActive: false,
				controlNodeStyle: {
					// all node options are valid.
					shape: 'dot',
					size: 2,
					color: {
						background: '#ff0000',
						border: '#3c3c3c',
						highlight: {
							background: '#07f968',
							border: '#3c3c3c'
						}
					},
					borderWidth: 2,
					borderWidthSelected: 2
				}

			}
		}
	}

	init() {

		// Modify to initialize the graph browser with the modules of your choice
		let graphBrowser = new GraphBrowser({
			graphContainer: 'graph-container',
			sigmaSettings: settings,
			moduleSettings: {
				nodeExpander: false,
				currentNodeTypes: {
					container: 'graph-legend',
					classes: 'btn btn-block'
				}
			}
		});

		graphBrowser.bindEvent('dragend', handleDragEndNodeEvent);
		graphBrowser.bindEvent('drag',      handleDragNodeEvent);
		graphBrowser.bindEvent('startdrag', handleStartDragNodeEvent);
		graphBrowser.bindEvent('clickNode', handleClickNodeEvent);
		graphBrowser.bindEvent('clickEdge', handleClickEdgeEvent);
		graphBrowser.bindEvent('doubleClickEdge', handleDoubleClickEdgeEvent);
	}

	addNode(id, name, position) {

		var node = {
			id: id,
			label: name,
			color: {
				background: '#eeeeee',
				border: '#eeeeee'
			}
		};

		if (position) {
			node.x = position.x;
			node.y = position.y;
		}

		this.data.nodes.add(node);
		return id;
	}

	addEdge(id, name, source, target, active) {

		var key = source + '.' + target;
		if (this.cache[key]) {
			return;
		}

		this.cache[key] = true;

		var edge = {
			id: id,
			from: source,
			to: target,
			label: name,
			color: {
				color: active ? '#81ce25' : '#666666',
				opacity: active ? 1.0 : 0.1
			},
			physics: true,
			dashes: !active
		};

		this.data.edges.add(edge);
		return id;
	}

	layout() {
		window.setTimeout(() => {
			this.network.stopSimulation();
		}, 1000);
	}

	update(key, value) {

		switch (key) {
			case 'roundness':
				this.network.setOptions({
					edges: {
						smooth: {
							roundness: value
						}
					}
				});
				window.setTimeout(() => {
					this.network.stopSimulation();
				}, 100);
				break;
			case 'gravity':
				this.network.setOptions({
					physics: {
						forceAtlas2Based: {
							gravitationalConstant: -value
						}
					}
				});
				window.setTimeout(() => {
					this.network.stopSimulation();
				}, 100);
				break;
		}
	}

	on(event, func) {
		this.network.off(event).on(event, func);
	}

	focus(id) {
		this.network.focus(id, {
			locked: false,
			scale: 2.0,
			animation: {
				duration: 300,
				easingFunction: 'easeInOutQuad'
			}
		});
	}

	enableEdgeCreationMode() {
		this.network.addEdgeMode();
		this.network.disableEditMode();
	}

	enableEditMode() {

		this.network.addEdgeMode();
		this.network.setOptions({
			interaction: {
				hover: true
			}
		});
		this.network.stopSimulation();
		$(this.container).css('cursor', 'copy');
		this.network.on('click', (e) => this.handleAddNode(e));
	}

	handleAddNode(e) {

		$(this.container).css('cursor', 'wait');

		Command.create({
			type: 'SchemaNode',
			name: 'Unnamed'
		}, (result) => {
			if (result) {
				this.addNode(result.id, '<b>' + result.name + '</b>', e.pointer.canvas);
				this.addEdge(result.id + '0000', '', result.id, '0000', false);
			}
			$(this.container).css('cursor', 'default');
		});
	}

	handleAddEdge(data, callback) {

		$(this.container).css('cursor', 'wait');

		Command.create({
			type: 'SchemaRelationshipNode',
			relationshipType: 'UNKNOWN',
			sourceNode: data.from,
			targetNode: data.to,
			name: null
		}, (result) => {
			if (result) {
				this.addEdge(result.id, 'UNKNOWN', data.from, data.to, true);
			}
			$(this.container).css('cursor', 'default');
		}, true);
	}

	disableEditMode() {

		this.network.disableEditMode();
		this.network.setOptions({
			interaction: {
				hover: false
			}
		});
		this.network.stopSimulation();

		var c = $(this.container);
		if (c.css('cursor') !== 'wait') {

			c.css('cursor', 'default');
		}

		this.network.off('click');
	}

	getNodes() {
		return this.data.nodes;
	}

	getEdges() {
		return this.data.edges;
	}

	disableInteraction() {

		this.network.setOptions({

			interaction: {

				dragNodes: false,
				dragView: false,
				hideEdgesOnDrag: false,
				hideNodesOnDrag: false,
				hover: false,
				hoverConnectedEdges: false,
				keyboard: {
					enabled: false,
					speed: {x: 10, y: 10, zoom: 0.02},
					bindToWindow: false
				},
				multiselect: false,
				navigationButtons: false,
				selectable: false,
				selectConnectedEdges: false,
				zoomView: false
			}
		});
	}

	enableInteraction() {

		this.network.setOptions({

			interaction: {
				dragNodes: false,
				dragView: true,
				hideEdgesOnDrag: false,
				hideNodesOnDrag: false,
				hover: false,
				hoverConnectedEdges: true,
				keyboard: {
					enabled: false,
					speed: {x: 10, y: 10, zoom: 0.02},
					bindToWindow: true
				},
				multiselect: true,
				navigationButtons: false,
				selectable: true,
				selectConnectedEdges: true,
				tooltipDelay: 300,
				zoomView: true
			}
		});
	}
}