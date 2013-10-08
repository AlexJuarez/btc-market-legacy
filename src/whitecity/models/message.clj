(ns whitecity.models.message
  (:use [korma.db :only (defdb)]
        [whitecity.db])
  (:require 
        [korma.core :as sql]
        [metis.core :as v]
        [whitecity.models.schema :as schema]))

(sql/defentity messages)

;;Gets
(defn get-messages-count [id]
  (:cnt (first (sql/select messages
    (sql/aggregate (count :*) :cnt)
    (sql/where {:user_id id})))))
