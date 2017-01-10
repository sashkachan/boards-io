(require '[cljs.build.api :as b])

(b/build (b/inputs "src/cljs" "src/clj" "src/test")
  {:target :nodejs
   :main 'boards-io.run-tests
   :output-to "target/test/test.js"
   :output-dir "target/test/out"
   :parallel-build true
;   :optimizations :simple
   :compiler-stats true
   :static-fns true
   :cache-analysis true
   
   :verbose true})

(System/exit 0)

