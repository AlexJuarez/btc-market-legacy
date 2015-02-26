(ns whitecity.test.util
  (:use clojure.test
        whitecity.util))

(deftest test-page-max
  (testing "page max"
    (is (= 0 (page-max 0 10)))
    (is (= 2 (page-max 20 10)))
    (is (= 1 (page-max 1 10)))))

(deftest test-parse-int
  (testing "parse-int"
    (is (= 4 (parse-int 4)))
    (is (= nil (parse-int "")))
    (is (= nil (parse-int "//adasf'")))
    (is (number? (parse-int "4")))
    (is (not (float? (parse-int "1.0"))))))

(deftest test-parse-float
  (testing "parse-float"
    (is (= 4.0 (parse-float "4.0")))
    (is (= 0.0 (parse-float "")))
    (is (= 0.0 (parse-float "/asafse34##43")))
    (is (= 1.0 (parse-float 1.0)))
    (is (= 0.5 (parse-float ".5")))))

(deftest test-params
  (testing "params"
    (is (= "test=1" (params {:test 1})))))
