package fr.avote.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.avote.domain.Chamber;
import fr.avote.service.ParliamentarianQueryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ParliamentarianController.class)
class ParliamentarianControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    ParliamentarianQueryService service;

    @Test
    void listsWithFiltersAndDefaults() throws Exception {
        when(service.search(any(), any(), any(), eq(0), eq(24)))
                .thenReturn(new PageResponse<>(List.of(), 0, 24, 0, 0));

        mvc.perform(get("/api/v1/parliamentarians?chamber=SENAT&group=4&q=  durand "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(24));

        verify(service).search(Chamber.SENAT, 4L, "durand", 0, 24);
    }

    @Test
    void rejectsOversizedPageAndUnknownChamber() throws Exception {
        mvc.perform(get("/api/v1/parliamentarians?size=1000")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/parliamentarians?chamber=XX")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/parliamentarians?page=-1")).andExpect(status().isBadRequest());
    }

    @Test
    void returns404WhenUnknown() throws Exception {
        when(service.find(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/parliamentarians/99")).andExpect(status().isNotFound());
    }
}
