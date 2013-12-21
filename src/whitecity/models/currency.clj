(ns whitecity.models.currency
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db]))

(defn get [id]
  (first
    (select currency
            (where {:id id}))))

(defn all []
  (select currency))

(defn add! [currencies]
  (insert currency (values currencies)))

(defn find [name]
  (first
    (select currency
            (where {:key name}))))
