[![Build Status](https://travis-ci.org/vodori/chronology.svg?branch=develop)](https://travis-ci.org/vodori/chronology) [![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/vodori/chronology/maven-metadata.xml.svg)](https://mvnrepository.com/artifact/com.vodori/chronology)


### Chronology

A library for scheduling tasks according to cron expressions. Also provides forward and backward
infinite sequences of DateTime objects for a given cron expression (quartz style).

### Install

```edn 
[com.vodori/chronology "0.1.0"]
```

### Usage

___

#### Cron sequences:

Chronology lets you convert a cron expression into an infinite sequence
of DateTime instances. Internally it just does this and then converts the
sequence into a channel by using [chime](https://github.com/jarohen/chime).

```clojure

(require '[chronology.utils :refer :all])

(def tick (time/now))

(def next-five-ticks (take 5 (forward-cron-sequence tick cron)))

(def last-five-ticks (take 5 (backward-cron-sequence tick cron)))

```

___

#### Scheduling tasks:

Tasks in chronology are just functions that receive a single `ctx` map.
The context is whatever context you provide when scheduling the function
merged with information maintained by the library itself (the task group, 
task key, and the tick DateTime).

Your function is always executed on a dedicated thread and the result is put onto a
result channel which is returned when you schedule the task. The result channel contains
records of type Success or type Failure depending on if the function threw. Each result
also contains the full context and a duration of how long the task took to complete (or throw).
The result channel is unbuffered so you *must* take the result in order for the task to keep executing.

```clojure

(require '[chronology.core :refer :all])

(def key     (make-key "group1" "task1"))
(def cron    "* * * * * ?")
(def context {:customerId 1})

(defn task [{:keys [group key tick customerId] :as ctx}]
  (send-emails-for customerId))

(def result-chan (schedule-cron key cron task context))

(async/go-loop []
  (when-some [result (async/<! result-chan)]
    (if (failure? result) 
       (.printStackTrace (:error result))
       (println (:result result))) 
    (recur)))
```

___ 


### License
This project is licensed under [MIT license](http://opensource.org/licenses/MIT).