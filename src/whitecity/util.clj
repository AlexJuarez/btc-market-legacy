(ns whitecity.util
    (:use hiccup.form hiccup.util)
    (:require [noir.response :as resp]
              [clojure.java.io :refer [as-url]]
              [noir.session :as session]
              [whitecity.db :as db]
              [noir.io :as io]
              [clojure.string :as s]
              [markdown.core :as md])
    (:import net.sf.jlue.util.Captcha))

(def alphabet "0123456789abcdefghijklmnopqrstuvwxyz")

(defn int-to-base36
  "Converts an integer to a tx id"
  ([n] (int-to-base36 (rem n 36) (quot n 36) ""))
  ([remainder number accum]
   (cond
     (zero? number) (str (nth alphabet remainder) accum)
     :else (recur (rem number 36) (quot number 36) (str (nth alphabet remainder) accum)))))

(defn base36-to-int
  "Converts tx to an id"
  ([str] (base36-to-int (reverse str) 0 0))
  ([inverse-str power accum]
   (cond
     (empty? inverse-str) accum
     :else (base36-to-int
             (rest inverse-str)
             (inc power)
             (+ accum (* (long (Math/pow 36 power))
                         (.indexOf alphabet (str (first inverse-str)))))))))

(defn format-title-url [id title]
  (if title
    (let [sb (new StringBuffer)]
      (doseq [c (.toLowerCase title)]
        (if (or (= (int c) 32) (and (> (int c) 96) (< (int c) 123))) 
        (.append sb c)))      
        (str id "-" (url-encode (.toString sb))))))

(defn make-form [& fields]
  (reduce-kv 
  (fn [table i [id name value]]
      (conj table
        [:tr 
          [:td (label id name)] 
          [:td ((if (.startsWith id "pass") password-field text-field) 
          {:tabindex (inc i)} id value)]]))
        [:table]
        (vec (partition 3 fields))))

(defn parse-int [s]
  (if (string? s)
    (let [i (re-find #"\d+" s)]
      (if-not (s/blank? i)
        (Integer. i)))
    s))

;;Probably not needed
(defn format-time
    "formats the time using SimpleDateFormat, the default format is
       \"dd MMM, yyyy\" and a custom one can be passed in as the second argument"
    ([time] (format-time time "dd MMM, yyyy"))
    ([time fmt]
         (.format (new java.text.SimpleDateFormat fmt) time)))

(defn parse-time [time-str time-format]
    (.parse 
          (new java.text.SimpleDateFormat 
                        (or time-format "yyyy-MM-dd HH:mm:ss.SSS")) 
          time-str))

(defn md->html
    "reads a markdown file from public/md and returns an HTML string"
    [filename]
    (->> 
          (io/slurp-resource filename)      
          (md/md-to-html-string)))

(defn gen-captcha-text []
    (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))

(defn gen-captcha []
    (let [text (gen-captcha-text)
                  captcha (doto (new Captcha))]
          (session/put! :captcha {:text text :image (.gen captcha text 250 40)})))
