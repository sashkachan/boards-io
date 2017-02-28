(use 'figwheel-sidecar.repl-api)
(use 'boards-io.system)

(system-start)
(start-figwheel!) ;; <-- fetches configuration
(cljs-repl)
