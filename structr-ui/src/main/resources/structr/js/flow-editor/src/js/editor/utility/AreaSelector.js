export class AreaSelector {

	constructor(editor) {
		this._editor = editor;

		let svg = document.querySelector('svg.area-selector');
		if (svg === undefined || svg === null) {
			this._svg = this._createSVGOverlay();
		} else {
			this._svg = d3.select(svg);
		}
	}

	enable() {
		this._svg.attr('visibility','visible');
	}

	disable() {
		this._svg.attr('visibility','hidden');
	}


	_createSVGOverlay() {

		const self = this;

		const rect = function rect(x, y, w, h) {
			return 'M'+[x,y]+' l'+[w,0]+' l'+[0,h]+' l'+[-w,0]+'z';
		};

		const rootElement = this._editor._rootElement.parentNode;
		const svg = d3.select(rootElement).append('svg')
			.attr('class','area-selector')
			.attr('visibility', 'hidden');

		const selection = svg.append('path')
			.attr('class', 'selection')
			.attr('visibility', 'hidden');

		const startSelection = function (start) {
			selection.attr('d', rect(start[0], start[0], 0, 0))
				.attr('visibility', 'visible');
		};

		const moveSelection = function (parent, start, moved) {

			// relativize the rectangle to the parent element
			const parentOffset = parent.getBoundingClientRect();

			selection.attr('d', rect(start[0] + parentOffset.x, start[1] + parentOffset.y, (moved[0] + parentOffset.x) - (start[0] + parentOffset.x), (moved[1] + parentOffset.y) - (start[1] + parentOffset.y)));
		};

		const endSelection = function (start, end) {
			selection.attr('visibility', 'hidden');

			let p1 = [0,0];
			let p2 = [0,0];

			if (start[0] < end[0]) {
				// left to right
				if (start[1] < end[1]) {
					// up to down
					p1 = [start[0], start[1]];
					p2 = [end[0], end[1]];
				} else {
					// down to up
					p1 = [start[0], end[1]];
					p2 = [end[0], start[1]];
				}
			} else {
				// right to left
				if (start[1] < end[1]) {
					// up to down
					p1 = [end[0], start[1]];
					p2 = [start[0], end[1]];
				} else {
					// down to up
					p1 = [end[0], end[1]];
					p2 = [start[0], start[1]];
				}
			}

			self._editor.selectAllNodesInRectangle(p1,p2);
		};

		svg.on('mousedown', function() {
            const subject = d3.select(window), parent = this.parentNode,
				start = d3.mouse(parent);

			startSelection(start);
			subject
				.on('mousemove.selection', function() {
					moveSelection(parent, start, d3.mouse(parent));
				}).on('mouseup.selection', function() {
				endSelection(start, d3.mouse(parent));
				subject.on('mousemove.selection', null).on('mouseup.selection', null);
			});
		});

		svg.on('touchstart', function() {
			const subject = d3.select(this), parent = this.parentNode,
				id = d3.event.changedTouches[0].identifier,
				start = d3.touch(parent, id);
			let pos;
			startSelection(start);
			subject
				.on('touchmove.'+id, function() {
					if (pos = d3.touch(parent, id)) {
						moveSelection(parent, start, pos);
					}
				}).on('touchend.'+id, function() {
				if (pos = d3.touch(parent, id)) {
					endSelection(start, pos);
					subject.on('touchmove.'+id, null).on('touchend.'+id, null);
				}
			});
		});

		return svg;

	}

}