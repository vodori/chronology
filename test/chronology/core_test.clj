(ns chronology.core-test
  (:require [clojure.test :refer :all]
            [chronology.core :refer :all]
            [clojure.core.async :as async]))


(defn aggregator [chan]
  (async/go-loop [results []]
    (if-some [result (async/<! chan)]
      (recur (conj results result))
      results)))


(deftest one-time
  (let [key    "demo1"
        cron   "* * * * * ?"
        output (schedule-once-at-next-tick key cron (constantly 1))]
    (is (= [1] (mapv :result (async/<!! (aggregator output)))))))


(deftest many-times
  (let [key     "demo2"
        cron    "* * * * * ?"
        output  (schedule-cron key cron (constantly 1))
        results (aggregator output)]
    (Thread/sleep 5500)
    (async/close! output)
    (is (= [1 1 1 1 1] (mapv :result (async/<!! results))))))


(deftest throwing-exceptions
  (let [key     "demo3"
        cron    "* * * * * ?"
        f       (fn [& args] (throw (ex-info "Stuff" {:wow 1})))
        output  (schedule-cron key cron f)
        results (aggregator output)]
    (Thread/sleep 5500)
    (async/close! output)
    (is (= [{:wow 1} {:wow 1} {:wow 1} {:wow 1} {:wow 1}]
           (mapv (comp ex-data :error) (async/<!! results))))))


(deftest unscheduling-by-key
  (let [key     "demo4"
        cron    "* * * * * ?"
        output  (schedule-cron key cron (constantly 1))
        results (aggregator output)]
    (Thread/sleep 2200)
    (is (= #{key} (unschedule-by-key key)))
    (is (= [1 1] (mapv :result (async/<!! results))))))


(deftest unscheduling-by-group
  (let [key     (make-key "cats" "demo5")
        cron    "* * * * * ?"
        output  (schedule-cron key cron (constantly 1))
        results (aggregator output)]
    (Thread/sleep 2200)
    (is (= #{key} (unschedule-by-group "cats")))
    (is (= [1 1] (mapv :result (async/<!! results))))
    (is (not (key-scheduled? key)))))