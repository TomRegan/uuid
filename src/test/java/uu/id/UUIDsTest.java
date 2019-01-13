/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uu.id;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static uu.id.UUIDs.*;

class UUIDsTest {

    @Test
    void realTimestamp() {
        Instant expected = Instant.parse("2018-05-27T17:43:10.101Z");
        Instant actual = UUIDs.realTimestamp(137467357901010000L);
        assertEquals(expected, actual);
    }

    @Test
    void timestamp() {
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> UUIDs.realTimestamp(nilUUID())),
                () -> assertThrows(UnsupportedOperationException.class, () -> UUIDs.realTimestamp(v3UUID(NS.URL, "test"))),
                () -> assertThrows(UnsupportedOperationException.class, () -> UUIDs.realTimestamp(v4UUID())),
                () -> assertThrows(UnsupportedOperationException.class, () -> UUIDs.realTimestamp(v5UUID(NS.URL, "test"))),
                () -> assertDoesNotThrow(() -> UUIDs.realTimestamp(timeBasedUUID())));
    }

    @Test
    void versions() {
        assertAll(
                () -> assertEquals(0, nilUUID().version(), "v0"),
                () -> assertEquals(1, v1UUID().version(), "v1"),
                () -> assertEquals(3, v3UUID(NS.URL, "test").version(), "v3"),
                () -> assertEquals(4, v4UUID().version(), "v4"),
                () -> assertEquals(5, v5UUID(NS.URL, "test").version(), "v5"));
    }

    @Test
    void variants() {
        assertAll(
                () -> assertEquals(0, nilUUID().variant(), "v0"),
                () -> assertEquals(2, v1UUID().variant(), "v1"),
                () -> assertEquals(2, v3UUID(NS.URL, "test").variant(), "v3"),
                () -> assertEquals(2, v4UUID().variant(), "v4"),
                () -> assertEquals(2, v5UUID(NS.URL, "test").variant(), "v5"));
    }

    @Test
    void comparator() {
        // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7025832
        // JDK's UUID#compareTo is intentionally incorrect since inception
        // this test case demonstrates an edge case where signed comparison
        // produces the incorrect relation:
        UUID zeroth = UUIDs.nilUUID();  // sanity check
        UUID first = uuid("20000000-0000-4000-8000-000000000000");
        UUID second = uuid("e0000000-0000-4000-8000-000000000000");
        // this assertion use of a correctly implemented comparator
        List<UUID> uuids = Arrays.asList(zeroth, first, second);
        uuids.sort(UUIDs.comparator());
        assertThat(uuids, contains(zeroth, first, second));

        // sort using UUID#compareTo, nb the order is not what we would expect
        List<UUID> control = Arrays.asList(zeroth, first, second);
        Collections.sort(control);
        assertThat(control, contains(second, zeroth, first));
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    void v1ComparableSequence() {
        UUID oldest = timeBasedUUID();
        UUID same = timeBasedUUID();
        UUID latest = timeBasedUUID();
        assertAll(
                () -> assertEquals(-1, oldest.compareTo(latest)),
                () -> assertEquals(0, same.compareTo(same)),
                () -> assertEquals(1, latest.compareTo(oldest)));
        List<UUID> uuids = Arrays.asList(latest, oldest);
        uuids.sort(UUIDs.comparator());
        assertThat(uuids, contains(oldest, latest));
    }

    @Test
    void v3CanonicalValues() {
        assertAll(
                () -> assertEquals(uuid("1cf93550-8eb4-3c32-a229-826cf8c1be59"), v3UUID(NS.URL, "test")),
                () -> assertEquals(uuid("45a113ac-c7f2-30b0-90a5-a399ab912716"), v3UUID(NS.DNS, "test")),
                () -> assertEquals(uuid("61df151d-7508-321d-ada6-27936752b809"), v3UUID(NS.OID, "test")),
                () -> assertEquals(uuid("d9c53a66-fde2-3d04-b5ad-dce3848df07e"), v3UUID(NS.X500, "test")));
    }

    @Test
    void v5CanonicalValues() {
        assertAll(
                () -> assertEquals(uuid("da5b8893-d6ca-5c1c-9a9c-91f40a2a3649"), v5UUID(NS.URL, "test")),
                () -> assertEquals(uuid("4be0643f-1d98-573b-97cd-ca98a65347dd"), v5UUID(NS.DNS, "test")),
                () -> assertEquals(uuid("b428b5d9-df19-5bb9-a1dc-115e071b836c"), v5UUID(NS.OID, "test")),
                () -> assertEquals(uuid("63a3ab2b-61b8-5b04-ae2f-70d3875c6e97"), v5UUID(NS.X500, "test")));
    }
}
