(ns whitecity.models.category
  (:use [cheshire.core :as jr]
        [whitecity.db]
        [korma.core]
        [korma.db :only (transaction)]
        [clojure.string :only (split lower-case)])
  (:require
    [whitecity.cache :as cache]))

(defn all []
  (select category))

(defn add! [categories]
  (insert category (values categories)))

(defn load-fixture []
  (add! (map #(-> {
                   :name (first %)
                   :lte (second %)
                   :gte (nth % 2)
                   :level (nth % 3)})
             (jr/parse-string (slurp "resources/categories.json")))))
