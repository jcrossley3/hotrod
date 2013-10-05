(ns immutant.cache.datomic
  (:require [immutant.cache.hotrod :as hotrod]
            [immutant.daemons      :as daemon]))

(defn start-datastore [& opts]
  (daemon/create "hotrod" (apply hotrod/daemon "datomic" opts) :singleton false))
