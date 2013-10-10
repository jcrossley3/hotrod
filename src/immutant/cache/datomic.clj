(ns immutant.cache.datomic
  (:require [immutant.cache.hotrod :as hotrod]
            [immutant.daemons      :as daemon]))

(defn start-datastore [& opts]
  (let [server (apply hotrod/daemon "datomic" opts)]
    (daemon/create "hotrod" server :singleton false)
    server))
