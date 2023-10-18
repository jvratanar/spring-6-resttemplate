package guru.springframework.spring6resttemplate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6resttemplate.config.RestTemplateBuilderConfig;
import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerDTOPageImpl;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest
@Import(RestTemplateBuilderConfig.class)
public class BeerClientMockTest {

    static final String URL = "http://localhost:8080";

    BeerClient beerClient;

    MockRestServiceServer server;

    @Autowired
    RestTemplateBuilder restTemplateBuilderConfigured;

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    RestTemplateBuilder mockRestTemplateBuilder = new RestTemplateBuilder(new MockServerRestTemplateCustomizer());

    BeerDTO dto;

    String dtoJson;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        RestTemplate restTemplate = this.restTemplateBuilderConfigured.build();
        this.server = MockRestServiceServer.bindTo(restTemplate).build();
        when(mockRestTemplateBuilder.build()).thenReturn(restTemplate);
        this.beerClient = new BeerClientImpl(this.mockRestTemplateBuilder);
        this.dto = this.getBeerDto();
        this.dtoJson = objectMapper.writeValueAsString(this.dto);
    }

    @Test
    void testUpdateBeer() {
        this.server.expect(method(HttpMethod.PUT))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.GET_BEER_BY_ID_PATH, dto.getId()))
                .andRespond(withNoContent());
        this.mockGetOperation();

        BeerDTO responseDto = this.beerClient.updateBeer(this.dto);
        assertThat(responseDto.getId()).isEqualTo(this.dto.getId());
    }

    @Test
    void testCreateBeer() {
        URI uri = UriComponentsBuilder.fromPath(BeerClientImpl.GET_BEER_BY_ID_PATH).build(dto.getId());

        this.server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + BeerClientImpl.GET_BEER_PATH))
                .andRespond(withAccepted().location(uri));

        this.mockGetOperation();

        BeerDTO responseDto = this.beerClient.createBeer(this.dto);
        assertThat(responseDto.getId()).isEqualTo(this.dto.getId());
    }

    @Test
    void testListBeers() throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(getPage());

        this.server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + BeerClientImpl.GET_BEER_PATH))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Page<BeerDTO> dtos = this.beerClient.listBeers();
        assertThat(dtos.getContent().size()).isGreaterThan(0);
    }

    @Test
    void testGetBeerById() {
        this.mockGetOperation();

        BeerDTO responseDto = this.beerClient.getBeerById(this.dto.getId());
        assertThat(responseDto.getId()).isEqualTo(this.dto.getId());
    }

    private void mockGetOperation() {
        this.server.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.GET_BEER_BY_ID_PATH, dto.getId()))
                .andRespond(withSuccess(this.dtoJson, MediaType.APPLICATION_JSON));
    }

    BeerDTO getBeerDto(){
        return BeerDTO.builder()
                .id(UUID.randomUUID())
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("123245")
                .build();
    }

    BeerDTOPageImpl getPage(){
        return new BeerDTOPageImpl(Arrays.asList(getBeerDto()), 1, 25, 1);
    }
}
