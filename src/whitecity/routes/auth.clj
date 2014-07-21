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

(def words ["the" "and" "for" "are" "but" "not" "can" "one" "day"
            "get" "man" "new" "now" "old" "two" "boy" "put" "her" "dad" "zoo"
            "tan" "saw" "mad" "jet" "far" "cat" "map" "key" "dog" "god" "bat"])

(defn registration-page
  ([params]
   (layout/render "register.html" (merge {:captcha (util/gen-captcha)} (set-info))))
  ([login pass confirm captcha]
   (if (= (:text (session/flash-get :captcha)) captcha)
     (let [user (users/add! {:login login :pass pass :confirm confirm})]
       (if (nil? (:errors user))
         (do
           (session/flash-put! :success {:success "User has been created"})
           (resp/redirect "/"))
         (layout/render "register.html" (merge (set-info) user {:login login :captcha (util/gen-captcha)}))))
     (layout/render "register.html" (merge {:captcha (util/gen-captcha) :errors {:captcha ["The captcha was entered incorrectly"]}
                                            :login login :pass pass :confirm confirm} (set-info) )))))
;;if you change the session name change it here too
(defn login-page
  ([]
    (layout/render "login.html" (merge {:success (session/flash-get :success)} (set-info))))
  ([login pass cookies]
    (let [user (users/login! {:login login :pass pass :session (:value (cookies "session"))})]
      (if (nil? (:error user))
        (let [{:keys [id vendor auth pub_key]} user]
          (do
            (when vendor (session/put! :sales (order/count-sales id)))
            (session/put! :authed (not (and auth (not (nil? pub_key)))))
            (session/put! :user_id id)
            (session/put! :user user)
            (session/put! :orders (order/count id))
            (session/put! :messages (message/count id))
            (if (session/get :authed) (resp/redirect "/") (resp/redirect "/login/auth"))))
        (layout/render "login.html" (merge (set-info) user))))))

(defn auth-page
  ([]
   (let [k (reduce str (map #(if (or true %) (str (get words (rand-int 32)))) (range 6)))
         user (session/get :user)]
     (when user
       (session/flash-put! :key k)
       (layout/render "auth.html" (merge {:error (session/flash-get :error) :decode (pgp/encode (:pub_key user) k)} (set-info))))))
  ([{response :response}]
   (if (= (session/flash-get :key) response)
     (do (session/put! :authed true)
       (resp/redirect "/"))
     (do (session/flash-put! :error "incorrect key") (auth-page)))))

(defroutes auth-routes
  (context
   "/login" []
   (GET "/"          [] (login-page))
   (POST "/"         {{login :login pass :pass} :params cookies :cookies} (login-page login pass cookies))
   (GET "/auth"      [] (auth-page))
   (POST "/auth"     {params :params} (auth-page params)))

  (GET "/register"  {params :params} (registration-page params))
  (POST "/register" [login pass confirm captcha] (registration-page login pass confirm captcha))
  (GET "/logout"    []
       (session/clear!)
       (resp/redirect "/")))
