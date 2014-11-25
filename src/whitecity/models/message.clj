(ns whitecity.models.message
  (:refer-clojure :exclude [count])
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require
        [whitecity.validator :as v]
        [whitecity.cache :as cache]
        [whitecity.util.pgp :as pgp]
        [whitecity.util :as util]))

;;Gets
(defn count [id]
  (let [counts (select messages
                       (aggregate (count :*) :cnt)
                       (fields :read)
                       (group :read)
                       (where {:user_id id}))]
    {:unread (or (:cnt (first (filter #(= false (:read %)) counts))) 0)
     :total (reduce + (map #(:cnt %) counts))}))

(defn update! [id receiver-id]
  (update messages
          (set-fields {:read true})
          (where {:user_id id :sender_id receiver-id})))

(defn sent [id]
  (select messages
    (fields :subject :content :created_on :user_id :sender_id :read)
    (with users (fields [:login :user_login] [:alias :user_alias]))
    (where {:sender_id id})))

(defn all
  ([id page per-page]
    (select messages
            (fields :id [:user.login :user_login] [:user.alias :user_alias] :feedback_id :subject :content :created_on :user_id :sender_id :read)
            (join
             users (= :user.id :sender_id))
            (where {:user_id id})
            (order :created_on :DESC)
            (limit per-page)
            (offset (* (- page 1) per-page))))
  ([ticket_id]
   (let [tid (util/parse-int ticket_id)]
     (select messages
               (fields :id :subject :content :created_on :user_id :sender_id :feedback_id :read)
               (with senders (fields [:alias :user_alias]))
               (where {:feedback_id tid})
               (order :created_on :ASC))))
  ([id receiver-id]
   (let [rid (util/parse-int receiver-id)]
     (do
       (when (not (empty? (update! id rid))) (util/update-session id :messages))
       (select messages
               (fields :id :subject :content :created_on :user_id :sender_id :read)
               (with senders (fields [:alias :user_alias]))
               (where (or {:sender_id id :user_id rid} {:sender_id rid :user_id id}))
               (order :created_on :ASC))))))

(defn prep [{:keys [encrypt subject content sender_id user_id]}]
  (let [recipient (first (select users (fields :pub_key) (where {:id (util/parse-int user_id)})))]
    {:subject subject
     :content (if (and (= "true" encrypt) (not (nil? (:pub_key recipient)))) (pgp/encode (:pub_key recipient) content) content)
     :user_id (util/parse-int user_id)
     :sender_id sender_id }
    )
  )

(defn store! [message user-id receiver-id]
  (util/update-session receiver-id :messages)
  (insert messages (values (prep (merge message {:user_id receiver-id :sender_id user-id})))))

(defn remove! [id user-id]
  (util/update-session user-id :messages)
  (delete messages (where {:id (util/parse-int id) :user_id user-id})))

(defn add! [message user-id receiver-id]
  (let [check (v/message-validator message)]
    (if (empty? check)
      (do (store! message user-id receiver-id) nil)
      (conj {:errors check} message))))
