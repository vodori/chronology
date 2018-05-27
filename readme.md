A clojure library for scheduling tasks according to cron expressions.

#### Coordinates

```edn 
[com.vodori.pepper/chronology "0.1.0"]
```


#### Scheduling tasks:

```edn
(def key     "task1")
(def cron    "* * * * * ?")
(def context {:data {:stuff 1}})

(defn task [{:keys [key tick data] :as ctx}]
  (update data :stuff inc))

(def result-chan (schedule-cron key cron task context))

(async/go 
  (when-some [result (async/<! result-chan)]
    (if (failure? result) 
       (.printStackTrace (:error result))
       (println (:result result))) 
    (recur)))
```


#### Cron sequences:

```edn
(def tick (time/now))
(def next-five-ticks (take 5 (forward-cron-sequence tick cron)))
(def last-five-ticks (take 5 (backward-cron-sequence tick cron)))
```


