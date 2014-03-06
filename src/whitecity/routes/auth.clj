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

(defn login-page
  ([params]
    (layout/render "login.html" (session/flash-get :success)))
  ([login pass]
    (let [user (users/login! {:login login :pass pass})]
      (if (nil? (:error user))
        (let [{:keys [id vendor]} user]
          (do 
            (when vendor (session/put! :sales (order/count-sales id)))
            (session/put! :user user)
            (session/put! :orders (order/count id))
            (session/put! :messages (message/count id))  
            (session/put! :cart {})
            (resp/redirect "/market/")))
        (layout/render "login.html" user)))))
   
(defroutes auth-routes
  (GET "/test" request (resp/edn request))
  (GET "/"         {params :params} (login-page params))
  (POST "/"        [login pass]    (login-page login pass))
  (GET "/register" {params :params} (registration-page params))
  (POST "/register"[login pass confirm] (registration-page login pass confirm))
  (GET "/logout" []
       (session/clear!)
       (resp/redirect "/")))
