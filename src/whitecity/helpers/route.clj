(ns whitecity.helpers.route
  (:require
    [taoensso.timbre :refer [trace debug info warn error fatal]]
    [taoensso.timbre.profiling :as profiling
    :refer (profile p)]
    [whitecity.cache :as cache]
    [whitecity.util :as util]
    [whitecity.models.user :as user]
    [whitecity.models.report :as report]
    [whitecity.models.order :as order]
    [whitecity.models.message :as message]
    [whitecity.util.hashids :as hashids]
    [image-resizer.core :as resizer]
    [image-resizer.format :as format]
    [image-resizer.fs :as fs]
    [noir.io :as noirio]
    [clojure.java.io :as io]
    [whitecity.models.image :as image]
    [clojure.string :as string]
    [noir.response :as resp]
    [noir.session :as session])
  (:import
    [java.io File]
    [javax.imageio ImageIO]))

(defn convert-order-price [{:keys [price postage_price postage_currency currency_id quantity] :as order}]
  (when order
    (let [price (util/convert-currency order)
          postage (util/convert-currency postage_currency postage_price)
          total (+ (* price quantity) postage)]
    (-> order (assoc :price price
                     :total total
                     :postage_price postage)))))

(defn encrypt-id [m]
  (when m
    (assoc m :id (hashids/encrypt (:id m)))))

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

(defn save-file [buffered-file path]
  (ImageIO/write buffered-file (fs/extension path) (File. path)))

;;TODO: refactor this into the image model
(defn parse-image [image_id image]
  (if (and (not (nil? image)) (= 0 (:size image)))
    image_id
    (if (and (< (:size image) 800000) (not (empty? (re-find #"jpg|jpeg" (string/lower-case (:filename image))))))
      (let [image_id (:id (image/add! (user-id)))]
        (try
          (do
            (noirio/upload-file (str (noirio/resource-path) "uploads") (assoc image :filename (str image_id ".jpg")))
            (save-file (resizer/resize-and-crop (clojure.java.io/file (str (noirio/resource-path) "uploads/" image_id ".jpg")) 400 300) (str (noirio/resource-path) "uploads/" image_id "_max.jpg"))
            (save-file (resizer/resize-and-crop (clojure.java.io/file (str (noirio/resource-path) "uploads/" image_id ".jpg")) 180 135) (str (noirio/resource-path) "uploads/" image_id "_thumb.jpg"))
            (io/delete-file (str (noirio/resource-path) "uploads/" image_id ".jpg")))
          (catch Exception ex
            (error ex (str "File upload failed for image " image_id))))
          image_id))))

(defn set-info []
  (merge
   {:cart-count (count (session/get :cart))}
   (if (= (not (nil? (session/get :user_id))) (session/get :authed))
     (let [{:keys [id vendor] :as user} (util/current-user)]
       {:user
        (merge user
               {:logged_in true
                :conversion (util/convert-currency 1 1)}
               {:balance (util/convert-currency 1 (:btc user))}
               (when vendor {:sales (util/session! :sales (order/count-sales id))})
               {:orders (util/session! :orders (order/count id))
                :messages (util/session! :messages (message/count id))})})
     {:user {:currency_symbol "$"
             :conversion (util/convert-price 1 26 1)
             :currency_id 26}})))
