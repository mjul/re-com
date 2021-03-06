(require 'leiningen.core.eval)

(def http-port 3449)

;; ---------------------------------------------------------------------------------------

(defproject         re-com "lein-git-inject/version"
  :description      "Reusable UI components for Reagent"
  :url              "https://github.com/day8/re-com.git"
  :license          {:name "MIT"}

  :dependencies [[org.clojure/clojure         "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript   "1.10.773" :scope "provided"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs        "2.11.4" :scope "provided"]
                 [reagent                     "0.10.0" :scope "provided"]
                 [org.clojure/core.async      "1.3.610"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]]

  :plugins      [[day8/lein-git-inject "0.0.14"]
                 [lein-shadow          "0.3.1"]
                 [lein-ancient         "0.6.15"]
                 [lein-shell           "0.5.0"]
                 [lein-pprint          "1.3.2"]]


  :middleware   [leiningen.git-inject/middleware]

  :profiles {:dev      {:source-paths ["dev-src"]
                        :dependencies [[clj-stacktrace "0.2.8"]
                                       [binaryage/devtools "1.0.2"]]
                        :plugins      [[org.clojure/data.json "0.2.6"]]}

             :demo     {:dependencies [[alandipert/storage-atom "2.0.1"]
                                       [com.cognitect/transit-cljs "0.8.256"] ;; Overrides version in storage-atom which prevents compiler warnings about uuid? and boolean? being replaced
                                       [clj-commons/secretary "1.2.4"]]}}

  :source-paths    ["src"]
  :test-paths      ["test"]
  :resource-paths  ["run/resources"]

  :clean-targets ^{:protect false} [:target-path
                                    "shadow-cljs.edn"
                                    "node_modules"
                                    "run/resources/public/compiled_dev"
                                    "run/resources/public/compiled_prod"
                                    "run/resources/public/compiled_test"]

  :deploy-repositories [["clojars"  {:sign-releases false
                                     :url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_TOKEN}]]

  ;; Exclude the demo and compiled files from the output of either 'lein jar' or 'lein install'
  :jar-exclusions   [#"(?:^|\/)re_demo\/" #"(?:^|\/)demo\/" #"(?:^|\/)compiled.*\/" #"html$"]

  :shadow-cljs {:nrepl  {:port 7777}

                :builds {:demo         {:target   :browser
                                        :modules  {:demo {:init-fn  re-demo.core/mount-demo
                                                          :preloads [day8.app.dev-preload]}}
                                        :dev      {:asset-path       "/compiled_dev/demo"
                                                   :output-dir       "run/resources/public/compiled_dev/demo"
                                                   :compiler-options {:external-config {:devtools/config {:features-to-install [:formatters :hints]}}}}
                                        :release  {:output-dir "run/resources/public/compiled_prod/demo"}
                                        :devtools {:http-port ~http-port
                                                   :http-root "run/resources/public"
                                                   :push-state/index "index_dev.html"}}

                         :browser-test {:target           :browser-test
                                        :ns-regexp        "-test$"
                                        :test-dir         "run/resources/public/compiled_test/demo"
                                        :compiler-options {:external-config {:devtools/config {:features-to-install [:formatters :hints]}}}
                                        :devtools         {:http-port 8021
                                                           :http-root "run/resources/public/compiled_test/demo"
                                                           :preloads  [day8.app.dev-preload]}}
                         :karma-test    {:target :karma
                                         :ns-regexp ".*-test$"
                                         :output-to "target/karma/test.js"
                                         :compiler-options {:pretty-print true}}}}

  :release-tasks [["deploy-aws"]
                  ["deploy" "clojars"]]

  :shell {:commands {"karma" {:windows         ["cmd" "/c" "karma"]
                              :default-command "karma"}
                     "open"  {:windows         ["cmd" "/c" "start"]
                              :macosx          "open"
                              :linux           "xdg-open"}}}

  :aliases          {;; *** DEV ***
                     "watch"   ["with-profile" "+dev,+demo" "do"
                                   ["clean"]
                                   ["shadow" "watch" "demo" "browser-test" "karma-test"]]

                     ;; *** PROD ***
                     "prod-once"  ["with-profile" "+prod-run,+demo,-dev" "do"
                                   ["clean"]
                                   ["shadow" "release" "demo"]]

                     "deploy-aws" ["with-profile" "+prod-run,+demo,-dev" "do"
                                   ["clean"]
                                   ["shadow" "release" "demo"]
                                   ~["shell" "aws" "s3" "sync" "run/resources/public" "s3://re-demo/" "--acl" "public-read" "--cache-control" "max-age=2592000,public"]]

                     ;; *** TEST ***
                     "ci" ["do"
                             ["with-profile" "+dev" "do"
                              ["clean"]
                              ["shadow" "compile" "karma-test"]
                              ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]
                             ["with-profile" "+demo,-dev" "do"
                              ["clean"]
                              ["shadow" "release" "demo"]]]})


