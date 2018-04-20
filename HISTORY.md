# JLineup Release History

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
