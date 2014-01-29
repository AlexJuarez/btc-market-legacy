(ns whitecity.models.image
  (:use [korma.db :only (defdb)]
        [korma.core]
        [clojure.java.io :as io]
        [whitecity.db]))

(defn add! [user-id]
  (insert images (values {:user_id user-id}))) 

(defn get 
  ([user-id]
    (select images
            (where {:user_id user-id})))
  ([id user-id]
   (select images
           (where {:user_id user-id :id id}))))

(defn remove! [id user-id]
  (let [image (get id user-id)]
    
