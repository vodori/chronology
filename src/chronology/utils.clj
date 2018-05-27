(ns chronology.utils
  (:require [clojure.core.async :as async]
            [clj-time.core :as time]
            [chime :as chime])
  (:import (com.cronutils.model.time ExecutionTime)
           (java.time ZonedDateTime)
           (org.joda.time DateTime DateTimeZone)
           (com.cronutils.descriptor CronDescriptor)
           (com.cronutils.model.definition CronDefinitionBuilder)
           (com.cronutils.model CronType)
           (com.cronutils.parser CronParser)))

(defn- parse-cron [expression]
  (let [definition (CronDefinitionBuilder/instanceDefinitionFor CronType/QUARTZ)
        parser     (CronParser. definition)]
    (.parse parser expression)))

(defn- date-time->zoned-date-time [^DateTime date-time]
  (.toZonedDateTime (.toGregorianCalendar date-time)))

(defn- zoned-date-time->date-time [^ZonedDateTime date-time]
  (let [id (DateTimeZone/forID (.getId (.getZone date-time)))]
    (DateTime. (.toEpochMilli (.toInstant date-time)) id)))

(defn- time-sequence->ticker [cron-sequence]
  (let [buffered (async/chan (async/sliding-buffer 1))]
    (chime/chime-ch cron-sequence buffered)))

(defn- cron-sequence
  [start expression forward?]
  (let [parsed     (parse-cron expression)
        executions (ExecutionTime/forCron parsed)]
    (letfn [(step [now]
              (.get
                (if forward?
                  (.nextExecution executions now)
                  (.lastExecution executions now))))]
      (->> (iterate step (date-time->zoned-date-time start))
           (map zoned-date-time->date-time)
           (filter (partial not= start))))))

(defn explain-cron [expression]
  (let [parsed     (parse-cron expression)
        descriptor (CronDescriptor/instance)]
    (.describe descriptor parsed)))

(defn forward-cron-sequence
  ([expression] (forward-cron-sequence (time/now) expression))
  ([start expression] (cron-sequence start expression true)))

(defn backward-cron-sequence
  ([expression] (backward-cron-sequence (time/now) expression))
  ([start expression] (cron-sequence start expression false)))

(defn periodic-ticker
  ([expression]
   (periodic-ticker (time/now) expression))
  ([start expression]
   (->> (forward-cron-sequence start expression)
        (time-sequence->ticker))))