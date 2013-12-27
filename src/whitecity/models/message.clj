(ns whitecity.models.message
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.util :as util]))

;;Gets
(defn count [id]
  (:cnt (first (select messages
    (aggregate (count :*) :cnt)
    (where (and {:read false } (or {:sender_id id} {:user_id id})))))))

(defn update! [id receiver-id]
  (transaction
    (update users
            (set-fields {:messages (raw "messages - 1")})
            (where {:id receiver-id}))
    (update messages
            (set-fields {:read true})
            (where {:user_id id :sender_id receiver-id}))))

(defn sent [id]
  (select messages
    (fields :subject :content :created_on :user_id :sender_id :read)
    (with users (fields [:login :user_login] [:alias :user_alias]))
    (where {:sender_id id})))

(defn all 
  ([id]
    (select messages
      (fields [:user.login :user_login] [:user.alias :user_alias] :subject :content :created_on :user_id :sender_id :read)
      (join
        users (= :user.id :sender_id)) 
      (where {:user_id id}) 
      (order :created_on :ASC)))
  ([id receiver-id]
   (let [rid (util/parse-int receiver-id)]
     (do 
       (update! id rid) 
       (select messages
        (fields :subject :content :created_on :user_id :sender_id :read)
        (with users (fields [:login :user_login] [:alias :user_alias]))
        (where (or {:sender_id id :user_id rid} {:sender_id rid :user_id id})))))))

(defn prep [{:keys [subject content sender_id user_id]}]
  {:subject subject 
   :content content 
   :user_id (util/parse-int user_id)
   :sender_id sender_id})

(defn store! [message user-id receiver-id]
  (transaction
    (update users
            (set-fields {:messages (raw "messages + 1")})
            (where {:id receiver-id}))
    (insert messages (values (prep (merge message {:user_id receiver-id :sender_id user-id}))))))

(defn add! [message user-id receiver-id]
  (let [check (v/message-validator message)]
    (if (empty? check)
      (do (store! message user-id receiver-id) nil)
      (conj {:errors check} message))))
