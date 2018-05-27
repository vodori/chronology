(ns chronology.utils-test
  (:require [chronology.utils :refer :all]
            [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as f]))

(defn dates [& isos]
  (map f/parse isos))


(deftest get-forward-cron
  (testing "I can follow a cron expression into the future."
    (let [now    (time/epoch)
          stamps (forward-cron-sequence now "0 * * * * ?")]
      (is (= (dates "1970-01-01T00:01:00.000Z"
                    "1970-01-01T00:02:00.000Z"
                    "1970-01-01T00:03:00.000Z"
                    "1970-01-01T00:04:00.000Z"
                    "1970-01-01T00:05:00.000Z")
             (take 5 stamps))))))


(deftest get-backward-cron
  (testing "I can follow a cron expression into the past."
    (let [now    (f/parse "2018-05-10T16:22:00.000Z")
          stamps (backward-cron-sequence now "0 * * * * ?")]
      (is (= (dates "2018-05-10T16:21:00.000Z"
                    "2018-05-10T16:20:00.000Z"
                    "2018-05-10T16:19:00.000Z"
                    "2018-05-10T16:18:00.000Z"
                    "2018-05-10T16:17:00.000Z")
             (take 5 stamps))))))


(deftest explaining-a-cron
  (testing "I can explain a cron expression."
    (is (= "every second" (explain-cron "* * * * * ?")))))