(ns immutant.cache.hotrod
  (:use [immutant.cache.core :only (manager builder start)]
        [immutant.daemons :only (Daemon)]
        [immutant.util :only (mapply)])
  (:import org.infinispan.commons.equivalence.ByteArrayEquivalence
           org.infinispan.manager.DefaultCacheManager
           org.infinispan.configuration.global.GlobalConfigurationBuilder
           org.infinispan.remoting.transport.jgroups.JGroupsTransport
           org.infinispan.server.hotrod.HotRodServer
           org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder))

;;; This is effectively a dependency declaration. It ensures that the
;;; JGroups transport is initialized since that only occurs lazily
;;; when the first cache is created. Otherwise, we risk a race
;;; condition when we try to create a cache using the CacheManager for
;;; the HotRodServer *before* the AS CM (from which we get our config)
;;; has initialized.
(.getCache @manager)

(defn hotrod-config-builder
  "Configuration builder for the HotRod server"
  [& {:keys [host port] :or {host "127.0.0.1", port 11222}}]
  (.. (HotRodServerConfigurationBuilder.) (host host) (port port)))

(defn manager-config-builder
  "Use the configuration builder from the AS CacheManager,
  hijacking its JGroups channel when clustered"
  []
  (let [current (.getCacheManagerConfiguration @manager)
        builder (.. (GlobalConfigurationBuilder.)
                    (read current)
                    (classLoader (.getContextClassLoader (Thread/currentThread))))]
    (if (.. current transport transport) ; non-nil means we're clustered
      (.. builder transport (transport (JGroupsTransport.))
          (withProperties (doto (java.util.Properties.)
                            (.putAll (.. current transport properties))
                            (.put JGroupsTransport/CHANNEL_LOOKUP "org.immutant.cache.ChannelProvider"))))
      builder)))

(defn cache-config-builder
  "Use the default configuration builder for caches created from
  immutant.cache, with a comparator for hotrod's byte[] keys"
  []
  (.. (builder {})
      dataContainer (keyEquivalence ByteArrayEquivalence/INSTANCE)))

(defn cache-manager
  "HotRod requires its very own CacheManager, so it can hook into its
  lifecycle events. This prevents us from directly exposing the AS CM,
  but we do borrow its config."
  ([] (cache-manager (manager-config-builder)))
  ([builder] (DefaultCacheManager. (.build builder))))

(defn configure-cache
  "Create a cache via the passed CacheManager"
  ([name manager]
     (configure-cache name manager (cache-config-builder)))
  ([name manager builder]
     (start name (.build builder) manager)))

(defn build-with-port-offset
  [hotrod-builder]
  (if-let [offset (System/getProperty "jboss.socket.binding.port-offset")]
    (let [current (.build hotrod-builder)]
      (.build (.. hotrod-builder (port (+ (.port current) (read-string offset))))))
    (.build hotrod-builder)))

(defn daemon
  "Returns an immutant.daemons/Daemon instance that when started,
  starts a CacheManager, a single Cache and a HotRod server, each
  configured by its respective configuration builder, any of which may
  be overridden via options"
  [cache-name & {:keys [cache-builder manager-builder hotrod-builder host port] :as opts}]
  (let [cache-cb   (or cache-builder   (cache-config-builder))
        manager-cb (or manager-builder (manager-config-builder))
        hotrod-cb  (or hotrod-builder  (mapply hotrod-config-builder opts))
        manager (atom nil)             ; cannot be re-used once stopped
        hotrod  (HotRodServer.)]
    (reify Daemon
      (start [_]
        (reset! manager (cache-manager manager-cb))
        (configure-cache cache-name @manager cache-cb)
        (.start hotrod (build-with-port-offset hotrod-cb) @manager))
      (stop [_]
        (.stop hotrod)
        (.stop @manager)))))
