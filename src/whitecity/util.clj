(ns whitecity.util
    (:use hiccup.form
          [hiccup.core :only [html]]
          hiccup.util korma.core whitecity.db)
    (:require [taoensso.timbre :refer [trace debug info warn error fatal]]
              [taoensso.timbre.profiling :as profiling
            :refer (profile p)]
              [noir.response :as resp]
              [clojure.java.io :refer [as-url]]
              [noir.session :as session]
              [whitecity.cache :as cache]
              [clojure.java.io :as io]
              [noir.io :as noirio]
              [whitecity.models.exchange :as exchange]
              [clojure.string :as s]
              [whitecity.util.markdown :as md])
    (:import net.sf.jlue.util.Captcha
             javax.imageio.ImageIO
             (java.io ByteArrayInputStream ByteArrayOutputStream)
             (org.apache.commons.io IOUtils)
             (org.apache.commons.codec.binary Base64)))

(defn page-max [items per-page]
  {:pre (> 0 per-page)}
  (let [items (or items 0)]
    (+ (if (> (mod items per-page) 0) 1 0) (int (/ items per-page)))))

(defn bytes-to-base64 [bytes]
  (.toString (Base64/encodeBase64String bytes)))

(defn generate-salt []
  (let [b (byte-array 20)]
    (.nextBytes (java.security.SecureRandom.) b)
    (bytes-to-base64 b)))

(defn read-image [id]
  (let [path (str (noirio/resource-path) "/uploads/" id ".jpg")]
    (with-open [in (io/input-stream (io/file path))]
      (bytes-to-base64  (IOUtils/toByteArray in)))))

(defn params [params]
  (s/join "&" (map #(str (name (key %)) "=" (val %)) params)))

(defn create-uuid [string]
 "creates a uuid from a string"
 (try
  (java.util.UUID/fromString string)
  (catch Exception ex
   (error ex "an error has occured while creating the uuid from string")
   (.getMessage ex))))

(defmacro session! [key func]
  `(let [value# (session/get ~key)]
    (if (nil? value#)
      (let [value# ~func]
        (session/put! ~key value#)
        value#)
      value#)))

(defn user-id []
  (session/get :user_id ))

(defn current-user []
  (session! :user
            (if (nil? (session/get :user_id))
              {:currency_id 26}
              (-> (select users (with currency (fields [:key :currency_key] [:symbol :currency_symbol]))
                          (where {:id (session/get :user_id)})) first (dissoc :salt :pass)))))

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
   (convert-currency currency_id price))
  ([currency_id price]
    (let [user_currency (:currency_id (current-user))
          currencies [1 26]]
      (if (or
           (= user_currency currency_id)
           (some #(= user_currency %) currencies)
           (some #(= currency_id %) currencies))
        (convert-price currency_id user_currency price)
        (convert-price 1 user_currency (convert-price currency_id 1 price))))))

(defn parse-int [s]
  (if (string? s)
    (try (Integer/parseInt s)
      (catch Exception ex))
    s))

(defn parse-float [s]
  (if (string? s)
     (try (Float/parseFloat s)
       (catch Exception ex
         0.0))
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

(defn md->html [string]
  (md/md->html string))

(defn format-time
    "formats the time using SimpleDateFormat, the default format is
       \"dd MMM, yyyy\" and a custom one can be passed in as the second argument"
    ([time] (format-time time "dd MMM, yyyy"))
    ([time fmt]
         (.format (new java.text.SimpleDateFormat fmt) time)))

(defn gen-captcha-text []
    (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))

(defn gen-captcha []
    (let [text (gen-captcha-text)
          captcha (doto (new Captcha))]
      (session/flash-put! :captcha {:text text})
      (with-open [out (new ByteArrayOutputStream)]
        (ImageIO/write (.gen captcha text 218 26) "jpeg" out)
        (.flush out)
        (bytes-to-base64 (.toByteArray out)))))
