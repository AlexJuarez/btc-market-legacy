(defproject
  whitecity
  "0.1.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.7.1"]
   [compojure "1.1.5"]
   [ring-server "0.3.0"]
   [selmer "0.4.3"]
   [com.taoensso/timbre "2.6.2"]
   [com.postspectacular/rotor "0.1.0"]
   [clojurewerkz/spyglass "1.1.0"]
   [com.taoensso/tower "1.7.1"]
   [markdown-clj "0.9.33"]
   [clj-time "0.6.0"]
   [net.sf.jlue/jlue-core "1.3"]
   [postgresql/postgresql "9.1-901.jdbc4"]
   [ring-anti-forgery "0.2.1"]
   [metis "0.3.3"]
   [korma "0.3.0-RC6"]
   [lobos "1.0.0-beta1"]
   [cheshire "5.2.0"]
   [log4j
    "1.2.17"
    :exclusions
    [javax.mail/mail
     javax.jms/jms
     com.sun.jdmk/jmxtools
     com.sun.jmx/jmxri]]]
  :ring
  {:handler whitecity.handler/war-handler,
   :init whitecity.handler/init,
   :destroy whitecity.handler/destroy}
  :profiles
  {:production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}},
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.2.0"]]}}
  :url
  "http://example.com/FIXME"
  :plugins
  [[lein-ring "0.8.7"]]
  :description
  "FIXME: write description"
  :min-lein-version "2.0.0")
