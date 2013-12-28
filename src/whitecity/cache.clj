(ns whitecity.cache
  (:require [clojurewerkz.spyglass.client :as c]
            [whitecity.models.currency :as curr]))

(def store (atom {}))

(def ce (c/text-connection "127.0.0.1:11211"))

(defn set [key value]
  (c/set ce key (+ (* 60 10) (rand-int 600)) value)) ;;Prevent stampede

(defn delete [key]
  (do (swap! store dissoc key)
  (c/delete ce key)))

(defn get [key]
  (if-let [value (@store key)]
    value
    (if-let [v (c/get ce key)]
      (do (swap! store assoc key v) v))))

(defmacro get-set [key & forms]
  `(if-let [value# (get ~key)]
     value#
     (let [v# (do ~@forms)]
       (set ~key v#) v#)))
