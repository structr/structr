var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

(function() {
	'use strict';

	var _callbackMethods;

	Graphbrowser.Control.ConnectionControl = function(callbackMethods){
		var self = this;
		self.name = 'ConnectionControl';
		_callbackMethods = callbackMethods;
	};

	Graphbrowser.Control.ConnectionControl.prototype.init =  function(){
		var self = this;

		_callbackMethods.conn = {
			getNodes: self.getNodes.bind(self),
			getData: self.getData.bind(self),
			saveGraphView: self.saveGraphView.bind(self),
			getGraphViews: self.getGraphViews.bind(self),
			getGraphViewData: self.getGraphViewData.bind(self),
			getSchemaInformation: self.getSchemaInformation.bind(self),
			createRelationship: self.createRelationship.bind(self),
			deleteRelationship: self.deleteRelationship.bind(self),
			getStructrPage: self.getStructrPage.bind(self),
			getRelationshipsOfType: self.getRelationshipsOfType.bind(self)
		};
	};

	Graphbrowser.Control.ConnectionControl.prototype.sendRequest = function(url, method, data, contentType){
		var dataString = undefined;
		if (window.domainOnly && window.domainOnly === true) {
			url += '?domainOnly=true';
		}
		if(contentType === undefined) contentType = "application/json";
		if(typeof data === "object" || typeof data === "string" || $.isArray(data))
			dataString = JSON.stringify(data);

		return $.ajax({
			url: url,
			data: dataString || data,
			contentType: contentType,
			method: method
		});		
	};

	Graphbrowser.Control.ConnectionControl.prototype.restRequest = function(ajax, onResult, attr, onError){
		ajax.done(function(data, textStatus) {
			onResult(data.result, attr);
		}).fail(function(jqXHR, textStatus, errorThrown){
			console.log("Graph-Browser-Rest: Status " + jqXHR.status + " - ERROR: " + errorThrown);
			if(onError)	onError(attr, jqXHR.responseJSON.message);
		});			
	};

	Graphbrowser.Control.ConnectionControl.prototype.htmlRequest = function(ajax, onResult, attr, onError){
		ajax.done(function(html, textStatus) {
			onResult(html, attr);
		}).fail(function(jqXHR, textStatus, errorThrown){
			console.log("Graph-Browser-Rest: Status " + jqXHR.status + " - ERROR: " + errorThrown);
			if(onError)	onError(attr, jqXHR.responseJSON.message);
		});			
	};

	Graphbrowser.Control.ConnectionControl.prototype.getNodes = function(node, inOrOut, onResult, view){
		var self = this;
		if(!view) view = "_graph"
		var url = "/structr/rest/" + node.id + "/" + inOrOut + "/" + view;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult, node);	
	};

	Graphbrowser.Control.ConnectionControl.prototype.getData = function(data, nodeOrEdge, onResult, view){
		var self = this;
		var url = "/structr/rest/";
		var method;
		if(view === undefined) view = "_graph";

		if($.isArray(data)){
			//resolver cannot handle array of relationships
			url += "resolver";	
			method = "POST";
		}  
		else{
			url += data.id + "/" + view;
			method = "GET";
		}

		var ajax = self.sendRequest(url, method, method === "POST" ? data : undefined);
		self.restRequest(ajax, onResult, nodeOrEdge);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getGraphViews = function(currentId, view, onResult){
		var self = this;
		var url = '/structr/rest/GraphView/' + view + '/?viewSource=' + currentId;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getGraphViewData = function(id, view, onResult){
		var self = this;
		var url = '/structr/rest/GraphView/' + view + '/' + id;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.saveGraphView = function(name, graph, data, viewSource, onFinished){
		var self = this;
		var newGraphview = {name: name, graph: graph, data: data, viewSource: viewSource};
		var url = '/structr/rest/GraphView'
		var ajax = self.sendRequest(url, "POST", newGraphview, 'application/json; charset=utf-8');
		self.restRequest(ajax, onFinished);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getSchemaInformation = function(onResult){
		var self = this;
		var url = '/structr/rest/_schema/';
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getRelationshipsOfType = function(relType, onResult, onError){
		var self = this;
		var url = '/structr/rest/' + relType;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult, undefined, onError);
	};

	Graphbrowser.Control.ConnectionControl.prototype.deleteRelationship = function(relId, onResult){
		var self = this;
		var url = '/structr/rest/' + relId;
		var ajax = self.sendRequest(url, "DELETE");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.createRelationship = function(source, target, relType, onResult, attr, onError){
		var self = this;
		var newRel = {sourceId: source, targetId: target};
		var url = '/structr/rest/' + relType;
		var ajax = self.sendRequest(url, "POST", newRel);
		self.restRequest(ajax, onResult, attr, onError);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getStructrPage = function(page, currentId, onResult, onError){
		var self = this;
		var url = "/" + page + "/" + currentId;
		var ajax = self.sendRequest(url, "GET");
		self.htmlRequest(ajax, onResult, currentId, onError);
	};
}).call(window);