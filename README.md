# Extra Malli Schemas

This project contains a couple of extra Malli schemas and transformers.

#### DateTime

In `com.github.roklenarcic/malli-datetime` namespaces there is a plethora of schemas
that work on top of Java 8 Time API classes and `java.util.Date`.

Given require `[com.roklenarcic.malli-datetime :as dt]` you can use new schemas as such:

```
(m/validate dt/date #inst "1999")
```

You can encode with the provided transformer (which you can combine with your other transformers):

```
(m/encode dt/date #inst "1999" (dt/transformer {}))
=> "1999-01-01T00:00:00Z"
```

The empty map is where you can provide your own date-time formatters, here are the defaults:

```clojure
{:year "yyyy"
 :year-month "yyyy-MM"
 :local-date-time DateTimeFormatter/ISO_LOCAL_DATE_TIME
 :zoned-date-time DateTimeFormatter/ISO_ZONED_DATE_TIME
 :offset-date-time DateTimeFormatter/ISO_OFFSET_DATE_TIME
 :local-time DateTimeFormatter/ISO_LOCAL_TIME
 :offset-time DateTimeFormatter/ISO_OFFSET_TIME
 :local-date DateTimeFormatter/ISO_LOCAL_DATE
 :date DateTimeFormatter/ISO_INSTANT
 :inst DateTimeFormatter/ISO_INSTANT}
```

You can specify your own formatter as formatter object or string.

You can specify formatter for a particular schema at the schema definition itself using option key `:fmt`:

```
(m/encode [dt/date {:fmt "yyyy"}] #inst "1999" (dt/transformer {}))
=> "1999-01-01T00:00:00Z"
```

#### *Important*

*`date` and `inst` parsing and formatting require a formatter that either doesn't use a timezone (like the ISO format) 
or your provided `DateTimeFormatter` object will need to have timezone override set:*

```clojure
[dt/inst {:fmt (-> (DateTimeFormatter/ofPattern "yyyy") (.withZone (ZoneId/of "GMT")))}]
```

*If you provide formatter as a String, this will be done for you automatically for `date` and `inst` schemas.
The `ZoneId` used will be identified by `:tz` option in transformer constructor or by override in schema properties:*  

```clojure
[dt/inst {:fmt "yyyy" :tz "America/Detroit"}]
```

#### Inline

In `com.github.roklenarcic/malli-inline` namespaces there are schemas
that validate the schemas they are wrapping, but at transformation time
they modify transformation of underlying schema.

Using require `[com.github.roklenarcic.malli-inline :as mi]`

```clojure
(m/validate [mi/->base64 string?] "A")
```

This does the same thing as just `string?`, the schema just delegates
validation to child schema.

But when you use transformer:

```clojure
(m/encode [mi/->base64 bytes?] (byte-array [1 2 3]) mi/transformer)
=> "AQID"
```

The base64 schemas require that the result of underlying schema's transformation is a byte array. 
To encode string bytes to base64, we can combine this schema with another schema `->utf-8`:

```clojure
(m/encode [mi/->base64 [mi/->utf8 string?]] "ABC" mi/transformer)
=> "QUJD"
```

You can write your own such schemas easily, for instance to inline object as xml string:

```clojure
(def ->xml (mi/-inline-xf `->xml))
(def my-transfomer {:name ::xml 
                    :decoders {`->xml {:enter xml-decoder-fn}}
                    :encoders {`->xml {:leave xml-encoder-fn}}})
```

You might be wondering why encoder transformation is `:leave`.

All inline schemas will run it's own `:enter` transformation first, then the child's `:enter` transformation.
The `:leave` transformation is done in reverse, first the child's then the inline schema's.

Obviously when encoding we want to use child's transformation to get a byte array first (or whatever the inline-xf requires)
and then run the `-inline-xf` schema's tranformation. That means the `:leave` composition is what we want.

## License

Copyright © 2019-2020 Rok Lenarčič.

Available under the terms of the Eclipse Public License 2.0, see `LICENSE`.
