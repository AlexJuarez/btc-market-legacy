(ns whitecity.models.image
  (:refer-clojure :exclude [get])
  (:use [korma.db :only (defdb)]
        [noir.io :only (resource-path)]
        [korma.core]
        [clojure.java.io :as io]
        [whitecity.util :as util]
        [whitecity.db]))

(defn add! [user-id]
  (insert images (values {:user_id user-id}))) 

(defn get 
  ([user-id]
    (select images
            (where {:user_id user-id})))
  ([id user-id]
   (first (select images
           (where {:user_id user-id :id (util/parse-int id)})))))

(defn remove! [id user-id]
  (let [id (util/parse-int id)]
    (if-let [image (get id user-id)]
      (do (io/delete-file (str (resource-path) "uploads/" id ".jpg"))
        (delete images
                (where {:user_id user-id :id id}))))))

(defn update! [id data]
  (let [id (util/parse-int id)]
    (update images
            (set-fields data)
            (where {:id id}))))
