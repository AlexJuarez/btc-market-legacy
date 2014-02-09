(ns whitecity.cache
  (:require [clojurewerkz.spyglass.client :as c]
            [whitecity.models.currency :as curr]
            [noir.session :as session]))

(def ce (c/text-connection "127.0.0.1:11211"))

(defn set [key value]
  (do
    (session/remove! key) ;;update the store aka clear the old value
    (c/set ce key (+ (* 60 10) (rand-int 600)) value))) ;;Prevent stamped

(defn delete [key]
  (do 
    (session/remove! key)
    (c/delete ce key)))

(defn get [key]
  (if-let [value (session/get key)]
    value
    (if-let [v (c/get ce key)]
      (do (session/put! key v) v))))

(defmacro get-set [key & forms]
  `(if-let [value# (get ~key)]
     value#
     (let [v# (do ~@forms)]
       (set ~key v#) v#)))
