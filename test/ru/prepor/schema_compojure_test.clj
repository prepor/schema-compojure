(ns ru.prepor.schema-compojure-test
  (:require [clojure.test :refer :all]
            [schema.coerce :as coerce]
            [schema.core :as s]
            [ru.prepor.schema-compojure :refer [defroutes context GET]]))


(defroutes basic
  (GET "/" {:matcher coerce/string-coercion-matcher}
    [[:params number :- s/Int]]
    (format "Hello, %s" (inc number))))

(defroutes basic2 {:matcher coerce/string-coercion-matcher}
  (GET "/" [[:params number :- s/Int]]
    (format "Hello, %s" (inc number))))

(defroutes nested {:matcher coerce/string-coercion-matcher}
  (context "/admin" []
    (context "/users/:id" [[:params id :- s/Int]]
      (GET "/" []
        (format "Hello, %s" (inc id)))))
  (GET "/" [] "Hello world!"))

(def req {:request-method :get :uri "/" :params {:number "2"}})

(deftest a-test
  (is (= "Hello, 3" (-> (basic req) :body)))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Value does not match schema"
       (basic {:request-method :get :uri "/" :params {:number "foo"}})))
  (testing "nested matcher"
    (is (= "Hello, 3" (-> (basic2 req) :body)))))


(deftest nested-routes
  (is (= "Hello world!" (-> (nested {:request-method :get
                                     :uri "/"})
                            :body)))
  (is (= "Hello, 3" (-> (nested {:request-method :get
                                 :uri "/admin/users/2"})
                        :body))))
