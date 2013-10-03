(ns immutant.cache.hotrod
  (:use [immutant.cache.core :only (manager builder)]
        [immutant.daemons :only (Daemon)])
  (:import org.infinispan.commons.equivalence.ByteArrayEquivalence
           org.infinispan.manager.DefaultCacheManager
           org.infinispan.configuration.global.GlobalConfigurationBuilder
           org.infinispan.remoting.transport.jgroups.JGroupsTransport
           org.infinispan.server.hotrod.HotRodServer
           org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder))

(defn manager-config-builder
  []
  (let [current (.getCacheManagerConfiguration @manager)
        props (assoc (into {} (.. current transport properties)) JGroupsTransport/CHANNEL_LOOKUP "org.immutant.cache.ChannelProvider")]
    (.. (GlobalConfigurationBuilder.)
        (read current)
        (classLoader (.getContextClassLoader (Thread/currentThread)))
        transport (transport (JGroupsTransport.)) (withProperties (doto (java.util.Properties.) (.putAll props))))))

(defn cache-config-builder
  []
  (.. (builder {})
      dataContainer (keyEquivalence ByteArrayEquivalence/INSTANCE)))

(defn cache-manager
  ([] (cache-manager (manager-config-builder)))
  ([builder] (DefaultCacheManager. (.build builder))))

(defn configure-cache
  ([name manager]
     (configure-cache name manager (cache-config-builder)))
  ([name manager builder]
     (.defineConfiguration manager name (.build builder))
     (.getCache manager name)))

(defn daemon
  ([cache-name]
     (daemon cache-name (cache-config-builder)))
  ([cache-name cache-builder]
     (daemon cache-name cache-builder (manager-config-builder)))
  ([cache-name cache-builder manager-builder]
     (let [manager (atom nil)
           hotrod (HotRodServer.)
           hotrod-builder (HotRodServerConfigurationBuilder.)]
       (reify Daemon
         (start [_]
           (reset! manager (cache-manager manager-builder))
           (configure-cache cache-name @manager cache-builder)
           (.start hotrod (.build hotrod-builder) @manager))
         (stop [_]
           (.stop hotrod)
           (.stop @manager))))))
