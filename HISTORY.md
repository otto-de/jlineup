# JLineup Release History

## Release 4.13.8 - 2025-06-27

* Feature: The web version exposed a prometheus endpoint now.
* Update: Selenium 4.33.0
* Update: Dependency updates

## Release 4.13.7 - 2025-05-06

* Update: Selenium 4.32.0
* Update: Dependency updates

## Release 4.13.6 - 2025-04-14

* Fix: Release 4.13.5 does not include executable jars

## Release 4.13.5 - 2025-04-11

* Update: Selenium 4.31.0
* Update: Further dependency updates
* Internal Feature: JReleaser is now used to release JLineup.

## Release 4.13.4 - 2025-03-26

* Update: Selenium 4.30.0
* Update: Further dependency updates

## Release 4.13.3 - 2025-03-14

* Bugfix: Also add cookies for browsing to setup-paths and cleanup-paths
* Dependency updates

## Release 4.13.2 - 2025-01-18

* Feature: Add possibility to override the browser version via environment variables, command line parameter or
  `settings.properties`. Use `JLINEUP_CHROME_VERSION` or `jlineup.chrome-version` to set the Chrome version and
  `JLINEUP_FIREFOX_VERSION` or `jlineup.firefox-version` to set the Firefox version. This can be useful if you want to
  use a specific browser version for your tests. The command line options are named `--chrome-version` and
  `--firefox-version`. Selenium will try to download the specified version and a matching driver if it's not already
  installed on the system.
* Breaking change: If you used `management.endpoint.loggers.enabled = true` in your application.properties for the web
  variant, please replace it with `management.endpoint.loggers.access = unrestricted` to enable the loggers endpoint.

## Release 4.13.1 - 2024-08-23

* Feature: Add security feature that limits the allowed urls to certain defined prefixes for the web version. You can
  define those prefixes in the application properties with the `jlineup.allowed-urls-prefixes` property.
* Update:  Selenium 4.23.1
* Update:  Further Dependency Updates

## Release 4.13.0 - 2024-07-07

* Bugfix: The `--crop-last-screenshot` option was adjusted to fill cropped areas with pixels in the background color to
  match screenshot sizes.
* Feature: It's possible to add a `message` field to the job config. This message is displayed in the report.
* Feature: There's a new `approval-link` field in the job config. If set, the report will contain a link to this URL in
  case of a difference. This can be used to directly approve the differences to continue a CI run.
* Improvement: The lambda browser retries on more error messages now to improve stability.
* Dependency updates

## Release 4.12.1 - 2024-05-17

* Bugfix: Possible NullPointerException in Pixelmatch implementation
* Update: Selenium was updated to 4.21.0

## Release 4.12.0 - 2024-05-15

* Improvement: Set some more Chrome options to try to improve deterministic rendering
* Improvement: The max detected color difference is now printed in the report, additionally
  the delta is visualized in the diff image (brighter means more difference)
* Feature: Add option to crop last screenshot to match the previous screenshots on a scrolling page.
  The cli parameter is `--crop-last-screenshot` and can be set to true or false (false is default).
  The `JLINEUP_CROP_LAST_SCREENSHOT` environment variable can be used to set this also (in lambda for example).
  If you prefer to use a settings.properties file you can set `jlineup.crop-last-screenshot=true` in there.

## Release 4.11.1 - 2024-05-07

* Feature: New additional anti-alias detection ported from Pixelmatch. Runs automatically if
  ìgnore-anti-aliasing` is set to `true`.

## Release 4.11.0 - 2024-05-06

* Feature: Add `merge-config` field to the global config. The merge config can include a full config that is
  merged with the main config.
* Feature: Make max anti-alias color difference configurable via `max-anti-alias-color-difference` field in the
  global config.
* Improvement: Cookie, LocalStorage and SessionStorage values are now masked in the report and in logs to not
  accidentally leak sensitive data.
* Bugfix: The `setup-paths` and `cleanup-paths` feature did not work since introduction of the `--merge-config` option.

## Release 4.10.0 - 2024-04-10

* Feature: First release of the lambda module. You can now run the screenshots part of JLineup as a lambda function in
  AWS.

## Release 4.9.2 - 2023-12-19

* Dependency updates - especially Selenium 4.16.1

## Release 4.9.1 - 2023-10-05

* Feature: Add `--cleanup-profile` option to CLI to clean up the profile directory after a run.
* Feature: Add `jlineup.cleanupProfile` option to web version to clean up custom profile directories after a run.
  __If you use custom profile directories, you have to set this option to `false` to keep the profile.__

## Release 4.9.0 - 2023-08-16

* Bugfix: Recent changes to Chrome's webdriver management broke the automatic webdriver download
  This was realized through a change from the webdrivermanager library to Selenium's built-in driver manager
* **Breaking change**: Many dependency updates (Spring Boot 3, including the move from javax.servlet to
  jakarta.servlet packages) that bump the Java minimum requirements to Java 17.

## Release 4.8.4 - 2023-06-03

* Dependency updates

## Release 4.8.3 - 2023-03-09

* Bugfix: Add `--remote-allow-origins=*` to Chrome options to fix issues with Chrome 111 and the webdriver connection.
    * JLineup might update to Java 11 or 17 soon to switch to Java HttpClient
      (https://www.selenium.dev/blog/2022/using-java11-httpclient/).
    * Some background information: https://groups.google.com/g/chromedriver-users/c/xL5-13_qGaA/m/oTA9W3cQAgAJ
* Bugfix: Use screenshot height as truth for viewport height and print out a warning if it differs from the viewport
  height that is calculated via javascript. This should help us to overcome strange scrolling issues with Chrome 110+.

## Release 4.8.2 - 2023-02-22

* Bugfix: Chrome 110 crashed when specifying a `--user-data-dir` and using multiple threads. This was fixed by adding
  random child folders if you specify a user data directory in the web version. If you run the CLI version and you want
  to specify a user data dir, you can use the new `{random-folder}` placeholder to not share user data dirs between
  multiple threads.

## Release 4.8.1 - 2023-02-14 (Valentine's Day Edition <3)

* Bugfix: Headless mode in Chrome 110 has changed and didn't work anymore with former JLineup implementations.
  **Important: You may need Chrome 110 or later for this release of JLineup to work accordingly**

## Release 4.8.0 - 2023-02-14

* First attempt to fix headless mode in Chrome 110+, use 4.8.1 instead, which includes another bugfix.

## Release 4.7.0 - 2022-11-23

* Feature: --keep-existing option to keep existing 'before'-screenshots when doing another 'before' run
* Feature: --refresh-url option to refresh the 'before'-screenshots of one specific URL config entry only while keeping
  the others
* Feature: --merge-config option that merges the run config with another merge config file which may include repetitive
  options. The URL keys in the merge config file are matched by regex.
* Feature: The URL can not only be specified as the key of an URL config, but also by using the new `url` field instead.
  This opens the possibility to have multiple URL configs for the same URL. URL config keys still have to be unique.

## Release 4.6.3 - 2022-08-12

* Feature: --open-report or -o when using jlineup cli opens the HTML report after the run
* Bugfix: CLI jar size was accidentally increased by inclusion of lambda project

## Release 4.6.2 - 2022-08-11

* Bugfix: Report was broken in some cases when multiple URLs were configured

## Release 4.6.1 - 2022-07-27

* Bugfix: Write provisional report html under correct filename
* Dependency updates

## Release 4.6.0 - 2022-04-24

* Bugfix: All cookies were part of screenshot context hash calculation since version 4.4.0, which lead
  to problems when changing cookie content between before and after steps. To fix this, only alternating cookies
  (introduced in 4.4.0) are part of the context hash from this release on.

## Release 4.5.2 - 2022-03-31

* Updates to Spring Boot 2.6.6 and Spring Framework 5.3.18 to fix Spring4Shell issue

## Release 4.5.1 - 2022-03-30

* Dependency updates

## Release 4.5.0 - 2022-03-07

* Bugfix: Example config (--print-example) was not in the right format
* Remove PhantomJS support
* Dependency Updates (Selenium 4 etc.)

## Release 4.4.0 - 2022-01-13

* New HTML report format
* New `error-signals` feature in `http-check` which allows to check the targeted page not only by http return code
  but by strings in response body
* New `alternating-cookies` feature which allows to iterate over different cookie setups per page easily

## Release 4.3.4 - 2021-08-26

* Fix rounding bug in maxDiff configuration (https://github.com/otto-de/jlineup/issues/82)

## Release 4.3.3 - 2021-07-08

* Dependency updates

## Release 4.3.2 - 2021-03-19

* Dependency updates

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
* Fix Exception with the latest Firefox
* Minor improvements to HTML Report

## Release 4.0.0-rc2

* Only show often used options and non-defaults in config prints by default, example-config shows everything

## Release 4.0.0-rc1

* Massively improved similar colors detection
* Anti-Alias detection option
* DeviceConfig option

## Release 3.0.3

* Update of Webdrivermanager to support the latest browsers
* Replace GSON with Jackson, which is used anyway for the web module with Spring
* JLineup can be build with Java 11
* Update multiple dependencies

## Release 3.0.2

* Bugfix: Implemented workaround for strange scrolling behavior in Firefox (sometimes window.scrollBy(0, x) scrolls
  double the amount given)

## Release 3.0.1

* JLineup detects webdriver that fits to installed browsers automatically

## Release 3.0.0

* Massively improved documentation
* chromedriver 2.45
* Some chrome flags are added to improve deterministic rendering

## Release 3.0.0-rc7

* Revert because it didn't work: Ability to wait for images to be fully loaded before making screenshots
* hide all images using javascript once the URL configuration `hideImages` is enabled

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

* Enhancement: Add jobConfig option `check-for-errors-in-log` to toggle whether jlineup stops on errors in browser log.
  Default is `true`

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

* Bugfix: Exit correctly when URL is not reachable
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

* Cache warmup is always done with the greatest horizontal resolution of an URL jobConfig
* It's possible to specify session storage, similar to local storage
* Increasing waiting time bug fixed (long sleep phases when using multiple threads and many paths/URLs)

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

* Report image links work if URL contains ? and/or =

## Release 1.0.10

* Report image links work if URL contains hash (#) characters

## Release 1.0.9

* Report is interactive now

## Release 1.0.8

* Switch to Selenium 3
* New --debug option to be more verbose

## Release 1.0.7

* Scroll to top via JavaScript after initial page load
  (new Chrome sometimes moves a re-opened page to the last known scroll position)
