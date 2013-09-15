Modeling and Enacting BPMNData Choreographies
=============================================

Please read the [modeler docs](https://github.com/BPMNData/camunda-modeler/blob/bpmn_data/documentation/BPMNData.md) on how to build an enhanced choreography model. The resulting process model can executed in the platform by following these steps:

Additional Manual Process Configuration
---------------------------------------

* Make sure that for every `message` element, `id` and `name` match and are unique. This can be already ensured when modeling the process.
* Ensure that message `id` and `name` match across process boundaries. For exaple, if you develop two process models implementing different participants of the same conversation, the representations of the same message have to be consistent.

Avoid Known Pitfalls
--------------------

* If you use a global data model with correlation properties that are not mapped in the schemamapping, this results in invalid `CorrelationProperty` elements in the XML

Building and Deploying a Process Application
--------------------------------------------

When deploying to the Tomcat distro, please follow the [camunda docs on how to build a Process Application](http://docs.camunda.org/guides/user-guide/) or look at an application from our demo use case, like the [travel agency application](/examples/bpmn-data/demo_agency). There is no additional deployment configuration neccessary when using a platform-shared process engine.

Testing a BPMNData process application
--------------------------------------

See [SqlDerivation](/engine/src/test/java/de/hpi/uni/potsdam/test/bpmn_to_sql/SqlDerivation.java) on how to test a process without message exchanges properly. We have added facilities to automatically setup the database before a test case and make assertions against it throughout the process. To inspect the database, a MyBatis mapping for the data object tables is required. See [an example mapping](/engine/src/test/resources/de/hpi/uni/potsdam/test/bpmn_to_sql/db) for the afore-mentioned test.

See [TravelAgencyTest](/examples/bpmn-data/demo_agency/src/test/java/de/hpi/uni/potsdam/test/bpmn_to_sql/final_use_case/agency/TravelAgencyTest.java) for a test case that tests the local process of the travely agency including message exchanges. To mock a BPMNData endpoint, we use the [WireMock](http://wiremock.org/) library.