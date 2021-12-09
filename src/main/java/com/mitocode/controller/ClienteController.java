package com.mitocode.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;

import javax.validation.Valid;

import org.cloudinary.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mitocode.model.Cliente;
import com.mitocode.service.IClienteService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;
import static reactor.function.TupleUtils.function;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
	
	@Autowired
	private IClienteService service;
	
	@GetMapping
	public Mono<ResponseEntity<Flux<Cliente>>> listar() {
		Flux<Cliente> fxClientes = service.listar();
		
		return Mono.just(ResponseEntity
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(fxClientes)
				);
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Cliente>> listarPorId(@PathVariable("id") String id) {
		return service.listarPorId(id)
				.map(p -> ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(p)
						)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@PostMapping
	public Mono<ResponseEntity<Cliente>> registrar(@Valid @RequestBody Cliente c, final ServerHttpRequest req) {
		return service.registrar(c)
				.map(cl -> ResponseEntity
						.created(URI.create(req.getURI().toString().concat("/").concat(cl.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(cl)
					);
	}
	
	@PutMapping("/{id}")
	public Mono<ResponseEntity<Cliente>> modificar(@Valid @PathVariable("id") String id, @RequestBody Cliente c) {
		Mono<Cliente> monoBody = Mono.just(c);
		Mono<Cliente> monoBD = service.listarPorId(id);
		
		return monoBD
				.zipWith(monoBody, (bd, cl) -> {
					bd.setId(id);
					bd.setNombres(cl.getNombres());
					bd.setApellidos(cl.getApellidos());
					bd.setFechaNac(cl.getFechaNac());
					bd.setUrlFoto(cl.getUrlFoto());
					return bd;
				})
				.flatMap(service::modificar)
				.map(cl -> ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(cl))
				.defaultIfEmpty(new ResponseEntity<Cliente>(HttpStatus.NOT_FOUND));
	}
	
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> eliminar(@PathVariable("id") String id) {
		return service.listarPorId(id)
				.flatMap(c -> {
					return service.eliminar(c.getId())
							.then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
				})
				.defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
	}
	
	private Cliente clienteHateoas;
	
	@GetMapping("/hateoas/{id}")
	public Mono<EntityModel<Cliente>> listarHateoasPorId(@PathVariable("id") String id) {
		//localhost:8080/platos/616cb8e0ebf8266e381f42ae
				Mono<Link> link1 = linkTo(methodOn(ClienteController.class).listarPorId(id)).withSelfRel().toMono();
				Mono<Link> link2 = linkTo(methodOn(ClienteController.class).listarPorId(id)).withSelfRel().toMono();
				
				//PRACTICA NO RECOMENDADA
				/*return service.listarPorId(id)
						.flatMap(c -> {
							clienteHateoas = c;
							return link1;
						})
						.map(lk -> EntityModel.of(clienteHateoas, lk));*/
				
				//PRACTICA INTERMEDIA
				/*return service.listarPorId(id)
						.flatMap(c -> {
							return link1.map(lk -> EntityModel.of(c, lk));
						});*/
				
				/*return service.listarPorId(id)
						.zipWith(link1, (c, lk) -> EntityModel.of(c, lk));*/
				
				//Mas de 1 link
				return link1
						.zipWith(link2)
						.map(function((izq, der) -> Links.of(izq, der)))
						.zipWith(service.listarPorId(id), (lk, c) -> EntityModel.of(c, lk));
	}
	
	@PostMapping("/v1/subir/{id}")
	public Mono<ResponseEntity<Cliente>> subirV1(@PathVariable("id") String id, @RequestPart FilePart file) throws IOException {
		Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
				"cloud_name", "dtczfrs87",
				"api_key", "925323884868951",
				"api_secret", "fZvCroSFSBNpfGYzpXxEc6gEML0"));
		
		File f = Files.createTempFile("temp", file.filename()).toFile();
		
		return file.transferTo(f)
				.then(service.listarPorId(id)
						//.publishOn(Schedulers.boundedElastic())
						.flatMap(c -> {
							Map response;

							try {
								response = cloudinary.uploader().upload(f, ObjectUtils.asMap("resource_type", "auto"));
								
								JSONObject json = new JSONObject(response);
								String url = json.getString("url");
								
								c.setUrlFoto(url);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							return service.modificar(c).thenReturn(ResponseEntity.ok().body(c));
						})
						.defaultIfEmpty(ResponseEntity.notFound().build())
					);
	}
	
	@PostMapping("/v2/subir/{id}")
	public Mono<ResponseEntity<Cliente>> subirV2(@PathVariable("id") String id, @RequestPart FilePart file) throws IOException {
		Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
				"cloud_name", "dtczfrs87",
				"api_key", "925323884868951",
				"api_secret", "fZvCroSFSBNpfGYzpXxEc6gEML0"));
		
		return service.listarPorId(id)
				.flatMap(c -> {
					try {
						File f = Files.createTempFile("temp", file.filename()).toFile();
						file.transferTo(f).block();
						
						Map response = cloudinary.uploader().upload(f, ObjectUtils.asMap("resource_type", "auto"));
						
						JSONObject json = new JSONObject(response);
						String url = json.getString("url");
						
						c.setUrlFoto(url);

						return service.modificar(c).thenReturn(ResponseEntity.ok().body(c));
					} catch (Exception e) {
					}

					return Mono.just(ResponseEntity.ok().body(c));
				})
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
}
