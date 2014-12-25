(ns ru.prepor.schema-compojure
  (:require [compojure.core :as comp]
            [plumbing.core :refer :all]
            [schema.coerce :as coerce]
            [clout.core :as clout]
            [schema.utils :as schema-utils]
            [plumbing.fnk.pfnk :as pfnk]
            [ring.util.codec :as codec]))

(def ^:dynamic *current-matcher* nil)

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

(defmacro let-request
  [options [bindings request] & body]
  `(let [f# (fnk ~bindings ~@body)
         schema# (pfnk/input-schema f#)
         coercer# (if-let [matcher# (get ~options :matcher *current-matcher*)]
                    (coerce/coercer schema# matcher#)
                    identity)]
     (let [coerced# (coercer# ~request)]
       (if-let [error# (schema-utils/error-val coerced#)]
         (throw (ex-info (format "Value does not match schema: %s" (pr-str error#))
                         {:schema schema# :value ~request :error error#}))
         (f# coerced#)))))

;; (defmacro with-validation
;;   [bindings matcher handler-body handler-sym inner-body outer-body]
;;   `(let [handler# (fnk ~bindings ~@handler-body)
;;          schema# (pfnk/input-schema handler#)
;;          coercer# (if ~matcher
;;                     (coerce/coercer schema# ~matcher)
;;                     identity)
;;          ~handler-sym (partial run-route handler# schema# coercer#)]
;;      ~@body))

(defn compile-route
  [method path args]
  (let [[options bindings & body] (if (map? (first args))
                                    args
                                    (cons nil args))]
    `(comp/make-route
      ~method ~(prepare-route path)
      (fn [request#]
        (let-request ~options [~bindings request#]
                     ~@body)))))

(defmacro with-matcher
  [matcher & body]
  `(binding [*current-matcher* ~matcher]
     ~@body))

(defn routing
  "Apply a list of routes to a Ring request map."
  [options request & handlers]
  (with-matcher (get options :matcher *current-matcher*)
    (some #(% request) handlers)))

(defn routes
  "Create a Ring handler by combining several handlers into one."
  [options & handlers]
  #(apply routing options % handlers))

(defmacro defroutes
  [n & args]
  (let [[options & routes] (if (map? (first args))
                             args (cons nil args))]
    `(def ~n (routes ~options ~@routes))))

(defn- assoc-route-params [request params]
  (merge-with merge request {:route-params params, :params params}))

(defn- decode-route-params [params]
  (map-vals codec/url-decode params))

(defn- remove-suffix [path suffix]
  (subs path 0 (- (count path) (count suffix))))

(defn- if-context [route handler]
  (fn [request]
    (if-let [params (clout/route-matches route request)]
      (let [uri     (:uri request)
            path    (:path-info request uri)
            context (or (:context request) "")
            subpath (:__path-info params)
            params  (dissoc params :__path-info)]
        (handler
         (-> request
             (assoc-route-params (decode-route-params params))
             (assoc :path-info (if (= subpath "") "/" subpath)
                    :context   (remove-suffix uri subpath))))))))

(defn- context-route [route]
  (let [re-context {:__path-info #"|/.*"}]
    (cond
     (string? route)
     `(clout/route-compile ~(str route ":__path-info") ~re-context)
     (vector? route)
     `(clout/route-compile
       (str ~(first route) ":__path-info")
       ~(merge (apply hash-map (rest route)) re-context))
     :else
     `(clout/route-compile (str ~route ":__path-info") ~re-context))))

(defmacro context
  [path bindings & routes]
  `(#'if-context
    ~(context-route path)
    (fn [request#]
      (let-request nil [~bindings request#]
                   (routing nil request# ~@routes)))))

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
