# JLineup Release History

## Current snapshot
* Document snapshot changes here

## 1.1.0-SNAPSHOT
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