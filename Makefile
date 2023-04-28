repl: rebel-repl
classic-repl:
	exec clojure -M:repl
rebel-repl:
	exec clojure -M:repl/rebel
run:
	exec clojure -J-Xmx16G -M:none -m app.core
upgrade-deps:
	exec clojure -M:outdated --upgrade
javac:
	exec clojure -T:build javac
