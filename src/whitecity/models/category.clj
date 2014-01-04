(ns whitecity.models.category
  (:use [cheshire.core :as jr]
        [whitecity.db]
        [korma.core]
        [korma.db :only (transaction)]
        [clojure.string :only (split lower-case)])
  (:require
    [whitecity.cache :as cache]))

(defn all []
  (select category (order :id :ASC)))

(defn walk-tree [list parent]
  (if-let [curr (first list)]
    (let [{n :name c :count p :parent id :id} curr]
      (if (= parent p)
        (flatten (conj [{:name n :count c :id id :children (walk-tree (next list) id)}] (walk-tree (next list) parent)))
        (if-not (= id parent) (walk-tree (next list) parent))))
    []))

(defn tally-count [tree]
  (if-let [children (:children tree)]
    (assoc tree :count (reduce + (:count tree) (map #(:count (tally-count %)) children)) :children (map tally-count children))
    tree))

(defn public []
  (let [cats (all)]
    (tally-count (first (walk-tree cats 0)))))

(defn add! [categories]
  (insert category (values categories)))

(defn load-fixture []
  (add! (jr/parse-string (slurp "resources/categoriesTea.json"))))
