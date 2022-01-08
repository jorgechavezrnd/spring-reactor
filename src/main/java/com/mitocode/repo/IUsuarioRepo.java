package com.mitocode.repo;

import com.mitocode.model.Usuario;

import reactor.core.publisher.Mono;

public interface IUsuarioRepo extends IGenericRepo<Usuario, String> {

	// SELECT * FROM USUARIO U WHERE U.USUARIO = ?
	// {usuario : ?}
	// DerivedQueries
	Mono<Usuario> findOneByUsuario(String usuario);
	
}
