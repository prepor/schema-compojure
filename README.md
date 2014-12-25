# ru.prepor/schema-compojure

[![Travis status](https://secure.travis-ci.org/prepor/schema-compojure.png)](http://travis-ci.org/prepor/schema-compojure)

[![Clojars Project](http://clojars.org/ru.prepor/schema-compojure/latest-version.svg)](http://clojars.org/twarc)

This is [Compojure](https://github.com/weavejester/compojure) based library for integration with Prismatic's [fnk syntax](https://github.com/Prismatic/plumbing#bring-on-defnk) and [Schema](https://github.com/Prismatic/schema) validation and coercion. See [ru.prepor.component](https://github.com/prepor/component) for integration with [component](https://github.com/stuartsierra/component)

## Usage

```clojure
(require '[ru.prepor.schema-compojure :refer [defroutes context GET]])

(defroutes app {:matcher coerce/string-coercion-matcher}
  (context "/admin" []
    (context "/users/:id" [[:params id :- s/Int]]
      (GET "/" []
        (format "Hello, %s" (inc id)))))
  (GET "/" [] "Hello world!"))

(prn "Result" (app {:request-method :get :uri "/admin/users/2"}))
```
## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
