(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject com.vodori.pepper/chronology "0.1.0"

  :repositories
  [["releases" {:username      :env/ARTIFACTORY_USERNAME
                :password      :env/ARTIFACTORY_PASSWORD
                :sign-releases false
                :url           "http://artifactory.vodori.com/artifactory/libs-release-local/"}]

   ["snapshots" {:username      :env/ARTIFACTORY_USERNAME
                 :password      :env/ARTIFACTORY_PASSWORD
                 :sign-releases false
                 :url           "http://artifactory.vodori.com/artifactory/libs-snapshot-local/"}]]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [com.cronutils/cron-utils "7.0.1"]
                 [jarohen/chime "0.2.2"]
                 [clj-time "0.14.4"]]

  :plugins [[test2junit "1.2.2"]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :test2junit-output-dir "target/test-reports")
