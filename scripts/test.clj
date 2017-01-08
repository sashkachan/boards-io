(require '[cljs.build.api :as b])

(b/build (b/inputs "src/cljs" "src/test")
  {:target :nodejs
   :main 'boards-io.run-tests
   :output-to "target/test/test.js"
   :output-dir "target/test/out"
   :parallel-build true
   :compiler-stats true
   :static-fns true
   :verbose true})

(System/exit 0)

