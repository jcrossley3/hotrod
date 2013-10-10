(defproject org.immutant/hotrod "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.infinispan/infinispan-server-hotrod "5.3.0.Final"
                  :exclusions [org.infinispan/infinispan-core]]])
