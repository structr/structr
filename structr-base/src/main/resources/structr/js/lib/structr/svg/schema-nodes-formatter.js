'use strict';

class SchemaNodesFormatter {

	constructor(inheritanceRels) {
		this.inheritanceRels = inheritanceRels;
	}

	createAndAddDiagramNode(svg, child, offset, color) {

		let shape = this.createShapeForType(svg, child, offset, color);

		svg.appendChild(shape);

		// ports
		if (child?.ports?.length) {

			for (let port of child.ports) {

				let portShape = document.createElementNS('http://www.w3.org/2000/svg', 'circle');

				portShape.setAttribute('cx', child.x + (port.x || 0) + offset.x);
				portShape.setAttribute('cy', child.y + (port.y || 0) + offset.y);
				portShape.setAttribute('r', 5);
				portShape.setAttribute('fill', '#999999');

				svg.appendChild(portShape);

				console.log(child);
			}
		}

		if (child?.labels?.length) {

			let label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
			let text  = document.createTextNode(child.labels[0].text);

			label.setAttribute('text-anchor', 'middle');
			label.setAttribute('dominant-baseline', 'middle');
			label.setAttribute('font-size', '10pt');
			label.setAttribute('font-weight', '500');
			label.setAttribute('text-rendering', 'optimizeLegibility');
			label.setAttribute('x', child.x + offset.x + (child.width / 2));
			label.setAttribute('y', child.y + offset.y + (child.height / 2) + 1);
			label.setAttribute('stroke', 'none');
			label.setAttribute('fill', '#333333');
			label.setAttribute('pointer-events', 'none');

			label.appendChild(text);

			svg.appendChild(label);
		}

		if (child.children) {

			for (let grandChild of child.children) {

				this.createAndAddDiagramNode(svg, grandChild, { x: child.x + offset.x, y: child.y + offset.y }, color + 8);
			}
		}

		return shape;
	}

	createShapeForType(svg, child, offset, color) {

		// create default shape (rectangle)
		let shape = document.createElementNS('http://www.w3.org/2000/svg', 'rect');

		shape.classList.add('node');

		if (child?.labels?.length > 1) {

			let type = child.labels[1].text;
			switch (type) {

				case 'PropertyDataSource':
				case 'ListDataSource':
				case 'ActionMapping':
				case 'Template':
					color = 224;
					break;

				case 'Page':
					shape = this.createPagePolygon(child, offset);

				default:
					color = 248;
					break;
			}
		}

		let fillColor         = '#ffffff';
		let strokeColor       = '#ffffff';

		shape.setAttribute('id', child.id);
		shape.setAttribute('fill', fillColor);
		shape.setAttribute('stroke', strokeColor);
		shape.setAttribute('x', child.x + offset.x);
		shape.setAttribute('y', child.y + offset.y);
		shape.setAttribute('rx', 0);
		shape.setAttribute('width', child.width);
		shape.setAttribute('height', child.height);

		return shape;
	}

	createPagePolygon(child, offset) {

		let points = [];
		let x      = child.x + offset.x;
		let y      = child.y + offset.y;
		let w      = child.width;
		let h      = child.height;

		points.push([ x,          y      ].join(',')); // top left
		points.push([ x + w - 20, y      ].join(','));
		points.push([ x + w,      y + 20 ].join(','));
		points.push([ x + w,      y + h  ].join(',')); // bottom right
		points.push([ x,          y + h  ].join(',')); // bottom right

		let shape = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
		shape.setAttribute("points", points.join(' '));

		return shape;

	}

	createAndAddDiagramEdge(svg, edge) {

		let path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
		path.setAttribute('fill', 'none');

		let lines  = [];
		let radius = 20;

		if (edge?.sections?.length) {

			for (let segment of edge?.sections) {

				lines.push('M ' + segment.startPoint.x + ',' + segment.startPoint.y);

				if (segment?.bendPoints) {

					let triples = this.getTriples(segment);

					if (triples.length) {

						for (let triple of triples) {

							let p1 = triple.p1;
							let p2 = triple.p2;
							let p3 = triple.p3;

							let path = this.getRoundedCornerPath(p1, p2, p3, radius);

							lines.push('L' + path.begin.x + ',' + path.begin.y);
							lines.push('Q' + path.control.x + ',' + path.control.y + ' ' + path.end.x + ',' + path.end.y);
						}

					} else {

						for (let p of segment.bendPoints) {

							// ORTHOGONAL
							lines.push('L ' + p.x + ',' + p.y);
						}
					}
				}

				lines.push('L ' + segment.endPoint.x + ',' + segment.endPoint.y);
			}
		}

		path.setAttribute('d', lines.join(' '));
		path.setAttribute('marker-end', 'url(#arrow)');
		path.setAttribute('stroke', '#81ce25');
		path.setAttribute('stroke-width', 4);
		path.setAttribute('stroke-linejoin', 'round');

		if (edge?.labels?.length) {

			for (let label of edge.labels) {

				this.createLabel(svg, label);
			}
		}

		svg.appendChild(path);
	}

	renderInheritanceRels(parent) {

		// additional manual step after everything else has been rendered
		for (let rel of this.inheritanceRels) {

			let source = parent.querySelector('#' + rel.source);
			let target = parent.querySelector('#' + rel.target);

			if (source && target) {

				let path  = document.createElementNS('http://www.w3.org/2000/svg', 'path');
				let lines = [];

				lines.push('M' + source.getAttribute('x') + ',' + source.getAttribute('y'));
				lines.push('L' + target.getAttribute('x') + ',' + target.getAttribute('y'));

				path.setAttribute('fill', '#dddddd');
				path.setAttribute('d', lines.join(' '));
				path.setAttribute('stroke', '#dddddd');
				path.setAttribute('stroke-width', 4);

				path.setAttribute('stroke-linejoin', 'round');
				path.setAttribute('stroke-dasharray', '10 10');

				parent.appendChild(path);

			} else {

				console.log('not found: ' + rel.source + ' / ' + rel.target);
			}
		}
	}

	createLabel(svg, child) {

		// create default shape (rectangle)
		let shape = document.createElementNS('http://www.w3.org/2000/svg', 'rect');

		let fillColor         = '#ffffff';
		let strokeColor       = '#81ce25';

		shape.setAttribute('fill', fillColor);
		shape.setAttribute('stroke', strokeColor);
		shape.setAttribute('stroke-width', 1);
		shape.setAttribute('x', child.x - 2);
		shape.setAttribute('y', child.y);
		shape.setAttribute('rx', 0);
		shape.setAttribute('width', child.width);
		shape.setAttribute('height', child.height);

		let label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
		let text  = document.createTextNode(child.text);

		label.setAttribute('text-anchor', 'middle');
		label.setAttribute('dominant-baseline', 'middle');
		label.setAttribute('font-size', '11pt');
		label.setAttribute('text-rendering', 'optimizeLegibility');
		label.setAttribute('x', child.x + (child.width / 2));
		label.setAttribute('y', child.y + (child.height / 2) + 1);
		label.setAttribute('stroke', 'none');
		label.setAttribute('fill', '#666666');
		label.setAttribute('pointer-events', 'none');

		label.appendChild(text);

		svg.appendChild(shape);
		svg.appendChild(label);
	}

	getRoundedCornerPath(p1, bend, p2, radius) {

		let segment1 = Vector.fromPoints(bend, p1);
		let segment2 = Vector.fromPoints(bend, p2);
		let n1       = segment1.normalized();
		let n2       = segment2.normalized();

		return {
			begin: {
				x: bend.x + (n1.x * radius),
				y: bend.y + (n1.y * radius)
			},
			end: {
				x: bend.x + (n2.x * radius),
				y: bend.y + (n2.y * radius)
			},
			control: {
				x: bend.x,
				y: bend.y
			}
		};
	}

	getTriples(segment) {

		let points  = [];
		let triples = [];

		points.push(segment.startPoint);

		for (let p of segment.bendPoints) {
			points.push(p);
		}

		points.push(segment.endPoint);

		let num = points.length;

		for (let i=0; i<num-2; i++) {

			triples.push({
				p1: points[i],
				p2: points[i+1],
				p3: points[i+2]
			});
		}

		return triples;
	}
}

class Vector {

	constructor(x, y) {
		this.x = x;
		this.y = y;
	}

	normalized() {

		let result = new Vector(this.x, this.y);
		let length = this.length();

		result.scale(1.0 / length);

		return result;
	}

	length() {
		return Math.sqrt(this.x * this.x + this.y * this.y);
	}

	scale(f) {
		this.x *= f;
		this.y *= f;
	}

	static fromPoints(p1, p2) {
		return new Vector(p2.x - p1.x, p2.y - p1.y);
	}
}