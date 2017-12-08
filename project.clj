(defproject quiz-server "0.1.0-SNAPSHOT"
  :description "Server for a quiz app"
  :url "http://github.com/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.json "0.2.6"]
                 [com.cemerick/url "0.1.1"]
                 [http-kit "2.2.0"]]
  :main ^:skip-aot quiz-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
