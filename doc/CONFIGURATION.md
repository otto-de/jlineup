# Configuration

JLineup has many configuration options. A JLineup run with it's _before_ and _after_ steps is called _job_ throughout
this documentation. Job configuration settings can be separated between two scopes, _global_ and __url__. Some of the
options exist in global as in page scope. Url specific settings always have precedence before global settings. If you want to
have the same settings for all sites in your JLineup job, then you can simply specify those settings globally.

### Format

A JLineup config is a json document. The default name of the config file is config.json. The command line version looks
in 

### Basic config

A basic configuration with default settings can look like this:

    {
      "urls": {
        "https://www.otto.de": {
        }
      }
    }
    
This config leads JLineup to https://www.otto.de. All other settings like window size, timeouts stay default.

What about those __global__ and __url__ settings?

    {
      "urls": {
        "https://www.otto.de": {
            -> URL SETTINGS GO HERE <-
        }
      }
      -> GLOBAL SETTINGS GO HERE <-
    }
    
#### Example

This basic example shows the concept of global and url settings:

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
        }
      },{
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
    
As you can see, _paths_, _window-widths_ and _max-scroll-height_ are configured on url level, the _browser_ and the _wait-after-page-load_
options are global for all sites in this job.

