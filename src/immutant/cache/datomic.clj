(ns immutant.cache.datomic
  (:require [immutant.cache.hotrod :as hotrod]
            [immutant.daemons      :as daemon]))

(defn start-datastore
  []
  (daemon/create "hotrod" (hotrod/daemon "datomic")))
