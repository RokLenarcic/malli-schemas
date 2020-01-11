(ns com.github.roklenarcic.malli-inline-test
  (:require [clojure.test :refer :all]
            [malli.core :as m]
            [com.github.roklenarcic.malli-inline :as in])
  (:import (clojure.lang ExceptionInfo)))

(deftest construction-test
  (testing "Valid schemas are constructed"
    (are [x] (satisfies? m/Schema (m/schema [x int?]))
             in/->base64
             in/->base64mime
             in/->base64url
             in/->utf8))
  (testing "Requires children"
    (are [x] (thrown? ExceptionInfo (m/schema x))
             in/->base64
             in/->base64mime
             in/->base64url
             in/->utf8)))

(deftest validation-test
  (testing "schema validates objects"
    (are [x] (m/validate [x int?] 1)
             in/->base64
             in/->base64mime
             in/->base64url
             in/->utf8)))

(deftest transformation-test
  (testing "encodes the objects"
    (are [expected schema value]
      (= expected (m/encode schema value in/transformer))
      ;(byte-array [65 66 67]) [in/->utf8 string?] "ABC"
      "/+9AAA==" [in/->base64 bytes?] (byte-array [0xff 0xef 0x40 0x00])
      "_-9AAA==" [in/->base64url bytes?] (byte-array [0xff 0xef 0x40 0x00])
      "/+9AAA==" [in/->base64mime bytes?] (byte-array [0xff 0xef 0x40 0x00])
      "QUJD" [in/->base64 [in/->utf8 bytes?]] "ABC"
      "QUJD" [in/->base64url [in/->utf8 bytes?]] "ABC"
      "QUJD" [in/->base64mime [in/->utf8 bytes?]] "ABC"))
  (testing "decodes the objects"
    (are [schema value]
      (= (seq (byte-array [0xff 0xef 0x40 0x00])) (seq (m/decode schema value in/transformer)))
      [in/->base64 bytes?] "/+9AAA=="
      [in/->base64url bytes?] "_-9AAA=="
      [in/->base64mime bytes?] "/+9AAA==")
    (are [schema]
      (= "ABC" (m/decode schema "QUJD" in/transformer))
      [in/->base64 [in/->utf8 bytes?]]
      [in/->base64url [in/->utf8 bytes?]]
      [in/->base64mime [in/->utf8 bytes?]])))
