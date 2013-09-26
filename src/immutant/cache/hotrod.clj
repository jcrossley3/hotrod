(ns immutant.cache.hotrod
  (:use [immutant.cache.core :only (manager builder)]
        [immutant.daemons :only (Daemon create)])
  (:import org.infinispan.commons.equivalence.ByteArrayEquivalence
           org.infinispan.manager.DefaultCacheManager
           org.infinispan.configuration.global.GlobalConfigurationBuilder
           ;; org.infinispan.remoting.transport.jgroups.JGroupsTransport
           org.infinispan.server.hotrod.HotRodServer
           org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder))

(defn manager-config-builder
  ([name]
     (.. (GlobalConfigurationBuilder.)
         (read (.getCacheManagerConfiguration @manager))
         (classLoader (.getContextClassLoader (Thread/currentThread)))
         transport (clusterName name))))

(defn cache-config-builder
  []
  (.. (builder {})
      dataContainer (keyEquivalence ByteArrayEquivalence/INSTANCE)))

(defn cache-manager
  ([] (cache-manager (manager-config-builder "hotrod")))
  ([builder] (DefaultCacheManager. (.build builder))))

(defn configure-cache
  ([name manager]
     (configure-cache name manager (cache-config-builder)))
  ([name manager builder]
     (.defineConfiguration manager name (.build builder))
     (.getCache manager name)))

(defn daemon
  [manager]
  (let [hotrod (HotRodServer.)
        hotrod-builder (HotRodServerConfigurationBuilder.)]
    (reify Daemon
      (start [_]
        (.start hotrod (.build hotrod-builder) manager))
      (stop [_]
        (.stop hotrod)
        (.stop manager)))))
