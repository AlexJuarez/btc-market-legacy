(ns whitecity.routes.auth
  (:use compojure.core hiccup.core hiccup.form)
  (:require [whitecity.models.schema :as schema]
            [whitecity.util :as util] 
            [whitecity.views.layout :as layout]
            [noir.util.crypt :as crypt]
            [noir.util.cache :as cache]
            [noir.session :as session]
            [noir.response :as resp]
            [whitecity.models.user :as user]
            [clojure.string :as s]))

(defn registration-page
  ([params]
   (layout/render "register.html"))

  ([login pass confirm]
   (let [user (user/add! {:login login :pass pass :confirm confirm})]
     (if (nil? (:errors user))
       (do 
         (session/flash-put! :success {:success "User has been created"})
         (resp/redirect "/"))
       (layout/render "register.html" (conj user {:login login} ))))))

(defn login-page
  ([params]
    (layout/render "login.html" (session/flash-get :success)))

  ([login pass]
    (let [user (user/login! {:login login :pass pass})]
      (if (nil? (:error user))
        (do (session/put! :user user)
            (session/put! :cart {})
            (resp/redirect "/market/"))
          (layout/render "login.html" user)))))
   
(defroutes auth-routes
  (GET "/"         {params :params} (login-page params))
  (POST "/"        [login pass]    (login-page login pass))
  (GET "/register" {params :params} (registration-page params))
  (POST "/register"[login pass confirm] (registration-page login pass confirm))
  (GET "/logout" []
       (session/clear!)
       (cache/invalidate! :home)
       (resp/redirect "/")))
