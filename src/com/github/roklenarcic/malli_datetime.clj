(ns com.github.roklenarcic.malli-datetime
  (:require [malli.core :as m]
            [malli.transform :as mt])
  (:import (java.time Instant OffsetDateTime LocalDateTime ZonedDateTime LocalDate OffsetTime LocalTime YearMonth Year ZoneId)
           (java.util Date)
           (java.time.format DateTimeFormatter)))

(defn- ^DateTimeFormatter -dtf [fmt tz]
  (if (string? fmt)
    (cond-> (DateTimeFormatter/ofPattern fmt)
      (instance? ZoneId tz) (.withZone tz)
      (string? tz) (.withZone (ZoneId/of tz)))
    fmt))

(defn- -dtschema [v]
  ^{:type ::m/into-schema}
  (reify m/IntoSchema
    (-into-schema [_ properties children options]
      (let [form (m/create-form v properties children)
            validator ({`year             #(instance? Year %)
                        `year-month       #(instance? YearMonth %)
                        `local-date-time  #(instance? LocalDateTime %)
                        `zoned-date-time  #(instance? ZonedDateTime %)
                        `offset-date-time #(instance? OffsetDateTime %)
                        `local-time       #(instance? LocalTime %)
                        `offset-time      #(instance? OffsetTime %)
                        `local-date       #(instance? LocalDate %)
                        `date             #(instance? Date %)
                        `inst             #(instance? Instant %)} v)]
        (when (seq children)
          (m/fail! ::child-error {:name v, :properties properties, :children children, :min 0, :max 0}))
        ^{:type ::m/schema}
        (reify
          m/Schema
          (-name [_] v)
          (-validator [_] validator)
          (-explainer [this path]
            (fn [value in acc]
              (if-not (validator value) (conj acc (m/error path in this value)) acc)))
          (-transformer [this transformer method options]
            (m/-value-transformer transformer this method options))
          (-accept [this visitor opts] (visitor this (vec children) opts))
          (-properties [_] properties)
          (-options [_] options)
          (-form [_] form))))))

(def year (-dtschema `year))
(def year-month (-dtschema `year-month))
(def local-date-time (-dtschema `local-date-time))
(def zoned-date-time (-dtschema `zoned-date-time))
(def offset-date-time (-dtschema `offset-date-time))
(def local-time (-dtschema `local-time))
(def offset-time (-dtschema `offset-time))
(def local-date (-dtschema `local-date))
(def date (-dtschema `date))
(def inst (-dtschema `inst))

(defn -encoder [formatter tz]
  {:compile (fn [schema _]
              (let [dtf (-dtf (:fmt (m/-properties schema) formatter)
                              (:tz (m/-properties schema) tz))]
                #(.format dtf %)))})

(defn -decoder [formatter constructor tz]
  {:compile (fn [schema _]
              (let [dtf (-dtf (:fmt (m/-properties schema) formatter)
                              (:tz (m/-properties schema) tz))]
                #(constructor (.parse dtf %))))})

(defn transformer
  "Create a transformer configuration for Java 8 date and time types and java.util.Date.

  It takes a map of formatters for each type, with ISO formatters as defaults, so it works with
  empty map. Formatters can be strings or DateTimeFormatter objects.

  DateTimeFormatter objects will be used as-is, which means that to format java.util.Date and
  java.time.Instant you need to use formatter that doesn't require a timezone or you must set a timezone override.

  `tz` parameter specifies timezone that will be used by default when formatter for inst or date is specified
  as string.

  "
  [{:keys [year year-month local-date-time zoned-date-time offset-date-time
           local-time offset-time local-date date inst tz]
    :or   {year             "yyyy"
           year-month       "yyyy-MM"
           local-date-time  DateTimeFormatter/ISO_LOCAL_DATE_TIME
           zoned-date-time  DateTimeFormatter/ISO_ZONED_DATE_TIME
           offset-date-time DateTimeFormatter/ISO_OFFSET_DATE_TIME
           local-time       DateTimeFormatter/ISO_LOCAL_TIME
           offset-time      DateTimeFormatter/ISO_OFFSET_TIME
           local-date       DateTimeFormatter/ISO_LOCAL_DATE
           date             DateTimeFormatter/ISO_INSTANT
           inst             DateTimeFormatter/ISO_INSTANT
           tz               "UTC"}}]
  (mt/transformer
    {:name :date-time
     :decoders {`year (-decoder year #(Year/from %) nil)
                `year-month (-decoder year-month #(YearMonth/from %) nil)
                `local-date-time (-decoder local-date-time #(LocalDateTime/from %) nil)
                `zoned-date-time (-decoder zoned-date-time #(ZonedDateTime/from %) nil)
                `offset-date-time (-decoder offset-date-time #(OffsetDateTime/from %) nil)
                `local-time (-decoder local-time #(LocalTime/from %) nil)
                `offset-time (-decoder offset-time #(OffsetTime/from %) nil)
                `local-date (-decoder local-date #(LocalDate/from %) nil)
                `inst (-decoder inst #(Instant/from %) tz)
                `date {:compile (fn [schema _]
                                  (let [dtf (-dtf (:fmt (m/-properties schema) date)
                                                  (:tz (m/-properties schema) tz))]
                                    #(Date. (.toEpochMilli (Instant/from (.parse dtf %))))))}}
     :encoders {`year (-encoder year nil)
                `year-month (-encoder year-month nil)
                `local-date-time (-encoder local-date-time nil)
                `zoned-date-time (-encoder zoned-date-time nil)
                `offset-date-time (-encoder offset-date-time nil)
                `local-time (-encoder local-time nil)
                `offset-time (-encoder offset-time nil)
                `local-date (-encoder local-date nil)
                `inst (-encoder inst tz)
                `date {:compile (fn [schema _]
                                  (let [dtf (-dtf (:fmt (m/-properties schema) date)
                                                  (:tz (m/-properties schema) tz))]
                                    #(.format dtf (Instant/ofEpochMilli (.getTime ^Date %)))))}}}))
