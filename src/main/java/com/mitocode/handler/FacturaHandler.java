package com.mitocode.handler;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.mitocode.model.Factura;
import com.mitocode.service.IFacturaService;
import com.mitocode.validators.RequestValidator;

import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
public class FacturaHandler {

	@Autowired
	private IFacturaService service;
	
	@Autowired
	private Validator validador;
	
	@Autowired
	private RequestValidator validadorGeneral;
	
	public Mono<ServerResponse> listar(ServerRequest req) {
		return ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.listar(), Factura.class);
	}
	
	public Mono<ServerResponse> listarPorId(ServerRequest req) {
		String id = req.pathVariable("id");
		
		return service.listarPorId(id)
				.flatMap(f -> ServerResponse
							.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.body(fromValue(f))
				)
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> registrar(ServerRequest req) {
		Mono<Factura> monoCliente = req.bodyToMono(Factura.class);
		
		/*return monoPlato
				.flatMap(p -> {
					Errors errores = new BeanPropertyBindingResult(p, Factura.class.getName());
					validador.validate(p, errores);
					
					if (errores.hasErrors()) {
						return Flux.fromIterable(errores.getFieldErrors())
								.map(error -> new ValidacionDTO(error.getField(), error.getDefaultMessage()))
								.collectList()
								.flatMap(listaErrores -> {
									return ServerResponse
											.badRequest()
											.contentType(MediaType.APPLICATION_JSON)
											.body(fromValue(listaErrores));
								});
					} else {
						return service.registrar(p)
								.flatMap(pdb -> ServerResponse
										.created(URI.create(req.uri().toString().concat(p.getId())))
										.contentType(MediaType.APPLICATION_JSON)
										.body(fromValue(pdb))
										);
					}
				});*/
		
		return monoCliente
				.flatMap(validadorGeneral::validate)
				.flatMap(service::registrar)
				.flatMap(f -> ServerResponse.created(URI.create(req.uri().toString().concat(f.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(fromValue(f))
				);
	}
	
	public Mono<ServerResponse> modificar(ServerRequest req) {
		Mono<Factura> monoCliente = req.bodyToMono(Factura.class);
		Mono<Factura> monoBD = service.listarPorId(req.pathVariable("id"));
		
		return monoBD
				.zipWith(monoCliente, (bd, f) -> {
					bd.setId(req.pathVariable("id"));
					bd.setCliente(f.getCliente());
					bd.setDescripcion(f.getDescripcion());
					bd.setObservacion(f.getObservacion());
					bd.setItems(f.getItems());
					return bd;
				})
				.flatMap(validadorGeneral::validate)
				.flatMap(service::modificar)
				.flatMap(f -> ServerResponse
							.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.body(fromValue(f))
				)
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> eliminar(ServerRequest req) {
		String id = req.pathVariable("id");
		
		return service.listarPorId(id)
				.flatMap(f -> service.eliminar(f.getId())
						.then(ServerResponse.noContent().build()))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
}
