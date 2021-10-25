package com.mitocode.controller;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import com.mitocode.model.Factura;
import com.mitocode.service.IFacturaService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;
import static reactor.function.TupleUtils.function;

@RestController
@RequestMapping("/facturas")
public class FacturaController {
	
	@Autowired
	private IFacturaService service;
	
	@GetMapping
	public Mono<ResponseEntity<Flux<Factura>>> listar() {
		Flux<Factura> fxFacturas = service.listar();
		
		return Mono.just(ResponseEntity
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(fxFacturas)
				);
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Factura>> listarPorId(@PathVariable("id") String id) {
		return service.listarPorId(id)
				.map(f -> ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(f)
						)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@PostMapping
	public Mono<ResponseEntity<Factura>> registrar(@Valid @RequestBody Factura f, final ServerHttpRequest req) {
		return service.registrar(f)
				.map(fa -> ResponseEntity
						.created(URI.create(req.getURI().toString().concat("/").concat(fa.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(fa)
					);
	}
	
	@PutMapping("/{id}")
	public Mono<ResponseEntity<Factura>> modificar(@Valid @PathVariable("id") String id, @RequestBody Factura f) {
		Mono<Factura> monoBody = Mono.just(f);
		Mono<Factura> monoBD = service.listarPorId(id);
		
		return monoBD
				.zipWith(monoBody, (bd, fa) -> {
					bd.setId(id);
					bd.setCliente(fa.getCliente());
					bd.setDescripcion(fa.getDescripcion());
					bd.setObservacion(fa.getObservacion());
					bd.setItems(fa.getItems());
					return bd;
				})
				.flatMap(service::modificar)
				.map(fa -> ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(fa))
				.defaultIfEmpty(new ResponseEntity<Factura>(HttpStatus.NOT_FOUND));
	}
	
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> eliminar(@PathVariable("id") String id) {
		return service.listarPorId(id)
				.flatMap(f -> {
					return service.eliminar(f.getId())
							.then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
				})
				.defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
	}
	
	private Factura facturaHateoas;
	
	@GetMapping("/hateoas/{id}")
	public Mono<EntityModel<Factura>> listarHateoasPorId(@PathVariable("id") String id) {
		//localhost:8080/platos/616cb8e0ebf8266e381f42ae
				Mono<Link> link1 = linkTo(methodOn(FacturaController.class).listarPorId(id)).withSelfRel().toMono();
				Mono<Link> link2 = linkTo(methodOn(FacturaController.class).listarPorId(id)).withSelfRel().toMono();
				
				//PRACTICA NO RECOMENDADA
				/*return service.listarPorId(id)
						.flatMap(f -> {
							facturaHateoas = f;
							return link1;
						})
						.map(lk -> EntityModel.of(facturaHateoas, lk));*/
				
				//PRACTICA INTERMEDIA
				/*return service.listarPorId(id)
						.flatMap(f -> {
							return link1.map(lk -> EntityModel.of(f, lk));
						});*/
				
				/*return service.listarPorId(id)
						.zipWith(link1, (f, lk) -> EntityModel.of(f, lk));*/
				
				//Mas de 1 link
				return link1
						.zipWith(link2)
						.map(function((izq, der) -> Links.of(izq, der)))
						.zipWith(service.listarPorId(id), (lk, f) -> EntityModel.of(f, lk));
	}
	
}
