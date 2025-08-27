$.schedule(() => {

    $.doAs($.find('User', { name: 'A' })[0], () => {

        $.create('TestDoAs', { name: 'First - in schedule() and doAs() with user A' });

        $.schedule(() => {

            $.doAs($.find('User', { name: 'C' })[0], () => {
                $.create('TestDoAs', { name: 'Second - in schedule(), doAs(), schedule() and doAs() with user C' });
            });

        });

    });

    $.doAs($.find('User', { name: 'B' })[0], () => {
        $.create('TestDoAs', { name: 'Third - in schedule() and doAs() with user B' });
    });

    $.create('TestDoAs', { name: 'Fourth - in schedule() with superuser' });
});

$.create('TestDoAs', { name: 'Fifth - on root level with superuser' });