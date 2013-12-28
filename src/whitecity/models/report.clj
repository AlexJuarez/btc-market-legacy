(ns whitecity.models.report
  (:use [korma.core]
        [whitecity.db])
  (:require
        [whitecity.util :as util]))

(defn get [object-id user-id table]
  (first
    (select reports
            (where {:object_id object-id :user_id user-id :type table}))))

(defn reported? [object-id user-id table]
  (not (empty? (get object-id user-id table))))

(defn add! [object-id user-id table]
  (insert reports
          (values {:object_id object-id :user_id user-id :type table})))

(defn remove! [object-id user-id table]
  (delete reports
          (values {:object_id object-id :user_id user-id :type table})))
