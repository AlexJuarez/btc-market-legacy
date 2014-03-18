(ns whitecity.util
    (:use hiccup.form hiccup.util korma.core whitecity.db)
    (:require [taoensso.timbre :refer [trace debug info warn error fatal]]
              [noir.response :as resp]
              [clojure.java.io :refer [as-url]]
              [noir.session :as session]
              [whitecity.cache :as cache]
              [noir.io :as io]
              [whitecity.models.exchange :as exchange]
              [clojure.string :as s]
              [markdown.core :as md])
    (:import net.sf.jlue.util.Captcha))

(defn create-uuid [string]
 "creates a uuid from a string"
 (try
  (java.util.UUID/fromString string)
  (catch Exception ex
   (error ex "an error has occured while creating the uuid from string")
   (.getMessage ex)))) 

(defn user-id []
  (:id (session/get :user)))

(defn convert-price [from to price]
  (if-not (= from to)
    (let [rate (exchange/get from to)]
         (if-not (nil? rate) 
           (* price rate)
           price))
    price))

(defn convert-currency 
  "converts a currency_id to the users preferred currency
   takes a currency_id and price"
  ([{:keys [currency_id price]}]
    (let [user_currency (:currency_id (session/get :user))]
      (convert-price currency_id user_currency price)))
  ([currency_id price]
    (let [user_currency (:currency_id (session/get :user))]
      (convert-price currency_id user_currency price))))

(defn parse-int [s]
  (if (string? s)
    (let [i (re-find #"\d+" s)]
      (if-not (s/blank? i)
        (Integer. i)))
    s))

(defn parse-float [s]
  (if (string? s)
    (let [f (re-find #"[0-9]*\.?[0-9]+" s)]
      (if-not (s/blank? f)
        (Float. f)))
    s))

(defmacro update-session 
  [user-id & terms]
    `(let [id# (parse-int ~user-id)
           user-id# (session/get :user_id)]
      (if (= id# user-id#) 
        (doall (map session/remove! (list :user ~@terms)))
        (let [user# (first (select users (fields :session) (where {:id id#})))]
          (when (:session user#)
            (let [session# (.toString (:session user#))
                  sess# (cache/get session#)
                  ttl# (* 60 60 10)]
              (if (not (nil? sess#))
                (cache/set session# 
                           (assoc sess# :noir (dissoc (:noir sess#) ~@terms :user)) ttl#))))))))

(defn format-time
    "formats the time using SimpleDateFormat, the default format is
       \"dd MMM, yyyy\" and a custom one can be passed in as the second argument"
    ([time] (format-time time "dd MMM, yyyy"))
    ([time fmt]
         (.format (new java.text.SimpleDateFormat fmt) time)))

;;Probably not needed

;;TODO: sanitize links out of md->core
(defn md->html
    "reads a markdown string and returns the html"
    [string]
    (->> string     
         (md/md-to-html-string)))

(defn gen-captcha-text []
    (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))

(defn gen-captcha []
    (let [text (gen-captcha-text)
                  captcha (doto (new Captcha))]
          (session/put! :captcha {:text text :image (.gen captcha text 250 40)})))
