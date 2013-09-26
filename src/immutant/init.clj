(ns immutant.init
  (:require [immutant.daemons       :as daemon]
            [immutant.cache.hotrod  :as hotrod]))

(let [manager (hotrod/cache-manager)]
  (hotrod/configure-cache "datomic" manager)
  (daemon/create "hotrod" (hotrod/daemon manager)))
