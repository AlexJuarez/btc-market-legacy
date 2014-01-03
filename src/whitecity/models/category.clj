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

(defn public [id]
  (let [cats (all)]
    (loop [output {} x cats]
      (let [curr (first x) id (:id curr) parent (:parent curr) slug {:children [] :name (:name curr) :count (:count curr)}]
        (if (empty? x)
          output
          (recur 
            (-> output 
              (assoc id slug)
              (assoc parent (if-let [p (output parent)] (assoc p :children (conj (:children p) slug)))))
            (next x)))))))

(defn add! [categories]
  (insert category (values categories)))

(defn load-fixture []
  (add! (jr/parse-string (slurp "resources/categories.json"))))
