'use strict';

export class Rest {

    constructor() {
        this.config = {};
    }

    async get(url) {

        return (await fetch(url, {
            headers: this.headers(),
            method: 'GET',
            credentials: 'same-origin'
        })).json();

    }

    async put(url, data) {

        return (await fetch(url, {
            headers: this.headers(),
            method: 'PUT',
            body: JSON.stringify(data),
            credentials: 'same-origin',
        })).json();
    }

    async post(url, data) {

        return (await fetch(url, {
            headers: this.headers(),
            method: 'POST',
            body: JSON.stringify(data),
            credentials: 'same-origin'
        })).json();
    }

    async delete(url, data) {

        return (await fetch(url, {
            headers: this.headers(),
            method: 'DELETE',
            body: JSON.stringify(data),
            credentials: 'same-origin'
        })).json();
    }

    headers() {
        let result = new Headers();
        //result.set("X-User", this.config['user']);
        //result.set("X-Password", this.config['password']);
        return result;
    }



}
