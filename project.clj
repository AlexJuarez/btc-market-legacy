(defproject
  whitecity
  "0.1.0-SNAPSHOT"
  :repl-options
  {:init-ns whitecity.repl}
  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [lib-noir "0.8.2"];;for io and session utils
   [compojure "1.1.6"]
   [ring-server "0.3.1"]
   [selmer "0.6.6"];;templating engine
   [image-resizer "0.1.6"]
   [com.taoensso/timbre "3.1.4"];;logging
   [com.postspectacular/rotor "0.1.0"];;logging
   [clojurewerkz/spyglass "1.1.0"];;couchbase interface
   [environ "0.4.0"]
   [com.taoensso/tower "2.0.2"];;localization lib
   [markdown-clj "0.9.43"];;markdown parser
   [hashids "0.1.0"];;for anon hashing
   [clj-http "0.7.8"];;for reading bitcoins prices from coinbase.com
   [net.sf.jlue/jlue-core "1.3"];;why?
   [org.clojure/java.jdbc "0.2.3"];;dependency for korma
   [postgresql/postgresql "9.1-901.jdbc4"]
   [org.clojars.mikejs/ring-gzip-middleware "0.1.0-SNAPSHOT"]
   [ring-anti-forgery "0.3.0"]
   [clj-btc "0.1.1"]
   [http-kit "2.1.16"]
   [metis "0.3.3"];;validator
   [korma "0.3.0-beta11"];;dbl
   [lobos "1.0.0-beta1"]
   [org.bouncycastle/bcpg-jdk15on "1.50"]
   [log4j
    "1.2.17"
    :exclusions
    [javax.mail/mail
     javax.jms/jms
     com.sun.jdmk/jmxtools
     com.sun.jmx/jmxri]]]
  :main whitecity.core
  :ring
  {:handler whitecity.handler/app,
   :init whitecity.handler/init,
   :destroy whitecity.handler/destroy}
  :profiles
  {:user {:env {:db-user "devil"
                :db-pass "admin"}}
   :uberjar
   {:aot :all},
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}},
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.2.0"]]
    :env {:dev true}}}
  :plugins
  [[lein-ring "0.8.7"] [lein-environ "0.4.0"]]
  :min-lein-version "2.0.0")
