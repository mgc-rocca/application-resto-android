package fr.martinrocca.resto.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatRulesTest {
    @Test
    fun `only declared backup versions are accepted`() {
        assertTrue(isSupportedBackupVersion(CURRENT_BACKUP_VERSION))
        assertFalse(isSupportedBackupVersion(MIN_SUPPORTED_BACKUP_VERSION - 1))
        assertFalse(isSupportedBackupVersion(CURRENT_BACKUP_VERSION + 1))
    }

    @Test
    fun `backup identifiers cannot escape routes or paths`() {
        assertTrue(isSafeBackupId("8d42cc6f-7ed1-4c2b-8f80-88598a9716fe"))
        assertFalse(isSafeBackupId("../restaurant"))
        assertFalse(isSafeBackupId("restaurant/visit"))
        assertFalse(isSafeBackupId(""))
    }
}
