(defproject simple-experiments "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [com.7theta/re-frame-fx "0.2.1"]
                 [org.clojure/test.check "0.9.0"]
                 [reagent "0.8.1" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server cljsjs/create-react-class]]
                 [re-frame "0.10.5"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.14"]]
  :clean-targets ["target/" "index.android.js" #_($PLATFORM_CLEAN$)]
  :aliases {"prod-build" ^{:doc "Recompile code with prod profile."}
            ["do" "clean"
             ["with-profile" "prod" "cljsbuild" "once"]]
            "advanced-build" ^{:doc "Recompile code for production using :advanced compilation."}
            ["do" "clean"
             ["with-profile" "advanced" "cljsbuild" "once"]]}
  :jvm-opts ["-XX:+IgnoreUnrecognizedVMOptions" "--add-modules=java.xml.bind"]
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.14"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/test.check "0.9.0"]]
                   :source-paths ["src" "env/dev"]
                   :cljsbuild
                   {:builds [{:id           "android"
                              :source-paths ["src" "env/dev"]
                              :figwheel     true
                              :compiler     {:output-to     "target/android/index.js"
                                             :main          "env.android.main"
                                             :output-dir    "target/android"
                                             :optimizations :none
                                             :target :nodejs}}
                             #_($DEV_PROFILES$)]}
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :prod {:cljsbuild
                    {:builds [{:id           "android"
                               :source-paths ["src" "env/prod"]
                               :compiler     {:output-to     "index.android.js"
                                              :main          "env.android.main"
                                              :output-dir    "target/android"
                                              :static-fns    true
                                              :optimize-constants true
                                              :optimizations :simple
                                              :target :nodejs
                                              :closure-defines {"goog.DEBUG" false}}}
                              #_($PROD_PROFILES$)]}}
             :advanced {:dependencies [[react-native-externs "0.1.0"]]
                        :cljsbuild
                        {:builds [{:id           "android"
                                   :source-paths ["src" "env/prod"]
                                   :compiler     {:output-to     "index.android.js"
                                                  :main          "env.android.main"
                                                  :output-dir    "target/android"
                                                  :static-fns    true
                                                  :optimize-constants true
                                                  :optimizations :advanced
                                                  :target :nodejs
                                                  :closure-defines {"goog.DEBUG" false}}}
                                  #_($ADVANCED_PROFILES$)]}}})
