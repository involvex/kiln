package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebDavPropfindParserTest {
    @Test
    fun parse_listsArchivesAndSkipsCollections() {
        val xml = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/kiln-backups/com.example/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>com.example</d:displayname>
                    <d:resourcetype><d:collection/></d:resourcetype>
                  </d:prop>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/kiln-backups/com.example/com.example_1.0_100.kilnbackup.zip</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>com.example_1.0_100.kilnbackup.zip</d:displayname>
                    <d:getlastmodified>Wed, 15 Nov 2023 12:00:00 GMT</d:getlastmodified>
                    <d:resourcetype/>
                  </d:prop>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val archives = WebDavPropfindParser.archiveEntries(xml)
        assertEquals(1, archives.size)
        assertEquals("com.example_1.0_100.kilnbackup.zip", archives[0].displayName)
        assertTrue(archives[0].lastModifiedEpochMs > 0L)
    }
}
