'use strict';

export class FlowSockets {

    constructor() {
        this._sockets = {};

        let prevSocket = new D3NE.Socket('prev', 'Previous node', 'Connect a node\'s next port to this.');
        let nextSocket = new D3NE.Socket('next', 'Next node', 'Connect to a node\'s prev port.');
        nextSocket.combineWith(prevSocket);
        this._sockets['prev'] = prevSocket;
        this._sockets['next'] = nextSocket;

        let dataSource = new D3NE.Socket('dataSource', 'Data Source Node', 'The connected node will provide data for this node.');
        let dataTarget = new D3NE.Socket('dataTarget', 'Data Target Node', 'Connect to a node\'s prev port.');
        dataTarget.combineWith(dataSource);
        this._sockets['dataSource'] = dataSource;
        this._sockets['dataTarget'] = dataTarget;

        let parameters = new D3NE.Socket('parameters', 'Parameter Data Nodes', 'Multiple parameter nodes can be connected to this input.');
        let call = new D3NE.Socket('call', 'Call Node', 'Connects to a FlowCall and provides it with arguments.');
        call.combineWith(parameters);
        this._sockets['parameters'] = parameters;
        this._sockets['call'] = call;

    }

    static getInst() {
        if (window._flowSocketInst === undefined) {
            window._flowSocketInst = new FlowSockets();
        }
        return _flowSocketInst;
    }

    getSocket(key) {
        return this._sockets[key];
    }

}