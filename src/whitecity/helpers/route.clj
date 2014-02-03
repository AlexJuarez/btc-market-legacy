(ns whitecity.helpers.route
  (:require [whitecity.models.user :as users]
            [whitecity.cache :as cache]
            [whitecity.util :as util]
            [whitecity.models.order :as order]
            [whitecity.models.message :as message]
            [noir.session :as session]))

(defn user-id []
  (util/user-id))

(defn set-info 
  ([]
    {:user 
      (assoc 
          (util/user-blob)
        :cart (count (session/get :cart)))})
  ([user]
    {:user 
      (assoc 
        (util/user-blob user)
        :cart (count (session/get :cart)))}))
