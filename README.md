# JLineup

## Status

[![Travis CI](https://travis-ci.org/otto-de/jlineup.svg?branch=master)](https://travis-ci.org/otto-de/jlineup)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.otto/jlineup/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.otto/jlineup)
[![Dependency Status](https://www.versioneye.com/user/projects/58175e12d33a712754f2ab3d/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/58175e12d33a712754f2ab3d)

## About
JLineup is a command line tool which is useful for visual acceptance tests in continuous
delivery pipelines. It can make and compare screenshots from before and after a deployment
of a web page. Through comparison of the screenshots it detects every changed pixel.
JLineup generates a HTML report and a JSON report.
Behind the scenes, it uses Selenium and a browser of choice (currently Chrome, Firefox and
PhantomJS are supported).

JLineup is a configuration compatible replacement
for Lineup, implemented in Java. The original
[Lineup](https://github.com/otto-de/lineup) is
a Ruby tool. We did a rewrite in Java, because we can
leverage some quicker image comparison here and we can
get rid of Ruby in our JVM-based pipelines.

## Howto

JLineup comes as executable Java Archive.
You need a working Java 8 Runtime Environment on your system.
Open a terminal, navigate to the place where your jlineup.jar lives and type

    java -jar jlineup.jar --help
  
to get some idea how to use it.

## Browser compatibility

JLineup 1.5.1 was tested successfully with

* Chrome 56.0.x
* Firefox 52.0.x
* PhantomJS 2.1.1 (auto-downloaded by JLineup if not installed)
        
Chrome or Firefox have to be installed on the system if you want to use one of them.

## Third party libraries

JLineup uses some third party tools and libraries

##### Selenium

* [Selenium](http://www.seleniumhq.org/) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

##### Webdrivermanager

* [Webdrivermanager](https://github.com/bonigarcia/webdrivermanager) is licensed under the [GNU Lesser General Public License](https://www.gnu.org/licenses/lgpl-2.1.html)
  
##### JCommander

* [JCommander](http://jcommander.org/) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

##### Gson

* [google-gson](https://github.com/google/gson) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0) 

##### Logback

* [Logback](http://logback.qos.ch/) is licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html)
* The [SLF4J](http://www.slf4j.org) API is licensed under the [MIT License](http://www.slf4j.org/license.html)

##### PhantomJS

* [PhantomJS](http://phantomjs.org/) is licensed under the [BSD-3-Clause](https://github.com/ariya/phantomjs/blob/master/LICENSE.BSD)

##### Thymeleaf

* [Thymeleaf](http://www.thymeleaf.org/) is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0) 