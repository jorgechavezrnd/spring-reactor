package com.mitocode;

import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.mitocode.controller.PlatoController;
import com.mitocode.model.Plato;
import com.mitocode.repo.IPlatoRepo;
import com.mitocode.service.impl.PlatoServiceImpl;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

//@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = PlatoController.class, excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class})
@Import(PlatoServiceImpl.class)
class PlatoControllerTest {

	@MockBean
	private IPlatoRepo repo;
	
	@Autowired
	private WebTestClient client;

	@Test
	void listarTest() {
		Plato plato = new Plato();
		plato.setId("1");
		plato.setNombre("Pizza");
		plato.setPrecio(25.50);
		plato.setEstado(true);
		
		List<Plato> list = new ArrayList<>();
		list.add(plato);
		
		Flux<Plato> fxPlatos = Flux.fromIterable(list);
		
		Mockito.when(repo.findAll()).thenReturn(fxPlatos);
		
		client.get()
		.uri("/platos")
		.accept(MediaType.APPLICATION_JSON)
		.exchange()
		.expectStatus().isOk()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBodyList(Plato.class)
		.hasSize(1);
	}
	
	@Test
	void registrarTest() {
		Plato plato = new Plato();
		plato.setId("1");
		plato.setNombre("pachamanca");
		plato.setPrecio(20.0);
		plato.setEstado(true);
		
		Mockito.when(repo.save(any())).thenReturn(Mono.just(plato));
		
		client.post()
		.uri("/platos")
		.body(Mono.just(plato), Plato.class)
		.accept(MediaType.APPLICATION_JSON)
		.exchange()
		.expectStatus().isCreated()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBody()
		.jsonPath("$.nombre").isNotEmpty()
		.jsonPath("$.precio").isNumber();
	}
	
	@Test
	void modificarTest() {
		Plato plato = new Plato();
		plato.setId("1");
		plato.setNombre("pachamancax");
		plato.setPrecio(30.0);
		plato.setEstado(true);
		
		Mockito.when(repo.findById("1")).thenReturn(Mono.just(plato));
		Mockito.when(repo.save(any())).thenReturn(Mono.just(plato));
		
		client.put()
		.uri("/platos/" + plato.getId())
		.body(Mono.just(plato), Plato.class)
		.accept(MediaType.APPLICATION_JSON)
		.exchange()
		.expectStatus().isOk()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBody()
		.jsonPath("$.nombre").isNotEmpty()
		.jsonPath("$.precio").isNumber();
	}
	
	@Test
	void eliminarTest() {
		Plato plato = new Plato();
		plato.setId("1");
		
		Mockito.when(repo.findById("1")).thenReturn(Mono.just(plato));
		Mockito.when(repo.deleteById("1")).thenReturn(Mono.empty());
		
		client.delete()
		.uri("/platos/" + plato.getId())
		.exchange()
		.expectStatus().isNoContent();
	}

}
