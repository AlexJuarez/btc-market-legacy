(ns whitecity.models.message
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.models.schema :as schema]))

;;Gets
(defn count [id]
  (:cnt (first (select messages
    (aggregate (count :*) :cnt)
    (where {:user_id id})))))

(defn all [id]
  (select messages
          (where {:user_id id}) 
          (order :created_on :ASC))) 
