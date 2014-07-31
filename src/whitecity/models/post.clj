(ns whitecity.models.post
  (:refer-clojure :exclude [get])
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require
   [whitecity.validator :as v]
   [whitecity.util :as util]))


(defn all [user-id]
  (select posts
          (where {:user_id user-id})))

(defn get [id]
  (first (select posts
                 (where {:id (util/parse-int id)}))))

(defn store! [slug]
  (insert posts (values slug)))

(defn prep [{:keys [subject content public]} user-id]
  {:subject subject
   :content content
   :user_id user-id
   :public (= public "true")})

(defn add! [slug user-id]
  (let [check (v/news-validator slug)]
    (if (empty? check)
      (-> slug (prep user-id) store!)
      (conj {:errors check} slug))))
