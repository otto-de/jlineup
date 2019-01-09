# Configuration

JLineup has many configuration options. A JLineup cycle with it's _before_ and _after_ steps is called _job_ throughout
this documentation. Job configuration settings can be separated between two scopes, _global_ and _site_. Some of the
options exist in global as in site scope for convenience.

Site specific settings always have precedence before global settings.

### Format

A JLineup job config is a json document. 

For JLineup CLI, the default name of the config file is ___lineup.json___.
It's searched in the current working directory. A different path can be specified with the
`--config` option.

If using the web server variant, you have to POST the config via REST API.

### Basic config

A basic configuration with default settings can look like this:

```json
{
  "urls": {
    "https://www.otto.de": {
    }
  }
}
```
 
This config leads JLineup to https://www.otto.de. All other settings like window size, timeouts etc. stay default.

What about those __global__ and __site__ settings?

```json
{
  "urls": {
    "https://www.otto.de": {
        "->": "SITE SETTINGS GO HERE"
    }
  },
  "->": "GLOBAL SETTINGS GO HERE"
}
```
    
### Example

This basic example shows the concept of global and url settings:

```json
{
  "urls": {
    "http://www.otto.de": {
      "paths": [
        "/"
      ],
      "window-widths": [
        1200
      ],
      "max-scroll-height":50000
    },
    "http://www.example.com": {
      "paths": [
        "/",
        "somepath"
      ],
      "window-widths": [
        800,
        1000
      ],
      "max-scroll-height":10000
    }
  },
  "browser": "chrome",
  "wait-after-page-load": 1
}
```
    
As you can see, _paths_, _window-widths_ and _max-scroll-height_ are configured on url level, the _browser_ and the _wait-after-page-load_
options are global for all sites in this job.

### Full configuration

This is a full configuration with example values:

```json
{
  "urls": {
    "http://www.example.com": {
      "paths": [
        "/",
        "someOtherPath"
      ],
      "max-diff": 0.0,
      "cookies": [
        {
          "name": "exampleCookieName",
          "value": "exampleValue",
          "domain": "http://www.example.com",
          "path": "/",
          "expiry": "1970-01-01T01:00:01+0100",
          "secure": true
        }
      ],
      "env-mapping": {
        "live": "www"
      },
      "local-storage": {
        "exampleLocalStorageKey": "value"
      },
      "session-storage": {
        "exampleSessionStorageKey": "value"
      },
      "window-widths": [
        600,
        800,
        1000
      ],
      "max-scroll-height": 100000,
      "wait-after-page-load": 0.0,
      "wait-after-scroll": 0.0,
      "wait-for-no-animation-after-scroll": 0.0,
      "warmup-browser-cache-time": 0.0,
      "wait-for-fonts-time": 0.0,
      "javascript": "console.log(\u0027This is JavaScript!\u0027)",
      "hide-images": false,
      "http-check": {
        "enabled": false
      },
      "max-color-diff-per-pixel": 1
    }
  },
  "browser": "PhantomJS",
  "wait-after-page-load": 0.0,
  "page-load-timeout": 120,
  "window-height": 800,
  "report-format": 2,
  "screenshot-retries": 0,
  "threads": 0,
  "timeout": 600,
  "debug": false,
  "log-to-file": false,
  "check-for-errors-in-log": true,
  "http-check": {
    "enabled": false
  }
}
```

---

## Reference

What are all those options about? Here are all the details.

---

### `urls`

 JLineup job settings for one or multiple sites have to be configured as JSON subdocument here
 
 * Scope: Global
 * Type: JSON Document
 * Default: None - ***urls*** is a mandatory config option
 * Example: `"urls": {
                   "https://www.otto.de": {}
            }`
            
---
            
### `browser`

 Defines, which browser is used for the JLineup job. The chosen browser has to be installed on the used system.
 One exception is PhantomJS¹. If it's configured and not installed, JLineup downloads and uses it.
 
 *Advice*: PhantomJS shouldn't be used, because it lacks more and more features of modern web and it's not 
 maintained any more.¹
 
 JLineup downloads a webdriver, but it doesn't install a real browser during runtime! 
 
 * Scope: Global
 * Type: String
 * Possible Values: `PhantomJS`, `Chrome`, `Firefox`, `Chrome-Headless`, `Firefox-Headless`
 * Default: `"browser": "PhantomJS"`
 * Example: `"browser": "Chrome-Headless"`
 
 ¹) PhantomJS Development has been suspended. For more details go to https://github.com/ariya/phantomjs/issues/15344
 
---

### `paths`         
               
 These paths are appended to the current site url and screenshotted individually
                                 
 * Scope: Site
 * Type: List of Strings
 * Default: `[ "" ]`
 * Example: `"paths": [ "/", "someOtherPath" ]`

---
 
### `max-diff`

 This is the maximum accepted difference of a single screenshot of the current site run. If the difference is
 greater than the configured maximum, then the failing url + path + window-width combination AND the complete JLineup
 job are marked as erroneous.
 
 The difference of every single screenshot at every scrolling position is calculated as a percentage between 0% (0.0) 
 and 100% (1.0) which means that every pixel of the two compared images is different.
 
 *Advice:* This option should be handled with care. I strongly suggest a value of 0.0 (which is default) because every
 accepted pixel difference could be *any* unintended change - which should be looked at by a human being! A CI job can't
 decide what change is intended or not through simply accept a percentage of pixel differences. 
 
 * Scope: Site or Global
 * Type: Float
 * Default: `0.0`
 * Min: `0.0`
 * Max: `1.0`
 * Example: `"max-diff": 0.01`     
 
---
                 
### `cookies`

 A list of cookies that are set on the site. A cookie document can simply consist of `name` and `value`.
 Alternatively, you can specify a full cookie with `name`, `value`, `domain`, `path`, `expiry` and `secure`.
 See the example for details. The expiration time has to be written as ISO 8601 string.

 * Scope: Site
 * Type: List of cookie documents
 * Default: `{}`
 * Example: `
            "cookies": [
               {
                 "name": "exampleCookieName",
                 "value": "exampleValue",
                 "domain": "http://www.example.com",
                 "path": "/",
                 "expiry": "1970-01-01T01:00:01+0100",
                 "secure": true
               },
               {
                 "name": "anotherCookie",
                 "value": "anotherValue"
               }
            ]
            `
---
    
### `env-mapping`

 This is a convenience option that can replace parts of the domain that are specified in the config before making
 screenshots.
 
 This can be nice if you want to replace environments that are generated into your lineup.json that don't match the 
 real site url. One Example: At OTTO, the generated live step would result in a generated url like https://live.otto.de,
 which is in reality reachable under https://www.otto.de.   
                  
 * Scope: Site
 * Type: Map
 * Default: `{}`
 * Example: `
            "env-mapping": {
                "live": "www"
            }
            `
            
---

### `local-storage`

 Sets key value pairs to the local storage of the site
 * Scope: Site
 * Type: Map
 * Default: Empty
 * Example: `"local-storage": {
        "exampleLocalStorageKey": "exampleLocalStorageValue"
      }`
      
---
      
### `session-storage`

 Sets key value pairs to the session storage of the site
 * Scope: Site
 * Type: Map
 * Default: Empty
 * Example: "session-storage": {
        "exampleSessionStorageKey": "exampleSessionStorageValue"
      }`
      
---      
      
### `window-widths`
  
 Every path in the site config will be screenshotted in these given window-widths of the browser
* Scope: Site
* Type: List of integers
* Unit: Pixels
* Default: `[ 800 ]`
* Example: `"window-widths": [
        600,
        800,
        1000
      ]`
      
---      
      
### `max-scroll-height`

 When using Chrome or Firefox, JLineup scrolls the page like a normal browser user, because that's the way the web
 works today. Nearly no page fits the screen, and modern pages tend to grow to huge sizes. To not scroll endlessly on
 large pages, there is a maximum value.
 
 * Scope: Site
 * Type: Integer
 * Unit: Pixels
 * Default: `50000`
 * Example: `"max-scroll-height": 100000`
 
--- 
 
### `wait-after-page-load`

 On modern web pages with JavaScript and asynchronous loading mechanisms, it's hard to tell when a page is really 
 loaded completely. JLineup uses Selenium under the hood. Selenium's get() method blocks until the browser/page fires
 an onload event (files and images referenced in the html have been loaded, but there might be JS calls that load more
 stuff dynamically afterwards). This option waits for the specified amount of time after this onload event was fired.
 This value should be chosen carefully by observing the time your screenshotted page needs until it's complete.
 
 If your reports show half loaded pages, you should increase the value until this does not happen any more. If you set
 it too high, you waste time in your runs.
 
 * Scope: Site or Global
 * Type: Float
 * Unit: Seconds
 * Default: `0`
 * Example: `"wait-after-page-load": 3.5`
 
 ---
 
### `wait-after-scroll`

 If your reports show half loaded artifacts in lower parts of the page, you should increase this value until this does
 not happen any more. If you set it too high, you increase the time for site scrolling drastically. As long as your page
 does not load content dynamically during scrolling, you can keep this very low or at default.
 
 * Scope: Site
 * Type: Float
 * Unit: Seconds
 * Default: `0`
 * Example: `"wait-after-scroll": 1.1`
 
---
 
### `warmup-browser-cache-time`

 If this value is greater than 0, JLineup simply loads the page prior to the screenshot run and waits the specified
 amount of seconds to fill the browser cache with everything that can be cached during page load. It can be helpful
 to have a prewarmed cache while doing the first screenshots of a site in a run.

 * Scope: Site
 * Type: Float
 * Unit: Seconds
 * Default: `0`
 * Example: `"warmup-browser-cache-time": 5`
 
--- 

### `wait-for-fonts-time`

 If your web fonts load too slow, there may be cases that your page is rendered without a default alternate font,
 which is replaced by the loaded font during a screenshot run. You can explicitly wait for font loading with this
 option.
 
 This should be rarely needed. 

 * Scope: Site
 * Type: Float
 * Unit: Seconds
 * Default: 0
 * Example: `"wait-for-fonts-time": 3`
 
--- 
 
### `javascript`

 This is a mighty option. You can specify and run any given JavaScript after loading the page and prior to making the
 screenshots. This can be used for various things, like throwing things out of the DOM by id or class, replacing images etc.
 Be creative but careful :D
 
 * Scope: Site
 * Type: String
 * Default: `null`
 * Example: `"javascript": "console.log('This is JavaScript!')"`
 
--- 

### `hide-images`

 This option can be used to hide all images on the page before making screenshots. This can be useful, if you only
 want to check the layout, but not the possibly changing content of pictures on a site.

 * Scope: Site
 * Type: Boolean
 * Default: `false`
 * Example: `"hide-images": true`
 
---

### `http-check`

 It may be required to check connectivity to a page by checking a http return code before making screenshots of it.
 Real browsers don't report a return code to Selenium, because Selenium looks at a page from a user perspective.
 JLineup can make this extra check for any desired http return codes.
 
 This can be helpful, if you want to fail a JLineup job if the page shows a 404. If the *before* and *after* run are
 done on a non-reachable page without a HTTP check, the job would return no error - both error pages are 
 visually identical.
 
 If `enabled` is set to true, all return codes that are not in the whitelist of allowed codes are regarded as failure.
 If you don't explicitly specify allowed codes in the http-check document, there is a default list of these accepted 
 HTTP return codes: `200,202,204,205,206,301,302,303,304,307,308`
 
 * Scope: Site or Global
 * Type: JSON Document
 * Default: `{ "enabled": false, allowed-codes: [ 200,202,204,205,206,301,302,303,304,307,308 ] }`
 * Example: `
              "http-check": {
                "enabled": true,
                "allowed-codes": [
                  200,
                  202,
                  204,
                  205,
                  206
                ]
              }
            `
            
---            
            
### `max-color-diff-per-pixel`

 For special cases (i.e. rare anti alias issues), you can allow an accepted difference on RGB pixel level. 
 To be precise, it should be called *max-color-diff-per-color-per-pixel*, because every RGB color is handled
 individually. A comparison of the pixel RGB(255,255,255) and RGB(253,254,255) would result in a difference
 of 3. If max color diff would be below 3 in this case, an error would be reported.
 
 It doesn't count how often in the picture a difference occurs. The check is reset after each
 pixel while going through the image.
 
 * Scope: Site
 * Type: Integer
 * Default: `0`
 * Example: `"max-color-diff-per-pixel": 1`
 
---

### `page-load-timeout`

 This value is passed through to Selenium for loading the pages. From the Selenium docs:
 
    Sets the amount of time to wait for a page load to complete before throwing an error.
    If the timeout is negative, page loads can be indefinite.
    
 This timeout can help to not wait indefinitely when there are connectivity or page loading problems.
    
 * Scope: Global
 * Type: Integer
 * Unit: Seconds
 * Default: `120`
 * Example: `"page-load-timeout": 180`
  
--- 

### `name`

 You can define a name for your specific JLineup job. The name is shown in the report and on the jobs overview
 page if you use the web version of JLineup.
 
 * Scope: Global
 * Type: String
 * Default: `null`
 * Example: `"name": "Cool name for my JLineup Job"`
 
--- 
 
### `window-height`

 This is the height of the browser window which JLineup uses.
 
 * Scope: Global
 * Type: Integer
 * Unit: Pixels
 * Default: `800`
 * Example: `"window-height": 1000`
 
---
 
### `report-format`

 This is a deprecated option to switch to an older JSON report format, which is not that useful. Shouldn't be used any
 more and will be removed in the future.
 
 * Scope: Global
 * Type: Integer
 * Range: `1` or `2`
 * Default: `2`
 * Example: `"report-format": 2`
 
---

### `screenshot-retries`

 __This option should be used with care. Better don't use it at all. It *may* help in a flaky environment, but it's better
 to fix the flakiness then to use this option.__
 
 JLineup makes screenshots for every permutation of `url` + `path` + `window-width`. Internally, this combination is 
 called a **screenshot context**. If you specify retries and there is any error during screenshotting one of those
 contexts, this context is retried until a maximum of specified retries is reached. 
   
 * Scope: Global
 * Type: Integer
 * Default: `0`
 * Example: `"screenshot-retries": 2`
 
--- 

### `threads`

 If you specify multiple urls, paths or window widths in your config, JLineup can run multiple browser instances in 
 parallel to speed up the job. Keep in mind, that memory and CPU requirements increase when multiple browsers are opened
 at the same time.
 
 If you use the CLI version, this simply sets the number of threads. Although the default is `0`, JLineup uses at least 
 one thread, so `0` or `1` make no difference in that case.
 
 The `0` has another meaning for the web server version, because a maximum of allowed threads per job is configured in
 the web server properties. If the job config says `0` (which is default), the job uses the maximum of allowed
 threads. You can override this through setting an explicit value. If your job configuration value is
 greater than the maximum in the web server properties, the latter wins - so your job cannot use more threads than the
 server allows. Simply leave this configuration at default until you have special requirements for this particular job.
 
 * Scope: Global
 * Type: Integer
 * Default: `0`
 * Example: `"threads": 2`
 
--- 
 
### `timeout`

 This is a *global* and *hard* timeout for the whole JLineup job. If the job isn't finished after this time, it fails.
 This can help to end stalled jobs. Of course, a job shouldn't stall, but there may be rare cases
 (which should be fixed!) - this timeout can help to not have an indefinite wait for JLineup in your pipeline in those 
 cases.
 
 * Scope: Global
 * Type: Integer
 * Unit: Seconds
 * Default: `600`
 * Example: `"timeout": 300`
 
--- 
 
### debug

 This option is for CLI only. If you set this to true, JLineup logs on `DEBUG` level. Can be useful for the CLI version, if you track down strange
 behavior or bugs - or if you simply want to know which steps JLineup is doing in detail.
     
 * Scope: Global
 * Type: Boolean
 * Default: `false`
 * Example: `"debug": true`
 
--- 

### log-to-file

 This option is for CLI only. The job output is logged to the file `jlineup.log` in the current working directory if 
 specified.
 
 * Scope: Global
 * Type: Boolean
 * Default: `false`
 * Example: `"log-to-file": true`
 
--- 
 
### check-for-errors-in-log

 This option only works for Chrome/Chromium. If there is any error logged to the Chrome console and this setting is
 set to `true`, an error is raised for this screenshot and the run fails.
 
 * Scope: Global
 * Type: Boolean
 * Default: `false`
 * Example: `"check-for-errors-in-log": false`
 
 
