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

(defn prep [{:keys [subject content public published]}]
  {:subject subject
   :content content
   :published (= published "true")
   :public (= public "true")})

(defn remove! [id user-id]
  (delete posts
          (where {:user_id user-id :id (util/parse-int id)})))

(defn add! [slug user-id]
  (let [check (v/news-validator slug)]
    (if (empty? check)
      (-> slug prep (assoc :user_id user-id) store!)
      (conj {:errors check} slug))))

(defn publish! [id user-id]
  (update posts
          (set-fields {:published true})
          (where {:user_id user-id :id (util/parse-int id)})))

(defn update! [slug user-id]
  (let [check (v/news-validator slug)
        post (-> slug prep)]
    (if (empty? check)
      (update posts
              (set-fields post)
              (where {:user_id user-id :id (util/parse-int (:id slug))}))
      (conj {:errors check} slug)
      )))
