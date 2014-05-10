(ns whitecity.handler
  (:use
    [whitecity.views.tags])
  (:require
    [whitecity.routes.market :refer [market-routes]]
    [whitecity.routes.message :refer [message-routes]]
    [whitecity.routes.auth :refer [auth-routes]]
    [whitecity.routes.sales :refer [sales-routes]]
    [whitecity.routes.account :refer [account-routes]]
    [whitecity.routes.cart :refer [cart-routes]]
    [whitecity.routes.orders :refer [order-routes]]
    [whitecity.routes.listings :refer [listing-routes]]
    [environ.core :refer [env]]
    [compojure.core :refer [defroutes]]
    [whitecity.models.schema :as schema]
    [noir.util.middleware :refer [app-handler]]
    [whitecity.middleware :as middleware]
    [selmer.parser :as parser :refer [cache-off!]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [compojure.route :as route]
    [taoensso.timbre :as timbre]
    [whitecity.cache :as cache]
    [noir.session :as session]
    [com.postspectacular.rotor :as rotor]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn user-access [request]
  (and
    (not (nil? (session/get :user_id)))
    (session/get :authed)))

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

  (if (env :dev) (parser/cache-off!))

  (if-not (schema/actualized?)
    (do (schema/actualize) (schema/load-fixtures)))

  (timbre/info "whitecity started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "whitecity is shutting down..."))

(def app
  (app-handler
   ;; add your application routes here
   [auth-routes
    market-routes
    account-routes
    sales-routes
    cart-routes
    order-routes
    listing-routes
    message-routes
    app-routes]
   :session-options {:cookie-attrs {:http-only true
                                    :max-age (* 60 60 10)}
                     :cookie-name "session"
                     :store (cache/store)}

   ;; add custom middleware here
   :middleware [wrap-anti-forgery middleware/error-page middleware/template-error-page middleware/log-request]
   ;; add access rules here
   :access-rules [user-access]
   ;; I can only assume
   ;; serialize/deserialize the following data formats
   ;; available formats:
   ;; :json :json-kw :yaml :yaml-kw :edn :yaml-in-html
   :formats [:json-kw :edn]))
