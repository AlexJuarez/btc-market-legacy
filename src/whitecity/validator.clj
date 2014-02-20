(ns whitecity.validator
  (:use metis.core
      [whitecity.db])
  (:require
    [metis.core :as v]
    [whitecity.util :as util]
    [korma.core :as sql]))

(defn get-by-login [login]
  (first (sql/select users
    (sql/where (or {:alias login} {:login login})))))

(defn get-by-alias [alias]
  (first (sql/select users
    (sql/where (and (or (= :alias alias) (= :login alias)) (not (= :id (util/user-id))))))))

(defn login-taken [map key _]
  (when-not (empty? (get-by-login (get map key)))
    "This username is already taken"))

(defn alias-taken [map key _]
  (when-not (empty? (get-by-alias (get map key)))
    "This alias is already taken"))

(defn in-range [map key options]
  (when-not (and (>= (count (get map key)) (:start options)) (<= (count (get map key)) (:end options)))
    (str "This needs to between " (:start options) " and " (:end options))))

(v/defvalidator user-validator
  [:login [:presence :login-taken :in-range {:start 3 :end 64}]]
  [:pass [:presence :in-range {:start 8 :end 128} :confirmation {:confirm :confirm}]])

(v/defvalidator user-update-validator
  [:alias [:presence :alias-taken :in-range {:start 3 :end 64}]])

(v/defvalidator listing-validator
  [:title [:presence :in-range {:start 4 :end 100}]]
  [:price [:presence :numericality {:gte 0}]]
  [:currency_id [:presence]]
  [:quantity [:presence :numericality {:gte 0}]]) 

(v/defvalidator postage-validator
  [:price [:presence :numericality {:gte 0}]]
  [:title [:presence :in-range {:start 4 :end 100}]])

(v/defvalidator message-validator
  [:content [:presence]])

(v/defvalidator resolution-refund-validator
  [:refund [:presence :numericality :in-range {:start 0 :end 100}]])

(v/defvalidator resolution-extension-validator
  [:extension [:presence :numericality :in-range {:start 0 :end 90}]])
