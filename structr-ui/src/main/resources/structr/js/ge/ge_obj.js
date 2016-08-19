/*
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

function Node(g, entity, size, pos, depth) {

    var self = this;

    this.id = entity.id;
    this.type = entity.type;
    this.pos = limitPositionToCanvas(pos);
    this.size = size;
    this.depth = depth;

    this.element = $(nodeHtml(self))[0];

    var div = $(this.element);

    div.append('<img title="Delete node ' + entity.id + '" alt="Delete node ' + entity.id + '" class="delete_icon button" src="' + _Icons.delete_icon + '">');
    $('.delete_icon', div).on('click', function(e) {
        e.stopPropagation();
        _Entities.deleteNode(this, entity);
    });

    _Entities.appendEditPropertiesIcon(div, entity);

    this.relIds = [];

    $(g.element).append(this.element);

//    $(this.element).on('mouseover', function(event) {
//        $(this).addClass('nodeActive');
//    });
//
//    $(this.element).on('mouseout', function(event) {
//        $(this).removeClass('nodeActive');
//    });

    $(this.element).dblclick(function(event, ui) {
        g.expand(self);
    });

    _Entities.setMouseOver(div, true);

    $(this.element).draggable({
        containment: 'parent',
        stack: '.node',
        distance: 0,
        start: function(event, ui) {
        },
        drag: function(event, ui) {
            g.redrawRelationships();
        },
        stop: function(event, ui) {
            var n = $(this);
            self.pos = [n.position().left, n.position().top];
            g.redrawRelationships();
        }
    });

    $(this.element).mousewheel(function(event, delta) {
        if (delta > 0) {
            self.zoomIn();
        } else if (delta < 0) {
            self.zoomOut();
        }
    });

//    this.redrawRelationships = function() {
//        console.log(self.relIds);
//        for (var i=0; i<self.relIds.length; i++) {
//            var id = self.relIds[i];
//            var rel = g.relationships[id];
//            console.log(id, rel);
//            if (rel) rel.redraw();
//        }
//    };

    $(this.element).resize(function() {
        g.redrawRelationships();
    });

    this.zoomIn = function() {

        switch (self.size) {
            case "dot" :
                self.changeSize("dot", "tiny");
                break;
            case "tiny" :
                self.changeSize("tiny", "small");
                break;
            case "small" :
                self.changeSize("small", "medium");
                $('.body', self.element).show();
                $('.foot', self.element).show();
                break;
            case "medium" :
                self.changeSize("medium", "large");
                break;
        }
    };

    this.zoomOut = function() {

        switch (self.size) {
            case "large" :
                self.changeSize("large", "medium");
                break;
            case "medium" :
                $('.body', self.element).hide();
                $('.foot', self.element).hide();
                self.changeSize("medium", "small");
                break;
            case "small" :
                self.changeSize("small", "tiny");
                break;
            case "tiny" :
                self.changeSize("tiny", "dot");
                break;
        }
    };

    this.changeSize = function(oldSize, newSize) {
        self.size = newSize;
        $(self.element).removeClass(oldSize);
        $(self.element).addClass(newSize);
        g.redrawRelationships();
    };
}

function Relationship(g, id, type, sourceId, targetId) {

    var self = this;

    this.id = id;
    this.type = type;
    this.sourceId = sourceId;
    this.targetId = targetId;

    var startNode = g.nodes[sourceId];
    if (startNode) {
        startNode.relIds.push(id);
    }
    var endNode = g.nodes[targetId];
    if (endNode) {
        endNode.relIds.push(id);
    }

    this.redraw = function() {

        var startNodeElement = Structr.node(self.sourceId);
        var endNodeElement = Structr.node(self.targetId);
        
        if (startNodeElement && startNodeElement.size() > 0 && endNodeElement && endNodeElement.size() > 0) {

            var w1 = startNodeElement.width();
            var h1 = startNodeElement.height();
            var w2 = endNodeElement.width();
            var h2 = endNodeElement.height();

            var fromX = startNodeElement.position().left + w1 / 2;
            var fromY = startNodeElement.position().top + h1 / 2;
            var toX = endNodeElement.position().left + w2 / 2;
            var toY = endNodeElement.position().top + h2 / 2;

            //console.log('drawing rel', context, self.type, self.id, fromX, fromY, toX, toY, w1, h1, w2, h2);

            drawRel(context, self.type, self.id, fromX, fromY, toX, toY, w1, h1, w2, h2);

        } else {
            //console.log('could not draw rel ' + self.id);
        }
    }
}

function limitPositionToCanvas(pos) {
    var newPos = [ Math.min(canvas.w-176, Math.max(12, pos[0])), Math.min(canvas.h-101, Math.max(12, pos[1])) ];
    return newPos;
}
