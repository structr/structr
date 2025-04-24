
let tasks    = [];
let projects = [];

for (var i=0; i<6; i++) {
    tasks.push($.create('Task', {
        name: 'Task' + i,
        beginDate: new Date(2022, i, 1),
        endDate: new Date(2022, i, 20),
    }));
}

for (var i=0; i<4; i++) {

    let project = $.create('Project', { name: 'Project' + i });

    projects.push(project);

    project.tasks.push(tasks[i+0]);
    project.tasks.push(tasks[i+1]);
    project.tasks.push(tasks[i+2]);
}

// create complex search object
let project = projects[0];
let id = tasks[5].id;
let begin = new Date(2021, 0, 1);
let end = new Date(2023, 0, 1);

$.find('Task',
    $.predicate.and(
        $.predicate.equals('project', project),
        $.predicate.or(
            $.predicate.equals('beginDate', $.predicate.range(begin, end, true, false)),
            $.predicate.equals('endDate', $.predicate.range(begin, end, false, true)),
            $.predicate.and(
                $.predicate.equals('beginDate', $.predicate.range(null, begin)),
                $.predicate.equals('endDate', $.predicate.range(end, null))
            )
        ),
        $.predicate.not($.predicate.equals('id', id))
    )
);