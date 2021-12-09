package com.mitocode.controller;

import java.net.URI;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mitocode.model.Plato;
import com.mitocode.pagination.PageSupport;
import com.mitocode.service.IPlatoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;
import static reactor.function.TupleUtils.function;

@RestController
@RequestMapping("/platos")
public class PlatoController {
	
	
	private static final Logger log = LoggerFactory.getLogger(PlatoController.class);
	
	@Autowired
	private IPlatoService service;
	
	@GetMapping
	public Mono<ResponseEntity<Flux<Plato>>> listar() {
		
		//service.listar().repeat(3).publishOn(Schedulers.single()).subscribe(i -> log.info(i.toString()));
		//service.listar().repeat(3).parallel().runOn(Schedulers.parallel()).subscribe(i -> log.info(i.toString()));
		
		Flux<Plato> fxPlatos = service.listar();

		return Mono.just(ResponseEntity
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(fxPlatos)
				);
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Plato>> listarPorId(@PathVariable("id") String id) {
		return service.listarPorId(id)
				.map(p -> ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(p)
						)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@PostMapping
	public Mono<ResponseEntity<Plato>> registrar(@Valid @RequestBody Plato p, final ServerHttpRequest req) {
		return service.registrar(p)
				.map(pl -> ResponseEntity
						.created(URI.create(req.getURI().toString().concat("/").concat(pl.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(pl)
					);
	}
	
	@PutMapping("/{id}")
	public Mono<ResponseEntity<Plato>> modificar(@Valid @PathVariable("id") String id, @RequestBody Plato p) {
		Mono<Plato> monoBody = Mono.just(p);
		Mono<Plato> monoBD = service.listarPorId(id);
		
		return monoBD
				.zipWith(monoBody, (bd, pl) -> {
					bd.setId(id);
					bd.setNombre(pl.getNombre());
					bd.setPrecio(pl.getPrecio());
					bd.setEstado(pl.getEstado());
					return bd;
				})
				.flatMap(service::modificar)
				.map(pl -> ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(pl))
				.defaultIfEmpty(new ResponseEntity<Plato>(HttpStatus.NOT_FOUND));
	}
	
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> eliminar(@PathVariable("id") String id) {
		return service.listarPorId(id)
				.flatMap(p -> {
					return service.eliminar(p.getId())
							.then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
				})
				.defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
	}
	
	private Plato platoHateoas;
	
	@GetMapping("/hateoas/{id}")
	public Mono<EntityModel<Plato>> listarHateoasPorId(@PathVariable("id") String id) {
		//localhost:8080/platos/616cb8e0ebf8266e381f42ae
		Mono<Link> link1 = linkTo(methodOn(PlatoController.class).listarPorId(id)).withSelfRel().toMono();
		Mono<Link> link2 = linkTo(methodOn(PlatoController.class).listarPorId(id)).withSelfRel().toMono();
		
		//PRACTICA NO RECOMENDADA
		/*return service.listarPorId(id)
				.flatMap(p -> {
					platoHateoas = p;
					return link1;
				})
				.map(lk -> EntityModel.of(platoHateoas, lk));*/
		
		//PRACTICA INTERMEDIA
		/*return service.listarPorId(id)
				.flatMap(p -> {
					return link1.map(lk -> EntityModel.of(p, lk));
				});*/
		
		/*return service.listarPorId(id)
				.zipWith(link1, (p, lk) -> EntityModel.of(p, lk));*/
		
		//Mas de 1 link
		return link1
				.zipWith(link2)
				.map(function((izq, der) -> Links.of(izq, der)))
				.zipWith(service.listarPorId(id), (lk, p) -> EntityModel.of(p, lk));
	}
	
	@GetMapping("/pageable")
	public Mono<ResponseEntity<PageSupport<Plato>>> listarPageable(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "5") int size
			) {
		Pageable pageRequest = PageRequest.of(page, size);
		
		return service.listarPage(pageRequest)
				.map(pag -> ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(pag)
						)
				.defaultIfEmpty(ResponseEntity.noContent().build());
	}
	
}
