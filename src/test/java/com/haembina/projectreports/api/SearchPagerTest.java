package com.haembina.projectreports.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchPagerTest {

    private static String page(int total, int from, int count) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < count; index++) {
            items.append(index == 0 ? "" : ",").append("{\"n\":").append(from + index).append('}');
        }
        return "{\"paging\":{\"total\":" + total + "},\"items\":[" + items + "]}";
    }

    @Test
    void asksForTheFirstPageAtTheSizeGiven() {
        FakeConnector connector = new FakeConnector((path, params) -> page(3, 0, 1));

        JsonObject first = new SearchPager(new LocalApi(connector)).first("api/x", Map.of("q", "a"), 1);

        assertThat(SearchPager.total(first)).isEqualTo(3);
        assertThat(connector.calls()).containsExactly(Map.of("path", "api/x", "q", "a", "p", "1", "ps", "1"));
    }

    @Test
    void readsEveryPageUntilTheTotal() {
        FakeConnector connector = new FakeConnector((path, params) ->
            "1".equals(params.get("p")) ? page(700, 0, 500) : page(700, 500, 200));

        List<JsonObject> all = new SearchPager(new LocalApi(connector)).all("api/x", Map.of(), "items");

        assertThat(all).hasSize(700);
        assertThat(all.get(699).get("n").getAsInt()).isEqualTo(699);
        assertThat(connector.calls()).extracting(call -> call.get("p")).containsExactly("1", "2");
    }

    @Test
    void stopsAtTheResultCap() {
        FakeConnector connector = new FakeConnector((path, params) ->
            page(25_000, (Integer.parseInt(params.get("p")) - 1) * 500, 500));

        List<JsonObject> all = new SearchPager(new LocalApi(connector)).all("api/x", Map.of(), "items");

        assertThat(all).hasSize(SearchPager.RESULT_CAP);
        assertThat(connector.calls()).hasSize(20);
    }

    @Test
    void stopsOnAnEmptyPage() {
        FakeConnector connector = new FakeConnector((path, params) -> page(900, 0, 0));

        assertThat(new SearchPager(new LocalApi(connector)).all("api/x", Map.of(), "items")).isEmpty();
        assertThat(connector.calls()).hasSize(1);
    }
}
