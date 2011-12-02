function showResources() {
  $.ajax({
    url: rootUrl + 'resources',
    dataType: 'json',
    contentType: 'application/json; charset=utf-8',
    //headers: { 'X-User' : 457 },
    success: function(data) {        
      if (!data || data.length == 0 || !data.result) return;
      if ($.isArray(data.result)) {
        
        for (var i=0; i<data.result.length; i++) {
          var resource = data.result[i];
      
          var id = resource.id;
          $('#resources').append('<div class="editor_box"><div class="nested top resource" id="resource_' + id + '">'
                              + '<b>' + resource.name + '</b>'
                              //+ ' [' + id + ']'
                              + '<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png" onclick="addElement(' + id + ', \'#resource_' + id + '\')">'
                              + '<img class="delete_icon button" title="Delete '
                              + resource.name + '" alt="Delete '
                              + resource.name + '" src="icon/delete.png" onclick="deleteNode(' + id + ', \'#resource_' + id + '\')">'
                              + '</div></div>');
          showElementsOfResource(id, null);
      
          $('#previews').append('<a target="_blank" href="' + viewRootUrl + resource.name + '">' + viewRootUrl + resource.name + '</a><br><div class="preview_box"><iframe id="preview_'
                            + id + '" src="' + viewRootUrl + resource.name + '?edit"></iframe></div><div style="clear: both"></div>');
      
          $('#preview_' + id).load(function() {
            //console.log(this);
            var doc = $(this).contents();
            var head = $(doc).find('head');
            head.append('<style type="text/css">'
                                                   + '.structr-editable-area {'
                                                   + 'border: 1px dotted #a5a5a5;'
                                                   + 'margin: 2px;'
                                                   + 'padding: 2px;'
                                                   + '}'
                                                   + '.structr-editable-area-active {'
                                                   + 'border: 1px dotted #orange;'
                        + 'margin: 2px;'
                        + 'padding: 2px;'
                                                   + '}'
                                                   + '</style>');


            $(this).contents().find('.structr-editable-area').each(function(i,element) {
              //console.log(element);
              $(element).addClass('structr-editable-area');
              $(element).on({
                mouseenter: function() {
                  var self = $(this);
                  self.attr('contenteditable', true);
                  self.addClass('structr-editable-area-active');
                },
                mouseleave: function() {
                  var self = $(this);
                  self.attr('contenteditable', true);
                  self.removeClass('structr-editable-area-active');
                  var id = self.attr('id');
                  id = lastPart(id, '-');
                  updateContent(id, this.innerHTML);
                }
              });
            });          
          });
      
        }
      }
    }
  });
}

function updateContent(contentId, content) {
  console.log('update ' + contentId + ' with ' + content);
  var url = rootUrl + 'content' + '/' + contentId;
  var data = '{ "content" : ' + $.quoteString(content) + ' }';
  $.ajax({
    url: url,
    //async: false,
    type: 'PUT',
    dataType: 'json',
    contentType: 'application/json; charset=utf-8',
    data: data,
    success: function(data) {
      //refreshIframes();
      //keyEventBlocked = true;
      //enable(button);
      console.log('success');
    }
  });
}

function addResource() {
  //var pos = $('#group_' + groupId + ' > div.nested').length;
  //console.log('addNode(' + type + ', ' + resourceId + ', ' + elementId + ', ' + pos + ')');
  var url = rootUrl + 'resource';
  var data = '{ "type" : "resource", "name" : "resource_' + Math.floor(Math.random() * (9999 - 1)) + '" }';
  //console.log(data);
  var resp = $.ajax({
    url: url,
    //async: false,
    type: 'POST',
    dataType: 'json',
    contentType: 'application/json; charset=utf-8',
    data: data,
    success: function(data) {
      var nodeUrl = resp.getResponseHeader('Location');
      //console.log(nodeUrl);
      //setPosition(groupId, nodeUrl, pos);
      refreshMain();
    }
  });
}

function showElementsOfResource(resourceId, id) {
  var follow = followIds(resourceId, id);
  $(follow).each(function(i, nodeId) {
    if (nodeId) {
      console.log(rootUrl + nodeId);
      $.ajax({
        url: rootUrl + nodeId,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        //headers: { 'X-User' : 457 },
        success: function(data) {
          if (!data || data.length == 0 || !data.result) return;          
            var result = data.result;
          console.log(result);
          appendElement(resourceId, id, result);
          showElementsOfResource(resourceId, result.id);
        }      
      });
    }
  });
}