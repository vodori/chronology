(ns chronology.utils
  (:require [clojure.core.async :as async]
            [clj-time.core :as time]
            [chime.core-async :as chime-async]
  ;Chime now uses java.time (:require chime.joda-time) to preserve backward compatibility
            [chime.joda-time])
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
    (chime-async/chime-ch cron-sequence buffered)))

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
  "Get a sequence of forward marching timestamps according to the cron expression.

  start - The point in time (exclusive) that the sequence should march onward from.
  expression - The quartz-style cron expression.
  "
  ([expression] (forward-cron-sequence (time/now) expression))
  ([start expression] (cron-sequence start expression true)))

(defn backward-cron-sequence
  "Get a sequence of backward marching timestamps according to the cron expression.

  start - The point in time (exclusive) that the sequence should march backward from.
  expression - The quartz-style cron expression.
  "
  ([expression] (backward-cron-sequence (time/now) expression))
  ([start expression] (cron-sequence start expression false)))

(defn periodic-ticker
  "Gets a core.async channel that emits whenever a cron tick hits. Implements a sliding
  buffer so if not taken from before the next value is available the existing value will
  be dropped and only the new value will be available for taking."
  ([expression]
   (periodic-ticker (time/now) expression))
  ([start expression]
   (->> (forward-cron-sequence start expression)
        (time-sequence->ticker))))