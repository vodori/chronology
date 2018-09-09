(ns chronology.core
  (:require [clojure.core.async :as async]
            [chronology.utils :as utils]
            [clojure.string :as strings]
            [clojure.set :as set]))

(defn new-system []
  (let [state (atom {})]
    (add-watch state :watcher
      (fn [k r o n]
        (->>
          (set/difference
            (set (keys o))
            (set (keys n)))
          (select-keys o)
          (vals)
          (run! async/close!))))
    state))

(def ^:dynamic *schedule* (new-system))

(defmacro with-scheduling-system [system & body]
  `(binding [*schedule* ~system] ~@body))

(defmacro with-own-scheduling-system [& body]
  `(with-scheduling-system (new-system) ~@body))

(defn make-key [group key]
  (str group "/" key))

(defn key->group [key]
  (when (strings/includes? key "/")
    (first (strings/split key #"/" 2))))

(defn key-has-group? [group key]
  (strings/starts-with? key (make-key group "")))

(defn get-scheduled-keys []
  (set (keys @*schedule*)))

(defn get-scheduled-groups []
  (into #{} (keep key->group (get-scheduled-keys))))

(defn key-scheduled? [key]
  (contains? (get-scheduled-keys) key))

(defn group-scheduled? [group]
  (contains? (get-scheduled-groups) (key->group group)))

(defn unschedule-all
  ([] (unschedule-all *schedule*))
  ([schedule]
   (let [[o n] (reset-vals! schedule {})]
     (set/difference (set (keys o)) (set (keys n))))))

(defn unschedule-by-key [key]
  (let [[o n] (swap-vals! *schedule* dissoc key)]
    (set/difference (set (keys o)) (set (keys n)))))

(defn unschedule-by-group [group]
  (let [pred (partial key-has-group? group)
        [o n] (swap-vals!
                *schedule*
                (fn [schedule]
                  (->> (keys schedule)
                       (filter pred)
                       (apply dissoc schedule))))]
    (set/difference (set (keys o)) (set (keys n)))))

(defrecord Success [result])

(defrecord Failure [error])

(defn success? [r]
  (instance? Success r))

(defn failure? [r]
  (instance? Failure r))

(defn schedule
  ([key ticker f]
   (schedule key ticker f {}))
  ([key ticker f ctx]
   (let [system *schedule*]
     (let [context (or ctx {})
           results (async/chan)]
       (when (apply = (swap-vals! *schedule* #(if-not (contains? % key) (assoc % key ticker) %)))
         (throw (ex-info (format "Cannot schedule %s because it is already scheduled." key) {})))
       (async/go-loop []
         (if-some [tick (async/<! ticker)]
           (let [tick-context {:tick tick :key key :group (key->group key)}
                 result       (async/<!
                                (async/thread
                                  (with-scheduling-system system
                                    (let [start  (System/currentTimeMillis)
                                          result (try
                                                   (->Success (f (merge context tick-context)))
                                                   (catch Exception e (->Failure e)))
                                          stop   (System/currentTimeMillis)]
                                      (assoc result :duration (- stop start))))))]
             (async/>! results (merge result tick-context))
             (recur))
           (with-scheduling-system system
             (unschedule-by-key key)
             (async/close! results))))
       results))))

(defn schedule-cron
  ([key cron f]
   (schedule-cron key cron f {}))
  ([key cron f ctx]
   (let [context (assoc (or ctx {}) :cron cron)
         ticker  (utils/periodic-ticker cron)]
     (schedule key ticker f context))))

(defn schedule-once-at-next-tick
  ([key cron f]
   (schedule-once-at-next-tick key cron f {}))
  ([key cron f ctx]
   (let [context (assoc (or ctx {}) :cron cron)
         ticker  (->> (utils/periodic-ticker cron) (async/take 1))]
     (schedule key ticker f context))))