(ns com.github.roklenarcic.malli-datetime-test
  (:require [clojure.test :refer :all]
            [java-time :as jt]
            [malli.core :as m]
            [com.github.roklenarcic.malli-datetime :as dt])
  (:import (clojure.lang ExceptionInfo)
           (java.util Date)
           (java.time LocalTime LocalDateTime OffsetDateTime ZonedDateTime YearMonth Year Instant LocalDate OffsetTime Clock ZoneId)
           (java.time.format DateTimeFormatter)))

(deftest construction-test
  (testing "Valid schemas are constructed"
    (are [x] (satisfies? m/Schema (m/schema [x]))
             dt/date
             dt/local-time
             dt/offset-time
             dt/offset-date-time
             dt/zoned-date-time
             dt/local-date-time
             dt/local-date
             dt/year-month
             dt/year
             dt/inst))
  (testing "does not allow children"
    (are [x] (thrown? ExceptionInfo (m/schema [x int?]))
             dt/date
             dt/local-time
             dt/offset-time
             dt/offset-date-time
             dt/zoned-date-time
             dt/local-date-time
             dt/local-date
             dt/year-month
             dt/year
             dt/inst)))

(deftest validation-test
  (testing "schema validates objects"
    (are [x y] (m/validate x y)
               dt/date (Date.)
               dt/local-time (LocalTime/now)
               dt/local-date-time (LocalDateTime/now)
               dt/offset-time (OffsetTime/now)
               dt/offset-date-time (OffsetDateTime/now)
               dt/zoned-date-time (ZonedDateTime/now)
               dt/local-date (LocalDate/now)
               dt/year-month (YearMonth/now)
               dt/year (Year/now)
               dt/inst (Instant/now))))

(deftest transformation-test
  (let [clock (Clock/fixed (jt/instant #inst "1999-12-13T14:15:16.178") (ZoneId/of "GMT+6"))]
    (testing "encodes the objects"
      (are [expected schema value]
        (= expected (m/encode schema value (dt/transformer {})))
        "1999-12-13T14:15:16.178Z" dt/date (jt/java-date clock)
        "20:15:16.178" dt/local-time (jt/local-time clock)
        "1999-12-13T20:15:16.178" dt/local-date-time (jt/local-date-time clock)
        "20:15:16.178+06:00" dt/offset-time (jt/offset-time clock)
        "1999-12-13T20:15:16.178+06:00" dt/offset-date-time (jt/offset-date-time clock)
        "1999-12-13T20:15:16.178+06:00[GMT+06:00]" dt/zoned-date-time (jt/zoned-date-time clock)
        "1999-12-13" dt/local-date (jt/local-date clock)
        "1999-12" dt/year-month (jt/year-month clock)
        "1999" dt/year (jt/year clock)
        "1999-12-13T14:15:16.178Z" dt/inst (jt/instant clock)))
    (testing "encode with custom formatters for the objects"
      (are [schema value]
        (= "0820" (m/encode (vector schema {:fmt "hhHH"}) value (dt/transformer {})))
        dt/local-time (jt/local-time clock)
        dt/local-date-time  (jt/local-date-time clock)
        dt/offset-time (jt/offset-time clock)
        dt/offset-date-time (jt/offset-date-time clock)
        dt/zoned-date-time (jt/zoned-date-time clock))
      (are [schema value]
        (= "1999" (m/encode (vector schema {:fmt "yyyy"}) value (dt/transformer {})))
        dt/date (jt/java-date clock)
        dt/inst (jt/instant clock)
        dt/local-date (jt/local-date clock)
        dt/year-month (jt/year-month clock)
        dt/year (jt/year clock))
      (are [schema value k]
        (= "0820" (m/encode schema value (dt/transformer {k "hhHH"})))
        dt/local-time (jt/local-time clock) :local-time
        dt/local-date-time  (jt/local-date-time clock) :local-date-time
        dt/offset-time (jt/offset-time clock) :offset-time
        dt/offset-date-time (jt/offset-date-time clock) :offset-date-time
        dt/zoned-date-time (jt/zoned-date-time clock) :zoned-date-time)
      (are [schema value k]
        (= "1999" (m/encode schema value (dt/transformer {k "yyyy"})))
        dt/date (jt/java-date clock) :date
        dt/inst (jt/instant clock) :inst
        dt/local-date (jt/local-date clock) :local-date
        dt/year-month (jt/year-month clock) :year-month
        dt/year (jt/year clock) :year))
    (testing "encode dates and instants with custom formatters"
      (are [schema value]
        (= "0214" (m/encode (vector schema {:fmt "hhHH"}) value (dt/transformer {})))
        dt/date (jt/java-date clock)
        dt/inst (jt/instant clock))
      (are [schema value k]
        (= "0214" (m/encode schema value (dt/transformer {k "hhHH"})))
        dt/date (jt/java-date clock) :date
        dt/inst (jt/instant clock) :inst)
      (are [schema value]
        (= "0315" (m/encode (vector schema {:fmt "hhHH"
                                            :tz "GMT+1"})
                            value
                            (dt/transformer {})))
        dt/date (jt/java-date clock)
        dt/inst (jt/instant clock))
      (are [schema value k]
        (= "0315" (m/encode schema value (dt/transformer {k "hhHH" :tz "GMT+1"})))
        dt/date (jt/java-date clock) :date
        dt/inst (jt/instant clock) :inst))
    (testing "decodes the objects"
      (are [value schema expected]
        (= expected (m/decode schema value (dt/transformer {})))
        "1999-12-13T14:15:16.178Z" dt/date (jt/java-date clock)
        "20:15:16.178" dt/local-time (jt/local-time clock)
        "1999-12-13T20:15:16.178" dt/local-date-time (jt/local-date-time clock)
        "20:15:16.178+06:00" dt/offset-time (jt/offset-time clock)
        "1999-12-13T20:15:16.178+06:00" dt/offset-date-time (jt/offset-date-time clock)
        "1999-12-13T20:15:16.178+06:00[GMT+06:00]" dt/zoned-date-time (jt/zoned-date-time clock)
        "1999-12-13" dt/local-date (jt/local-date clock)
        "1999-12" dt/year-month (jt/year-month clock)
        "1999" dt/year (jt/year clock)
        "1999-12-13T14:15:16.178Z" dt/inst (jt/instant clock)))))

