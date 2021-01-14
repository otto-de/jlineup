[![JLineup](docs/jlineup-logo_small.png)](#)

## Status

![Build](https://github.com/otto-de/jlineup/workflows/Build/badge.svg)
Core: [![Known Vulnerabilities](https://snyk.io/test/github/otto-de/jlineup/badge.svg?targetFile=core/build.gradle)](https://snyk.io/test/github/otto-de/jlineup?targetFile=core/build.gradle)
CLI: [![Known Vulnerabilities](https://snyk.io/test/github/otto-de/jlineup/badge.svg?targetFile=cli/build.gradle)](https://snyk.io/test/github/otto-de/jlineup?targetFile=cli/build.gradle)
Web: [![Known Vulnerabilities](https://snyk.io/test/github/otto-de/jlineup/badge.svg?targetFile=web/build.gradle)](https://snyk.io/test/github/otto-de/jlineup?targetFile=web/build.gradle)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.otto/jlineup-cli/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.otto/jlineup-cli)

## About

JLineup is a tool which is useful for automated visual regression tests of web pages, especially in continuous delivery pipelines.
It can be used as a simple command line tool or as a small web service which is controlled via REST API.

JLineup shoots and compares screenshots of a web page at two consecutive points in time.
It does a pixel by pixel comparison of both runs and generates a HTML and a JSON report.
Behind the scenes, it uses Selenium and a browser of choice (currently Chrome, Firefox and
PhantomJS¹ are supported).

JLineup is a configuration compatible replacement
for Lineup, implemented in Java. The original
[Lineup](https://github.com/otto-de/lineup) is
a Ruby tool, but is not maintained any more.

Credit for original Lineup goes to [Finn Lorbeer](http://www.lor.beer/).

JLineup has no other dependencies than a JVM and a browser.

## Example

Let's take this little example [config](docs/CONFIGURATION.md) for a check of two paths on otto.de during a deployment:

```json
{
  "urls": {
    "https://www.otto.de": {
      "paths": [ 
	"/wohnen/?thema=thmn123nol_retro_chic",
	"/k/10072934231?a=224673"
      ],
      "window-widths": [ 500,1000,1200,1600 ]
    }
  },
  "wait-after-page-load" : 3,
  "window-height" : 800,
  "browser" : "chrome-headless"
}
```

JLineup runs before and after the deployment and generates a report like this:

[![Screenshot of HTML report](docs/html-report.png)](https://otto-de.github.io/jlineup/docs/example-report/report.html)

There's also a JSON report, which is great if you want to check things by script:

[Example JLineup JSON Report](docs/example-report/report.json)

## Quick Howto

JLineup CLI comes as executable Java Archive. Java 8 or higher has to be available to run it.

Open a terminal and download it like this:

    wget https://repo1.maven.org/maven2/de/otto/jlineup-cli/4.3.0/jlineup-cli-4.3.0.jar -O jlineup.jar

Then type

    java -jar jlineup.jar --help

to see the command line help.

See the [CLI documentation](docs/CLI.md) for more details and a small tutorial.

## Integration example

This is an example, how JLineup can be helpful in your automated build and deploy pipeline.
Let's assume, this is part of a continuous integration pipeline:

![Pipeline exampe](docs/pipeline-example.png)

## Browser Compatibility

JLineup 4.3.0 was tested successfully with

* Chrome 87.x
* Firefox 84.x
* PhantomJS 2.1.1 (auto-downloaded by JLineup if not installed)
        
Chrome or Firefox have to be installed on the system if you want to use one of them.

## Documentation

[JLineup as CLI tool](docs/CLI.md)

[JLineup as web server](docs/WEB.md)

[JLineup Job Configuration](docs/CONFIGURATION.md)

## Third Party Libraries

JLineup uses some third party tools and libraries

##### Selenium

* [Selenium](http://www.seleniumhq.org/) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

##### Webdrivermanager

* [Webdrivermanager](https://github.com/bonigarcia/webdrivermanager) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

##### Jackson

* [Jackson](https://github.com/FasterXML/jackson) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0) 

##### Logback

* [Logback](http://logback.qos.ch/) is licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html)
* The [SLF4J](http://www.slf4j.org) API is licensed under the [MIT License](http://www.slf4j.org/license.html)

##### PhantomJS

* [PhantomJS](http://phantomjs.org/) is licensed under the [BSD-3-Clause](https://github.com/ariya/phantomjs/blob/master/LICENSE.BSD)

##### Thymeleaf

* [Thymeleaf](http://www.thymeleaf.org/) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)


##### Edison Microservice

* [Edison Microservice](https://github.com/otto-de/edison-microservice) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

##### Spring Boot

* [Spring Boot](http://spring.io/projects/spring-boot) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)



### Contact

If you have questions or proposals, please open an issue or write an email to marco DOT geweke AT otto.de

### Footnotes

¹) PhantomJS Development has been suspended. For more details go to https://github.com/ariya/phantomjs/issues/15344

