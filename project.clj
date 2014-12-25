(defproject ru.prepor.schema-compojure "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [prismatic/plumbing "0.3.5"]]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.2.0"]]}})
