<img src="docs/jlineup-logo-2024.png" width="500" />

## Status
[![Build](https://github.com/otto-de/jlineup/workflows/Build/badge.svg)](https://github.com/otto-de/jlineup/actions?query=workflow%3ABuild)
[![Maven Central](https://img.shields.io/maven-central/v/de.otto/jlineup-cli?label=maven-central&nbsp;cli)](https://search.maven.org/search?q=g:de.otto%20a:jlineup-cli%20v:RELEASE%20p:jar)
[![Maven Central](https://img.shields.io/maven-central/v/de.otto/jlineup-web?label=maven-central&nbsp;web)](https://search.maven.org/search?q=g:de.otto%20a:jlineup-web%20v:RELEASE%20p:jar)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/otto-de/jlineup?label=GitHub&nbsp;Release)](https://github.com/otto-de/jlineup/releases)
![OSS Lifecycle](https://img.shields.io/osslifecycle?file_url=https%3A%2F%2Fraw.githubusercontent.com%2Fotto-de%2Fjlineup%2Fmain%2FOSSMETADATA)
![](https://snyk-widget.herokuapp.com/badge/mvn/de.otto/jlineup-cli/badge.svg)

## About

JLineup is a tool which is useful for automated visual regression tests of web pages, especially in continuous delivery pipelines.
It can be used as a simple command line tool or as a small web service which is controlled via REST API.

JLineup shoots and compares screenshots of a web page at two consecutive points in time.
It does a pixel by pixel comparison of both runs and generates a HTML and a JSON report.
Behind the scenes, it uses Selenium and a browser of choice (currently Chrome, Chromium and Firefox are supported).

JLineup has no other dependencies than web browser (Firefox or Chrome/Chromium) and a JVM.
*Experimental*: There's also a self-contained Linux AMD64 build of the CLI version that doesn't even require a JVM.

## Example

Let's take this little example [config](docs/CONFIGURATION.md) for a check of otto.de during a deployment:

```json
{
  "urls": {
    "https://www.otto.de": {
      "paths": [ 
	    "/"
      ],
      "devices" : [ {
        "width" : 850,
        "height" : 600,
        "pixel-ratio" : 1.0,
        "device-name" : "DESKTOP",
        "touch" : false
      }, {
        "width" : 1000,
        "height" : 850,
        "pixel-ratio" : 1.0,
        "device-name" : "DESKTOP",
        "touch" : false
      }, {
        "width" : 1200,
        "height" : 1000,
        "pixel-ratio" : 1.0,
        "device-name" : "DESKTOP",
        "touch" : false
      } ]
    }
  },
  "wait-after-page-load" : 0.5,
  "browser" : "chrome-headless"
}
```

JLineup runs before and after the deployment and generates a report like this:

[![Screenshot of HTML report](docs/html-report.png)](https://otto-de.github.io/jlineup/docs/example-report/report.html)

There's also a JSON report, which is great if you want to check things by script:

[Example JLineup JSON Report](docs/example-report/report.json)

## Quick Howto

JLineup CLI comes as executable Java Archive. Java 17 or higher has to be available to run it.

Open a terminal and download it like this:

    wget https://repo1.maven.org/maven2/de/otto/jlineup-cli/4.13.4/jlineup-cli-4.13.4.jar -O jlineup.jar

Then type

    java -jar jlineup.jar --help

to see the command line help.

See the [CLI documentation](docs/CLI.md) for more details and a small tutorial.

## Integration example

This is an example, how JLineup can be helpful in your automated build and deploy pipeline.
Let's assume, this is part of a continuous integration pipeline:

![Pipeline exampe](docs/pipeline-example.png)

## Browser Compatibility

JLineup 4.13.4 was tested successfully with

* Chrome 134.x
* Firefox 136.x
        
Chrome or Firefox have to be installed on the system if you want to use one of them.

## Documentation

[JLineup as CLI tool](docs/CLI.md)

[JLineup as web server](docs/WEB.md)

[JLineup Job Configuration](docs/CONFIGURATION.md)

## Third Party Libraries

JLineup uses some third party tools and libraries

##### Selenium

* [Selenium](http://www.seleniumhq.org/) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

##### Webdrivermanager

* [Webdrivermanager](https://github.com/bonigarcia/webdrivermanager) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

##### Jackson

* [Jackson](https://github.com/FasterXML/jackson) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

##### Logback

* [Logback](http://logback.qos.ch/) is licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).
* The [SLF4J](http://www.slf4j.org) API is licensed under the [MIT License](http://www.slf4j.org/license.html).

##### Thymeleaf

* [Thymeleaf](http://www.thymeleaf.org/) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

##### Edison Microservice

* [Edison Microservice](https://github.com/otto-de/edison-microservice) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

##### Spring Boot

* [Spring Boot](http://spring.io/projects/spring-boot) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

##### GraalVM Community Edition

* The binary cli version is built with [GraalVM Community Edition](https://github.com/oracle/graal/). GraalVM CE brings it's own [Product License](https://github.com/oracle/graal/blob/master/LICENSE).

##### Image Compare Viewer

* [Image Compare Viewer](https://github.com/kylewetton/image-compare-viewer) is licensed under the [MIT License](https://github.com/kylewetton/image-compare-viewer/blob/master/LICENSE).

##### Google Fonts

* [Google Fonts](https://developers.google.com/fonts/) are used in the HTML report. These are the [terms](https://developers.google.com/fonts/terms).

##### AWS SDK

* [AWS SDK for Java 2.0](https://github.com/aws/aws-sdk-java-v2) is licensed under the [Apache 2.0 License](https://github.com/aws/aws-sdk-java-v2?tab=Apache-2.0-1-ov-file#readme).

##### Pixelmatch

Some code from Pixelmatch was ported to Java for JLineup.
* [Pixelmatch](https://github.com/mapbox/pixelmatch) is licensed under the [ISC License](https://github.com/mapbox/pixelmatch?tab=ISC-1-ov-file#readme).

##### Looks-same

Some code from Looks-same was ported to Java for JLineup.
* [Looks-same](https://github.com/gemini-testing/looks-same) is licensed under the [MIT License](https://github.com/gemini-testing/looks-same#MIT-1-ov-file).

### Historic Facts

JLineup is a configuration compatible replacement
for Lineup, implemented in Java. The original
[Lineup](https://github.com/otto-de-legacy/lineup) was
a Ruby tool, but is not maintained any more.

Credit for original Lineup goes to [Finn Lorbeer](http://www.lor.beer/).


### Contact

If you have questions or proposals, please open an issue or write an email to marco DOT geweke AT otto.de

###

<div align="center"><img src="docs/jlineup-logo-2024.png" width="150" /></div>	

