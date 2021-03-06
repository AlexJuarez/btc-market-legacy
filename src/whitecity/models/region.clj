(ns whitecity.models.region
  (:refer-clojure :exclude [get])
  (:require
    [korma.db :refer [defdb]]
    [korma.core :refer [select where insert values delete]]
    [whitecity.cache :as cache])
  (:use
    [whitecity.db :only [region]]))

(defn get [id]
  (first
    (select region
            (where {:id id}))))
(defn all []
  (cache/cache! "regions" (select region)))

(defn add! [regions]
  (insert region (values regions)))
