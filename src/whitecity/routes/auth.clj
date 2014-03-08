(ns whitecity.routes.auth
  (:use compojure.core hiccup.core hiccup.form whitecity.helpers.route)
  (:require [whitecity.util :as util] 
            [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.models.message :as message]
            [noir.session :as session]
            [ring.middleware.session :as sess]
            [noir.response :as resp]
            [whitecity.models.user :as users]))

(defn registration-page
  ([params]
   (layout/render "register.html"))
  ([login pass confirm]
   (let [user (users/add! {:login login :pass pass :confirm confirm})]
     (if (nil? (:errors user))
       (do 
         (session/flash-put! :success {:success "User has been created"})
         (resp/redirect "/"))
       (layout/render "register.html" (conj user {:login login} ))))))

;;if you change the session name change it here too
(defn login-page
  ([]
    (layout/render "login.html" (session/flash-get :success)))
  ([login pass cookies]
    (let [user (users/login! {:login login :pass pass :session (:value (cookies "session"))})]
      (if (nil? (:error user))
        (let [{:keys [id vendor]} user]
          (do 
            (when vendor (session/put! :sales (order/count-sales id)))
            (session/put! :user_id id)
            (session/put! :user user)
            (session/put! :orders (order/count id))
            (session/put! :messages (message/count id))  
            (session/put! :cart {})
            (resp/redirect "/market/")))
        (layout/render "login.html" user)))))
   
(defroutes auth-routes
  (GET "/"          [] (login-page))
  (POST "/"         {{login :login pass :pass} :params cookies :cookies} (login-page login pass cookies))
  (GET "/register"  {params :params} (registration-page params))
  (POST "/register" [login pass confirm] (registration-page login pass confirm))
  (GET "/logout"    []
       (session/clear!)
       (resp/redirect "/")))
