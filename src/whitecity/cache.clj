(ns whitecity.cache
  (:require [clojurewerkz.spyglass.client :as c]
            [whitecity.models.currency :as curr]))

(def store (atom {}))

(def ce (c/text-connection "127.0.0.1:11211"))

(defn set [key value]
  (c/set ce key (+ (* 60 10) (rand-int 600)) value)) ;;Prevent stampede

(defn delete [key]
  (c/delete ce key))

(defn get [key]
  (let [value (@store key)]
    (if (nil? value)
      (let [v (c/get ce key)]
        (if-not (nil? v)
          (do (swap! store assoc key v) v)))
      value)))

(defmacro get-set [key & forms]
  `(let [value# (get ~key)]
    (if (nil? value#)
      (let [v# (do ~@forms)]
        (set ~key v#) v#)
      value#))) 
