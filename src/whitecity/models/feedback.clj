(ns whitecity.models.feedback
  (:refer-clojure :exclude [count])
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.util :as util]))

(defn prep [{:keys [subject content]} user-id]
  {:subject subject
   :content content
   :user_id user-id})

(defn add! [message user-id]
  (insert feedback (values (prep message user-id))))

