(ns whitecity.helpers.route
  (:require [taoensso.timbre :refer [trace debug info warn error fatal]]
            [whitecity.models.user :as users]
            [whitecity.cache :as cache]
            [whitecity.util :as util]
            [whitecity.models.user :as user]
            [whitecity.models.report :as report]
            [whitecity.models.order :as order]
            [whitecity.models.message :as message]
            [whitecity.util.hashids :as hashids]
            [noir.io :as io]
            [whitecity.models.image :as image]
            [clojure.string :as string]
            [noir.response :as resp]
            [noir.session :as session]))

(defn encrypt-id [m]
  (if m
  (assoc m :id (hashids/encrypt (:id m)))
  m))

(defn encrypt-ids [l]
  (map encrypt-id l))

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
      (let [image_id (:id (image/add! (user-id)))]
        (try
          (io/upload-file (str (io/resource-path) "/uploads") (assoc image :filename (str image_id ".jpg")))
          (catch Exception ex
            (error ex ("File upload failed for image " image_id))))
          image_id))))

(defmacro session! [key func]
  `(let [value# (session/get ~key)]
    (if (nil? value#)
      (let [value# ~func]
        (session/put! ~key value#)
        value#)
      value#)))

(defn set-info []
  (let [{:keys [id vendor] :as user} (session! :user (user/get (session/get :user_id)))]
    {:user 
     (merge user
            (when vendor {:sales (session! :sales (order/count-sales id))})
            {:cart (count (session/get :cart))
             :orders (session! :orders (order/count id))
             :messages (session! :messages (message/count id))})}))
