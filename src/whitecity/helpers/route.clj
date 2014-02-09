(ns whitecity.helpers.route
  (:require [whitecity.models.user :as users]
            [whitecity.cache :as cache]
            [whitecity.util :as util]
            [whitecity.models.user :as user]
            [noir.session :as session]))

(defn user-id []
  (util/user-id))

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
