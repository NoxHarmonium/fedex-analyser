(defproject fedex-analyser "0.1.0-SNAPSHOT"
  :description "Crawl the Fedex channel and get the links with the highest reaction count"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"] 
                 [org.julienxx/clj-slack "0.6.3"]
                 [cheshire "5.10.0"]
                 [environ "1.1.0"]]
  :main ^:skip-aot fedex-analyser.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
