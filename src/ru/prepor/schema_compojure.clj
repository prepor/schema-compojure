(ns ru.prepor.schema-compojure
  (:require [compojure.core :as comp]
            [plumbing.core :refer :all]
            [schema.coerce :as coerce]
            [clout.core :as clout]
            [schema.utils :as schema-utils]
            [plumbing.fnk.pfnk :as pfnk]))

(def ^:dynamic *current-coercer* nil)

(defn prepare-route [route]
  (cond
   (string? route)
   `(clout/route-compile ~route)
   (vector? route)
   `(clout/route-compile
     ~(first route)
     ~(apply hash-map (rest route)))
   :else
   `(if (string? ~route)
      (clout/route-compile ~route)
      ~route)))

(defn run-route
  [handler schema coercer request]
  (let [coerced (coercer request)]
    (if-let [error (schema-utils/error-val coerced)]
      (throw (ex-info (format "Value does not match schema: %s" (pr-str error))
                      {:schema schema :value request :error error}))
      (handler coerced))))

(defn compile-route
  [method path args]
  (let [[options bindings & body] (if (map? (first args))
                                    args
                                    (cons nil args))]
    `(let [handler# (fnk ~bindings ~@body)
           schema# (pfnk/input-schema handler#)
           coercer# (if-let [matcher# (or (:coercer ~options) *current-coercer*)]
                      (coerce/coercer schema# matcher#)
                      identity)]
       (comp/make-route
        ~method ~(prepare-route path)
        (partial run-route handler# schema# coercer#)))))

(defmacro defroutes
  [n & args]
  (let [[options & routes] (if (map? (first args))
                             args (cons nil args))]
    `(comp/defroutes ~n
       (binding [*current-coercer* ~(get options :coercer *current-coercer*)]
         ~@routes))))

(defmacro GET
  [path & args]
  (compile-route :get path args))

(defmacro POST
  [path & args]
  (compile-route :post path args))

(defmacro PUT
  [path & args]
  (compile-route :put path args))

(defmacro DELETE
  [path & args]
  (compile-route :delete path args))

(defmacro HEAD
  [path & args]
  (compile-route :head path args))

(defmacro OPTIONS
  [path & args]
  (compile-route :options path args))

(defmacro PATCH
  [path & args]
  (compile-route :patch path args))

(defmacro ANY
  [path & args]
  (compile-route nil path args))
