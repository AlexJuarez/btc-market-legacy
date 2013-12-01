(ns whitecity.helpers.route
  (:require [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.image :as image]
            [whitecity.models.postage :as postage]
            [whitecity.models.order :as order]
            [noir.session :as session]))

(defn user-id []
  (:id (session/get :user)))

(defn set-info []
  (let [user (session/get :user) id (:id user)]
    {:user (merge 
             user
             (when (:vendor user) 
               {:listings (listing/count id) 
                :sales (order/count-sales id)})
             {:errors {} 
              :orders (order/count id) 
              :messages (message/count id) 
              :cart (count (session/get :cart))})}))

