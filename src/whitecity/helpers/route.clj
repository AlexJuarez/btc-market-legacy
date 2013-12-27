(ns whitecity.helpers.route
  (:require [whitecity.models.user :as users]
            [whitecity.cache :as cache]
            [whitecity.models.order :as order]
            [noir.session :as session]))

(defn user-id []
  (:id (session/get :user)))

(defn user-blob 
  ([]
   (let [id (user-id) 
         u (cache/get-set (str "user_" id)
            (let [user (users/get id)]
            (merge 
               user
               (when (:vendor user) 
                 {:sales (order/count-sales id)})
               {:errors {} 
                :orders (order/count id)})))]
     (do (session/put! :user u) u)))
  ([user]
    (let [id (:id user) 
          u (cache/get-set (str "user_" id)
            (merge 
               user
               (when (:vendor user) 
                 {:sales (order/count-sales id)})
               {:errors {} 
                :orders (order/count id)}))]
          (do (session/put! :user u) u))))

(defn set-info 
  ([]
    {:user 
      (assoc 
          (user-blob)
        :cart (count (session/get :cart)))})
  ([user]
    {:user 
      (assoc 
        (user-blob user)
        :cart (count (session/get :cart)))}))
