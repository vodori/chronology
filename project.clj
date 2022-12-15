(defproject com.vodori/chronology "0.1.2"
  :description "A lightweight task scheduler and cron utilities"

  :url
  "https://github.com/vodori/chronology"

  :license
  {:name "MIT License" :url "http://opensource.org/licenses/MIT" :year 2018 :key "mit"}

  :scm
  {:name "git" :url "https://github.com/vodori/chronology"}

  :pom-addition
  [:developers
   [:developer
    [:name "Paul Rutledge"]
    [:url "https://github.com/rutledgepaulv"]
    [:email "paul.rutledge@vodori.com"]
    [:timezone "-5"]]
   [:developer
    [:name "Travis Stom"]
    [:url "https://github.com/travisstom"]
    [:email "travis.stom@vodori.com"]
    [:timezone "-4"]]]

  :deploy-repositories
  {"releases"  {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/" :creds :gpg}
   "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/" :creds :gpg}}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.6.673"]
                 [com.cronutils/cron-utils "9.2.0"]
                 [jarohen/chime "0.3.3"]
                 [clj-time "0.15.2"]])
