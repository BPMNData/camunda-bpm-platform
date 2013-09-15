BPMNData Extensions for the camunda BPM platform
================================================

In the following we give an overview of the extensions made to the camunda BPM platform to implement our concepts.

Additions/Extensions to the camunda modules
-------------------------------------------

### Engine

The core functionality has been added to the engine. The following gives a rough overview of the components added/extended


#### Data Input Jobs

We have implemented the repeated checking of data inputs against the database by leveraging the platform's [job execution concept](http://docs.camunda.org/guides/user-guide/#!/#job-executor). That means, whenever a task is approached, a job is created to asynchronously check the input. This allows to reuse the platforms capabilities for thread management and retrying failed jobs (i.e. tasks with insufficient data input).

Central classes and methods:

* [ExecutionEntity#performOperation](/engine/src/main/java/org/camunda/bpm/engine/impl/persistence/entity/ExecutionEntity.java): Schedules data input jobs for every executed activity that has data inputs
* [AsyncDataInputJobHandler](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/job/AsyncDataInputJobHandler.java): Responsible for processing jobs. Invokes data input checking and transformation of SQL results to XML.


#### Data Input Checking

The approach chosen for data input checking is the following: We abstractly describe the constellation of data input objects of an activity and generate SQL statements based on this description. This also allows to test the different BPMNData patterns isolated from process execution. Confer [DataObjectSpecificationTest](/engine/src/test/java/de/hpi/uni/potsdam/test/bpmn_to_sql/pattern/DataObjectSpecificationTest.java) for tests of some of the patterns. For SQL generation, see below.

Central packages, classes and methods:

* [RefactoredDataInputChecker](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/execution/RefactoredDataInputChecker.java): Builds data object specifications out of the data objects of a task.
* [pattern package](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/pattern): Provides a fluent API for specifying data object constellations and retrieving SQL statements for these.
* [DataObjectSpecification](/de/hpi/uni/potsdam/bpmn_to_sql/pattern/DataObjectSpecification.java): Entry point for declaratively building data object SQL statements. The example below returns a SQL statement that selects the number of line items in state `created` that references an order with id 42.

    `anyDataObject("LineItem", "lid").attribute("state", "created").references("oid", dataObject("Order", "oid", "42")).getSelectCountStatement();`


#### Data Output

Making updates to data objects follows the same idea as data input checking.

Central packages, classes and methods:

* [BpmnActivityBehavior#performOutgoingBehavior](/engine/src/main/java/org/camunda/bpm/engine/impl/bpmn/behavior/BpmnActivityBehavior.java): Invokes output data handling for every activity.
* [RefactoredDataOutputHandler](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/execution/RefactoredDataOutputHandler.java): Declaratively describes the data object constellation, similar to `RefactoredDataInputChecker`. Distinguishes between update, delete and insert cases. Furthermore retrives the xQuery result from the local `dataOutput` variable to populate the data objects with message content. See below for the xQuery handling.


#### SQL Statement Generation

To transform the abstract data object specifications to plain SQL, we provide a strongly typed API.

Central packages, classes and methods:

* [sql package](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/pattern/sql): Implements statement building. The entry points are the classes `SelectStatement`, `InsertStatement`, `UpdateStatement`, `DeleteStatement`.


#### Message Transformation

Whenever an activity with output/input data and transformations is executed, xQuery expressions are evaluated.
Their result is stored in execution local variables. The typical variable flow is as follows:

* Data Input: The transformation handler sets the retrieved objects as XML in the variable `dataInput`. A send task consumes this variable.
* Data Output: A task (e.g. receive) sets the variable `dataOutput`. The transformation handler applies the xQuery transformation and sets the variable `dataObjects`, which in turn is used by the data output handler to create SQL statements. 

Central packages, classes and methods:

* [TransformationHandler](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/execution/TransformationHandler.java): Applies xQuery transformation for input and output associations.


#### Send/Receive messages

For message exchange, we have added `ActivityBehavior` implementations. 

* [BpmnDataSendTaskBehavior](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/behavior/BpmnDataSendTaskBehavior.java): Sends the message and populates the correlation keys associated with it. Therefore stores the correlation property values in execution variables, which are reused when an incoming message is correlated, which is the expected BPMN key-based correlation behavior.
* [BpmnDataMessageStartEventBehavior](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/behavior/BpmnDataMessageStartEventBehavior.java): Adds the ability to use receive tasks for process instantiation. Populates the correlation keys accordingly.


#### Correlation

An incoming message is either used to instantiate a new process definition or signal an existing one. 

Central packages, classes and methods:

* [BpmnDataCorrelationHandler](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/correlation/BpmnDataCorrelationHandler.java): Implements correlation. When matching to executions, the correlation keys and properties are evaluated by applying the retrieval expressions.
* [CorrelateBpmnDataMessageCmd](/engine/src/main/java/de/hpi/uni/potsdam/bpmn_to_sql/correlation/CorrelateBpmnDataMessageCmd.java): Ensures that the correlation handler is invoked and stores the message in a local variable to be accessible by send/receive tasks.


### BPMNData Endpoint

The project `bpmn-data-endpoint` implements a simple `POST` interface that takes incoming messages and invokes correlation. It dispatches every message to the default process engine which is why you can only use the default engine for inter-engine communcation. The url of this resource is the endpoint that has to be set in process models interacting with it. In the distro, the default address is `http://localhost:8080/bpmn-data-endpoint/message`.


### Web Apps

We have extended the tasklist application slightly by adding the `documentation` property of a task to its form.


### Tomcat Distro

We have extended the Tomcat distribution by adding our artifacts (engine, endpoint) and preconfiguring it with our additional engine configuration. You cannot use the Glassfish and JBoss distributions with this prototype. **Important note**: The camunda distributions use UUIDs as entity IDs by default as opposed to human-readable IDs with increasing integers in the embedded case. As the code makes an assumption about this format to derive the scope's name from its id, we have changed the IDGenerator on the platform.


Demo Applications
-----------------

The [examples folder](/examples/bpmn-data) contains process applications that implement the travel agency example process including process models, data models, database setup scripts, task forms and unit tests. You can build the three projects [demo_agency](/examples/bpmn-data/demo_agency), [demo_airline](/examples/bpmn-data/demo_airline) and [demo_customer](/examples/bpmn-data/demo_customer) and deploy them to running engines. Make sure that the endpoint addresses of the processes match your Tomcat configuration.