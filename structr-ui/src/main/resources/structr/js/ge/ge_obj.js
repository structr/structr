function Node(g, id, size, pos) {

    var self = this;

    this.id = id;
    this.pos = pos;
    this.size = size;
    this.element = $(nodeHtml(id, size, pos[0], pos[1]))[0];

    this.relIds = [];

    $(g.element).append(this.element);

    $(this.element).dblclick(function() {
        reload(self.id);
    });

    $(this.element).draggable({
        containment: 'parent',
        stack: '.node',
        distance: 0,
        start: function(event, ui) {

        },
        drag: function(event, ui) {
            graph.redrawRelationships();
        },
        stop: function(event, ui) {
            graph.redrawRelationships();
        }
    });

    $(this.element).mousewheel(function(event, delta) {
        if (delta > 0) {
            self.zoomIn();
        } else if (delta < 0) {
            self.zoomOut();
        }
    });

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
    }

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
    }

    this.changeSize = function(oldSize, newSize) {
        self.size = newSize;
        $(self.element).removeClass(oldSize);
        $(self.element).addClass(newSize);
        g.redrawRelationships();
    }

}

function Relationship(g, id, type, startNodeId, endNodeId) {

    var self = this;

    this.id = id;
    this.type = type;
    this.startNodeId = startNodeId;
    this.endNodeId = endNodeId;

    this.redraw = function() {

        var startNodeElement = $('#node_' + self.startNodeId);
        var endNodeElement = $('#node_' + self.endNodeId);

        if (startNodeElement && startNodeElement.size() > 0 && endNodeElement && endNodeElement.size() > 0) {

            var w1 = startNodeElement.width();
            var h1 = startNodeElement.height();
            var w2 = endNodeElement.width();
            var h2 = endNodeElement.height();

            var fromX = startNodeElement.position().left + w1 / 2;
            var fromY = startNodeElement.position().top + h1 / 2;
            var toX = endNodeElement.position().left + w2 / 2;
            var toY = endNodeElement.position().top + h2 / 2;

            drawRel(context, self.type, self.id, fromX, fromY, toX, toY, w1, h1, w2, h2);

        } else {
            //console.log('could not draw rel ' + self.id);
        }
    }
}