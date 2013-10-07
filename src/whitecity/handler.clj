(ns whitecity.handler  
  (:use whitecity.routes.market
        whitecity.routes.auth)
  (:require [compojure.core :refer [defroutes]]            
            [whitecity.models.schema :as schema]
            [noir.util.middleware :as middleware]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [com.postspectacular.rotor :as rotor]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/append})

  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "whitecity.log" :max-size (* 512 1024) :backlog 10})
  
  (if-not (schema/actualized?)
    (schema/actualize))

  (timbre/info "whitecity started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "whitecity is shutting down..."))

(def app (middleware/app-handler
           ;; add your application routes here
           [auth-routes
            market-routes
            app-routes]
           ;; add custom middleware here
           :middleware []
           ;; add access rules here
           :access-rules []
           ;; serialize/deserialize the following data formats
           ;; available formats:
           ;; :json :json-kw :yaml :yaml-kw :edn :yaml-in-html
           :formats [:json-kw :edn]))

(def war-handler (middleware/war-handler app))
