(ns whitecity.middleware
  (:require 
            [taoensso.timbre :as timbre :refer [trace debug info warn error fatal]]
            [selmer.parser :as parser]
            [whitecity.views.layout :refer [template-path]]
            [environ.core :refer [env]]))

(defn log-request [handler]
  (if (env :dev)
    (fn [req]
      (debug req)
      (handler req))
    handler))

(defn error-page [handler]
  (if (env :dev)
    (fn [request]
      (try
        (handler request)
        (catch Exception ex
          (error ex)
          {:status 500
           :body (parser/render-file (str template-path "error.html") {})}
          )))
    handler
    ))

(defn template-error-page [handler]
  (if (env :dev)
    (fn [request]
      (try
        (handler request)
        (catch clojure.lang.ExceptionInfo ex
          (let [{:keys [type error-template] :as data} (ex-data ex)]
            (if (= :selmer-validation-error type)
              {:status 500
               :body (parser/render error-template data)}
              (throw ex))))))
    handler))
