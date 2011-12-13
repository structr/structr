function refreshEntities(type) {
    if (debug) console.log('refreshEntities(' + type + ')');
    var types = plural(type);
    var parentElement = $('#' + types);
    parentElement.empty();
    showEntities(type);
    parentElement.append('<div style="clear: both"></div>');
    parentElement.append('<img title="Add ' + type + '" alt="Add ' + type + '" class="add_icon button" src="icon/add.png">');
    $('.add_icon', main).on('click', function() {
        addEntity(type, this);
    });
    parentElement.append('<img title="Delete all ' + types + '" alt="Delete all ' + types + '" class="delete_icon button" src="icon/delete.png">');
    $('.delete_icon', main).on('click', function() {
        deleteAll(this, type);
    });
}

function showEntities(type) {
    if (debug) console.log('showEntities(' + type + ')');
    var data = '{ "command" : "LIST", "data" : { "type" : "' + type + '" } }';
    return send(data);
}

function appendEntityElement(entity, parentElement) {
    var element;
    if (parentElement) {
        element = parentElement;
    } else {
        element = $('#' + plural(entity.type.toLowerCase()));
    }
    //    console.log(element);
    element.append('<div class="nested top ' + entity.type.toLowerCase() + ' ' + entity.id + '_">'
        + (entity.iconUrl ? '<img class="typeIcon" src="' + entity.iconUrl + '">' : '')
        + '<b class="name">' + entity.name + '</b> '
        + '[' + entity.id + ']'
        + '</div>');
    div = $('.' + entity.id + '_', element);
    div.append('<img title="Delete ' + entity.name + ' [' + entity.id + ']" '
        + 'alt="Delete ' + entity.name + ' [' + entity.id + ']" class="delete_icon button" src="icon/delete.png">');
    $('.delete_icon', div).on('click', function() {
        deleteNode(this, entity)
    });
    div.append('<img title="Edit ' + entity.name + ' [' + entity.id + ']" alt="Edit ' + entity.name + ' [' + entity.id + ']" class="edit_icon button" src="icon/pencil.png">');
    $('.edit_icon', div).on('click', function() {
        showProperties(this, entity, 'all', $('.' + entity.id + '_', element));
    });
}

function addSourceToTarget(sourceId, targetId) {
	if (debug) console.log('Add ' + sourceId + ' to ' + targetId);
	var data = '{ "command" : "ADD" , "id" : "' + targetId + '" , "data" : { "id" : "' + sourceId + '" } }';
	return send(data);
}

function removeSourceFromTarget(sourceId, targetId) {
	if (debug) console.log('Remove ' + sourceId + ' from ' + targetId);
	var data = '{ "command" : "REMOVE" , "id" : "' + targetId + '" , "data" : { "id" : "' + sourceId + '" } }';
	return send(data);
}

function createEntity(entity, parentElement) {
    var toSend = {};
    toSend.data = entity;
    toSend.command = 'CREATE';
    if (debug) console.log($.toJSON(toSend));
    return send($.toJSON(toSend));
}

var buttonClicked;

function addEntity(type, button) {
    buttonClicked = button;
    if (isDisabled(button)) return;
    disable(button);
    createEntity($.parseJSON('{ "type" : "' + type + '", "name" : "New ' + type + ' ' + Math.floor(Math.random() * (999999 - 1)) + '" }'));
}

function plural(type) {
    var plural = type + 's';
    if (type.substring(type.length-1, type.length) == 'y') {
        plural = type.substring(0, type.length-1) + 'ies';
    //        console.log(plural);
    }
    return plural;
}
