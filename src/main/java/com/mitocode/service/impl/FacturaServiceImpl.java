package com.mitocode.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mitocode.model.Factura;
import com.mitocode.repo.IFacturaRepo;
import com.mitocode.repo.IGenericRepo;
import com.mitocode.service.IFacturaService;

@Service
public class FacturaServiceImpl extends CRUDImpl<Factura, String> implements IFacturaService {
	
	@Autowired
	private IFacturaRepo repo;
	
	@Override
	protected IGenericRepo<Factura, String> getRepo() {
		return repo;
	}
	
}
