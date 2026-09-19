package fr.martinrocca.resto.data.backup

internal const val MIN_SUPPORTED_BACKUP_VERSION = 1
internal const val CURRENT_BACKUP_VERSION = 1

private val SAFE_BACKUP_ID = "[A-Za-z0-9_-]{1,128}".toRegex()

internal fun isSupportedBackupVersion(version: Int): Boolean =
    version in MIN_SUPPORTED_BACKUP_VERSION..CURRENT_BACKUP_VERSION

internal fun isSafeBackupId(value: String): Boolean = SAFE_BACKUP_ID.matches(value)
