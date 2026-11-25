package com.example.midtermproject.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionCodecTest {
    @Test
    public void ac7_5_keepsOrder() {
        List<String> ids = Arrays.asList("VIP-T-A05", "STD-2-D07", "STA-A-A01");
        assertEquals("VIP-T-A05,STD-2-D07,STA-A-A01", SessionCodec.encode(ids));
        assertEquals(ids, SessionCodec.decode(SessionCodec.encode(ids)));
    }

    @Test
    public void ac7_5_emptyListRoundTripsToEmptyList() {
        assertEquals("", SessionCodec.encode(new ArrayList<String>()));
        assertTrue(SessionCodec.decode(SessionCodec.encode(new ArrayList<String>())).isEmpty());
    }

    @Test
    public void ac7_6_blankAndNullGiveEmpty() {
        assertTrue(SessionCodec.decode(null).isEmpty());
        assertTrue(SessionCodec.decode("").isEmpty());
        assertTrue(SessionCodec.decode("   ").isEmpty());
        assertEquals("", SessionCodec.encode(null));
        assertEquals("", SessionCodec.encode(Collections.singletonList((String) null)));
    }

    @Test
    public void ac7_6_garbageDoesNotThrow() {
        assertEquals(Arrays.asList("A", "B"), SessionCodec.decode(",,A, ,B,,"));
        assertEquals(Collections.singletonList("not a spot @#!"), SessionCodec.decode(" not a spot @#! "));
        // Chuỗi dài và ký tự lạ vẫn chỉ cho ra danh sách, việc loại mã sai là của VenueMapState.restore.
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            many.append("\u0000￿,").append(i).append(',');
        }
        assertEquals(10000, SessionCodec.decode(many.toString()).size());
    }

    @Test
    public void ac7_6_encodeSkipsNullAndEmptyIds() {
        assertEquals("A,B", SessionCodec.encode(Arrays.asList("A", null, "", "B")));
    }
}
