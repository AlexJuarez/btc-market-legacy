(ns whitecity.models.image
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db]))

(defn add! [user-id]
  (insert images (values {:user_id user-id}))) 

(defn get [user-id]
  (select images
          (where {:user_id user-id})))
