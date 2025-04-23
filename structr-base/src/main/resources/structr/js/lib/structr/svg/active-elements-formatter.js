'use strict';

class ActiveElementsFormatter {

	constructor(data) {
		this.data = data;
	}

	async createAndAddDiagramNode(svg, child, offset, color) {

		let shape = await this.createShapeForType(svg, child, offset, color);
		let text  = true;

		if (child?.labels[1]?.text === 'PropertyDataSource') { text = false; }
		if (child?.labels[1]?.text === 'ListDataSource') { text = false; }
		if (child?.labels[1]?.text === 'Condition') { text = false; }
		if (child?.labels[1]?.text === 'ActionMapping') { text = false; }

		svg.appendChild(shape);

		if (child?.labels?.length && text) {

			let label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
			let text  = document.createTextNode(child.labels[0].text);

			label.setAttribute('text-anchor', 'middle');
			label.setAttribute('dominant-baseline', 'middle');
			label.setAttribute('font-weight', 'normal');
			label.setAttribute('font-size', '8pt');
			label.setAttribute('x', child.x + offset.x + (child.width / 2));
			label.setAttribute('y', child.y + offset.y + (child.height / 2));
			label.setAttribute('stroke', '#333333');
			label.setAttribute('pointer-events', 'none');

			label.appendChild(text);

			svg.appendChild(label);
		}

		if (child.children) {

			let relativeOffset = { x: child.x + offset.x, y: child.y + offset.y };

			for (let grandChild of child.children) {

				await this.createAndAddDiagramNode(svg, grandChild, relativeOffset, color + 8);
			}

			if (child.edges) {

				for (let edge of child.edges) {
					await this.createAndAddDiagramEdge(svg, edge, relativeOffset);
				}
			}
		}
	}

	async createShapeForType(svg, child, offset, color) {

		let shape;

		if (child?.labels?.length > 1) {

			let type = child.labels[1].text;
			switch (type) {

				case 'ActionMapping':
					shape = this.createActionIcon(child, offset, '#dddddd', '#999999');
					break;

				case 'Template':
					shape = this.createDefaultRect(child, offset, '#dddddd', '#999999');
					break;

				case 'Page':
					shape = this.createPagePolygon(child, offset, '#bbbbbb', '#999999');
					break;

				case 'Condition':
					shape = this.createConditionIcon(child, offset, '#dddddd', '#999999');
					break;

				case 'PropertyDataSource':
				case 'ListDataSource':
					shape = this.createDatabaseIcon(child, offset, '#dddddd', '#999999');
					break;

				default:
					shape = this.createDefaultRect(child, offset, '#f8f8f8', '#999999');
					break;
			}
		}

		let nodeData = this.data[child.id];
		if (nodeData?.dataKey) {

			svg.appendChild(this.createDefaultRect(child, { x: offset.x + 6, y: offset.y + 6 }, '#dddddd', '#999999'));
			svg.appendChild(this.createDefaultRect(child, { x: offset.x + 3, y: offset.y + 3 }, '#eeeeee', '#999999'));
		}

		shape.dataset.nodeId = child.id;
		shape.classList.add('diagram-element');

		return shape;
	}

	createDefaultRect(child, offset, fillColor, strokeColor) {

		let shape = document.createElementNS('http://www.w3.org/2000/svg', 'rect');

		shape.setAttribute('fill', fillColor);
		shape.setAttribute('stroke', strokeColor);
		shape.setAttribute('x', child.x + offset.x);
		shape.setAttribute('y', child.y + offset.y);
		shape.setAttribute('rx', 2);
		shape.setAttribute('width', child.width);
		shape.setAttribute('height', child.height);

		return shape;
	}

	createPagePolygon(child, offset, fillColor, strokeColor) {

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
		shape.setAttribute('fill', fillColor);
		shape.setAttribute('stroke', strokeColor);
		shape.setAttribute('x', child.x + offset.x);
		shape.setAttribute('y', child.y + offset.y);
		shape.setAttribute('rx', 2);

		return shape;
	}

	createConditionIcon(child, offset, fillColor, strokeColor) {

		let points = [];
		let x      = child.x + offset.x;
		let y      = child.y + offset.y;
		let w      = child.width;
		let h      = child.height;
		let w2     = w / 2;
		let h2     = h / 2;

		points.push([ x + w2, y      ].join(',')); // top
		points.push([ x + w,  y + h2 ].join(',')); // right
		points.push([ x + w2, y + h  ].join(',')); // bottom
		points.push([ x,      y + h2 ].join(',')); // left

		let shape = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
		shape.setAttribute("points", points.join(' '));

		shape.setAttribute("points", points.join(' '));
		shape.setAttribute('fill', fillColor);
		shape.setAttribute('stroke', strokeColor);
		shape.setAttribute('x', child.x + offset.x);
		shape.setAttribute('y', child.y + offset.y);

		return shape;
	}

	createDatabaseIcon(child, offset, fillColor, strokeColor) {

		let x      = child.x + offset.x;
		let y      = child.y + offset.y + (child.height / 6);
		let w      = child.width;
		let h      = child.height - (child.height / 3);
		let w2     = w / 2;
		let h2     = h / 2;
		let w3     = w / 3;
		let h3     = h / 3;

		let group = document.createElementNS('http://www.w3.org/2000/svg', 'g');

		let outline = document.createElementNS('http://www.w3.org/2000/svg', 'path');
		let inner   = document.createElementNS('http://www.w3.org/2000/svg', 'path');

		outline.setAttribute('fill', fillColor);
		outline.setAttribute('stroke', strokeColor);
		outline.setAttribute('stroke-width', '1');

		let outlineLines = [];
		let innerLines   = [];

		outlineLines.push('M' + x + ',' + y);
		outlineLines.push('A 3 1 0 0 1 ' + (x + w) + ',' + y);
		outlineLines.push('L' + (x + w) + ',' + (y + h));
		outlineLines.push('A 3 1 0 0 1' + x + ',' + (y + h));
		outlineLines.push('L' + x + ',' + y);

		outline.setAttribute('d', outlineLines.join(' '));

		innerLines.push('M' + x + ',' + y);
		innerLines.push('A 3 1 0 0 0 ' + (x + w) + ',' + y);

		innerLines.push('M' + x + ',' + (y + h3));
		innerLines.push('A 3 1 0 0 0 ' + (x + w) + ',' + (y + h3));

		innerLines.push('M' + x + ',' + (y + h3 + h3));
		innerLines.push('A 3 1 0 0 0 ' + (x + w) + ',' + (y + h3 + h3));


		inner.setAttribute('d', innerLines.join(' '));
		inner.setAttribute('fill', 'none');
		inner.setAttribute('stroke', strokeColor);

		group.appendChild(outline);
		group.appendChild(inner);

		return group;
	}

	createActionIcon(child, offset, fillColor, strokeColor) {

		let group  = document.createElementNS('http://www.w3.org/2000/svg', 'g');
		let shape1 = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
		let shape2 = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
		let shape3 = document.createElementNS('http://www.w3.org/2000/svg', 'rect');

		shape1.setAttribute('fill', fillColor);
		shape1.setAttribute('stroke', strokeColor);
		shape1.setAttribute('x', child.x + offset.x);
		shape1.setAttribute('y', child.y + offset.y);
		shape1.setAttribute('rx', 10);
		shape1.setAttribute('ry', 10);
		shape1.setAttribute('width', child.width);
		shape1.setAttribute('height', child.height);

		shape2.setAttribute('fill', '#000000');
		shape2.setAttribute('stroke', '#000000');
		shape2.setAttribute('cx', child.x + offset.x + 10);
		shape2.setAttribute('cy', child.y + offset.y + 20);
		shape2.setAttribute('r', 5);

		group.appendChild(shape1);
		group.appendChild(shape2);

		return group;
	}

	createAndAddDiagramEdge(svg, edge, offset) {

		let path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
		path.setAttribute('fill', 'none');
		path.setAttribute('stroke-width', '1');

		let lines = [];

		if (edge?.sections?.length) {

			for (let segment of edge?.sections) {
				lines.push('M ' + (segment.startPoint.x + offset.x) + ',' + (segment.startPoint.y + offset.y));
				if (segment.bendPoints) {
					for (let p of segment.bendPoints) {

						lines.push('L ' + (p.x + offset.x) + ',' + (p.y + offset.y));
					}
				}
				lines.push('L ' + (segment.endPoint.x + offset.x) + ',' + (segment.endPoint.y + offset.y));
			}
		}

		path.setAttribute('d', lines.join(' '));
		path.setAttribute('marker-end', 'url(#arrow)');
		path.setAttribute('stroke', '#333333');

		svg.appendChild(path);
	}
}