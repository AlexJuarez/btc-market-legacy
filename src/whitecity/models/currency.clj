(ns whitecity.models.currency
  (:refer-clojure :exclude [get find])
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.cache :as cache]
        [whitecity.db]))

(defn get [id]
  (first
    (select currency
            (where {:id id}))))

(defn all []
  (cache/get-set :curriences (select currency)))

(defn add! [currencies]
  (insert currency (values currencies)))

(defn find [name]
  (first
    (select currency
            (where {:key name}))))
