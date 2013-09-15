BPMNData Extensions for the camunda BPM platform
================================================

This prototype implements a model-driven approach for building process choreographies from A to Z. The methodology has been developed by Kimon Batoulis, Sebastian Kruse, Thorben Lindhauer and Thomas Stoff during their Master studies at Hasso-Plattner-Institute Potsdam. This distribution of the execution platform is accompanied by an extended version of the camunda modeler. See the [Downloads](#downloads) and [Links](#links) section for binaries. Confer the [Extensions documentation](bpmn-data-docs/extension-overview.md) for technical details on the extensions we made in the platform.

Building engine & distro
------------------------

Build the complete platform by running `mvn clean install` from the root folder.

A shortcut is to build only those artifacts that we have extended and resolve the rest from camunda's repository. Proceed as follows:

1. Build the process engine: `cd /engine` -> `mvn clean install`
2. Build the BPMNData endpoint: `cd /bpmn-data-endpoint` -> `mvn clean install`
3. Build the web applications: `cd /webapps/camunda-webapp` -> `mvn clean install`
4. Build the Tomcat distro: `cd /distro/tomcat` -> `mvn clean install`

The resulting Maven artifacts follow the camunda naming, but have different artifact ids: `camunda-engine` becomes `bpmn-data-camunda-engine` and so on.

Platform Configuration
----------------------

1. Check the Tomcat distro: `/server/apache-tomcat-{version}/lib` should containt the `bpmn-data-camunda-engine` artifact. `/server/apache-tomcat-{version}/webapps` should contain `bpmn-data-endpoint.war`.
2. Check `/server/apache-tomcat-{version}/conf/bpm-platform.xml`: The default engine should have a proerty `bpmnDataAware` set to true. Furthermore, you can set the database connection properties for the data object persistence in the `bpmnDataProperties` element. Note that this prototype is restricted to MySQL databases. `dataJobRetryTimeCycle` allows to configure the timeout between two attempts to check the data input of a given activity. It follows the [ISO 8601](http://en.wikipedia.org/wiki/ISO_8601#Repeating_intervals) standard. The default is set to 20 seconds. (In general, you can also configure multiple engines with these extensions. However, please note that you can currently only use the default engine for BPMNData message exchanges. See the [BPMNData Endpoint docs](bpmn-data-docs/extension-overview.md#bpmndata-endpoint) for an explanation)
3. When running multiple instances on the same machine, you may also want to check `/server/apache-tomcat-{version}/conf/server.xml` and set different ports.

Enacting Choreographies
-----------------------

Confer the [usage docs](bpmn-data-docs/modeler-engine-integration.md) on how to deploy a developed process model.

Downloads
---------

* [BPMNData modeler update site](https://www.dropbox.com/s/a5yfx130sy04gbd/update-site.zip)
* [BPMNData Tomcat distro](https://www.dropbox.com/s/d4xifu61a7pyryn/camunda-bpm-tomcat-7.0.0-Final.zip)
* [BPMNData Demo Process Applications](https://www.dropbox.com/s/44odwcjogajsrhp/demo_process_applications.zip)


Links
-----

* [BPMNData modeler sources](https://github.com/BPMNData/camunda-modeler)
* [BPMNData modeler usage](https://github.com/BPMNData/camunda-modeler/blob/bpmn_data/documentation/BPMNData.md)
* [BPMNData modeler screencast](https://www.dropbox.com/s/p7gp28a2hbt0w5c/modeler-demo.mp4)
* [BPMNData project page](http://bpt.hpi.uni-potsdam.de/Public/BPMNData)