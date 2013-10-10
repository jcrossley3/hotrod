(ns immutant.init
  (:require [immutant.cache.datomic :as datomic]))

(def server (datomic/start-datastore))
