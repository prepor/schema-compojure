(ns ru.prepor.schema-compojure-test
  (:require [clojure.test :refer :all]
            [schema.coerce :as coerce]
            [schema.core :as s]
            [ring.mock.request :as mock]
            [ru.prepor.schema-compojure :refer [defroutes GET]]))


(defroutes basic
  (GET "/" {:coercer coerce/string-coercion-matcher}
    [[:params number :- s/Int]]
    (format "Hello, %s" (inc number))))

(defroutes basic2 {:coercer coerce/string-coercion-matcher}
  (GET "/" [[:params number :- s/Int]]
    (format "Hello, %s" (inc number))))

(def req {:request-method :get :uri "/" :params {:number "2"}})

(deftest a-test
  (is (= "Hello, 3" (-> (basic req) :body)))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Value does not match schema"
       (basic {:request-method :get :uri "/" :params {:number "foo"}})))
  (testing "nested coercer"
    (is (= "Hello, 3" (-> (basic2 req) :body)))))
