(ns whitecity.helpers.route
  (:require [whitecity.models.user :as users]
            [whitecity.cache :as cache]
            [whitecity.util :as util]
            [whitecity.models.user :as user]
            [whitecity.models.report :as report]
            [noir.io :as io]
            [whitecity.models.image :as image]
            [clojure.string :as string]
            [noir.response :as resp]
            [noir.session :as session]))

(defn user-id []
  (util/user-id))

(defn report-add [object-id user-id table referer]
  (report/add! object-id user-id table)
  (resp/redirect referer))

(defn report-remove [object-id user-id table referer]
  (report/remove! object-id user-id table)
  (resp/redirect referer))

;;TODO add thumbnail parsing with imagez
(defn parse-image [image_id image]
  (if (and (not (nil? image)) (= 0 (:size image)))
    image_id
    (if (and (< (:size image) 800000) (not (empty? (re-find #"jpg|jpeg" (string/lower-case (:filename image))))))
      (let [image_id (:id (image/add! (user-id))) 
            image_result (io/upload-file "/uploads" (assoc image :filename (str image_id ".jpg")))]
          image_id))))

(defn set-info 
  ([]
    {:user 
      (assoc 
          (user/user-blob)
        :cart (count (session/get :cart)))})
  ([user]
    {:user 
      (assoc 
        (user/user-blob user)
        :cart (count (session/get :cart)))}))
