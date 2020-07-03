function test() {
    let value = 'test';
    let secret = 'test';

    return $.hmac(value, secret);
}

let _structrMainResult = test();