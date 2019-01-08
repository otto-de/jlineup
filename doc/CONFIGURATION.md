# Configuration

JLineup has many configuration options. A JLineup run with it's _before_ and _after_ steps is called _job_ throughout
this documentation. Job configuration settings can be separated between two scopes, _global_ and _url_. Some of the
options exist in global as in page scope. Url specific settings always have precedence before global settings. If you want to
have the same settings for all sites in your JLineup job, then you can simply specify those settings globally.

### Format

A JLineup config is a json document. The default name of the config file is ___lineup.json___.
The command line version looks for it in the current working directory. A different path can be specified with the
`--config` option when you call jlineup via command line.

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
 
This config leads JLineup to https://www.otto.de. All other settings like window size, timeouts stay default.

What about those __global__ and __url__ settings?

```json
{
  "urls": {
    "https://www.otto.de": {
        "->": "URL SETTINGS GO HERE"
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

## Reference

What are all those options about? Look here for details:

| Option                       | Type            | Default    | Explanation                                                                | Scope        |
|------------------------------|---------------- |--------    |----------------------------------------------------------------------------|--------------|
| urls                         | JSON            | no default | This is the map of urls and their url configs                              | Global       |
| paths                        | List of Strings | [""]       | These paths are appended to the current url                                | Url          |
| max-diff                     | Float           | 0          | public final List<String> Maximum allowed difference for image comparison. | Url          |
| cookies                      | JSON            | []         | A list of cookie documents that are put on the url                         | Url          |
| env-mapping                  | Map             | null       | With environment mapping, you can replace subdomains in your config        | Url          |

##### local-storage

 Sets key value pairs to the local storage of the site
 * Scope: Site
 * Type: Map
 * Default: Empty
 * Example: ```"local-storage": {
        "exampleLocalStorageKey": "exampleLocalStorageValue"
      }```
      
##### session-storage

 Sets key value pairs to the session storage of the site
 * Scope: Site
 * Type: Map
 * Default: Empty
 * Example: ```"session-storage": {
        "exampleSessionStorageKey": "exampleSessionStorageValue"
      }```
      
##### window-widths
  
 Every path in the site config will be screenshotted in these given window-widths of the browser
* Scope: Site
* Type: List of integers
* Default: ```[ 800 ]```
* Example: ```"window-widths": [
        600,
        800,
        1000
      ]```
      
##### max-scroll-height
##### wait-after-page-load
##### wait-after-scroll
##### warmup-browser-cache-time
##### wait-for-fonts-time
##### javascript
##### hide-images
##### http-check
##### max-color-diff-per-pixel


