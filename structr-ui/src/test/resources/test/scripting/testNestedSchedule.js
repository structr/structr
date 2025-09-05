$.log("Starting schedule test.")

// Test data
const expectedLogContent =
`Storing: 21
outer
inner
inner2
inner3
Current Data: 21
Storing: 11
Current Data: 11
Final data: 11`;

// Helper functions
const getCurrentData = () => {
    const data = $.applicationStore.scheduleTestData;
    logTask(`Current Data: ${data}`)();
    return data;
};


// Task definitions
const scheduleTasks = (...tasks) => () => tasks.forEach(t => $.schedule(t));

const logTask = (str) => () => {
    if (!$.applicationStore.scheduleTestLog) {
        $.applicationStore.scheduleTestLog = str;
    } else {
        $.applicationStore.scheduleTestLog += `\n${str}`;
    }
}

const storeTask = (data) => () => {
    logTask(`Storing: ${data}`)();
    $.applicationStore.scheduleTestData = data
};

const cleanStoreTask = () => () => {
    $.applicationStore.scheduleTestData = null;
    $.applicationStore.scheduleTestLog = null;
    $.applicationStore.scheduleTestValidationPassed = false;
}

const validateLogTask = () => () => {
    $.applicationStore.scheduleTestValidationPassed = $.applicationStore.scheduleTestLog === expectedLogContent;
    logTask(`Validation passed: ${$.applicationStore.scheduleTestValidationPassed}`)();
}

// Test
scheduleTasks(
    // Clean application store for test
    cleanStoreTask(),
    // Store constant value
    storeTask(21),
    // Perform basic logging test
    logTask("outer"),

    // Sub-schedule tasks
    scheduleTasks(

        // More logging tests
        logTask("inner"), logTask("inner2"), logTask("inner3"),

        // 3rd layer of sub-scheduling with a logging test
        scheduleTasks(logTask("innerMost")),

        // Custom task to manipulate current data at the time of evaluation
        () => {
            const data = getCurrentData();
            storeTask((data - 10))();
        },

        // Get and log the final data value at the time of evaluation
        () => logTask((`Final data: ${getCurrentData()}`))(),

        // Validate the produced log with the expected log
        validateLogTask(),

        // Print the produced log
        () => $.log($.applicationStore.scheduleTestLog),
    )
)();

$.log("Schedule test done.")