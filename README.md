# fedex-analyser

Crawl the [Fedex day](http://www.ideachampions.com/weblogs/archives/2011/12/atlassian_is_a.shtml)
Slack channel at Agile Digital and get the links with the highest reaction count.

## Usage

Export an environment variable called `SLACK_API_TOKEN` with the "Bot User OAuth Access Token"

Then run:

```bash
$ lein run
```

To debug use the repl:

```bash
$ lein repl
```

You can reload the main namespace and run it with the following command:

```clojure
(require 'fedex-analyser.core :reload-all)(-main)
```
