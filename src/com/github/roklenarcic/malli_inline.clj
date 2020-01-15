(ns com.github.roklenarcic.malli-inline
  (:require [malli.core :as m]
            [malli.transform :as mt])
  (:import (java.util Base64)
           (java.nio.charset StandardCharsets)))

(defn -inline-xf
  "Create a schema type with which will chain encoder/decoder with the child one"
  [v]
  ^{:type ::m/into-schema}
  (reify m/IntoSchema
    (-into-schema [_ properties children opts]
      (when-not (= 1 (count children))
        (m/fail! ::child-error {:name v, :properties properties, :children children, :min 1, :max 1}))
      (let [schema' (-> children first (m/schema opts))
            validator (m/-validator schema')
            form (m/create-form v properties [(m/-form schema')])]
        ^{:type ::m/schema}
        (reify
          m/Schema
          (-name [_] v)
          (-validator [_] validator)
          (-explainer [this path]
            (fn [value in acc]
              (if-not (validator value) (conj acc (m/error path in this value)) acc)))
          (-transformer [this transformer method options]
            (let [{:keys [enter leave] :as xf} (m/-value-transformer transformer this method options)
                  child-xf (m/-value-transformer transformer schema' method options)]
              (if child-xf
                (-> child-xf
                    (update :enter #(if (and % enter) (comp % enter) (or % enter)))
                    (update :leave #(if (and % leave) (comp leave %) (or % leave))))
                xf)))
          (-accept [this visitor opts] (visitor this [(m/-accept schema' visitor opts)] opts))
          (-properties [_] properties)
          (-form [_] form))))))

(def ->base64url (-inline-xf `->base64url))
(def ->base64 (-inline-xf `->base64))
(def ->base64mime (-inline-xf `->base64mime))
(def ->utf8 (-inline-xf `->utf8))

(def encoders {`->utf8 {:leave #(.getBytes ^String % StandardCharsets/UTF_8)}
               `->base64url {:leave #(.encodeToString (Base64/getUrlEncoder) %)}
               `->base64 {:leave #(.encodeToString (Base64/getEncoder) %)}
               `->base64mime {:leave #(.encodeToString (Base64/getMimeEncoder) %)}})

(def decoders {`->utf8 #(String. ^bytes % StandardCharsets/UTF_8)
               `->base64url #(.decode (Base64/getUrlDecoder) ^String %)
               `->base64 #(.decode (Base64/getDecoder) ^String %)
               `->base64mime #(.decode (Base64/getMimeDecoder) ^String %)})

(def transformer
  (mt/transformer
    {:name ::inline
     :encoders encoders
     :decoders decoders}))
