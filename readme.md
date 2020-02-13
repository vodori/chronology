[![Build Status](https://travis-ci.com/vodori/chronology.svg?branch=develop)](https://travis-ci.com/vodori/chronology) [![Maven metadata URL](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/com/vodori/chronology/maven-metadata.xml.svg)](https://mvnrepository.com/artifact/com.vodori/chronology)


### Chronology

A library for scheduling tasks using core.async according to cron expressions. Chronology also 
provides forward and backward infinite sequences of DateTime objects for a given cron expression. 
Cron expressions are in the style of [quartz](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html).

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

(def tick (f/parse "2000-01-01T00:00:00.000Z"))

(def cron "0 */5 * * * ?")

(defn datetime->iso [dt] 
  (f/unparse (f/formatter :date-time) dt))
  
(def human-readable (explain-cron cron))
;=> "every 5 minutes"

(->> (forward-cron-sequence tick cron)
     (map datetime->iso)
     (take 5))
     
;=> ("2000-01-01T00:05:00.000Z" 
;    "2000-01-01T00:10:00.000Z" 
;    "2000-01-01T00:15:00.000Z" 
;    "2000-01-01T00:20:00.000Z" 
;    "2000-01-01T00:25:00.000Z")


(->> (backward-cron-sequence tick cron)
     (map datetime->iso)
     (take 5))
     
;=> ("1999-12-31T23:55:00.000Z" 
;    "1999-12-31T23:50:00.000Z" 
;    "1999-12-31T23:45:00.000Z" 
;    "1999-12-31T23:40:00.000Z" 
;    "1999-12-31T23:35:00.000Z")

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
