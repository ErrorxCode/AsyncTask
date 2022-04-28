
# AsyncTask ~ Threading made easy :-)

This library provides a convininence API for creating async tasks. A task is
a **callable** or a **function** that executes works syncronously and gives out
the result. You can perform that heavy bloaking-task on a background thread and can
publish the result on public thread with optional callbacks.





## Implementation

#### Gradle

Add it in your root build.gradle at the end of repositories
```groovy
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Add the dependency in your module build.gradle
```groovy
	dependencies {
	    implementation 'com.github.ErrorxCode:AsyncTask:1.0'
	}
```
#### Maven
Declare the repositories
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
Add the dependency
```xml
	<dependency>
	    <groupId>com.github.ErrorxCode</groupId>
	    <artifactId>AsyncTask</artifactId>
	    <version>1.0</version>
	</dependency>
```

## Usage/Examples
### Creating task
To run your code asynchronously, wrap it in a Callable and pass it to `callAsync()` method.

**Example**:
```java
AsyncTask task = AsyncTask.callAsync(() -> {
    ...    // do some blocking work
    return fetchSomethingOrThrow();
});
```

It takes a callable which if returns the value, the task is considered successful. If the callable throws an exception, the task is considered failed.

### Fetching task results
This method will immediately return the task and your code would be running in a background thread. Now you can set a listener for getting the result.
```java
task.setOnSuccessCallback(result -> {
    // your callable return value is here
}).setOnErrorCallback(e -> {
    // your callable exception is here
});
```
or else,
```java
task.setOnCompleteCallback(task -> {
    if (task.isSuccessful){
        // Success, get the result
        Object result = task.result;
    } else {
        // Error, get the exception
        Exception e = task.exception;
    }
});
```

### Chaining tasks
If you have multimple tasks and every next tasks depends on previous tasks
result then you can chain them using `thenPerform()` method.

**Example**:
```java
placeOrder("Plain rice")
        .thenPerform((AsyncFunction<String, String>) dish -> getOrderId(dish))
        .thenPerform((AsyncFunction<String, Integer>) id -> getOrderPrice(id))
        .thenPerform((AsyncFunction<Integer, Void>) price -> payAndEnque(price))
        .setOnSuccessCallback(result -> System.out.println("Order placed successfully"))
        .setOnErrorCallback(e -> System.out.println("Order failed"));
```
In this example, we have a placeOrder task which places a order with the dish name. Once 
the first task completed, its result i.e dish code is deliver to the next `thenPerform()`
method. This same thing happens till the last task and then finally overall **result** is
called. **If any of the task is failed, the overall result fails.** 

If you have multiple tasks but they are independent of each other, you can join
all of them to build a single task using `AsyncTask.allOff()` method.

**Example**
```java
AsyncTask<String> task1 = getTask();
AsyncTask<Integer> task2 = getTask();
AsyncTask<Boolean> task3 = getTask();

AsyncTask.allOf(task1, task2, task3)
        .setOnSuccessCallback(result -> {
            // Overall success
            System.out.println("All tasks completed successfully");
        })
        .setOnErrorCallback(e -> {
            // Overall error
            System.out.println("Any of the tasks failed");
        });
```

### Syncronous approch
If you want the result immediately and don't want to move further, you can use
`getResult()` method. This method will block the thread until the timed out or
result is fetched.

**Example:**
```java
try {
    String result = task.getResult(5);
} catch (Exception e) {
    // Failed
}
```

## That's it
That is all about what you need to know before implimenting it in your program.
A gread way would be to return the tasks from the methods of your API/library/class
so that client can handle it accordingly.


