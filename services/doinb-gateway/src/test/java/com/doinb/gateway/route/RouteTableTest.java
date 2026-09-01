package com.doinb.gateway.route;

import com.doinb.common.config.DoinbProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RouteTableTest {

    private RouteTable routeTable;

    @BeforeEach
    void setUp() {
        DoinbProperties properties = new DoinbProperties();
        properties.getServices().setUser("http://user");
        properties.getServices().setVideo("http://video");
        properties.getServices().setLive("http://live");
        properties.getServices().setInteract("http://interact");
        properties.getServices().setMessage("http://message");
        routeTable = new RouteTable(properties);
    }

    @Test
    void reactionGoesToInteractBeforeVideo() {
        assertEquals("http://interact", routeTable.resolve("/video/reaction"));
        assertEquals("http://interact", routeTable.resolve("/video/reaction/summary"));
        assertEquals("http://video", routeTable.resolve("/video/list"));
        assertEquals("http://video", routeTable.resolve("/admin/video/pending"));
        assertEquals("http://video", routeTable.resolve("/uploads/videos/a.mp4"));
        assertEquals("http://video", routeTable.resolve("/uploads/covers/a.png"));
    }

    @Test
    void otherPrefixesMatchTaskCard() {
        assertEquals("http://user", routeTable.resolve("/user/account/login"));
        assertEquals("http://user", routeTable.resolve("/admin/account/login"));
        assertEquals("http://user", routeTable.resolve("/admin/personal/info"));
        assertEquals("http://user", routeTable.resolve("/uploads/avatars/a.png"));
        assertEquals("http://interact", routeTable.resolve("/comment/list"));
        assertEquals("http://interact", routeTable.resolve("/subscription/feed"));
        assertEquals("http://live", routeTable.resolve("/live/list"));
        assertEquals("http://message", routeTable.resolve("/notification/list"));
        assertEquals("http://message", routeTable.resolve("/message/send"));
        assertNull(routeTable.resolve("/search"));
        assertNull(routeTable.resolve("/health"));
        assertNull(routeTable.resolve("/unknown"));
    }
}
