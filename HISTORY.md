# JLineup Release History

## Release 4.3.1 - 2021-01-16
* Bugfix: CLI version is compiled with Java 8 compatibility again
* Dependency updates

## Release 4.3.0 - 2021-01-12
* Disable PhantomJS by default in web version (Reason: https://nvd.nist.gov/vuln/detail/CVE-2019-17221)
* Add /exampleRun controller endpoint for quick testing
* Java 11 or later is required for web version now, cli variant works with Java 8
* Web version has a basic job persistence in file system now which survives service restarts
* Add OWASP dependency check to build for improved security

## Release 4.2.1 - 2020-10-12
* Add possibility to use runId ({id}) in browser launch parameters

## Release 4.2.0 - 2020-06-09
* New `setup-paths` and `cleanup-paths` feature to set up and cleanup test data on target systems.
* New `jlineup.sleep(milliseconds)` function inside simple JS blocks

## Release 4.1.0
* New `wait-for-selectors` and `remove-selectors` features.

## Release 4.0.0
* New device-config feature (which adds mobile emulation support in chrome and more)

## Release 4.0.0-rc4
* Use only relevant fields in context hash calculation (fixes bugs if 'before' and 'after' configs differ)

## Release 4.0.0-rc3
* Use latest Webdrivermanager
* Fix Exception with latest Firefox
* Minor improvements to HTML Report

## Release 4.0.0-rc2
* Only show often used options and non-defaults in config prints by default, example-config shows everything

## Release 4.0.0-rc1
* Massively improved similar colors detection
* Anti-Alias detection option
* DeviceConfig option

## Release 3.0.3
* Update of Webdrivermanager to support latest browsers
* Replace GSON with Jackson, which is used anyway for the web module with Spring
* JLineup can be build with Java 11
* Update multiple dependencies

## Release 3.0.2
* Bugfix: Implemented workaround for strange scrolling behavior in Firefox (sometimes window.scrollBy(0, x) scrolls double the amount given)

## Release 3.0.1
* JLineup detects webdriver that fits to installed browsers automatically

## Release 3.0.0
* Massively improved documentation
* chromedriver 2.45
* Some chrome flags are added to improve deterministic rendering

## Release 3.0.0-rc7
* Revert because it didn't work: Ability to wait for images to be fully loaded before making screenshots
* hide all images using javascript once the url configuration `hideImages` is enabled

## Release 3.0.0-rc6
* Ability to wait for images to be fully loaded before making screenshots. 
Especially important when using progressive jpegs in page.
 
## Release 3.0.0-rc5
* chromedriver 2.42
* geckodriver 0.23.0
* Version info displayed in web frontend
* Fix cookie warning in httpcheck

## Release 3.0.0-rc2
* Fix pom artifactIds (this helps if you try to download JLineup via Maven/Gradle)

## Release 3.0.0-rc1
* Major Refactoring
* JLineup can run as web service now

## Release 2.4.1
* Enhancement: Add jobConfig option `check-for-errors-in-log` to toggle whether jlineup stops on errors in browser log. Default is `true`

## Release 2.4.0
* Improvement: Better handling of global timeout
* Dependency updates
* New project structure

## Release 2.3.5
* Bugfix: Don't crash when no webdriver was downloaded before

## Release 2.3.4
* New Selenium and Webdrivermanager versions

## Release 2.3.3
* Bugfix: Don't stop jlineup run when there are severe errors in chrome after initial loading of page

## Release 2.3.2
* Bugfix: Limit max file lenght for screenshot files

## Release 2.3.1
* Enhancement: New log-to-file option in jobConfig enables logging to a file in the working dir

## Release 2.3.0
* Enhancement: Animated gifs are not played in firefox.

## Release 2.2.1
* Bugfix: Exit correctly when url is not reachable
* Experimental support for headless firefox (needs firefox nightly >55)

## Release 2.2.0
* Add headless support without the need for Xvfb for Chrome >59 (use browser "chrome-headless" in jobConfig)

## Release 2.1.1
* Update to chromedriver 2.30 to support Chrome 59.x

## Release 2.1.0
* Feature: Introduce global page load timeout, defaults to 120 seconds
* Feature: Global retry option if some Exception occurs during takeScreenshots, defaults to 0

## Release 2.0.1
* Bugfix: Fix difference image link in html report
* Update Geckodriver to 0.16.1

## Release 2.0.0
* Breaking change: New json report format with summary. Use "report-format":1 to use legacy format for json report.
* Breaking change: Screenshots folder is now below report folder by default. HTML report is in report folder.
* Breaking change: JLineup returns error code 1 if report shows a difference
* Selenium upgrade to 3.4
* Support for Firefox 53.x, Chrome 58.x
* Geckodriver 0.16.0, Chromedriver 2.29

## Release 1.5.3
* Some exceptions lead to stalling browser sessions, now every exception is thrown

## Release 1.5.2
* Bugfix: Close open browsers and webdrivers when some Selenium exception is throwing

## Release 1.5.1
* Bugfix: Don't start multiple webdriver downloads when running in multiple threads

## Release 1.5.0
* Update Geckodriver for Firefox to 0.15.0
* Update Chromedriver for Chrome to 2.28

## Release 1.4.1
* Bugfix: Version pinning of webdriver didn't work because of a change in webdrivermanager
  (https://github.com/bonigarcia/webdrivermanager/commit/29c531266c78399f3999b246da479163d734bee8)

## Release 1.4.0
* Cache warmup is always done with the greatest horizontal resolution of an url jobConfig
* It's possible to specify session storage, similar to local storage
* Increasing waiting time bug fixed (long sleep phases when using multiple threads and many paths/urls)

## Release 1.3.3
* It's possible to specify debug mode in jobConfig now ("debug":true), additionally to command line param

## Release 1.3.2
* Bugfix: JLineup 1.3.1 was not making any screenshots when wait-for-fonts-time was not 0

## Release 1.3.1
* Bugfix: Use legacy report format as default (will change with 2.0.0)

## Release 1.3.0
* Ability to wait for webfonts to be loaded before making screenshots

## Release 1.2.5
* Log uncaught Exceptions in Threadpool

## Release 1.2.4
* New wait after scroll option

## Release 1.2.3
* Move mouse pointer to 0,0 before making screenshots -
 some mouseover effects could affect the screenshots otherwise

## Release 1.2.2
* Use close before quitting webdrivers

## Release 1.2.1
* Fix for browser startup shuffle time

## Release 1.2.0
* It's possible to use multiple threads and browser instances to speed up

## Release 1.1.2
* --print-jobConfig now shows full example configuration

## Release 1.1.1
* The report contains the JLineup version at the bottom

## Release 1.1.0
* You can specify custom JavaScript that is executed before page screenshots start
  (i.e. useful to remove parts that change dynamically through third party
  - like ads - from the page)

## Release 1.0.12
* Don't strip http or https from image filenames
* Sanitize filenames properly

## Release 1.0.11
* Report image links work if url contains ? and/or =

## Release 1.0.10
* Report image links work if url contains hash (#) characters

## Release 1.0.9
* Report is interactive now

## Release 1.0.8
* Switch to Selenium 3
* New --debug option to be more verbose

## Release 1.0.7
* Scroll to top via JavaScript after initial page load
 (new Chrome sometimes moves a re-opened page to the last known scroll position)
