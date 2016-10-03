**JLineup**

JLineup is a command line tool which is useful for visual acceptance tests in continuous
delivery pipelines. It can make and compare screenshots from before and after a deployment
of a web page. Through comparison of the screenshots it detects every changed pixel.
JLineup generates a HTML report and a JSON report.
Behind the scenes, it uses Selenium and a browser of choice (currently Chrome and
PhantomJS are supported, Firefox is work in progress).

JLineup is a configuration compatible replacement
for Lineup, implemented in Java. The original
[Lineup](https://github.com/otto-de/lineup) is
a Ruby tool. We did a rewrite in Java, because we can
leverage some quicker image comparison here and we can
get rid of Ruby in our JVM-based pipelines.



