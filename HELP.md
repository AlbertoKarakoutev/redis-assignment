# Read Me First
The following was discovered as part of building this project:

# Getting Started

### Running the project
The project can be run using the built in `bootRun` gradle task. This will cause the relevant helper services to be brought up 
via the supplied docker compose configuration.

### Project Structure
This project defines two main modules:
* [`producer`](producer) - Responsible for streaming messages to a predefined redis channel `messages:published` which would be consumed. 
  Arguments are based on environmental variables, which are passed to the container when running.
* [`consumer-group`](consumer-group) - Responsible for defining processors for the produced messages. The processors use pooled connections whenever possible  
  in order to use up less resources. The processors also use the reactive Redis client implementation as much as possible, which allows for a smoother, 
  non-blockng execution of the program.

### Main functionality
 * `RedisConnectionService` - Used for managing redis connections. Can be used to obtain a sync, async and reactive connection from a connection pool.
 Also used to connect to pub/sub channels
 * `RedisLockService` - Used for obtaining locks when processing separate messages
 * `AbstractConsumer` - Abstraction for a message processor, which parses, validates, processes and records messages. Uses reactive subscription to an incoming
  pub/sub channel and processes incoming messages. Can be further extended with specific types, in this case by `JsonConsumer`, and furthermore - `MessageIdJsonConsumer`.
  After initialization, the consumer is registered in a specific key and is considered active. The key is marked with an expiration and the consumer is responsible for refreshing it
  so that in the event that a consumer fails. it will be deregistered from the hash set.
 * `StateReportService` - Responsible for aggregating data for a past period and reporting processing speeds 

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* redis: [`redis:latest`](https://hub.docker.com/_/redis)
* redis-producer: [`redis-producer`](producer/src/main/resources/redis-producer.py)

When running the project in dev mode, the docker compose plugin is used and automatically brings up the proper environment. The `redis-producer` image is 
custom-built using docker tools for gradle, as seen in the [build file](producer/build.gradle.kts).

Please review the tags of the used images and set them to the same as you're running in production.

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/3.4.4/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`redis:latest`](https://hub.docker.com/_/redis)

### Feedback

Feedback

Such a technical assignment is not a one-way street. We want to learn what you think about it.

#### How hard did you find the tasks?
There are a lot of ways to approach the problem and finding the one that seems best was the hardest task 
Answer: 3

#### Do you think that the estimated times for the tasks are realistic?
Answer: Depending on the depth of the implementation. A code base and it's underlying mechanics can always be improved, so considering a project as *completed* can be a hard thing.
For covering the core requirements - 2

#### How useful did you find the task?
The task covered a lot of knowledge and preparing the solution was informative and interesting
1

### Suggestions
1. Preparing the producer as a stream, instead of using pub/sub could remove the need for creating locks. Using a stream, 
the message can be `xack`-ed, and we can move on more smoothly
2. As implemented - setting an expiration on the consumer keys helps by offloading the responsibility of deregistering them to the built-in redis functionality
3. Writing the processed messages in a set/sorted set, instead of a stream would allow for checking if a message has already been processed in the past. 
Of course, the need for such a check depends on the use case and this example problem doesn't require it, but in some cases it could be better
