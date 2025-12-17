'use strict';

let debug = false;

import {Rest} from './Rest.js';

export class StructrRest {

    constructor(basePath = "") {
        this.restEndPoint = basePath + "/structr/rest/";
        this._rest = new Rest();
    }

    get(type, view) {
        let query = this.restEndPoint + type;
        if (view !== undefined && view !== null && view.length > 0) {
            query += ("/" + view);
        }
        return this._rest.get(query);
    }

    getById(type, id, view) {
        let query = this.restEndPoint;
        if (type !== undefined && type !== null && type.length > 0) {
            query += (type + "/");
        }

        query += id;

        if (view !== undefined && view !== null &&view.length > 0) {
            query += ("/" + view);
        }

        return this._rest.get(query);
    }

    getByName(type, name, view) {
        let query = this.restEndPoint;
        if (type !== undefined && type !== null && type.length > 0) {
            query += (type + "/");
        }

        if (view !== undefined && view !== null && view.length > 0) {
            query += view;
        }

        query += ("?name=" + name);

        return this._rest.get(query);
    }

    put(type, id, data) {
        return this._rest.put(this.restEndPoint + type + "/" + id, data);
    }

    post(type, data) {
        return this._rest.post(this.restEndPoint + type, data);
    }

    delete(type, id) {
        return this._rest.delete(this.restEndPoint + type + "/" + id);
    }

}

if (debug === true) {
    window._structrRest = new StructrRest();
}

