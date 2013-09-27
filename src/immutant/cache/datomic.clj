(ns immutant.cache.datomic
  (:require [immutant.cache.hotrod :as hotrod]
            [immutant.daemons      :as daemon]))

(defn start-datastore
  []
  (let [manager (hotrod/cache-manager)]
    (hotrod/configure-cache "datomic" manager)
    (daemon/create "hotrod" (hotrod/daemon manager))))
