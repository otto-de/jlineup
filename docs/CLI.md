# Command Line Interface (CLI) instructions

JLineup started as a small command line only tool, and we're striving
to keep the CLI version small, fast and feature complete.

Currently, the CLI version is running on Linux, which is the platform we
support. It may also work under MacOS or Windows, but we don't test this.

## Installation

You can simply download the CLI version by getting the jlineup-cli.jar
from Maven Central in a terminal window:

`wget https://repo1.maven.org/maven2/de/otto/jlineup-cli/4.5.2/jlineup-cli-4.5.2.jar -O jlineup.jar`

Now you have a `jlineup.jar` in your current directory.

For best results, you need a "real" web browser installed on your system.
JLineup supports Google Chrome (Chromium also works) or Firefox at the
moment. As JLineup is written in Java, you need a Java Runtime
Environment in version 8 or higher. OpenJDK is fine!

If those prerequisites are fulfilled, you can make a test run:

`java -jar jlineup.jar --help`

If you see an error message now, something is wrong. You should see
a usage explanation with all possible command line arguments for JLineup.

JLineup doesn't need any more installation, just make sure you have the
jlineup.jar in your current directory or somewhere in $PATH to use it.

## Basic usage

For a first test, you don't need any job configuration:

`java -jar jlineup.jar --url https://time.gov/ --step before`

Notice the step declaration 'before'. JLineup is printing the generated
config to standard out and it starts to browse to time.gov and to make
screenshots of it's current state.

Wait some seconds and make the 'after' run:

`java -jar jlineup.jar --url https://time.gov/ --step after`

Below your current directory, there should be a 'report' folder now:

```
➜  ~ ls report -al
total 28
drwxrwxr-x  3 marco marco 4096 Jan 11 15:02 .
drwxr-xr-x 84 marco marco 4096 Jan 11 15:09 ..
-rw-rw-r--  1 marco marco 1817 Jan 11 15:02 jlineup.log
-rw-rw-r--  1 marco marco 4548 Jan 11 15:02 report.html
-rw-rw-r--  1 marco marco 1755 Jan 11 15:02 report.json
drwxrwxr-x  2 marco marco 4096 Jan 11 15:02 screenshots
```
Open the report.html file with a browser and you can see your first
JLineup comparison report! For programmatic analysis of the JLineup
job outcome, you also find the report.json file with detailed insights
about the job's outcome.

If the 'before' and the 'after' run of a job show no differences, the
'after' run of JLineup exits with return code `0`. If there are
differences, it exits with `1`.

For 'real' usage, you shouldn't use the --url parameter, but you should
write a lineup.json job [configuration](CONFIGURATION.md) which defines which page(s) with which
settings should be compared.

Here's a small example config:

```json
  {
    "urls": {
      "https://time.gov/": {
        "paths": [
          ""
        ],
        "max-diff": 0.0,
        "window-widths": [
          800,1000,1200
        ],
        "max-scroll-height": 100000,
        "wait-after-page-load": 5,
        "wait-after-scroll": 0
      }
    },
    "browser": "Firefox",
    "window-height": 800
  }
```

Copy the config and save it as file with name `lineup.json`.

If lineup.json is in the current working directory when you run JLineup,
it's used automatically. You can also specify a different config name
with the --config parameter.

Now you're ready to run JLineup again. A "real" Firefox window should
open and you can watch JLineup at work. If you want Chrome, change
browser to Chrome in the config above.

`java -jar jlineup.jar --config lineup.json --step before`

Wait a bit and run 'after'

`java -jar jlineup.jar --config lineup.json --step after`

A new report was written, it now should have the time.gov page
in three widths, as defined in the configuration. There should be
differences, because time always changes. :)

If you want to see a 'green' report without changes, use a static page
like 'http://example.com/'

You're ready to play with JLineup's possibilites now. Have a look at all
the [configuration](CONFIGURATION.md) options for bigger setups.

---

## Command Line Parameter Reference

### `--config, -c`

JobConfig file
* Default: `lineup.json`

---

### `--step, -s`

JLineup step - 'before' just takes screenshots, 'after' takes
screenshots and compares them with the 'before'-screenshots in the
screenshots directory. 'compare' just compares existing screenshots,
it's also included in 'after'.
* Default: `before`
* Possible Values: `before, after, compare`

---

### `--debug`

Sets the log level to DEBUG, produces verbose information about the
current task.

---

### `--help, -?`

Shows a quick help

---

### `--log`

Sets the log level to DEBUG and logs to a file in the current working
directory.

---

### `--print-config`

Prints the current (if existing) or a default config file to standard
out.

---

### `--print-example`

Prints a default example config file to standard out. Useful as quick
start.

---

### `--replace-in-url, -R`

The given keys are replaced with the corresponding values in all urls
that are tested.
Syntax: --replace-in-urlkey=value

---

### `--report-dir, -rd`

HTML report directory name - relative to working directory
* Default: `report`

---

### `--screenshot-dir, -sd`

Screenshots directory name - relative to working directory
* Default: `report/screenshots`

---

### `--url, -u`

If you run JLineup without config file, this is the one url that is
tested with the default config.

---

### `--version, -v`

Prints version information.

---

### `--working-dir, -d`

Path to the working directory
Default: `.`

---

### `--chrome-parameter`

Additional command line parameters for spawned chrome processes.
* Example: `--chrome-parameter "--use-shm=false"`

---

### `--firefox-parameter`

Additional command line parameters for spawned firefox processes.

---

[Back to Main page](../README.md)