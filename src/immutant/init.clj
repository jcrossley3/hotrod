(ns immutant.init
  (:require [immutant.cache.datomic :as datomic]))

;;; Uncomment to see deadlock at deploy
;; (datomic/start-datastore)
