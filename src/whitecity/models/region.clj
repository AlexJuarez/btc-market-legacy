(ns whitecity.models.region
  (:refer-clojure :exclude [get])
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db]))

(defn get [id]
  (first
    (select region
            (where {:id id}))))

(defn all []
  (select region))

(defn add! [regions]
  (insert region (values regions)))
