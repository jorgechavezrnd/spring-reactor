package com.mitocode.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.mitocode.model.Plato;
import com.mitocode.service.IPlatoService;

import reactor.core.publisher.Mono;

@Component
public class PlatoHandler {

	@Autowired
	private IPlatoService service;
	
	public Mono<ServerResponse> listar(ServerRequest req) {
		return ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.listar(), Plato.class);
	}
	
}
