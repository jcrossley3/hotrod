(ns immutant.init
  (:require [immutant.cache.datomic :as datomic]))

(datomic/start-datastore)
