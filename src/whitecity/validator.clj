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

(defn pin-match [map key _]
  (if-not (or (nil? (:pin (util/current-user))))))

(defn in-range [map key options]
  (when-not (and (>= (count (get map key)) (:start options)) (<= (count (get map key)) (:end options)))
    (str "This needs to between " (:start options) " and " (:end options))))

;;Bcypt only looks at the first 73 characters, and saves 60 of them
(v/defvalidator user-validator
  [:login [:presence :login-taken :formatted {:pattern #"[A-Za-z0-9]+" :message "Only alphanumeric characters are valid"} :in-range {:start 3 :end 64}]]
  [:pass [:presence :in-range {:start 8 :end 73} :confirmation {:confirm :confirm}]])

(v/defvalidator user-update-password-validator
  [:pass [:presence :in-range {:start 8 :end 73} :confirmation {:confirm :confirm}]])

(v/defvalidator user-update-validator
  [:alias [:presence :formatted {:pattern #"[A-Za-z0-9]+" :message "Only alphanumeric characters are valid"} :alias-taken :in-range {:start 3 :end 64}]])

(v/defvalidator user-pin-validator
  [:pin [:presence :in-range {:start 6 :end 73} :confirmation {:confirm :confirmpin}]])

(v/defvalidator listing-validator
  [:title [:presence :in-range {:start 4 :end 100}]]
  [:price [:presence :numericality {:greater-than-or-equal-to 0}]]
  [:currency_id [:presence]]
  [:quantity [:presence :numericality {:greater-than-or-equal-to 0}]])

(v/defvalidator postage-validator
  [:price [:presence :numericality {:greater-than-or-equal-to 0}]]
  [:title [:presence :in-range {:start 4 :end 100}]])

(v/defvalidator message-validator
  [:content [:presence]])

(v/defvalidator resolution-refund-validator
  [:value [:presence :numericality {:greater-than-or-equal-to 0 :less-than-or-equal-to 100}]])

(v/defvalidator resolution-extension-validator
  [:value [:presence :numericality {:greater-than-or-equal-to 0 :less-than-or-equal-to 30}]])
