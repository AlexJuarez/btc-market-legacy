(ns whitecity.views.tags
  (:require [selmer.parser :refer [add-tag!]]
            [clojure.java.io :as io]
            [noir.io :as noirio]
            [ring.util.anti-forgery :refer [anti-forgery-field]])
  (:use selmer.filters
        hiccup.core)
  (:import 
    (org.apache.commons.io IOUtils)
    (org.apache.commons.codec.binary Base64)))

;;this means that we will use a data;mime type to embed the image instead
(def img-data true)

(defn- parse-args [args]
  (into [] (map keyword (.split args "\\."))))

(defn- computed-args [args context-map]
  (map #(get-in context-map (parse-args %)) args))

(defn- read-image [id]
  (let [path (str (noirio/resource-path) "/uploads/" id ".jpg")]
    (with-open [in (io/input-stream (io/file path))]
      (.toString (Base64/encodeBase64String (IOUtils/toByteArray in))))))

(add-tag! :csrf-token (fn [_ _] (anti-forgery-field)))

(defn- create-image [id extension]
    (if img-data
      (let [data (str "data:image/jpeg;base64," (read-image (str id extension)))]
        (html [:img {:src data}]))
      (let [url (str "/uploads/" id extension ".jpg")]
        (html [:img {:src url}]))))

(add-tag! :image (fn [args context-map] 
  (let [id (first (computed-args args context-map))]
    (create-image id "_max"))))

(add-tag! :image-thumbnail (fn [args context-map] 
   (let [id (first (computed-args args context-map))]
     (create-image id "_thumb"))))
