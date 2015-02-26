(ns whitecity.test.handler
  (:use clojure.test
        kerodon.core
        kerodon.test
        ring.mock.request
        whitecity.handler))

(deftest user-can-login
  (-> (session app)
      (visit "/")
      (follow "login")
      (fill-in [:#handle] "test")
      (fill-in [:#pass] "testtest")
      (press "login")
      (follow-redirect)
      (within [:div.account :strong]
        (has (text? "test")))
      ))
