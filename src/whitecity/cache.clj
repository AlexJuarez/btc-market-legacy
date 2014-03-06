(ns whitecity.cache
  (:refer-clojure :exclude [set get])
  (:require [clojurewerkz.spyglass.client :as c]
            [noir.util.cache :as cache]
            [whitecity.models.currency :as curr]))

(defonce ce (c/text-connection "127.0.0.1:11211"))

(defn set [key value]
  (c/set ce key (+ (* 60 10) (rand-int 600)) value)) ;;Prevent stampede

(defn get [key]
  (c/get ce key))

(defn delete [key]
  (cache/invalidate! key)
  (c/delete ce key))

(defmacro get-set [key & forms]
  `(cache/cache! ~key 
                 (let [value# (get ~key)] 
                   (if (nil? value#) 
                     (let [v# (do ~@forms)] 
                       (set ~key v#) v#) 
                     value#))))
