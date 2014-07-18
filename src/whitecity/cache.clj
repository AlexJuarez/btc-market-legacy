(ns whitecity.cache
  (:refer-clojure :exclude [set get])
  (:require [clojurewerkz.spyglass.client :as c]
            [ring.middleware.session.store :as session-store]
            [noir.util.cache :as cache]))

(def ^:private address "127.0.0.1:11211")

(defonce ce (c/text-connection address))

(defrecord CouchBaseSessionStore [conn ttl-secs]
  session-store/SessionStore
  (read-session [_ key] (or (when key (c/get conn key)) {}))
  (delete-session [_ key] (c/delete conn key) nil)
  (write-session [_ key data]
    (let [key (or key (str (java.util.UUID/randomUUID)))]
      (c/set conn key (+ ttl-secs (rand-int ttl-secs)) data)
      key)))

(defn store
  []
  (->CouchBaseSessionStore ce (* 60 60 10)))

(defn set [key value & ttl]
  (try
    (c/set ce key (or (first ttl) (+ (* 60 10) (rand-int 600))) value) ;;Prevent stampede
    (catch Exception ex
      nil)))

(defn get [key]
  (try
    (c/get ce key)
    (catch Exception ex
      nil)))

(defn delete [key]
  (cache/invalidate! key)
  (c/delete ce key))

(defmacro cache! [key & forms]
  `(cache/cache! ~key
                 (let [value# (get ~key)]
                   (if (nil? value#)
                     (let [v# (do ~@forms)]
                       (set ~key v#)
                       v#)
                     value#))))
