(ns whitecity.routes.auth
  (:use compojure.core hiccup.core hiccup.form whitecity.helpers.route)
  (:require [whitecity.util :as util]
            [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.models.message :as message]
            [noir.session :as session]
            [ring.middleware.session :as sess]
            [noir.response :as resp]
            [whitecity.util.pgp :as pgp]
            [whitecity.models.user :as users]))

(defn registration-page
  ([params]
   (layout/render "register.html" {:captcha (util/gen-captcha)}))
  ([login pass confirm captcha]
   (if (= (:text (session/flash-get :captcha)) captcha)
     (let [user (users/add! {:login login :pass pass :confirm confirm})]
       (if (nil? (:errors user))
         (do
           (session/flash-put! :success {:success "User has been created"})
           (resp/redirect "/"))
         (layout/render "register.html" (conj user {:login login :captcha (util/gen-captcha)}))))
     (layout/render "register.html" (conj {:captcha (util/gen-captcha) :errors {:captcha ["The captcha was entered incorrectly"]}} {:login login :pass pass :confirm confirm} )))))


;;if you change the session name change it here too
(defn login-page
  ([]
    (layout/render "login.html" (session/flash-get :success)))
  ([login pass cookies]
    (let [user (users/login! {:login login :pass pass :session (:value (cookies "session"))})]
      (if (nil? (:error user))
        (let [{:keys [id vendor auth pub_key]} user]
          (do
            (when vendor (session/put! :sales (order/count-sales id)))
            (session/put! :authed (or (not (and auth (not (nil? pub_key)))) (not auth)))
            (session/put! :user_id id)
            (session/put! :user user)
            (session/put! :orders (order/count id))
            (session/put! :messages (message/count id))
            (session/put! :cart {})
            (if (session/get :authed) (resp/redirect "/market/") (resp/redirect "/auth"))))
        (layout/render "login.html" user)))))

(defn auth-page
  ([]
   (let [k "testermctest"
         user (session/get :user)]
     (when user
       (session/flash-put! :key k)
       (layout/render "auth.html" {:error (session/flash-get :error) :decode (pgp/encode (:pub_key user) k)}))))
  ([{response :response}]
   (if (= (session/flash-get :key) response)
     (do (session/put! :authed true)
       (resp/redirect "/market/"))
     (do (session/flash-put! :error "incorrect key") (auth-page)))))

(defroutes auth-routes
  (GET "/"          [] (login-page))
  (POST "/"         {{login :login pass :pass} :params cookies :cookies} (login-page login pass cookies))
  (GET "/auth"      [] (auth-page))
  (POST "/auth"      {params :params} (auth-page params))
  (GET "/register"  {params :params} (registration-page params))
  (POST "/register" [login pass confirm captcha] (registration-page login pass confirm captcha))
  (GET "/logout"    []
       (session/clear!)
       (resp/redirect "/")))
