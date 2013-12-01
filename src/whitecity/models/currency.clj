(ns whitecity.models.currency
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db]))


(defn all []
  (select currency))
(defn add! [currencies]
  (insert currency (values currencies)))
