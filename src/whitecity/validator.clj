(ns whitecity.validator
  (:use metis.core
      [whitecity.db])
  (:require
    [metis.core :as v]
    [korma.core :as sql]))

(defn get-by-login [login]
  (first (sql/select users
    (sql/where {:login login}))))

(defn login-taken [map key _]
  (when-not (empty? (get-by-login (get map key)))
    "This username is already taken"))

(defn in-range [map key options]
  (when-not (and (>= (count (get map key)) (:start options)) (<= (count (get map key)) (:end options)))
    (str "This needs to between " (:start options) " and " (:end options))))

(v/defvalidator user-validator
  [:login [:presence :login-taken :in-range {:start 3 :end 64}]]
  [:pass [:presence :in-range {:start 8 :end 128} :confirmation {:confirm :confirm}]])

(v/defvalidator listing-validator
  [:title [:presence :in-range {:start 4 :end 100}]]
  [:price [:presence :numericality {:gte 0}]]
  [:quantity [:presence :numericality {:gte 0}]]) 
