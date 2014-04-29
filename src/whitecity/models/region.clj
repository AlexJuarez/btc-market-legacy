(ns whitecity.models.region
  (:refer-clojure :exclude [get])
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db]
        [whitecity.cache :as cache]))

(defn get [id]
  (first
    (select region
            (where {:id id}))))
(defn all []
  (cache/get-set :regions (select region)))

(defn add! [regions]
  (insert region (values regions)))
