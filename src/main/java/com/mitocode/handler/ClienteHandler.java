package com.mitocode.handler;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.mitocode.model.Cliente;
import com.mitocode.service.IClienteService;
import com.mitocode.validators.RequestValidator;

import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
public class ClienteHandler {

	@Autowired
	private IClienteService service;
	
	@Autowired
	private Validator validador;
	
	@Autowired
	private RequestValidator validadorGeneral;
	
	public Mono<ServerResponse> listar(ServerRequest req) {
		return ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.listar(), Cliente.class);
	}
	
	public Mono<ServerResponse> listarPorId(ServerRequest req) {
		String id = req.pathVariable("id");
		
		return service.listarPorId(id)
				.flatMap(c -> ServerResponse
							.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.body(fromValue(c))
				)
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> registrar(ServerRequest req) {
		Mono<Cliente> monoCliente = req.bodyToMono(Cliente.class);
		
		/*return monoPlato
				.flatMap(p -> {
					Errors errores = new BeanPropertyBindingResult(p, Cliente.class.getName());
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
				.flatMap(c -> ServerResponse.created(URI.create(req.uri().toString().concat(c.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(fromValue(c))
				);
	}
	
	public Mono<ServerResponse> modificar(ServerRequest req) {
		Mono<Cliente> monoCliente = req.bodyToMono(Cliente.class);
		Mono<Cliente> monoBD = service.listarPorId(req.pathVariable("id"));
		
		return monoBD
				.zipWith(monoCliente, (bd, c) -> {
					bd.setId(req.pathVariable("id"));
					bd.setNombres(c.getNombres());
					bd.setApellidos(c.getApellidos());
					bd.setFechaNac(c.getFechaNac());
					bd.setUrlFoto(c.getUrlFoto());
					return bd;
				})
				.flatMap(validadorGeneral::validate)
				.flatMap(service::modificar)
				.flatMap(c -> ServerResponse
							.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.body(fromValue(c))
				)
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> eliminar(ServerRequest req) {
		String id = req.pathVariable("id");
		
		return service.listarPorId(id)
				.flatMap(c -> service.eliminar(c.getId())
						.then(ServerResponse.noContent().build()))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
}
