(defproject
  whitecity
  "0.1.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.8.1"];;for io and session utils
   [compojure "1.1.5"]
   [ring-server "0.3.0"]
   [selmer "0.5.9"];;templating engine
   [image-resizer "0.1.6"]
   [com.taoensso/timbre "3.1.4"];;logging
   [com.postspectacular/rotor "0.1.0"];;loggin
   [clojurewerkz/spyglass "1.1.0"];;couchbase interface
   [com.taoensso/tower "2.0.2"];;localization lib
   [markdown-clj "0.9.41"];;markdown parser
   [hashids "0.1.0"];;for anon hashing
   [clj-http "0.7.8"];;for reading bitcoins prices from coinbase.com
   [net.sf.jlue/jlue-core "1.3"];;why?
   [postgresql/postgresql "9.1-901.jdbc4"]
   [ring-anti-forgery "0.3.0"]
   [metis "0.3.3"];;validator
   [korma "0.3.0-RC6"];;dbl
   [lobos "1.0.0-beta1"]
   [org.bouncycastle/bcpg-jdk15on "1.50"]
   [log4j
    "1.2.17"
    :exclusions
    [javax.mail/mail
     javax.jms/jms
     com.sun.jdmk/jmxtools
     com.sun.jmx/jmxri]]]
  :ring
  {:handler whitecity.handler/app,
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
