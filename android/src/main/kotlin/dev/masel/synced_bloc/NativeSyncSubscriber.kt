package dev.masel.synced_bloc

class NativeSyncSubscriber private constructor(val masterId: String) {
    companion object {
        private val nativeSyncSubscribers: MutableList<NativeSyncSubscriber> = mutableListOf()

        fun withMasterId(masterId: String): NativeSyncSubscriber {
            val existing = nativeSyncSubscribers.firstOrNull { it.masterId == masterId }
            return if (existing != null) {
                existing.sync() //So always trigger change listeners
                existing
            } else {
                val new = NativeSyncSubscriber(masterId)
                SyncedBlocPlugin.getMasterWithId(masterId)?.let(new::setup)
                nativeSyncSubscribers.add(new)
                new
            }
        }

        internal fun onRegisterMaster(master: SyncMaster) {
            nativeSyncSubscribers.firstOrNull { it.masterId == master.masterId }?.setup(master)
        }

        internal fun onUnregisterMaster(masterId: String) {
            nativeSyncSubscribers.firstOrNull { it.masterId == masterId }?.syncMaster = null
        }

        internal fun onMasterChange(masterId: String) {
            nativeSyncSubscribers.firstOrNull { it.masterId == masterId }?.sync()
        }
    }

    fun interface ChangeListener {
        fun onSyncedBlocStateChange()
    }

    private val changeListeners: MutableList<ChangeListener> = mutableListOf()
    fun addChangeListener(listener: ChangeListener) = changeListeners.add(listener)
    fun removeChangeListener(listener: ChangeListener) = changeListeners.remove(listener)

    // Transitions from null after short init delay
    var jsonState: String? = null
        private set

    private var syncMaster: SyncMaster? = null

    private fun setup(syncMaster: SyncMaster) {
        this.syncMaster = syncMaster
        sync()
    }

    private fun sync() {
        syncMaster?.getState(
            onSuccess = { res ->
                jsonState = (res as String)
                changeListeners.forEach { it.onSyncedBlocStateChange() }
            },
            onError = { throw Error() }
        )
    }

    fun addEvent(event: String) {
        syncMaster?.addEvent(event)
    }
}