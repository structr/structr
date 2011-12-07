function refreshEntities(type) {
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
    $.ajax({
        url: rootUrl + type + 's/all',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        headers: headers,
        success: function(data) {
            if (data) $(data.result).each(function(i, entity) {
                appendEntityElement(entity);
            });
        }
    });
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

function createEntity(entity, parentElement) {
    //  console.log('Creating entity ..');
    //  console.log(entity);
    var url = rootUrl + entity.type.toLowerCase();

    var command = entity;
    var data = $.toJSON(command);
    console.log(data);

    //ws.send(data);

    var resp = $.ajax({
        url: url,
        //async: false,
        type: 'POST',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        headers: headers,
        data: $.toJSON(entity),
        success: function(data) {
            var getUrl = resp.getResponseHeader('Location');
            $.ajax({
                url: getUrl + '/all',
                headers: headers,
                success: function(data) {
                    //          console.log('Entity added: ' + getUrl);
                    entity.id = lastPart(getUrl, '/');
                    appendEntityElement(data.result, parentElement);
                    if (buttonClicked) enable(buttonClicked);
                }
            });
        }
    });
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