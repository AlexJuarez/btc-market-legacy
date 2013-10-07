(ns whitecity.routes.auth
  (:use compojure.core hiccup.core hiccup.form)
  (:require [whitecity.models.schema :as schema]
            [whitecity.util :as util] 
            [whitecity.views.layout :as layout]
            [noir.util.crypt :as crypt]
            [noir.util.cache :as cache]
            [noir.session :as session]
            [noir.response :as resp]
            [whitecity.models.db :as db]
            [clojure.string :as s]))

(defn create-admin-page [admin]
  (layout/render "create-admin.html" admin))

(defn login
  ([params]
      (layout/render "login.html"))

  ([handle pass]
    (if-let [admin ]
      (if (and (= handle (:handle admin))
               (crypt/compare pass (:pass admin)))
        (do (cache/invalidate! :home)
            (session/put! :admin admin))))
    (resp/redirect "/")))

(defn check-admin-fields [{:keys [title handle pass pass1] :as params}]
  (cond
    (not= pass pass1) (text :pass-mismatch)
    (empty? handle) (text :admin-required)
    (empty? title) (text :blog-title-required)
    :else nil))

(defn create-admin [admin]
  (if (db/get-admin)
    (resp/redirect "/")
    (if-let [error (check-admin-fields admin)]
      (create-admin-page (assoc admin :error error))
      (do
        (-> admin (dissoc :pass1) (update-in [:pass] crypt/encrypt) (db/set-admin))
        (resp/redirect "/login")))))

(defroutes auth-routes
  (GET "/register"  {params :params}  (create-admin-page params))
  (POST "/register" {params :params}  (create-admin params))
  (GET "/"         {params :params} (login params))
  (POST "/"        [handle pass]    (login handle pass))
  (GET "/market/logout" []
       (session/clear!)
       (cache/invalidate! :home)
       (resp/redirect "/")))
